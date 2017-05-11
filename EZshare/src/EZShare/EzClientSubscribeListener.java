package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;

import org.json.simple.JSONObject;

public class EzClientSubscribeListener extends Thread {
	
	private DataOutputStream out;
	private boolean finished = false;
	private boolean debug;
	private EzClient client;
	private Scanner scn;
	
	EzClientSubscribeListener(EzClient client,DataOutputStream out,boolean debug){
		this.out= out;
		this.debug = debug;
		this.client = client;
		this.scn = new Scanner(System.in);
	}
	
	public void run(){
		while(!finished){
			System.out.println("enter next command:");
			if(scn.hasNext()){
				String line = scn.nextLine();
				String[] args = line.split(" ");
				JSONObject message = client.generate_message(args);
				try {
					out.writeUTF(message.toJSONString());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
	}
	
	public void stopThread(){
		finished = true;
	}
}
