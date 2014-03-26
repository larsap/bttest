package com.example.bttest;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;

public class MainActivity extends ActionBarActivity {

	private final int REQUEST_ENABLE_BT = 1;
	private final int MESSAGE_READ = 1337;
	BluetoothAdapter mBluetoothAdapter = null;
	public Handler mHandler = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		
		mHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case SOCKET_CONNECTED: {
					mBluetoothConnection = (ConnectionThread) msg.obj;
					if (!mServerMode)
					mBluetoothConnection.write("this is a message".getBytes());
					break;
				}
				
				case DATA_RECEIVED: {
					data = (String) msg.obj;
					tv.setText(data);
					if (mServerMode)
					mBluetoothConnection.write(data.getBytes());
				}
				
				case MESSAGE_READ:
				// your code goes here
				}
			}
		};
		
		InitializeBluetooth();
	    
	    TelephonyManager tManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
	    String uuid = tManager.getDeviceId();
	    Log.i( "UUID", uuid );
	}
	
	// Initialize all of the bluetooth stuff
	private void InitializeBluetooth() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			Log.i("MAIN", "No Bluetooth support :(");
		}
		
		// If bluetooth is not enabled, ask for permission to turn on bluetooth
		if (!mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		
		// Register the BroadcastReceiver
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
		
	    final Handler h = new Handler();
	    h.postDelayed(new Runnable()
	    {
	        //private long time = 0;

	        @Override
	        public void run()
	        {
	            // do stuff then
	            // can call h again after work!
	            //time += 1000;
	            h.postDelayed(this, 1000);
	            if ( !mBluetoothAdapter.isDiscovering() )
	            	mBluetoothAdapter.startDiscovery();
	        }
	    }, 1000);
	}
	
	// Create a BroadcastReceiver for ACTION_FOUND
	final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        // When discovery finds a device
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	            // Get the BluetoothDevice object from the Intent
	            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	            // Add the name and address to an array adapter to show in a ListView
	            Log.i( BLUETOOTH_SERVICE, device.getName() + " - " + device.getAddress() );
	            
				Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
				// If there are paired devices
				if (pairedDevices.size() > 0) {
					// Loop through paired devices
					for (BluetoothDevice dev : pairedDevices) {
						// Add the name and address to an array adapter to show in a ListView
						Log.i( BLUETOOTH_SERVICE, dev.getName() + " - " + dev.getAddress() );
					}
				}
	        }
	    }
	};
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			if ( resultCode == RESULT_OK )
				Log.i( "MAIN", "Bluetooth turned on by request" );
			else
			{
				Log.i( "MAIN", "Bluetooth not turned on!" );
				System.exit( 0 );
			}
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
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
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() {
	        byte[] buffer = new byte[1024];  // buffer store for the stream
	        int bytes; // bytes returned from read()
	 
	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);
	                // Send the obtained bytes to the UI activity
	                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write(byte[] bytes) {
	        try {
	            mmOutStream.write(bytes);
	        } catch (IOException e) { }
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
}
