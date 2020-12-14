package com.example.fancontroller;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    Button btnSubtract, btnAdd, btnOff, btnRH, btnRL;
    TextView txtCounter;
    int min_counter = 0;
    int max_counter = 16;
    int counter = 0;

    public final static String MODULE_MAC_ESP = "FC:F5:C4:0B:FA:4A";
    public final static String MODULE_MAC_VANNO = "24:FD:52:6C:F7:44";
    public final static int REQUEST_ENABLE_BT = 1;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothAdapter bta;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    ConnectedThread btt = null;
    public Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSubtract = (Button) findViewById(R.id.btn_subtract);
        btnAdd = (Button) findViewById(R.id.btn_add);
        btnOff = (Button) findViewById(R.id.btn_off);
        btnRH = (Button) findViewById(R.id.btn_rh);
        btnRL = (Button) findViewById(R.id.btn_rl);

        txtCounter = (TextView) findViewById(R.id.txt_counter);

        updateUI();

        bta = BluetoothAdapter.getDefaultAdapter();

        // if bluetooth is not enabled then ask user to enabling it
        if(!bta.isEnabled()) {
            Intent enabledBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enabledBt, REQUEST_ENABLE_BT);
        }
        else {
            initializeBluetooth();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == REQUEST_ENABLE_BT){
            initializeBluetooth();
        }
    }

    public void substractCounter(View view) {
        counter--;
        updateUI();
        sendData(counter);
    }

    public void addCounter(View view) {
        counter++;
        updateUI();
        sendData(counter);
    }

    public void RHCounter(View view) {
        counter = 17;
        updateUI();
        sendData(counter);
    }

    public void RLCounter(View view) {
        counter = 18;
        updateUI();
        sendData(counter);
    }

    public void resetCounter(View view) {
        counter = 0;
        updateUI();
        sendData(counter);
    }

    public void updateUI() {
        String counterText;
        if(counter == 0) {
            counterText = "N";
        }
        else {
            counterText = "" + counter;
        }
        txtCounter.setText(counterText);

        if(counter <= min_counter) {
            btnSubtract.setEnabled(false);
            btnAdd.setEnabled(true);
        }
        else if(counter >= max_counter) {
            btnSubtract.setEnabled(true);
            btnAdd.setEnabled(false);
        }
        else {
            btnSubtract.setEnabled(true);
            btnAdd.setEnabled(true);
        }
    }

    public void initializeBluetooth() {
        if(bta.isEnabled()) {
            BluetoothSocket tmp = null;
            mmDevice = bta.getRemoteDevice(MODULE_MAC_ESP);

            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
                mmSocket = tmp;
                mmSocket.connect();

                Toast.makeText(this, "Connected to "+mmDevice.getName(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                try {
                    mmSocket =(BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(mmDevice,1);
                    mmSocket.connect();

                    Toast.makeText(this, "Connected to "+mmDevice.getName(), Toast.LENGTH_LONG).show();
                } catch (IOException | NoSuchMethodException ie) {
                    Toast.makeText(this, ie.getMessage(), Toast.LENGTH_LONG).show();
                    try{mmSocket.close();}catch(IOException c){return;}
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                } catch (InvocationTargetException ex) {
                    ex.printStackTrace();
                }
            }

            mHandler = new Handler(Looper.getMainLooper()){
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if(msg.what == ConnectedThread.RESPONSE_MESSAGE){
                        String txt = (String)msg.obj;
                        Toast.makeText(MainActivity.this, txt, Toast.LENGTH_SHORT).show();
                    }
                }
            };

            btt = new ConnectedThread(mmSocket,mHandler);
            btt.start();
        }
    }

    public void sendData(int number) {
        String strNumber = String.valueOf(number) + "\n";

        if (mmSocket.isConnected() && btt != null) {
            btt.write(strNumber.getBytes());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        Thread.sleep(4000);
                    }catch(InterruptedException e){
                        return;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //
                        }
                    });
                }
            }).start();
        }
        else {
            Toast.makeText(this, "Data can't be sent!", Toast.LENGTH_LONG).show();
        }
    }
}
