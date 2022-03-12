package com.example.fancontroller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
    ImageButton btnRefreshConn;
    TextView txtCounter, txtInRpm1, txtOutRpm1, txtTemp, txtPres1, txtPres2, txtPres3, txtPres4;
    EditText editCustomerName, editLocation, editPONumber, editUnitNumber, editTypeTransmission;
    AlertDialog.Builder downloadBuilder;
    LayoutInflater downloadInflater;
    View downloadDialogView;
    int min_counter = 0;
    int max_counter = 16;
    int counter = 0;

    public final static String MODULE_MAC_ESP = "7C:9E:BD:F1:58:F2";
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            setPermission();
        }

        btnSubtract = (Button) findViewById(R.id.btn_subtract);
        btnAdd = (Button) findViewById(R.id.btn_add);
        btnOff = (Button) findViewById(R.id.btn_off);
        btnRH = (Button) findViewById(R.id.btn_rh);
        btnRL = (Button) findViewById(R.id.btn_rl);
        btnRefreshConn = (ImageButton) findViewById(R.id.btn_refresh_conn);

        txtCounter = (TextView) findViewById(R.id.txt_counter);
        txtInRpm1 = (TextView) findViewById(R.id.txt_in_rpm_1);
        txtOutRpm1 = (TextView) findViewById(R.id.txt_out_rpm_1);
        txtTemp = (TextView) findViewById(R.id.txt_temp);
        txtPres1 = (TextView) findViewById(R.id.txt_pressure_1);
        txtPres2 = (TextView) findViewById(R.id.txt_pressure_2);
        txtPres3 = (TextView) findViewById(R.id.txt_pressure_3);
        txtPres4 = (TextView) findViewById(R.id.txt_pressure_4);

        timer = new Timer();
        dbHandler = new DBHandler(MainActivity.this);

        dialog = new ProgressDialog(this);
        dialog.setTitle("");
        dialog.setMessage("Connecting. Please wait...");
        dialog.setIndeterminate(true);

        bta = BluetoothAdapter.getDefaultAdapter();

        try {
            // if bluetooth is not enabled then ask user to enabling it
            if(!bta.isEnabled()) {
                Intent enabledBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enabledBt, REQUEST_ENABLE_BT);
            }
            else {
//                initializeBluetooth();
                refreshConn(btnRefreshConn);
            }
        }
        catch (Exception e) {
            updateUI();
            initializeBluetooth();

            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void setPermission() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
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
        if(bta != null) {
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
                                Log.i("[RECEIVE_RPM_IN]", preprocessedText[0]);
                                Log.i("[RECEIVE_RPM_OUT]", preprocessedText[1]);
                            }
                            else if(preprocessedText.length == 7) {
                                txtInRpm1.setText(preprocessedText[0].replaceAll("[^\\d]", ""));
                                txtOutRpm1.setText(preprocessedText[1].replaceAll("[^\\d]", ""));
                                txtTemp.setText(preprocessedText[2].replaceAll("[^\\d]", ""));
                                txtPres1.setText(preprocessedText[3].replaceAll("[^\\d]", ""));
                                txtPres2.setText(preprocessedText[4].replaceAll("[^\\d]", ""));
                                txtPres3.setText(preprocessedText[5].replaceAll("[^\\d]", ""));
                                txtPres4.setText(preprocessedText[6].replaceAll("[^\\d]", ""));

                                Log.i("[RECEIVE_RPM_IN]", preprocessedText[0]);
                                Log.i("[RECEIVE_RPM_OUT]", preprocessedText[1]);
                                Log.i("[RECEIVE_TEMP]", preprocessedText[2]);
                                Log.i("[RECEIVE_PRES_1]", preprocessedText[3]);
                                Log.i("[RECEIVE_PRES_2]", preprocessedText[4]);
                                Log.i("[RECEIVE_PRES_3]", preprocessedText[5]);
                                Log.i("[RECEIVE_PRES_4]", preprocessedText[6]);
                            }
                        }
                    }
                };

                btt = new ConnectedThread(mmSocket,mHandler);
                btt.start();
            }
        }
        dialog.cancel();
    }

    public void sendData() {
        String strNumber = String.valueOf(counter) + "\n";
        Log.i("FAN-APP", "Sending "+counter);

        if(btt != null) {
            if (mmSocket.isConnected()) {
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
    }

    public void executeDownload(String customerName, String location, String PONumber,
                                String unitNumber, String typeTransmission) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            setPermission();
            return;
        }

        String curr_timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
        String filename = "iveco_data_" + curr_timeStamp + ".pdf";
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + filename;

        ArrayList<IvecoData> dbResults = new ArrayList<>();
        Cursor cursor = dbHandler.fetch();

        while(!cursor.isAfterLast()) {
            String pdfGearPos = cursor.getString(cursor.getColumnIndex(DBHandler.GEAR_POS_COL));
            String pdfInRpm = cursor.getString(cursor.getColumnIndex(DBHandler.IN_RPM_COL));
            String pdfOutRpm = cursor.getString(cursor.getColumnIndex(DBHandler.OUT_RPM_COL));
            String pdfTemp = cursor.getString(cursor.getColumnIndex(DBHandler.TEMP_COL));
            String pdfPressure1 = cursor.getString(cursor.getColumnIndex(DBHandler.PRESSURE1_COL));
            String pdfPressure2 = cursor.getString(cursor.getColumnIndex(DBHandler.PRESSURE2_COL));
            String pdfPressure3 = cursor.getString(cursor.getColumnIndex(DBHandler.PRESSURE3_COL));
            String pdfPressure4 = cursor.getString(cursor.getColumnIndex(DBHandler.PRESSURE4_COL));
            String pdfTimestamp = cursor.getString(cursor.getColumnIndex(DBHandler.TIMESTAMP_COL));

            IvecoData ivecoData = new IvecoData(pdfGearPos, pdfInRpm, pdfOutRpm,
                    pdfTemp, pdfPressure1, pdfPressure2, pdfPressure3, pdfPressure4, pdfTimestamp);
            dbResults.add(ivecoData);
            cursor.moveToNext();
        }

        if(dbResults.isEmpty()) {
            Toast.makeText(this, "Data is unavailable", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            PDFUtility.createPdf(this,MainActivity.this, dbResults, path,
                    true, customerName, location, PONumber, unitNumber, typeTransmission);
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"Error Creating Pdf");
            Toast.makeText(this, "Error Creating Pdf", Toast.LENGTH_SHORT).show();
        }
        addDownloadNotification(filename, path);
    }

    public void refreshConn(View view) {
        dialog.show();

        if(btt != null) {
            btt.cancel();
        }
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

        String dbGearPos = txtCounter.getText().toString();
        String dbInRpm = txtInRpm1.getText().toString();
        String dbOutRpm = txtOutRpm1.getText().toString();
        String dbTemp = txtTemp.getText().toString();
        String dbPres1 = txtPres1.getText().toString();
        String dbPres2 = txtPres2.getText().toString();
        String dbPres3 = txtPres3.getText().toString();
        String dbPres4 = txtPres4.getText().toString();

        dbHandler.addNewData(dbGearPos, dbInRpm, dbOutRpm, dbTemp, dbPres1, dbPres2, dbPres3, dbPres4);
    }

    public void downloadData(View view) {
        downloadBuilder = new AlertDialog.Builder(MainActivity.this);
        downloadInflater = getLayoutInflater();
        downloadDialogView = downloadInflater.inflate(R.layout.dialog_download, null);

        downloadBuilder.setView(downloadDialogView);
        downloadBuilder.setCancelable(true);
        downloadBuilder.setTitle("Additional Data");

        editCustomerName = (EditText) downloadDialogView.findViewById(R.id.edit_customer_name);
        editLocation = (EditText) downloadDialogView.findViewById(R.id.edit_location);
        editPONumber = (EditText) downloadDialogView.findViewById(R.id.edit_po_number);
        editUnitNumber = (EditText) downloadDialogView.findViewById(R.id.edit_unit_number);
        editTypeTransmission = (EditText) downloadDialogView.findViewById(R.id.edit_type_transmission);

        downloadBuilder.setPositiveButton("Download", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                Boolean isCustomerName = false;
                Boolean isLocation = false;
                Boolean isPONumber = false;
                Boolean isUnitNumber = false;
                Boolean isTransmissionType = false;

                String strCustomerName = editCustomerName.getText().toString();
                String strLocation = editLocation.getText().toString();
                String strPONumber = editPONumber.getText().toString();
                String strUnitNumber = editUnitNumber.getText().toString();
                String strTypeTransmission = editTypeTransmission.getText().toString();

                if(!strCustomerName.equalsIgnoreCase("")) {
                    isCustomerName = true;
                }
                if(!strLocation.equalsIgnoreCase("")) {
                    isLocation = true;
                }
                if(!strPONumber.equalsIgnoreCase("")) {
                    isPONumber = true;
                }
                if(!strUnitNumber.equalsIgnoreCase("")) {
                    isUnitNumber = true;
                }
                if(!strTypeTransmission.equalsIgnoreCase("")) {
                    isTransmissionType = true;
                }

                if((!isCustomerName) || (!isLocation) || (!isPONumber) || (!isUnitNumber) || (!isTransmissionType)) {
                    Toast.makeText(MainActivity.this, "Please enter all the fields", Toast.LENGTH_SHORT).show();
                }
                else {
                    executeDownload(strCustomerName, strLocation, strPONumber,
                            strUnitNumber, strTypeTransmission);
                }
                dialogInterface.dismiss();
            }
        });

        downloadBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                dialogInterface.dismiss();
            }
        });
        downloadBuilder.show();
    }

    public void addDownloadNotification(String filename, String path) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.parse(path);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, TAG)
                .setSmallIcon(R.drawable.ic_baseline_arrow_downward_24)
                .setContentTitle(filename + "  downloaded")
                .setContentText("The recorded data has been downloaded")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            String channelId = TAG;
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Downloaded Data",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(channelId);
        }

        notificationManager.notify(0, builder.build());
    }

    public void clearData(View view) {
        AlertDialog.Builder clearBuilder = new AlertDialog.Builder(this);

        clearBuilder.setTitle("Confirmation");
        clearBuilder.setMessage("Are you sure you want to clear the recorded data?");

        clearBuilder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialogInterface, int which) {
                dialogInterface.dismiss();
                dbHandler.clearData();
                Toast.makeText(MainActivity.this, "Cleared!", Toast.LENGTH_SHORT).show();
            }
        });

        clearBuilder.setNegativeButton("NO", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog alert = clearBuilder.create();
        alert.show();
    }

    @Override
    public void onPDFDocumentClose(File file) {
        Toast.makeText(this,"Pdf Created",Toast.LENGTH_SHORT).show();
    }
}
