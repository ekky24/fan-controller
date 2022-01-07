package com.example.fancontroller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements PDFUtility.OnDocumentClose {
    Button btnSubtract, btnAdd, btnOff, btnRH, btnRL;
    TextView txtCounter, txtInRpm1, txtOutRpm1;
    int min_counter = 0;
    int max_counter = 16;
    int counter = 0;

    public final static String MODULE_MAC_ESP = "AC:67:B2:38:F9:32";
    public final static String MODULE_MAC_VANNO = "24:FD:52:6C:F7:44";
    public final static int REQUEST_ENABLE_BT = 1;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final static String TAG = "IVECO";
    BluetoothAdapter bta;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    ConnectedThread btt = null;
    public Handler mHandler;
    ProgressDialog dialog;
    Timer timer;
    DBHandler dbHandler;

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
        dbHandler = new DBHandler(MainActivity.this);

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

    public void recordData(View view) {
        Toast.makeText(MainActivity.this, "Recorded!", Toast.LENGTH_SHORT).show();

        String gearPos = txtCounter.getText().toString();
        String inRpm = txtInRpm1.getText().toString();
        String outRpm = txtOutRpm1.getText().toString();

        dbHandler.addNewData(gearPos, inRpm, outRpm);
    }

    public void downloadData(View view) {
        String curr_timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/iveco_data_" + curr_timeStamp + ".pdf";

        ArrayList<IvecoData> dbResults = new ArrayList<>();
        Cursor cursor = dbHandler.fetch();
        cursor.moveToFirst();

        while(!cursor.isAfterLast()) {
            String gearPos = cursor.getString(cursor.getColumnIndex(DBHandler.GEAR_POS_COL));
            String inRpm = cursor.getString(cursor.getColumnIndex(DBHandler.IN_RPM_COL));
            String outRpm = cursor.getString(cursor.getColumnIndex(DBHandler.OUT_RPM_COL));

            IvecoData ivecoData = new IvecoData(gearPos, inRpm, outRpm);
            dbResults.add(ivecoData);
            cursor.moveToNext();
        }

        if(dbResults.isEmpty()) {
            Toast.makeText(this, "Data is unavailable", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            PDFUtility.createPdf(this,MainActivity.this, dbResults, path,true);
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"Error Creating Pdf");
            Toast.makeText(this, "Error Creating Pdf", Toast.LENGTH_SHORT).show();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        }
    }

    public void clearData(View view) {
        Toast.makeText(MainActivity.this, "Cleared!", Toast.LENGTH_SHORT).show();
        dbHandler.clearData();
    }

    @Override
    public void onPDFDocumentClose(File file) {
        Toast.makeText(this,"Pdf Created",Toast.LENGTH_SHORT).show();
    }
}
