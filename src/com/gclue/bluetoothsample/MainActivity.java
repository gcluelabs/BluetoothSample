package com.gclue.bluetoothsample;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

	/**
	 * ���O�p�^�O
	 */
	private static final String TAG = "BLUETOOTH_SAMPLE";

	/**
	 * Bluetooth�A�_�v�^
	 */
	private BluetoothAdapter mAdapter;

	/**
	 * �y�A�����O�ς�BluetoothDevice��������Array
	 */
	private ArrayList<BluetoothDevice> mDevices;

	/**
	 * Button1
	 */
	private Button mButton1;

	/**
	 * Button2
	 */
	private Button mButton2;

	/**
	 * Button3
	 */
	private Button mButton3;

	/**
	 * Button4
	 */
	private Button mButton4;

	/**
	 * SPP��UUID
	 */
	private UUID MY_UUID = UUID.fromString("1111111-0000-1000-1111-00AAEECCAAFF");

	/**
	 * ServerThread
	 */
	private ServerThread serverThread;

	/**
	 * ClientThread
	 */
	private ClientThread clientThread;

	/**
	 * ������Bluetooth�[���̖��O
	 */
	private static final String NAME = "BLUETOOTH_ANDROID";

	/**
	 * �ڑ����̃f�[�^����M�����̂��߂�Thread
	 */
	private ConnectedThread connection;

	/**
	 * EditText(Input���󂯕t����)
	 */
	private EditText editText;

	/**
	 * TextView(�������`��)
	 */
	private TextView textView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Get Device List�{�^��
		mButton1 = (Button) findViewById(R.id.button1);
		mButton1.setOnClickListener(this);

		// Satrt Server�{�^��
		mButton2 = (Button) findViewById(R.id.button2);
		mButton2.setOnClickListener(this);

		// Start Client�{�^��
		mButton3 = (Button) findViewById(R.id.button3);
		mButton3.setOnClickListener(this);

		// Message�𑗕t
		mButton4 = (Button) findViewById(R.id.button4);
		mButton4.setOnClickListener(this);

		// EditInput
		editText = (EditText) findViewById(R.id.EditText01);

		// ��M�����������`�悷��TextView
		textView = (TextView) findViewById(R.id.TextView01);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void onClick(View view) {
		// �y�A�����O�ς݃f�o�C�X���X�g���擾����
		if (view.equals(mButton1)) {
			mDevices = new ArrayList<BluetoothDevice>();
			mAdapter = BluetoothAdapter.getDefaultAdapter();
			Set<BluetoothDevice> devices = mAdapter.getBondedDevices();

			// �y�A�����O�ς݃f�o�C�X�̃��X�g
			for (BluetoothDevice device : devices) {
				mDevices.add(device);
				// Toast�ŕ\������
				Toast.makeText(this, "Name:" + device.getName(), Toast.LENGTH_LONG).show();
			}
		}
		// Server���N��
		else if (view.equals(mButton2)) {
			serverThread = new ServerThread();
			serverThread.start();
		}
		// Client���N��
		else if (view.equals(mButton3)) {
			if (mDevices != null) {
				for (int i = 0; i < mDevices.size(); i++) {
					clientThread = new ClientThread(mDevices.get(i));
					clientThread.start();
				}
			}
		}
		// ������𑗐M����
		else if (view.equals(mButton4)) {
			String message = editText.getText().toString();
			if (message != null && !message.equals("")) {
				connection.write(message.getBytes());
			}
		}
	}

	/**
	 * Server��Thread
	 */
	private class ServerThread extends Thread {
		private final BluetoothServerSocket mmServerSocket;

		public ServerThread() {
			BluetoothServerSocket tmp = null;
			try {
				// MY_UUID��SPP��UUID���w��
				tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (IOException e) {
			}
			mmServerSocket = tmp;
		}

		/**
		 * Run���\�b�h
		 */
		public void run() {
			BluetoothSocket socket = null;

			/**
			 * While���[�v�̒��ŏ펞Client����̐ڑ��ҋ@��Polling
			 */
			while (true) {
				Log.i(TAG, "Polling");
				try {
					socket = mmServerSocket.accept();

				} catch (Exception e) {
					break;
				}

				// Client���ڑ������socket��null�ł͂Ȃ��Ȃ�
				if (socket != null) {
					// �ڑ������ƌĂяo�����
					manageConnectedSocket(socket);
					try {
						mmServerSocket.close();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
			}
		}

		/**
		 * �ڑ����I�����鎞�Ă΂��
		 */
		public void cancel() {
			try {
				mmServerSocket.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Client�p��Thread
	 */
	private class ClientThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ClientThread(BluetoothDevice device) {

			BluetoothSocket tmp = null;
			mmDevice = device;

			try {
				// SPP��UUID���w��
				// ���̏����ɂ� android.permission.BLUETOOTH_ADMIN �̃p�[�~�b�V�������K�v
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (Exception e) {
				Log.i(TAG, "Error:" + e);
			}
			mmSocket = tmp;
		}

		public void run() {

			// Discovery���[�h���I������
			mAdapter.cancelDiscovery();

			try {
				// �T�[�o�ɐڑ�
				mmSocket.connect();
			} catch (IOException connectException) {

				try {
					mmSocket.close();
				} catch (IOException closeException) {
				}
				return;
			}

			// �ڑ������ƌĂяo�����
			manageConnectedSocket(mmSocket);
		}

		/**
		 * �ڑ����I������ۂɌĂ΂��
		 */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Server, Client���� �ڑ����m�������ۂɌĂяo�����
	 */
	public void manageConnectedSocket(BluetoothSocket socket) {
		Log.i(TAG, "Connection");
		connection = new ConnectedThread(socket);
		connection.start();
	}

	/**
	 * �ڑ��m�����̃f�[�^����M�p��Thread
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			Log.i(TAG, "ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
			}

			// �f�[�^��M�p
			mmInStream = tmpIn;
			// �f�[�^���M�p
			mmOutStream = tmpOut;
		}

		public void run() {
			Log.i(TAG, "ConnectionThread#run()");
			byte[] buffer = new byte[1024];
			int bytes;

			// While���[�v�œ��͂������Ă���̂��펞�ҋ@
			while (true) {
				try {
					// InputStream����l���擾
					bytes = mmInStream.read(buffer);
					// �擾�����f�[�^��String�̕ϊ�
					String readMsg = new String(buffer, 0, bytes, "UTF-8");
					// Log�ɕ\��
					Log.d(TAG, "GET: " + readMsg);

					// Hanlder�o�R�ŉ�ʂɕ`��
					Message msg = new Message();
					msg.obj = readMsg;
					mHandler.sendMessage(msg);

				} catch (IOException e) {
					break;
				}
			}
		}

		/**
		 * �������ݏ���
		 */
		public void write(byte[] bytes) {
			try {
				mmOutStream.write(bytes);
			} catch (IOException e) {
			}
		}

		/**
		 * �L�����Z�����ɌĂ΂��
		 */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
			}
		}
	}

	final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			String readMsg = (String) msg.obj;
			textView.setText(readMsg);
			textView.invalidate();
		}
	};

}