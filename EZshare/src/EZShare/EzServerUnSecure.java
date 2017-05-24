package EZShare;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

import javax.net.ServerSocketFactory;

public class EzServerUnSecure extends Thread {

	private EzServer ezServer;
	private ServerSocket listen;

	public EzServerUnSecure(EzServer ezServer) {
		// TODO Auto-generated constructor stub
		this.ezServer = ezServer;
	}

	/**
	 * The thread's main method. Continually tries to place bicycles on the belt
	 * at random intervals.
	 */
	public void run() {
		ServerSocketFactory factory = ServerSocketFactory.getDefault();
		try {
			// listen
			listen = factory.createServerSocket(this.ezServer.getPort());

			System.out.println("Server unsecure ------connected on port " + listen.getLocalPort());
			int numberOfThreads = 0;
			boolean blocked = false;
			while (true) {
				System.out.println("listening for connection");
				Socket clientSocket = listen.accept();
				blocked = false;

				// get incoming ip
				String incomingIP = clientSocket.getInetAddress().toString();
				System.out.println("connection request from " + incomingIP);

				if (this.ezServer.getBlockedIP().containsKey(incomingIP)) {
					Date now = new Date();
					long time_diff = now.getTime() - this.ezServer.getBlockedIP().get(incomingIP).getTime();
					System.out.println("time difference of " + incomingIP + " is " + time_diff);
					if (time_diff < this.ezServer.getIntervalTime()) {
						blocked = true;
					}
				}

				// reject connection if blocked
				if (blocked) {
					System.out.println("this ip is in connection interval limit");
					clientSocket.close();
				} else {
					// else do service on the connection
					synchronized (this.ezServer.getNumberOfThreadLock()) {
						this.ezServer.numberOfThread++;
						int number = this.ezServer.numberOfThread;
						System.out.println("threads " + number + " created");
					}

					this.ezServer.getBlockedIP().put(incomingIP, new Date());

					Service s = new Service(clientSocket, this.ezServer, this.ezServer.getDebug());
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				listen.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
