package fr.bytel.bluetoothsample;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.app.*;

import java.util.ArrayList;
import java.util.UUID;


/**
 * Bluetooth Custom service used to manage all bluetooth operations read/write/discover services...
 *
 * @author Bertrand Martel Bouygues Telecom on 24/02/15.
 */
public class BluetoothCustomService extends Service {

    /*AWOX : AL-Bc7*/
    /*set state ON / OFF*/
    private final static String AWOX_AL_BC7_LIGHT_SET_STATE_SERVICE="33160fb9-5b27-4e70-b0f8-ff411e3ae078";
    private final static String AWOX_AL_BC7_LIGHT_SET_STATE_CHARAC="217887f8-0af2-4002-9c05-24c9ecf71600";
    private final static String AWOX_AL_BC7_LIGHT_STATE_READ_CHARAC ="217887f8-0af2-4002-9c05-24c9ecf71600"; //0x00 or 0x01
    private final static byte[] AWOX_AL_BC7_LIGHT_ON = new byte[]{0x01};
    private final static byte[] AWOX_AL_BC7_LIGHT_OFF = new byte[]{0x00};

    private final static String TAG = BluetoothCustomService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;

    private BluetoothAdapter mBluetoothAdapter;

    private String mBluetoothDeviceAddress;

    private BluetoothGatt mBluetoothGatt;

    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {

                    String intentAction;

                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        intentAction = ActionFilterGatt.ACTION_GATT_CONNECTED;
                        mConnectionState = STATE_CONNECTED;
                        broadcastUpdate(intentAction);
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" +
                                mBluetoothGatt.discoverServices());

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        intentAction = ActionFilterGatt.ACTION_GATT_DISCONNECTED;
                        mConnectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                        broadcastUpdate(intentAction);
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        /*read on/off state on the device*/
                        broadcastUpdate(ActionFilterGatt.ACTION_GATT_SERVICES_DISCOVERED);
                    } else {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ActionFilterGatt.ACTION_DATA_AVAILABLE, characteristic);
                    }
                }
            };

    public void readOnOffState()
    {
        if (mBluetoothGatt.getService(UUID.fromString(AWOX_AL_BC7_LIGHT_SET_STATE_SERVICE)) !=null &&
            mBluetoothGatt.getService(UUID.fromString(AWOX_AL_BC7_LIGHT_SET_STATE_SERVICE))
                    .getCharacteristic(UUID.fromString(AWOX_AL_BC7_LIGHT_STATE_READ_CHARAC))!=null)
        {
            readCharacteristic(mBluetoothGatt.getService(UUID.fromString(AWOX_AL_BC7_LIGHT_SET_STATE_SERVICE))
                    .getCharacteristic(UUID.fromString(AWOX_AL_BC7_LIGHT_STATE_READ_CHARAC)));
        }
    }
    /**
     * read value for a specific characterisitic
     *
     * @param charac
     *      characteristic
     */
    private void readCharacteristic(BluetoothGattCharacteristic charac)
    {
        mBluetoothGatt.readCharacteristic(charac);
    }

    /**
     * Set on/off state
     *
     * @param state
     *      led state
     */
    public void setOnOff(boolean state) {
        if (mBluetoothGatt!=null && mBluetoothGatt.getServices()!=null) {
            for (int i = 0; i < mBluetoothGatt.getServices().size(); i++) {
                if (mBluetoothGatt.getServices().get(i).getUuid().toString().equals(AWOX_AL_BC7_LIGHT_SET_STATE_SERVICE)) {
                    for (int j = 0; j < mBluetoothGatt.getServices().get(i).getCharacteristics().size(); j++) {

                        BluetoothGattCharacteristic charac = mBluetoothGatt.getServices().get(i).getCharacteristics().get(j);

                        System.out.println("service uuid : " + mBluetoothGatt.getServices().get(i).getUuid().toString());
                        System.out.println("charac  uuid : " + charac.getUuid().toString());

                    /* set on/off*/

                        if (charac.getUuid().toString().equals(AWOX_AL_BC7_LIGHT_SET_STATE_CHARAC)) {
                            if (state) {
                                writeCharacteristic(charac, AWOX_AL_BC7_LIGHT_ON);
                            } else {
                                writeCharacteristic(charac, AWOX_AL_BC7_LIGHT_OFF);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Write values to bluetooth characteristic
     *
     * @param charac
     *      bluetooth characterisitic
     * @param value
     *      value to write
     */
    private void writeCharacteristic(BluetoothGattCharacteristic charac,byte[] value)
    {
        charac.setValue(value);
        mBluetoothGatt.writeCharacteristic(charac);
    }

    public void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * broadcast characteristic value
     *
     * @param action
     *      action to be sent (data available)
     * @param charac
     *      characteristic response to manage
     */
    public void broadcastUpdate(final String action,BluetoothGattCharacteristic charac) {

        ArrayList<String> values = new ArrayList<String>();
        String valueName="";

        if (charac.getUuid().toString().equals(AWOX_AL_BC7_LIGHT_STATE_READ_CHARAC)) {

            if (charac.getValue().length == 1) {

                if (charac.getValue()[0]==0x01)
                {
                    Log.i(TAG,"led is ON");
                    values.add("ON");
                }
                else if (charac.getValue()[0]==0x00)
                {
                    Log.i(TAG,"led is OFF");
                    values.add("OFF");
                }
            }
            valueName="STATUS";
        }
        final Intent intent = new Intent(action);
        intent.putStringArrayListExtra(valueName,values);
        sendBroadcast(intent);
    }

    /**
     * Connect device
     */
    public boolean connect(String address)
    {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        //connect to gatt server on the device
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);

        return true;
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public class LocalBinder extends Binder {
        BluetoothCustomService getService() {
            return BluetoothCustomService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();
}