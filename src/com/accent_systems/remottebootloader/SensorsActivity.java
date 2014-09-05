package com.accent_systems.remottebootloader;

import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SensorsActivity extends Activity {
	
	private ImageView back, header, enable_accel, enable_gyro, enable_alt, enable_therm;
	private TextView batt_value, key_value, accel_value, gyro_value, alt_value, therm_value;
	private BluetoothGattCharacteristic battChar = null, keyChar=null, accelChar = null, accelConfChar = null, gyroChar = null, gyroConfChar = null, altChar = null, altConfChar = null, thermChar = null, thermConfChar = null,
			motionChar = null;
	private Thread sub, accel_notif, gyro_notif, alt_notif, therm_notif;
	private Button buzzer, vibrator, buzzvib;
	
	private boolean accel = false, gyro = false, alt = false, therm = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sensors_view);
		
		back = (ImageView) findViewById(R.id.back_button);
		header = (ImageView) findViewById(R.id.remotte_text_header_sensors);
		enable_accel = (ImageView) findViewById(R.id.enable_accel);
		enable_gyro = (ImageView) findViewById(R.id.enable_gyro);
		enable_alt = (ImageView) findViewById(R.id.enable_alt);
		enable_therm = (ImageView) findViewById(R.id.enable_therm);
		
		batt_value = (TextView) findViewById(R.id.batt_value);
		batt_value.setText("BATTERY LEVEL:");
		key_value = (TextView) findViewById(R.id.key_value);
		key_value.setText("BUTTON PRESSED: --");
		accel_value = (TextView) findViewById(R.id.accel_value);
		accel_value.setText("X: --    Y: --    Z: --");
		gyro_value = (TextView) findViewById(R.id.gyro_value);
		gyro_value.setText("X: --    Y: --    Z: --");
		alt_value = (TextView) findViewById(R.id.alt_value);
		alt_value.setText("T: --    A: --");
		therm_value = (TextView) findViewById(R.id.therm_value);
		therm_value.setText("TEMPERATURE: --");
		
		buzzer = (Button) findViewById(R.id.buzzerButt);
		vibrator = (Button) findViewById(R.id.vibratorButt);
		buzzvib = (Button) findViewById(R.id.buzzvibButt);
		
		//STORE ALL THE SENSORS CHARACTERISTICS IN SEPARATED VARIABLES 
		if (MainActivity.chars!=null){
			for(int i=0; i<MainActivity.chars.size(); i++){
				if(MainActivity.chars.get(i).getUuid().toString().contains("2a19")){
					battChar = MainActivity.chars.get(i);
				}else if(MainActivity.chars.get(i).getUuid().toString().contains("ffe1")){
					keyChar = MainActivity.chars.get(i);
				}else if(MainActivity.chars.get(i).getUuid().toString().contains("aa11")){
					accelChar = MainActivity.chars.get(i);
				}else if(MainActivity.chars.get(i).getUuid().toString().contains("aa12")){
					accelConfChar = MainActivity.chars.get(i);
				}else if(MainActivity.chars.get(i).getUuid().toString().contains("aa51")){
					gyroChar = MainActivity.chars.get(i);
				}else if(MainActivity.chars.get(i).getUuid().toString().contains("aa52")){
					gyroConfChar = MainActivity.chars.get(i);
				}else if(MainActivity.chars.get(i).getUuid().toString().contains("aa41")){
					altChar = MainActivity.chars.get(i);
				}else if(MainActivity.chars.get(i).getUuid().toString().contains("aa42")){
					altConfChar = MainActivity.chars.get(i);
				}else if(MainActivity.chars.get(i).getUuid().toString().contains("aa01")){
					thermChar = MainActivity.chars.get(i);
				}else if(MainActivity.chars.get(i).getUuid().toString().contains("aa02")){
					thermConfChar = MainActivity.chars.get(i);
				}else if(MainActivity.chars.get(i).getUuid().toString().contains("aa81")){
					motionChar = MainActivity.chars.get(i);
				}
			}
			sub = new Thread(subscribeNotifications);
			sub.start();
		}
		
		//ACTION BAR BACK BUTTON - DESTROYS THE ACTIVITY
		back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		//ACTION BAR BACK BUTTON - DESTROYS THE ACTIVITY
		header.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		//ENABLE/DISABLE ACCELEROMETER NOTIFICATIONS
		enable_accel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				accel_notif = new Thread(enableAccelNotif);
				accel_notif.start();
			}
		});
		
		//ENABLE/DISABLE GYROSCOPE NOTIFICATIONS
		enable_gyro.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				gyro_notif = new Thread(enableGyroNotif);
				gyro_notif.start();
			}
		});
		
		//ENABLE/DISABLE ALTIMETER NOTIFICATIONS
		enable_alt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				alt_notif = new Thread(enableAltNotif);
				alt_notif.start();
			}
		});
		
		//ENABLE/DISABLE THERMOMETER NOTIFICATIONS
		enable_therm.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				therm_notif = new Thread(enableThermNotif);
				therm_notif.start();
			}
		});
		
		//ACTIVATE BUZZER
		buzzer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				motionChar.setValue(hexStringToByteArray("02"));
				MainActivity.writeCharacteristic(motionChar);
			}
		});
		
		//ACTIVATE VIBRATOR
		vibrator.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				motionChar.setValue(hexStringToByteArray("01"));
				MainActivity.writeCharacteristic(motionChar);
			}
		});
		
		//ACTIVATE VIBRATOR AND BUZZER	
		buzzvib.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				motionChar.setValue(hexStringToByteArray("03"));
				MainActivity.writeCharacteristic(motionChar);
			}
		});
	}
	
	//THREAD WHICH READS BATTERY, ENABLE NOTIFICATIONS FOR THE BUTTONS AND CHECKS IF ANY OF THE SENSORS ARE ALREADY ACTIVATED AND SENDIND NOTIFICATIONS
	private Thread subscribeNotifications = new Thread(new Runnable(){
		public void run(){			
			MainActivity.readCharacteristic(battChar);		//READ BATTERY CHARACTERISTIC
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				return;
			}
			runOnUiThread(new Runnable(){		//GET BATTERY VALUE
	            public void run(){	
	            	if(bytesToHex(battChar.getValue()).equalsIgnoreCase("--")){
	            		batt_value.setText("BATTERY LEVEL: error");
	            	}else{
	            		batt_value.setText("BATTERY LEVEL: "+Integer.parseInt(bytesToHex(battChar.getValue()), 16)+"%");
	            	}
	            }
	        });
			if(!accel_value.getText().toString().contains("--")){
				runOnUiThread(new Runnable(){
		            public void run(){	
		            	accel = true;
		            	enable_accel.setImageDrawable(getResources().getDrawable(R.drawable.check));
						enable_accel.setBackground(getResources().getDrawable(R.drawable.custom_btn_blue));
		            }
		        });
			}
			if(!gyro_value.getText().toString().contains("--")){
				runOnUiThread(new Runnable(){
		            public void run(){	
		            	gyro = true;
		            	enable_gyro.setImageDrawable(getResources().getDrawable(R.drawable.check));
						enable_gyro.setBackground(getResources().getDrawable(R.drawable.custom_btn_blue));
		            }
		        });
			}
			if(!alt_value.getText().toString().contains("--")){
				runOnUiThread(new Runnable(){
		            public void run(){	
		            	alt = true;
		            	enable_alt.setImageDrawable(getResources().getDrawable(R.drawable.check));
						enable_alt.setBackground(getResources().getDrawable(R.drawable.custom_btn_blue));
		            }
		        });
			}
			if(!therm_value.getText().toString().contains("--")){
				runOnUiThread(new Runnable(){
		            public void run(){	
		            	therm = true;
		            	enable_therm.setImageDrawable(getResources().getDrawable(R.drawable.check));
						enable_therm.setBackground(getResources().getDrawable(R.drawable.custom_btn_blue));
		            }
		        });
			}
			//ACTIVATE MOTION KEYS NOTIFICATION
			MainActivity.mBluetoothGatt.setCharacteristicNotification(keyChar, true);	
			BluetoothGattDescriptor descriptor = keyChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			MainActivity.mBluetoothGatt.writeDescriptor(descriptor);
			//INTERUPT THREAD AFTER FINISH ALL TASKS
			sub.interrupt();
		}
	});
	
	//THREAD WHICH ENABLES/DISABLES ACCELEROMETER NOTIFICATION
	private Thread enableAccelNotif = new Thread(new Runnable(){
		public void run(){			
			if(accel){	//IF SENSOR ENABLED AND SENDING NOTIFICATIONS
				//DISABLE NOTIFICATIONS
				MainActivity.mBluetoothGatt.setCharacteristicNotification(accelChar, true);
				BluetoothGattDescriptor descriptor2 = accelChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
				descriptor2.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
				MainActivity.mBluetoothGatt.writeDescriptor(descriptor2);
				try {
					Thread.sleep(2000);
				} catch (Exception e) {
					return;
				}
				//DISABLE SENSOR & CLEAR VALUES
				accelConfChar.setValue(hexStringToByteArray("00"));
				MainActivity.writeCharacteristic(accelConfChar);
				runOnUiThread(new Runnable(){
					public void run(){	
						enable_accel.setImageDrawable(getResources().getDrawable(R.drawable.uncheck));
						enable_accel.setBackground(getResources().getDrawable(R.drawable.custom_btn_disable));
						accel_value.setText("X: --    Y: --    Z: --");
		            }
		        });
				accel = false;
			}else{	//IF SENSOR DISABLED 
				//ENABLE SENSOR
				accelConfChar.setValue(hexStringToByteArray("01"));
				MainActivity.writeCharacteristic(accelConfChar);
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					return;
				}
				//ENABLE NOTIFICATIONS
				MainActivity.mBluetoothGatt.setCharacteristicNotification(accelChar, true);
				BluetoothGattDescriptor descriptor2 = accelChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
				descriptor2.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
				MainActivity.mBluetoothGatt.writeDescriptor(descriptor2);
				runOnUiThread(new Runnable(){
					public void run(){	
						enable_accel.setImageDrawable(getResources().getDrawable(R.drawable.check));
						enable_accel.setBackground(getResources().getDrawable(R.drawable.custom_btn_blue));
		            }
		        });
				accel = true;
			}
			//STOP THREAD AFTER FINISH ALL TASKS
			accel_notif.interrupt();	
		}
	});
	
	//THREAD WHICH ENABLES/DISABLES GYROSCOPE NOTIFICATION
	private Thread enableGyroNotif = new Thread(new Runnable(){
		public void run(){			
			if(gyro){	//IF SENSOR ENABLED AND SENDING NOTIFICATIONS
				//DISABLE NOTIFICATIONS
				MainActivity.mBluetoothGatt.setCharacteristicNotification(gyroChar, true);
				BluetoothGattDescriptor descriptor = gyroChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
				descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
				MainActivity.mBluetoothGatt.writeDescriptor(descriptor);
				try {
					Thread.sleep(2000);
				} catch (Exception e) {
					return;
				}
				//DISABLE SENSOR & CLEAR VALUES
				gyroConfChar.setValue(hexStringToByteArray("00"));
				MainActivity.writeCharacteristic(gyroConfChar);
				runOnUiThread(new Runnable(){
					public void run(){	
						enable_gyro.setImageDrawable(getResources().getDrawable(R.drawable.uncheck));
						enable_gyro.setBackground(getResources().getDrawable(R.drawable.custom_btn_disable));
						gyro_value.setText("X: --    Y: --    Z: --");
		            }
		        });
				gyro = false;
			}else{	//IF SENSOR DISABLED 
				//ENABLE SENSOR
				gyroConfChar.setValue(hexStringToByteArray("01"));
				MainActivity.writeCharacteristic(gyroConfChar);
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					return;
				}
				//ENABLE NOTIFICATIONS
				MainActivity.mBluetoothGatt.setCharacteristicNotification(gyroChar, true);
				BluetoothGattDescriptor descriptor = gyroChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
				descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
				MainActivity.mBluetoothGatt.writeDescriptor(descriptor);
				runOnUiThread(new Runnable(){
					public void run(){	
						enable_gyro.setImageDrawable(getResources().getDrawable(R.drawable.check));
						enable_gyro.setBackground(getResources().getDrawable(R.drawable.custom_btn_blue));
		            }
		        });
				gyro = true;
			}
			//STOP THREAD AFTER FINISH ALL TASKS
			gyro_notif.interrupt();	
		}
	});
	
	//THREAD WHICH ENABLES/DISABLES ALTIMETER NOTIFICATION
	private Thread enableAltNotif = new Thread(new Runnable(){
		public void run(){			
			if(alt){	//IF SENSOR ENABLED AND SENDING NOTIFICATIONS
				//DISABLE NOTIFICATIONS
				MainActivity.mBluetoothGatt.setCharacteristicNotification(altChar, true);
				BluetoothGattDescriptor descriptor = altChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
				descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
				MainActivity.mBluetoothGatt.writeDescriptor(descriptor);
				try {
					Thread.sleep(2000);
				} catch (Exception e) {
					return;
				}
				//DISABLE SENSOR & CLEAR VALUES
				altConfChar.setValue(hexStringToByteArray("00"));
				MainActivity.writeCharacteristic(altConfChar);
				runOnUiThread(new Runnable(){
					public void run(){	
						enable_alt.setImageDrawable(getResources().getDrawable(R.drawable.uncheck));
						enable_alt.setBackground(getResources().getDrawable(R.drawable.custom_btn_disable));
						alt_value.setText("T: --    A: --");
		            }
		        });
				alt = false;
			}else{	//IF SENSOR DISABLED 
				//ENABLE SENSOR
				altConfChar.setValue(hexStringToByteArray("01"));
				MainActivity.writeCharacteristic(altConfChar);
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					return;
				}
				//ENABLE NOTIFICATIONS
				MainActivity.mBluetoothGatt.setCharacteristicNotification(altChar, true);
				BluetoothGattDescriptor descriptor = altChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
				descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
				MainActivity.mBluetoothGatt.writeDescriptor(descriptor);
				runOnUiThread(new Runnable(){
					public void run(){	
						enable_alt.setImageDrawable(getResources().getDrawable(R.drawable.check));
						enable_alt.setBackground(getResources().getDrawable(R.drawable.custom_btn_blue));
		            }
		        });
				alt = true;
			}
			//STOP THREAD AFTER FINISH ALL TASKS
			alt_notif.interrupt();	
		}
	});
	
	//THREAD WHICH ENABLES/DISABLES THERMOMETER NOTIFICATION
	private Thread enableThermNotif = new Thread(new Runnable(){
		public void run(){			
			if(therm){	//IF SENSOR ENABLED AND SENDING NOTIFICATIONS
				//DISABLE NOTIFICATIONS
				MainActivity.mBluetoothGatt.setCharacteristicNotification(thermChar, true);
				BluetoothGattDescriptor descriptor = thermChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
				descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
				MainActivity.mBluetoothGatt.writeDescriptor(descriptor);
				try {
					Thread.sleep(2000);
				} catch (Exception e) {
					return;
				}
				//DISABLE SENSOR & CLEAR VALUES
				thermConfChar.setValue(hexStringToByteArray("00"));
				MainActivity.writeCharacteristic(thermConfChar);
				runOnUiThread(new Runnable(){
					public void run(){	
						enable_therm.setImageDrawable(getResources().getDrawable(R.drawable.uncheck));
						enable_therm.setBackground(getResources().getDrawable(R.drawable.custom_btn_disable));
						therm_value.setText("TEMPERATURE: --");
		            }
		        });
				therm = false;
			}else{	//IF SENSOR DISABLED 
				//ENABLE SENSOR
				thermConfChar.setValue(hexStringToByteArray("01"));
				MainActivity.writeCharacteristic(thermConfChar);
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					return;
				}
				//ENABLE NOTIFICATIONS
				MainActivity.mBluetoothGatt.setCharacteristicNotification(thermChar, true);
				BluetoothGattDescriptor descriptor = thermChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
				descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
				MainActivity.mBluetoothGatt.writeDescriptor(descriptor);
				runOnUiThread(new Runnable(){
					public void run(){	
						enable_therm.setImageDrawable(getResources().getDrawable(R.drawable.check));
						enable_therm.setBackground(getResources().getDrawable(R.drawable.custom_btn_blue));
		            }
		        });
				therm = true;
			}
			//STOP THREAD AFTER FINISH ALL TASKS
			therm_notif.interrupt();	
		}
	});
	
	
	
	
	
	
	
	
	//RECEIVER WHICH HANDLES THE INTENTS GOT FROM THE MAIN ACTIVITY 
	private BroadcastReceiver valuesChanged = new BroadcastReceiver() {
		@SuppressLint("NewApi")
		@Override
    	public void onReceive(Context context, Intent intent){
    		//KEY PRESSED NOTIFICATION
			if (intent.getAction().equals("com.remotte.KEY_PRESSED")) {								//IF INTENT FROM KEY PRESSED NOTIFICATION
    			final int value = Integer.parseInt((intent.getStringExtra("key_value")), 16); //GET VALUE FROM INTENT
    			runOnUiThread(new Runnable() {
					@Override
					public void run() {
						switch (value){ //DISTINGUIS BETWEEN PRESSED BUTTONS
		    			case 0:
		    				key_value.setText("BUTTON PRESSED: --");
		    				break;
		    			case 1:
		    				key_value.setText("BUTTON PRESSED: ENTER BUTTON");
		    				break;
		    			case 2:
		    				key_value.setText("BUTTON PRESSED: POWER BUTTON");
		    				break;
		    			}
					}
				});
			}else if(intent.getAction().equals("com.remotte.BATTERY_CHANGED")) {					//IF INTENT FROM BATTERY CHANGED NOTIFICATION
    			final int value = Integer.parseInt((intent.getStringExtra("batt_value")), 16);
    			runOnUiThread(new Runnable() {
					@Override
					public void run() {
						batt_value.setText("BATTERY LEVEL: "+value+"%");  //UPDATE BATTERY LABEL WITH THE NEW VALUE
					}
				});
    		}else if(intent.getAction().equals("com.remotte.ACCELEROMETER")) {						//IF INTENT FROM ACCELEROMETER NOTIFICATION
    			final String value = (intent.getStringExtra("accel_value"));
    			runOnUiThread(new Runnable() {
					@Override
					public void run() {		//UPDATE LABEL WITH THE NEW ACCELERATION VALUES (BY COORDINATES)
						int X = Integer.parseInt(value.substring(0,2), 16);
						int Y = Integer.parseInt(value.substring(2,4), 16);
						int Z = Integer.parseInt(value.substring(4,6), 16);
						accel_value.setText("X: "+X+"    Y: "+Y+"    Z: "+Z);
					}
				});
    		}else if(intent.getAction().equals("com.remotte.GYROSCOPE")) {							//IF INTENT FROM GYROSCOPE NOTIFICATION
    			final String value = (intent.getStringExtra("gyro_value"));
    			runOnUiThread(new Runnable() {
					@Override
					public void run() {
						int X = Integer.parseInt(value.substring(0,2), 16);
						int Y = Integer.parseInt(value.substring(2,4), 16);
						int Z = Integer.parseInt(value.substring(4,6), 16);
						gyro_value.setText("X: "+X+"    Y: "+Y+"    Z: "+Z); ////UPDATE LABEL WITH THE NEW GYROSCOPE VALUES (BY COORDINATES)
					}
				});
    		}else if(intent.getAction().equals("com.remotte.ALTIMETER")) {							//IF INTENT FROM ALTIMETER NOTIFICATION
    			final String value = (intent.getStringExtra("alt_value"));
    			runOnUiThread(new Runnable() {
					@Override
					public void run() {
						int T = Integer.parseInt(value.substring(0,2), 16);
						int A = Integer.parseInt(value.substring(2,6), 16);
						alt_value.setText("T: "+value.substring(0,2)+"(Dec: "+T+")"+"    A: "+value.substring(2,6)+"(Dec: "+A+")");  //VALUES NOT PARSED YET: HEX & DEC VALUES POSTED
					}
				});
    		}else if(intent.getAction().equals("com.remotte.THERMOMETER")) {						//IF INTENT FROM THERMOMETER NOTIFICATION
    			final String value = (intent.getStringExtra("temp_value"));
    			runOnUiThread(new Runnable() {
					@Override
					public void run() {
						int T = Integer.parseInt(value.substring(0,4), 16);
						therm_value.setText("TEMPERATURE: "+value.substring(0,4)+"(Dec: "+T+")");  //HEX & DEC VALUE: STRING VALUE NOT PARSED PROPERLY
					}
				});
    		}else if(intent.getAction().equals("com.remotte.REMOTTE_DISCONNECTED")) {	//HANDLES DESCONNECTION. IT RECEIVES WHEN REMOTTE HAS BEEN DISCONNECTED. IT FINISHES/DESTROYS THE ACTIVITY RETURNING TO THE MAIN ONE.
    			Toast.makeText(SensorsActivity.this, "REMOTTE DISCONNECTED", Toast.LENGTH_SHORT).show();
    			finish();
    		}
    	}
    };
	
	//HEX STRING TO BYTE ARRAY CONVERTER
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
	
	//BYTE ARRAY TO HEX STRING CONVERTER
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
	
	
	//REGISTER THE BROADCAST RECEIVER WITH ALL THE FILTERS (INTENT TYPES) WHEN ACTIVITY STARTS/RESUMES
	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter();
        filter.addAction("com.remotte.KEY_PRESSED");
        filter.addAction("com.remotte.BATTERY_CHANGED");
        filter.addAction("com.remotte.ACCELEROMETER");
        filter.addAction("com.remotte.GYROSCOPE");
        filter.addAction("com.remotte.ALTIMETER");
        filter.addAction("com.remotte.THERMOMETER");
        filter.addAction("com.remotte.REMOTTE_DISCONNECTED");
        registerReceiver(valuesChanged, filter);
	}
	
	
	//UNREGISTER THE BROADCAST RECEIVER WHEN ACTIVITY PAUSED/DESTROYED
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(valuesChanged);
	}
	
}
