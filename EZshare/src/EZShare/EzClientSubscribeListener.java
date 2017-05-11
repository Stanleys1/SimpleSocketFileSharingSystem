package EZShare;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;

import org.json.simple.JSONObject;

/**
 * EzClient Subscribe listenerer
 * it creates a thread that listen to futher inputs from client
 * and send it to the server
 */
public class EzClientSubscribeListener extends Thread {
	
	private DataOutputStream out;
	private boolean finished = false;
	private boolean debug;
	private EzClient client;
	private Scanner scn;
	
	/**
	 * Constructor 
	 * @param client the EzClient client side
	 * @param out the output stream connected to server
	 * @param debug debug (print outgoing messages)
	 */
	EzClientSubscribeListener(EzClient client,DataOutputStream out,boolean debug){
		this.out= out;
		this.debug = debug;
		this.client = client;
		this.scn = new Scanner(System.in);
	}
	
	public void run(){
		//while it is not finished
		while(!finished){
			//ask for more commands
			System.out.println("enter next command:");
			if(scn.hasNext()){
				//get next line
				String line = scn.nextLine();
				
				
				//TODO
				//CHECK VALIDITY OF LINE
				
				
				
				//split the line into tokens
				String[] args = line.split(" ");
				
				
				//generate message from the function in the client
				JSONObject message = client.generate_message(args);
				
				try {
					//send the message out
					out.writeUTF(message.toJSONString());
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				//if message = TERMINATE break the loop and completes the thread
				if(message.containsKey("command")){
					if(message.get("command").equals("TERMINATE")){
						break;
					}
				}
			}
			
		}
	}
}
