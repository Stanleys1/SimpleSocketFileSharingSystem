package EZShare;

import org.apache.commons.cli.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;


//ARGUMENTS:
//-port 1000 connect to server at port 1000 (NEEDED TO RUN)
//-host localhost connect to server name provided(NEEDED TO RUN)
// the 6 Commands(publish, remove, share,exchange,fetch,query)

public class EzClient {
	
	private Options options;
	private String[] args;
	private String hostname;
	private int port;
	private boolean query_relay= false;
	boolean debug;
	boolean timeout;
	
	private String uri2;
	private String fileName2;
	private String command;
	
	
	private HelpFormatter formatter;
	
	//default timeout time (5s)
	public static final int TIMEOUT = 5000;
	
	public static final String DEFAULTHOST = "localhost";
	
	
	public EzClient(String[] args,boolean query, boolean timeout)throws NullPointerException, IOException{
		this.args = args; 
		this.query_relay = query;
		this.options = generateOptions();
		this.formatter = new HelpFormatter();
		this.timeout = timeout;
	}
	
	
	/**
	 * generate options for possible json options in arguments 
	 * @param 
	 * @return options 
	 */
	private Options generateOptions(){
		ArrayList<Option> op= new ArrayList<Option>();
		
		op.add(new Option("publish",false,"publish a resource"));
		op.add(new Option("exchange", false,"exchange serverlists to servers"));
		op.add(new Option("channel", true, "channel name for the resource"));
		op.add(new Option("debug",false,"debug mode on"));
		op.add(new Option("description",true,"description of the resource"));
		op.add(new Option("fetch",false,"fetch a resource from the server"));
		op.add(new Option("host",true,"ip address of the server"));
		op.add(new Option("name",true,"the name of the resource"));
		op.add(new Option("owner",true,"the owner name of the resource"));
		op.add(new Option("port",true,"the port number of the resource"));
		op.add(new Option("query",false,"query a resource using a template"));
		op.add(new Option("remove",false,"remove a resource from the server"));
		op.add(new Option("secret",true,"secret to be able to share in server"));
		op.add(new Option("servers",true,"the serverlist of other servers"));
		op.add(new Option("share",false, "share a file into the server"));
		op.add(Option.builder("tags").hasArgs().argName("multiple tags separated by space").
				desc("tags on the resources (multiple)").build());
		op.add(new Option("uri",true,"uri of the resource"));
		op.add(new Option("help",false,"get help on all options"));
		op.add(new Option("subscribe",false,"subscribe to server"));
		op.add(new Option("id", true, "id for the subscription"));
		
		
		Options options = new Options();
		for(int i = 0 ; i < op.size();i++){
			options.addOption(op.get(i));
		}
		return options;
	}
	/**
	 * construct the message sent to the server in form of JsonObject  
	 * @param arguments
	 * @return one JSONObject - message sent to server
	 */
    private JSONObject generate_message(String[] args){
		JSONObject message = new JSONObject();
		JSONArray exchange_servers= new JSONArray();
		
		CommandLine cmd; 
		CommandLineParser parser = new DefaultParser();
		try{
			cmd = parser.parse(options, args);
			Resource r = commands_getResource(cmd);
			if(cmd.hasOption("debug")){
				debug=true;
			}
			if(cmd.hasOption("help")){
				this.formatter.printHelp("Help", options);
				System.exit(0);
			}
			//if there is not server host provided by arguments, print and then end this client
			if(cmd.hasOption("host")){
				this.hostname = cmd.getOptionValue("host");
			}else{
				this.hostname = DEFAULTHOST;
			}
			//if there is not server port provided by arguments, print and then end this client
			if(cmd.hasOption("port")){
				String portString = cmd.getOptionValue("port");
				if(HelperFunction.IsInteger(portString)){
					this.port = Integer.parseInt(portString);
				}else{
					System.out.println("port given is not a number");
					System.exit(0);
				}
			}else{
				this.port = EzServer.DEFAULTPORT;
			}
			//exchange command
			if(cmd.hasOption("exchange")){
				if(!cmd.hasOption("servers")){
					System.out.println("servers must be present for exchange");
					System.exit(0);
				}
				String servers=cmd.getOptionValue("servers");
				if(!check_servers_invalidity(servers)){
					System.out.println("servers must be present properly for exchange,"
							+ "the format should like 115.146.85.165:3780,115.146.85.24:3780 ");
					System.exit(0);
				}
				String [] serversArray=servers.split(",");
				exchange_servers=exchange_getServerList(serversArray);
				message.put("serverList", exchange_servers);
				message.put("command", "EXCHANGE");
				command = "exchange";
				return message;
			}
			// publish command
			if(cmd.hasOption("publish")){
				if(r.getUri() == ""){
					System.out.println("no uri with publish");
					System.exit(0);
				}else{
					message.put("resource",r.getJSON());
					message.put("command","PUBLISH");
				}
				command = "publish";
				return message;
			}
			//remove command
			if(cmd.hasOption("remove")){
				if(r.getUri() == ""){
					System.out.println("no uri with remove");
					System.exit(0);
				}else{
					message.put("resource",r.getJSON());
					message.put("command","REMOVE");
					command = "remove";
					return message;
				}
			}
			//share command
			if(cmd.hasOption("share")){
				if(r.getUri() == ""){
					System.out.println("no uri with share");
					System.exit(0);
				}
				String share_secret="";
				if(cmd.hasOption("secret")){
					share_secret =cmd.getOptionValue("secret");
				}else{
					System.out.println("secret must be present for share");
					System.exit(0);
				}
				message.put("resource",r.getJSON());
				message.put("command","SHARE");
				message.put("secret", share_secret);
				command ="share";
				return message;
			}
			//query command
			if(cmd.hasOption("query")){	
				message.put("resourceTemplate",r.getJSON());
				message.put("command","QUERY");
				message.put("relay",query_relay);
				command = "query";
				return message;
			}
			
			if(cmd.hasOption("subscribe")){
				message.put("command", "SUBSCRIBE");
				message.put("resourceTemplate", r.getJSON());
				message.put("relay", query_relay);
				if(cmd.hasOption("id")){
					message.put("id", cmd.getOptionObject("id"));
				}else{
					System.out.println("please provide id with subscribe");
					System.exit(0);
				}
				command = "subscribe";
				return message;
			}
			//fetch command
			if(cmd.hasOption("fetch")){
				if(r.getUri() == ""){
					System.out.println("no uri with fetch");
					System.exit(0);
				}else if(r.getChannel() ==" "){
					System.out.println("channel must be present for fetch");
					System.exit(0);
				}else{
					message.put("resourceTemplate",r.getJSON());
					message.put("command","FETCH");
					uri2 = r.getUri();
					fileName2 = HelperFunction.getFileName(r.getUri());
					command = "fetch";
					return message;
				}
			}
		}catch(ParseException exception){
			System.out.println("Parse failed");
		}
		System.out.println("no valid option found");
		System.exit(0);
		return message;
	}
    /**
   	 * set chunk size  
   	 * @param long- the size of remaining file
   	 * @return the size of chunk,it is 1024*1024 if the size of remaining file is bigger,
   	 * otherwise,it is the size of remaining file 
   	 */
    public static int setChunkSize(long fileSizeRemaining){
		// Determine the chunkSize
		int chunkSize=1024*1024;
		
		// If the file size remaining is less than the chunk size
		// then set the chunk size to be equal to the file size.
		if(fileSizeRemaining<chunkSize){
			chunkSize=(int) fileSizeRemaining;
		}
		
		return chunkSize;
	}
    
