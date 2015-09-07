package com.test.opencvtest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import android.util.Log;

// send command to remote server via TCP socket
public class CommandSender extends Thread {

	public static final String TAG = CommandSender.class.getSimpleName();

	private String server = "android-e0454812c73389c0";
	// private String server = "192.168.1.10";
	private int port = 8888;
	private volatile String command;

	public void run() {
		Socket socket = null;
		try {
			socket = new Socket(server, port);

			while (true) {
				while (command == null) {
					synchronized (this) {
						this.wait();
					}
				}

				OutputStream out = socket.getOutputStream();
				PrintWriter output = new PrintWriter(out, true);
				output.println(command);
				Log.i(TAG, "sent " + command);

				BufferedReader input = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
				String inputLine = input.readLine();
				Log.i(TAG, "received " + inputLine);

				command = null;
			}

		} catch (Exception e) {
			Log.i(TAG, e.getMessage());

		} finally {
			try {
				if (socket != null) {
					socket.close();
				}
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
		}
	}

	public synchronized void sendCommand(String command) {
		this.command = command;
		this.notify();
	}

}
