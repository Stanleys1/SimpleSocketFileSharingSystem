package EZShare;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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
	private Scanner scn;
	
	/**
	 * Constructor 
	 * @param client the EzClient client side
	 * @param out the output stream connected to server
	 * @param debug debug (print outgoing messages)
	 */
	EzClientSubscribeListener(DataOutputStream out,boolean debug){
		this.out= out;
		this.debug = debug;
		this.scn = new Scanner(System.in);
	}
	
	public void run(){
		
		
		//ask for more commands
		System.out.println("\n\n\nThis subscribe accept multiple subscriptions in one connection");
		System.out.println("acceptable commands are :");
		System.out.println("-subscribe -id <id> + any template variables like name, tags,etc." );
		System.out.println("all additional subscribe will not be relayed, add -relay if needed");
		System.out.println("-unsubscribe -id <id> will unsubscribe that specific id");
		System.out.println("<ENTER> or -terminate to close all subscription");
		System.out.println("enter -help if you need help\n\n");
		
		//while it is not finished
		while(!finished){
			
				//get next line
			System.out.println("next command:");
			String line = scn.nextLine();
			String args[];
				
				//TODO
				//CHECK VALIDITY OF LINE
			if (line.isEmpty()){
				String arg = "-terminate";
				args = arg.split(" ");
			}else{
				//split the line into tokens
				args = line.split(" ");
			}
				
				
				
				
				
				//generate message from the function in the client
			JSONObject message = generate_message(args);
			
			if(message == null){
				//repeat the loop
			}else{
				
				try {
					//send the message out
					if(debug){
						System.out.println(message.toJSONString());
					}
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

	private JSONObject generate_message(String[] args) {
		JSONObject message = new JSONObject();
		CommandLine cmd; 
		CommandLineParser parser = new DefaultParser();
		Options options =this.generateOptions();
		HelpFormatter formatter = new HelpFormatter();
		boolean relay;
		try{
			cmd = parser.parse(options, args);
			Resource r = EzClient.commands_getResource(cmd);
			
			if(cmd.hasOption("help")){
				formatter.printHelp("Help", options);
				return null;
			}
			
			//subscribe
			if(cmd.hasOption("subscribe")){
				if(cmd.hasOption("relay")){
					relay = true;
				}else{
					relay = false;
				}
				message.put("command", "SUBSCRIBE");
				message.put("resourceTemplate", r.getJSON());
				message.put("relay", relay);
				if(cmd.hasOption("id")){
					message.put("id", cmd.getOptionValue("id"));
				}else{
					System.out.println("please provide id with subscribe");
					return null;
				}
				return message;
			}
			
			//unsub
			if(cmd.hasOption("unsubscribe")){
				message.put("command", "UNSUBSCRIBE");
				if(cmd.hasOption("id")){
					message.put("id", cmd.getOptionValue("id"));
				}else{
					System.out.println("please provide id with unsubscribe");
					return null;
				}
				return message;
			}
			
			
			//terminate
			if(cmd.hasOption("terminate")){
				message.put("command", "TERMINATE");
				return message;
			}
			
		}catch(ParseException exception){
			System.out.println("Parse failed");
			return null;
		}
		
		
		System.out.println("no valid option obtained");
		
		
		return null;
	}
	
	
	private Options generateOptions(){
		ArrayList<Option> op= new ArrayList<Option>();
		op.add(new Option("channel", true, "channel name for the resource"));
		op.add(new Option("description",true,"description of the resource"));
		op.add(new Option("name",true,"the name of the resource"));
		op.add(new Option("owner",true,"the owner name of the resource"));
		op.add(Option.builder("tags").hasArgs().argName("multiple tags separated by space").
				desc("tags on the resources (multiple)").build());
		op.add(new Option("uri",true,"uri of the resource"));
		op.add(new Option("help",false,"get help on all options"));
		op.add(new Option("subscribe",false,"subscribe to server"));
		op.add(new Option("unsubscribe",false,"unsubscribe an id to server"));
		op.add(new Option("id", true, "id for the subscription"));
		op.add(new Option("relay",false,"will be relayed"));
		op.add(new Option("terminate",false,"terminate subs"));
		Options options = new Options();
		for(int i = 0 ; i < op.size();i++){
			options.addOption(op.get(i));
		}
		return options;
	}
}