    /**
   	 * client run method  
   	 * @param 
   	 * @return the message received from the server 
     * @throws org.json.simple.parser.ParseException 
   	 */
	public ArrayList<String> run() throws IOException, NullPointerException {
		String message = generate_message(args).toJSONString();
		Socket s = null;
	    ArrayList<String> datas = new ArrayList<String>();
		
		try{
		    s = new Socket(hostname,port);  
		    if(timeout){
		    	s.setSoTimeout(TIMEOUT);
		    }
		    System.out.println("Connection Established");
		    DataInputStream in = new DataInputStream( s.getInputStream());
		    DataOutputStream out =new DataOutputStream( s.getOutputStream());
		    
		    if(debug){
		    	 System.out.println("SENT:"+message); 
		    }else{
		    	System.out.println("please use -debug in commands for more debug information");
		    }
		    
		    out.writeUTF(message); // UTF is a string encoding see Sn. 4.4
		    
		    out.flush();
		    JSONParser parser = new JSONParser();
		    boolean finished = false;
		    boolean result_0 = true;
		    
		    if(command.equals("fetch")||command.equals("query")){
		    	JSONObject response = (JSONObject) parser.parse(in.readUTF());
		    	if(response.get("response").equals("success")){
		    		datas.add(response.toJSONString());
		    		while(!finished){
		    			JSONObject next = (JSONObject) parser.parse(in.readUTF());
		    			if(next.containsKey("resultSize")){
		    				if((Long)next.get("resultSize")==1){
		    					result_0 = false;
		    				}
		    				datas.add(next.toJSONString());
		    				finished = true;
		    			}else{
		    				datas.add(next.toJSONString());
		    			}
		    		}
		    		
		    		// downloading file for fetch command
		    		if(command.equals("fetch")) { 
				    	//System.out.println("HHHHHH:" + data.substring(data.length()-2, data.length()));
				    	if (!result_0){ 
				    	
				    		URI u = new URI(uri2);	
				    		File f = new File(u);
				    		if(f.exists()) {
				    			// The file location
				    				String fileName = fileName2;
							
				    				// Create a RandomAccessFile to read and write the output file.
				    				RandomAccessFile downloadingFile = new RandomAccessFile(fileName, "rw");
						
				    				long fileSizeRemaining = (Long) f.length();
						
				    				int chunkSize = setChunkSize(fileSizeRemaining);
						
				    				// Represents the receiving buffer
				    				byte[] receiveBuffer = new byte[chunkSize];
						
				    				// Variable used to read if there are remaining size left to read.
				    				int num;
						
				    				System.out.println("Downloading "+fileName+" of size "+fileSizeRemaining);
				    				while((num=in.read(receiveBuffer))>0){
				    					// Write the received bytes into the RandomAccessFile
				    					downloadingFile.write(Arrays.copyOf(receiveBuffer, num));
							
				    					// Reduce the file size left to read..
				    					fileSizeRemaining-=num;
							
				    					// Set the chunkSize again
				    					chunkSize = setChunkSize(fileSizeRemaining);
				    					receiveBuffer = new byte[chunkSize];
							
				    					// If you're done then break
				    					if(fileSizeRemaining==0){
				    						break;
				    					}
				    				}
				    				System.out.println("File received!");
				    				downloadingFile.close();
				    		}
				    } 
				  }
		    	}else{
		    		datas.add(response.toJSONString());
		    	}
		    	
		    }else if(command.equals("subscribe")){
		    	JSONObject response = (JSONObject) parser.parse(in.readUTF());
		    	if(response.get("response").equals("success")){
		    		System.out.println(response.toJSONString());
		    		while(!finished){
		    			JSONObject next = (JSONObject) parser.parse(in.readUTF());
		    			if(next.containsKey("resultSize")){
		    				if((Long)next.get("resultSize")==1){
		    					result_0 = false;
		    				}
		    				System.out.println(next.toJSONString());
		    				finished = true;
		    			}else{
		    				System.out.println(next.toJSONString());
		    			}
		    		}
		    	}
		    	
		    }else{
		    	datas.add(in.readUTF());
		    }
		    
		    
		    s.close();
		}catch(UnknownHostException e){
			System.out.println("can't identify host name");
			e.printStackTrace();
			System.exit(0);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (org.json.simple.parser.ParseException e) {
			// TODO Auto-generated catch block
			System.out.println("message parse failed");
		}
		//print debug information
		if(debug){
			for(int i = 0 ;i <datas.size();i++){
				System.out.println(datas.get(i));
			}
		}
		return datas;
	}
	/*
	 * input:two servers including hostname and port after original server value in args split by ","
	 * output: JSONArray, each JSONObject in this array represents one server(hostname and port)
	 * description: Exchange_GetServerList split each server by ":"  and put them into one JSONObject, 
	 * these two JSONObjects consists of the output JSONArray.
	 * if there is no ":" in one server, treat as it just contains hostname but lacks of port.
	 */
	private JSONArray exchange_getServerList(String[] serversArray){
		JSONArray servers=new JSONArray();
		
		for(int i=0;i<serversArray.length;i++){
			String hostname="";
			Integer port=null;
			//String port = "";
			JSONObject oneServer=new JSONObject();
			
			String [] server=serversArray[i].split(":");
			
			if( server.length == 2 ){
				hostname=server[0];
				//port=server[1];
				port=Integer.parseInt(server[1]);
			}else{
				hostname=serversArray[i];
			}
			oneServer.put("hostname", hostname);
			oneServer.put("port", port);
			servers.add(oneServer);
		}
		return servers;
	}
	/*
	 * input: unblock the args according to options
	 * output:resource 
	 * ezserver is initialed as null rather than "" 
	 */
	private Resource commands_getResource(CommandLine cmd) {
		String name="";
		String description="";
		String uri="";
		String owner="";
		String channel="";
		String[] tags={};
		String ezserver=null;
		
		if(cmd.hasOption("name")){
			name = HelperFunction.handleString(cmd.getOptionValue("name"));
		}
		
		if(cmd.hasOption("description")){
			description =HelperFunction.handleString(cmd.getOptionValue("description"));
		}
		
		if(cmd.hasOption("uri")){
			uri = HelperFunction.handleString(cmd.getOptionValue("uri"));
		}
		
		if(cmd.hasOption("owner")){
			owner = HelperFunction.handleString(cmd.getOptionValue("owner"));
		}
		
		if(cmd.hasOption("channel")){
			channel = HelperFunction.handleString(cmd.getOptionValue("channel"));
		}
		
		if(cmd.hasOption("tags")){
			tags = cmd.getOptionValues("tags");
			for(int i = 0 ; i < tags.length; i++){
				tags[i]= HelperFunction.handleString(tags[i]);
			}
		}
		Resource r = new Resource(name,tags,description,
				uri,channel,owner,ezserver);
		return r;
	}
	/*
     * check servers arguments follow the format of "ip:port,ip:port"or not.
     * if there is only one address,ip is not an normal ip address format,port is not number, 
     * it is invalid. 
     */
	private boolean check_servers_invalidity(String servers) {
		// TODO Auto-generated method stub
		if(servers.length()==0){
			return false;
		}
		String [] serversArray=servers.split(",");
		for(int i=0;i<serversArray.length;i++){
			String [] server=serversArray[i].split(":");
			if(server.length != 2 ){
				return false;
			}
			if(!HelperFunction.IsIP(server[0])){
				return false;	
			}
			if(!HelperFunction.IsInteger(server[1])){
				return false;
			}
		}
		return true;
	}

}
