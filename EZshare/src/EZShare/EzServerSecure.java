package EZShare;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class EzServerSecure extends Thread {
	private EzServer ezServer;
	private SSLServerSocket listenSecureConnection;

	public EzServerSecure(EzServer ezServer) {
		this.ezServer = ezServer;
	}

	public void run() {
		// Specify the keystore details (this can be specified as VM arguments
		// as well)
		// the keystore file contains an application's own certificate and
		// private key
		HelperFunction.createFile(this.getClass(), "/serverKs.jks", "serverKs.jks");
		
		HelperFunction.createFile(this.getClass(), "/rootCA.jks", "rootCA.jks");
		System.setProperty("javax.net.ssl.keyStore", "serverKs.jks");
		//System.setProperty("javax.net.ssl.keyStore","serverKeystore/aGreatName");
		// Password to access the private key from the keystore file
		System.setProperty("javax.net.ssl.keyStorePassword", "comp90015");
		// Enable debugging to view the handshake and communication which
		// happens between the SSLClient and the SSLServer
		
		
		System.setProperty("javax.net.ssl.trustStore", "rootCA.jks");
		System.setProperty("javax.net.debug", "all");
		SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

		try {
			// listen
			listenSecureConnection = (SSLServerSocket) sslserversocketfactory
					.createServerSocket(this.ezServer.getSecurePort());

			System.out.println("Server connected on port " + listenSecureConnection.getLocalPort());
			boolean blocked = false;
			while (true) {
				System.out.println("listening for connection");
				SSLSocket clientSecureSocket = (SSLSocket) listenSecureConnection.accept();
				blocked = false;
				clientSecureSocket.setWantClientAuth(true);

				// get incoming ip
				String incomingIP = clientSecureSocket.getInetAddress().toString();
				System.out.println("secure connection request from " + incomingIP);

				if (this.ezServer.getBlockedSecureIP().containsKey(incomingIP)) {
					Date now = new Date();
					long time_diff = now.getTime() - this.ezServer.getBlockedSecureIP().get(incomingIP).getTime();
					System.out.println("time difference of " + incomingIP + " is " + time_diff);
					if (time_diff < this.ezServer.getIntervalTime()) {
						blocked = true;
					}
				}
				// reject connection if blocked
				if (blocked) {
					System.out.println("this ip is in secure connection interval limit");
					clientSecureSocket.close();
				} else {
					// else do service on the connection
					synchronized (this.ezServer.getNumberOfThreadLock()) {
						this.ezServer.numberOfThread++;
						int number = this.ezServer.numberOfThread;
						System.out.println("threads " + number + " created");
					}

					this.ezServer.getBlockedSecureIP().put(incomingIP, new Date());

					Service s = new Service(clientSecureSocket, this.ezServer, this.ezServer.getDebug());
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				listenSecureConnection.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
