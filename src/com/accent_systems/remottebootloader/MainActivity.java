package com.accent_systems.remottebootloader;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnMenuItemClickListener{
	
	private ProgressBar progress;
	private Button conn, reb, bootL, calib, sensors;
	private TextView connection;
	private ImageView pMenu;
	
	public static BluetoothAdapter mBluetoothAdapter;
	public static BluetoothManager mBluetoothManager;
	public static BluetoothGatt mBluetoothGatt;
	public static List<BluetoothGattCharacteristic> chars = null;
	private BluetoothGattCharacteristic mChar = null;
	
	private static final int REQUEST_ENABLE_BT = 1;
	
	private boolean scanning = false;
	private boolean connected = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_view);
		
		progress = (ProgressBar) findViewById(R.id.progress);
		progress.setVisibility(View.INVISIBLE);
		
		conn = (Button) findViewById(R.id.connectButt);
		reb = (Button) findViewById(R.id.rebootButt);
		bootL = (Button) findViewById(R.id.bootloaderButt);
		calib = (Button) findViewById(R.id.calibrateButt);
		sensors = (Button) findViewById(R.id.sensorsButt);
		
		pMenu = (ImageView) findViewById(R.id.menu_button);
		
		connection = (TextView) findViewById(R.id.connection_label);
		
		final PopupMenu popupMenu = new PopupMenu(MainActivity.this, pMenu);
		popupMenu.setOnMenuItemClickListener(MainActivity.this);
		popupMenu.inflate(R.menu.main_menu);
		
		
		pMenu.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				popupMenu.show();
			}
		});
		
		//CONNECT OR DISCONNECT FROM REMOTTE - BUTTON
		conn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(connected){
					disconnect();
					scanning = false;
				}else{
					if(scanning){
						mBluetoothAdapter.stopLeScan(mLeScanCallback);
						scanning = false;
						connection.setText("DISCONNECTED");
					    connection.setTextColor(Color.parseColor("#FF0000"));
					    conn.setText(getString(R.string.connect));
					    progress.setVisibility(View.INVISIBLE);
					    disconnect();
					}else{
						mBluetoothAdapter.startLeScan(mLeScanCallback);	
						scanning = true;
						connection.setText("SCANNING...");
					    connection.setTextColor(Color.parseColor("#FFB700"));
					    conn.setText("CANCEL CONNECTION");
					    progress.setVisibility(View.VISIBLE);
					    
					}
				}
			}
		});
		
	}
	
	//DEFINES ALL BUTTONS ONCE CONNECTED TO THE REMOTTE DEVICE
	private void setClickListeners(){
		
		reb.setBackground(getResources().getDrawable(R.drawable.custom_btn_blue));
		bootL.setBackground(getResources().getDrawable(R.drawable.custom_btn_blue));
		calib.setBackground(getResources().getDrawable(R.drawable.custom_btn_blue));
		sensors.setBackground(getResources().getDrawable(R.drawable.custom_btn_blue));
		
		//REBOOT DEVICE BUTTON AND DIALOG CALL
		reb.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mChar.setValue(hexStringToByteArray("01"));
				writeCharacteristic(mChar);
				throwDialog("reboot it!");
			}
		});
		
		//BOOTLOADER MODE BUTTON AND DIALOG CALL
		bootL.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mChar.setValue(hexStringToByteArray("02"));
				writeCharacteristic(mChar);
				throwDialog("launch it in BootLoader mode!");
			}
		});
		
		//CALIBRATE DEVICE BUTTON AND DIALOG CALL
		calib.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mChar.setValue(hexStringToByteArray("03"));
				writeCharacteristic(mChar);
				throwDialog("calibrate it!");
			}
		});
		
		//SENSORS ACTIVITY BUTTON
		sensors.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent sensorsIntent = new Intent(MainActivity.this, SensorsActivity.class);
				startActivity(sensorsIntent);
			}
		});
	}
	
	//INDICATIONS DYNAMIC DIALOG
	private void throwDialog(String s){
		final Dialog myDialog = new Dialog(this);
		myDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        myDialog.setContentView(R.layout.customdialog);
        TextView txtv=(TextView)myDialog.findViewById(R.id.dialog_body);
		txtv.setText("Please press the ENTER button on the remotte to "+s);
        Button finishbtn=(Button)myDialog.findViewById(R.id.acceptButt);
        finishbtn.setOnClickListener(new OnClickListener() {
             @Override
              public void onClick(View v) {
                    myDialog.cancel();
              }
        });
        myDialog.show();
	}
	
	//DISABLE BUTTONS FUNCTION ON DISCONNECTION FROM REMOTTE
	private void setClickListenersNull(){
		reb.setOnClickListener(null);
		bootL.setOnClickListener(null);
		calib.setOnClickListener(null);
		sensors.setOnClickListener(null);
		reb.setBackground(getResources().getDrawable(R.drawable.custom_btn_disable));
		bootL.setBackground(getResources().getDrawable(R.drawable.custom_btn_disable));
		calib.setBackground(getResources().getDrawable(R.drawable.custom_btn_disable));
		sensors.setBackground(getResources().getDrawable(R.drawable.custom_btn_disable));
	}
	
	//BLUETOOTH CHECKER FUNCTION. DETECTS WHETER THE DEVICE IS BLE READY AND IF TRUE, IT ASKS USER TO ENABLE IT
	private void checkForBluetooth(){
		if (!getBaseContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getBaseContext(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager =(BluetoothManager) getBaseContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(getBaseContext(), R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
	
	//BLUETOOTH SCANNING ADAPTER. CALLED WHEN SCANNING HAS STARTED. IT SCAN FOR ALL BLE DEVICES AND WHEN REMOTTE DETECTED
	//IT STOPS THE SCAN AND PROCEEDS TO CONNECTION WITH THE DEVICE
	private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	String name = device.getName();
                	if(name != null){
                		if(name.equalsIgnoreCase("HID Remotte")){ 												//IF REMOTTE FOUND...
                        	Toast.makeText(getBaseContext(), "Remotte found!", Toast.LENGTH_SHORT).show();
                        	mBluetoothAdapter.stopLeScan(mLeScanCallback); 										//STOP SCAN
                        	runOnUiThread(new Runnable() {
								@Override
								public void run() {
									connection.setText("CONNECTING...");
								    connection.setTextColor(Color.parseColor("#FFFF00"));
								}
							});
                        	scanning = false;
                        	initialize();																		//INICIALIZE BLUETOOTH ADAPTER
                        	connect(device.getAddress());														//START CONNECTION WITH REMOTTE DEVICE
                        }
                	}
                }
            });
        }
    };
    
    //BLUETOOTH INICIALIZATION FUNCTION
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getBaseContext().getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }

        return true;
    }
    
    //CONNECTION MANAGER WITH THE DETECTED REMOTTE DEVICE
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        runOnUiThread(new Runnable(){
            public void run(){
            	mBluetoothGatt = device.connectGatt(getBaseContext(), false, mGattCallback);  		//CONNECTING TO THE DEVICE GATT
            }
        });
        
        return true;
    }
    
    
    //DISCONNECT FUNCTION CALLED WHEN APP IS DESTROYED
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        runOnUiThread(new Runnable(){
            public void run(){
            	mBluetoothGatt.disconnect();  	//DISCONNECTS FROM GATT
            }
        });
        
    }
    
    //AFTER CONNECTED TO GATT, THIS FUNCTION IS CALLED TO SCAN FOR SERVICES AND CHARACTERISTICS
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        chars = new ArrayList<BluetoothGattCharacteristic>();
        for (BluetoothGattService gattService : gattServices) { 
        	List<BluetoothGattCharacteristic> gattCharacteristics =
        			gattService.getCharacteristics();
        	for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
        		chars.add(gattCharacteristic);
        		if(gattCharacteristic.getUuid().toString().contains("aa62")){	//SEARCH AND STORE FOR THE CONFIGURATION CHARACTERISTIC
        			mChar = gattCharacteristic;
        		}
        	}
        }
        
    }
    
    //BLUETOOTH GATT CALLBACK - MANAGES CONNECTION, DISCONNECTION AND STATE CHANGES WITH THE DEVICE.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        	if (newState == BluetoothProfile.STATE_CONNECTED) { 	//WHEN STATE CONNECTED IT CALLS THE FUNCTION TO DISCOVER SERVICES AND CHARACTERISTICS
        		mBluetoothGatt.discoverServices();
        	} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {  	//DISCONNECTED STATE
        		connected = false;
        		runOnUiThread(new Runnable() {
    				@Override
    				public void run() {
    					connection.setText("DISCONNECTED");
    		        	connection.setTextColor(Color.parseColor("#ff0000"));
    		        	connected = false;
    		        	conn.setText(getString(R.string.connect));
    		        	setClickListenersNull();
    				}
    			});
        		Intent intent = new Intent();
        		intent.setAction("com.remotte.REMOTTE_DISCONNECTED");	//ON DISCONNECTION STATE ALSO THROWS BROADCAST INTENT TO SENSORS ACTIVITY TO CLOSE IT
        		sendBroadcast(intent);
        	}
            
        }

        //FUNCTION CALLED WHEN SERVICES FOUND
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        	displayGattServices(getSupportedGattServices());
        	runOnUiThread(new Runnable() {
				@Override
				public void run() {
					connection.setText("CONNECTED");
		        	connection.setTextColor(Color.parseColor("#00ff00"));
		        	connected = true;
		        	conn.setText(getString(R.string.disconnect));
		        	progress.setVisibility(View.INVISIBLE);
		        	setClickListeners();
				}
			});
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
        }

        //FUNCTION CALLED WHEN THE APP RECEIVES A NOTIFICATION FROM THE REMOTTE. IT SENDS BROADCAST TO SENSORS ACTIVITY TO INFORM ABOUT THE CHANGE AND BRING THE NEW VALUE.
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        	if(characteristic.getUuid().toString().contains("2a19")){						//ON BATTERY CHANGE NOTIFICATION
        		Intent intent = new Intent();
        		intent.setAction("com.remotte.BATTERY_CHANGED");
        		intent.putExtra("batt_value", bytesToHex(characteristic.getValue()));
        		sendBroadcast(intent);
        	}else if(characteristic.getUuid().toString().contains("ffe1")){					//ON REMOTTE BUTTON PRESSED NOTIFICATION
        		Intent intent = new Intent();
        		intent.setAction("com.remotte.KEY_PRESSED");
        		intent.putExtra("key_value", bytesToHex(characteristic.getValue()));
        		sendBroadcast(intent);
        	}else if(characteristic.getUuid().toString().contains("aa11")){					//ON ACCELEROMETER VALUE CHANGED NOTIFICATION
        		Intent intent = new Intent();
        		intent.setAction("com.remotte.ACCELEROMETER");
        		intent.putExtra("accel_value", bytesToHex(characteristic.getValue()));
        		sendBroadcast(intent);
        	}else if(characteristic.getUuid().toString().contains("aa51")){					//ON GYROSCOPE VALUE CHANGED NOTIFICATION
        		Intent intent = new Intent();
        		intent.setAction("com.remotte.GYROSCOPE");
        		intent.putExtra("gyro_value", bytesToHex(characteristic.getValue()));
        		sendBroadcast(intent);
        	}else if(characteristic.getUuid().toString().contains("aa41")){					//ON ALTIMETER VALUE CHANGED NOTIFICATION
        		Intent intent = new Intent();
        		intent.setAction("com.remotte.ALTIMETER");
        		intent.putExtra("alt_value", bytesToHex(characteristic.getValue()));
        		sendBroadcast(intent);
        	}else if(characteristic.getUuid().toString().contains("aa01")){					//ON THERMOMETER VALUE CHANGED NOTIFICATION
        		Intent intent = new Intent();
        		intent.setAction("com.remotte.THERMOMETER");
        		intent.putExtra("temp_value", bytesToHex(characteristic.getValue()));
        		sendBroadcast(intent);
        	}
        }
    };
    
    //FUNCTION AUTOCALLED WHICH GETS THE DETECTED SERVICES
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
    
    //FUNCTION TO MODIFY/WRITE NEW VALUES IN A CERTAIN CHARACTERISTIC
    public static void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        try {
        	mBluetoothGatt.writeCharacteristic(characteristic);
		} catch (Exception e) {
		}
    	
    }
    
    //FUNCTION TO READ THE VALUE OF A CERTAIN CHARACTERISTIC
    public static void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        try {
        	mBluetoothGatt.readCharacteristic(characteristic);
		} catch (Exception e) {
		}
    	
    }
    
    //HEXADECIMA STRING TO BYTE ARRAY CONVERTER
    public static byte[] hexStringToByteArray(String s) {
    	try {
        	int len = s.length();
            byte[] data = new byte[len/2];
            for(int i = 0; i < len; i+=2){
                data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
            }
            return data;
		} catch (Exception e) {
		}   	
        return new byte[]{00};
    }
    
    //BYTE ARRAY TO HEXADECIMAL STRING CONVERTER
    public static String bytesToHex(byte[] bytes) {
    	char[] hexArray = "0123456789ABCDEF".toCharArray();
    	char[] hexChars = null;
        try{
        	hexChars = new char[bytes.length * 2];
        }catch(Exception e){}
    	if(hexChars!=null){
    		for ( int j = 0; j < bytes.length; j++ ) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }
    		return new String(hexChars);
    	}else{
    		return "--";
    	}
    }

    
    //POP-UP MENU 
	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
		 
		case R.id.item_website:
			Intent web_intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://remotte.com/remotte-glass/"));
            startActivity(web_intent);
			return true;
		case R.id.item_exit:
			finish();
			return true;
		default:
			return false;
		}
	}
	
	//CALLED ON ACTIVITY RESUME
	@Override
    protected void onResume() {
        super.onResume();
        checkForBluetooth();		//CHECK BLUETOOTH STATE CHANGED
        if(connected){				//ENABLE BUTTONS IF CONNECTED TO REMOTTE DEVICE
        	setClickListeners(); 
        }
    }
	
	//CALLED WHEN APP/ACTIVITY DESTROYED
	@Override
	protected void onDestroy() {
		disconnect();		//DISCONNECT FROM ANY CONNECTED DEVICE
		super.onDestroy();
	}

}
