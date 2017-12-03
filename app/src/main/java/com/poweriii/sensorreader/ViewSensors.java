package com.poweriii.sensorreader;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class ViewSensors extends Activity {

    Button btn_start, btn_stop;
    EditText text_angle_value;
    String address;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth;
    BluetoothSocket btSocket;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    ConnectedThread mConnectedThread;
    ArrayList<Float> values;
    boolean mReading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_sensors);

        // The 'calling activity' will give us the device address we need.
        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device
        new ConnectBT().execute(); //Call the class to connect to BT device

        btn_start = findViewById(R.id.btn_start);
        btn_stop  = findViewById(R.id.btn_stop);
        text_angle_value = findViewById(R.id.editAngleValueText);

        // Holds the values that were gonna get from the sensors.
        values = new ArrayList<>();

        // NOTE - both of these need to ensure that the buttons haven't been pressed

        // start button will empty the value array, create a thread, then start it.
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mReading){
                    values = new ArrayList<>(); // Empty the values array
                    mConnectedThread = new ConnectedThread(btSocket);
                    mConnectedThread.start();
                    mReading = true;
                } else {
                    msg("Already Reading.");
                }
            }
        });

        // stop button will end the thread. call a calculate_angle(values_array) method
        // then update the text view containing the angle.
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mReading){
                    mConnectedThread.cancel();
                    // the values array should hold all the numbers we need now.
                    // Note: eventually were gonna need to change this to whatever
                    // kinda of storage we need to interface with the math libraries.
                    // Will probably be several arrays, one for each of the parameters

                    Float angle = calculateAngle(values);
                    text_angle_value.setText(String.format("%f", angle));
                    mReading = false;
                }
            }
        });


    }

    //    placeholder for now. Will eventually do all our math.
    private Float calculateAngle(ArrayList<Float> vals){
        return 180.0f;
    };

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(ViewSensors.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        // Establishes a the connection, stored in the instances btSocket field.
        @Override
        protected Void doInBackground(Void... devices) {
            try {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            } catch (IOException e) {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (!ConnectSuccess) {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            } else {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {

            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            while(true) {
                try {
                    mmInStream.read(buffer);
                    // TODO - Add a value to the data list/array from the buffer
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
}

