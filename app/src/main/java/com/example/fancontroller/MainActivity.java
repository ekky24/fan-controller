package com.example.fancontroller;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    Button btnSubtract, btnAdd, btnOff, btnRH, btnRL;
    TextView txtCounter, txtInRpm1, txtOutRpm1;
    int min_counter = 0;
    int max_counter = 16;
    int counter = 0;

    public final static String MODULE_MAC_ESP = "AC:67:B2:38:F9:32";
    public final static String MODULE_MAC_VANNO = "24:FD:52:6C:F7:44";
    public final static int REQUEST_ENABLE_BT = 1;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothAdapter bta;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    ConnectedThread btt = null;
    public Handler mHandler;
    ProgressDialog dialog;
    Timer timer;

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
        txtInRpm1 = (TextView) findViewById(R.id.txt_in_rpm_1);
        txtOutRpm1 = (TextView) findViewById(R.id.txt_out_rpm_1);

        timer = new Timer();

        dialog = ProgressDialog.show(this, "",
                "Connecting. Please wait...", true);
        bta = BluetoothAdapter.getDefaultAdapter();

        updateUI();

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
    }

    public void addCounter(View view) {
        counter++;
        updateUI();
    }

    public void RHCounter(View view) {
        counter = 17;
        updateUI();
    }

    public void RLCounter(View view) {
        counter = 18;
        updateUI();
    }

    public void resetCounter(View view) {
        counter = 0;
        updateUI();
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

        timer.cancel();

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendData();
            }
        },  1000);
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
                        Log.i("[TXT]", txt);

                        String[] receivedText = txt.split("\\n");
                        String latestText = receivedText[receivedText.length-1];
                        String[] preprocessedText = latestText.split(",");

                        if(preprocessedText.length == 2) {
                            txtInRpm1.setText(preprocessedText[0].replaceAll("[^\\d]", ""));
                            txtOutRpm1.setText(preprocessedText[1].replaceAll("[^\\d]", ""));
                            Log.i("[RECEIVEIN]", preprocessedText[0]);
                            Log.i("[RECEIVEOUT]", preprocessedText[1]);
                        }
                    }
                }
            };

            btt = new ConnectedThread(mmSocket,mHandler);
            btt.start();
            dialog.cancel();
        }
    }

    public void sendData() {
        String strNumber = String.valueOf(counter) + "\n";
        Log.i("FAN-APP", "Sending "+counter);

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
            Log.i("FAN-APP", "Data cannot be sent!");
        }
    }

    public void refreshConn(View view) {
        dialog.show();
        btt.cancel();

        new CountDownTimer(1000, 1000) {

            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                updateUI();
                initializeBluetooth();
            }

        }.start();
    }
}
