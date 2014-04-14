package com.example.bttest;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class MainActivity extends ActionBarActivity {

	private final int REQUEST_ENABLE_BT = 1;
	private final int MESSAGE_READ = 1337;
	BluetoothAdapter mBluetoothAdapter = null;
	public Handler mHandler = null;
	public UUID MY_UUID = null;
	ConnectThread btt = null;
	ConnectedThread btt_connected = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add( R.id.container, new PlaceholderFragment() ).commit();
		}
		
		InitializeBluetooth();
	    
	    //TelephonyManager tManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
	    MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//UUID.fromString( tManager.getDeviceId() );
	    Log.i( BLUETOOTH_SERVICE, "UUID: " + MY_UUID.toString() );
	    
	    /*
	    try {
		    Process process = new ProcessBuilder()
		       .command( "/system/bin/ping", "-c 1", "-w 10", "-n", "oslo.dignio.com" )
		       .redirectErrorStream( true )
		       .start();
		    try {
		    	InputStream in = process.getInputStream();
		    	StringBuilder s = new StringBuilder();
		    	int i;
		    	
		    	while ( ( i = in.read() ) != -1 ) {
		    		s.append( ( char ) i );
		    		
		    		if ( ( char ) i == '\n' ) {
		    			Log.i( "PING", s.toString() );
		    			if ( s.toString().contains( "64 bytes from" ) ) {
		    				
		    			}
		    			s.delete( 0, s.length() );
		    		}
		    	}
		    }
		    catch ( IOException e ) {
		    	
		    }
		    finally {
		    	process.destroy();
		    }
	    }
	    catch ( Exception e ) {
	    	
	    }
	    */
	}
	
	// Initialize all of the bluetooth stuff
	private void InitializeBluetooth() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			Log.i("MAIN", "No Bluetooth support :(");
		}
		
		// If bluetooth is not enabled, ask for permission to turn on bluetooth
		if ( !mBluetoothAdapter.isEnabled() ) {
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
	            if ( btt == null && !mBluetoothAdapter.isDiscovering() )
	            	mBluetoothAdapter.startDiscovery();
	            
	            try {
	            	//btt_connected.write( "Hello world!\n".getBytes() );
	            }
	            catch ( Exception e ) { }
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
						if ( dev.getAddress().equals( device.getAddress() ) ) {
							btt = new ConnectThread( dev );
							btt.start();
						}
						//Log.i( BLUETOOTH_SERVICE, "Paired: " + dev.getName() + " - " + dev.getAddress() );
					}
				}
	        }
	    }
	};
	
	private class ConnectThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final BluetoothDevice mmDevice;
	 
	    public ConnectThread(BluetoothDevice device) {
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
	        mmDevice = device;
	 
	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	            // MY_UUID is the app's UUID string, also used by the server code
	            tmp = device.createRfcommSocketToServiceRecord( MY_UUID );
	        } catch (IOException e) { }
	        mmSocket = tmp;
	    }
	 
	    public void run() {
	    	Log.i( BLUETOOTH_SERVICE, "Running ConnectThread" );
	    	
	        // Cancel discovery because it will slow down the connection
	        mBluetoothAdapter.cancelDiscovery();

	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            mmSocket.connect();
	        } catch (IOException connectException) {
	            // Unable to connect; close the socket and get out
	            try {
	                mmSocket.close();
	            } catch (IOException closeException) { }
	            return;
	        }
	        
	        Log.i( BLUETOOTH_SERVICE, "Connected!" );
	 
	        // Do work to manage the connection (in a separate thread)
	       // manageConnectedSocket(mmSocket);
	        btt_connected = new ConnectedThread( mmSocket );
	        btt_connected.start();
	    }
	 
	    /** Will cancel an in-progress connection, and close the socket */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
	
	private class ConnectedThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	    private BufferedOutputStream mBos;
	 
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
	        
	        String dir = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DOWNLOADS ).getAbsolutePath();
	        Log.i( BLUETOOTH_SERVICE, "Downloads dir: " + dir );
	        
	        try {
				mBos = new BufferedOutputStream( new FileOutputStream( dir + "/test.wav" ) );
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	 
	    public void run() {
	    	Log.i( BLUETOOTH_SERVICE, "Running ConnectedThread" );
	    	
	        byte[] buffer = new byte[1024];  // buffer store for the stream
	        int bytes; // bytes returned from read()
	 
	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);
	                // Send the obtained bytes to the UI activity
	                //mHandler.obtainMessage( MESSAGE_READ, bytes, -1, buffer ).sendToTarget();
	                //Log.i( "RX", new String( buffer ).substring(0,bytes) );
	                
	                mBos.write( buffer, 0, bytes );
	                mBos.flush();
	                
	                //write( buffer, bytes );
	            } catch (IOException e) {
	                break;
	            }
	        }
	        
	        // Set the pointer to null so the discovery process can start over again
	        btt = null;
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write( byte[] buffer, int bytes ) {
	        try {
	            mmOutStream.write( buffer, 0, bytes );
	        } catch (IOException e) { }
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
	
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
}
