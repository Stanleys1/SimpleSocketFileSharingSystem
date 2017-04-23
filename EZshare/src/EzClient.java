import org.apache.commons.cli.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.rmi.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.simple.*;


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
	
	private String fileName2;
	private CommandLine cmd;
	
	public EzClient(String[] args,boolean query)throws NullPointerException, IOException{
		this.args = args; 
		this.query_relay = query;
		this.options = generateOptions();
	}
	
	public static void main (String[] args){
		//args must be present
		if(args.length==0){
			System.out.println("please enter one command");
			System.exit(0);
		}
		//commands must start with -
		if(!args[0].startsWith("-")){
			System.out.println("commands for client must start with '-'");
			System.exit(0);
		}
		
		try{
			EzClient client = new EzClient(args,true);
			String response =client.run();
			System.out.println("response from server = "+response);
		}catch(NullPointerException e){
			System.out.println("null pointer in client found");
			e.printStackTrace();
		}catch(IOException e){
			System.out.println("IO problems in client found");
			e.printStackTrace();
		}
		
		
	}
	
	
	private Options generateOptions(){
		ArrayList<Option> op= new ArrayList<Option>();
		
		op.add(new Option("publish",false,null));
		op.add(new Option("exchange", false, null));
		op.add(new Option("channel", true, "channelname"));
		op.add(new Option("debug",false,null));
		op.add(new Option("description",true,"description"));
		op.add(new Option("fetch",false,null));
		op.add(new Option("host",true,"ip"));
		op.add(new Option("name",true,"name"));
		op.add(new Option("owner",true,"name"));
		op.add(new Option("port",true,"portnumber"));
		op.add(new Option("query",false,null));
		op.add(new Option("remove",false,null));
		op.add(new Option("secret",true,"serversecret"));
		op.add(new Option("servers",true,"serverlist"));
		op.add(new Option("share",false, null));
		op.add(Option.builder("tags").hasArgs().argName("tag").build());
		op.add(new Option("uri",true,"uri"));
		
		
		Options options = new Options();
		
		
		for(int i = 0 ; i < op.size();i++){
			options.addOption(op.get(i));
		}
		return options;
		
	}
	
    private JSONObject generate_message(String[] args){
		
		boolean debug = false;
		JSONObject message = new JSONObject();
		JSONArray exchange_servers= new JSONArray();

		//CommandLine cmd;    Jason: cmd is also used in run() method
		CommandLineParser parser = new DefaultParser();
		
		

		try{
			cmd = parser.parse(options, args);
			Resource r = commands_getResource(cmd);
			if(cmd.hasOption("debug")){
				debug=true;
			}
			if(cmd.hasOption("host")){
				this.hostname = cmd.getOptionValue("host");
			}else{
				System.out.println("no host provided");
				System.exit(0);
			}
			
			if(cmd.hasOption("port")){
				String portString = cmd.getOptionValue("port");
				if(HelperFunction.IsInteger(portString)){
					this.port = Integer.parseInt(portString);
					System.out.println(port);
				}else{
					System.out.println("port given is not a number");
					System.exit(0);
				}
			}else{
				System.out.println("no port provided");
				System.exit(0);
			}
			
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
				if(debug){
					System.out.println(message.toJSONString());
				}
				return message;
			}
			
			//Resource r = Commands_GetResource(cmd);
			
			if(cmd.hasOption("publish")){
				if(r.getUri() == ""){
					System.out.println("no uri with publish");
					System.exit(0);
				}else{
					message.put("resource",r.getJSON());
					message.put("command","PUBLISH");
				}
				if(debug){
					System.out.println(message.toJSONString());
				}
				return message;
			}
			
			if(cmd.hasOption("remove")){
				if(r.getUri() == ""){
					System.out.println("no uri with remove");
					System.exit(0);
				}else{
					message.put("resource",r.getJSON());
					message.put("command","REMOVE");
					if(debug){
						System.out.println(message.toJSONString());
					}
					return message;
				}
			}
			
			if(cmd.hasOption("share")){
				if(r.getUri() == ""){
					System.out.println("no uri with share");
					System.exit(0);
				}
				String share_secret="";
				if(cmd.hasOption("secret")){
					share_secret =cmd.getOptionValue("secret");
					}
				if(share_secret==" "){
					System.out.println("secret must be present");
					System.exit(0);
					}
				message.put("resource",r.getJSON());
				message.put("command","SHARE");
				message.put("secret", share_secret);
				if(debug){
					System.out.println(message.toJSONString());
				}
				return message;
			}
			
			if(cmd.hasOption("query")){
				
				message.put("resourceTemplate",r.getJSON());
				message.put("command","QUERY");
				message.put("relay",query_relay);
				if(debug){
					System.out.println(message.toJSONString());
				}
				return message;
			}
			
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
					
					fileName2 = getFileName(r.getUri());
					
					if(debug){
						System.out.println(message.toJSONString());
					}
					return message;
				}
			}
			
		}catch(ParseException exception){
			System.out.println("Parse error:");
			exception.printStackTrace();
		}
		return message;
	}
    
    private String getFileName(String f) {
		String fileName = "";
		String fileName2;
		for(int i=(f.length()-1); i>-1; i--) {
			char c = f.charAt(i);
			
			if(c == '/') {
				break;
			}
			fileName = fileName + c;
		}
		fileName2 = new StringBuilder(fileName).reverse().toString();
		
		return fileName2;
	}
    
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
    

	public String run() throws IOException, NullPointerException {
		String message = generate_message(args).toJSONString();
		System.out.println(message.toString());
		Socket s = null;
	    String data="";

		
		try{
			//InetAddress host = InetAddress.getLocalHost(); //jason
			//String host = "sunrise.cis.unimelb.edu.au";
			
		    s = new Socket(hostname,port);  
		    System.out.println("Connection Established");
		    DataInputStream in = new DataInputStream( s.getInputStream());
		    DataOutputStream out =new DataOutputStream( s.getOutputStream());
		    System.out.println("SENT!"); 
		    out.writeUTF(message); // UTF is a string encoding see Sn. 4.4
		    
		    out.flush();
		    
		    data = in.readUTF();// read a line of data from the stream
		    
		    
		    if(cmd.hasOption("fetch")) {
		    	
		    	
				
				// Find out how much size is remaining to get from the server.
				File f = new File("server_files/"+fileName2);
				
				if(f.exists()) {
				// The file location
				String fileName = "client_files/"+fileName2;
					
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
		    	
		    } }
		    
		    System.out.println("RECEIVED!") ; 
		    s.close();
		    
    
		    
		}catch(UnknownHostException e){
			System.out.println("can't identify host name");
			e.printStackTrace();
			System.exit(0);
		}
		return data;
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
			name = cmd.getOptionValue("name");
		}
		if(cmd.hasOption("description")){
			description = cmd.getOptionValue("description");
		}
		
		if(cmd.hasOption("uri")){
			uri = cmd.getOptionValue("uri");
		}
		
		if(cmd.hasOption("owner")){
			owner = cmd.getOptionValue("owner");
		}
		
		if(cmd.hasOption("channel")){
			channel =cmd.getOptionValue("channel");
		}
		
		if(cmd.hasOption("tags")){
			tags = cmd.getOptionValues("tags");
		}
		if(cmd.hasOption("ezserver")){
			tags = cmd.getOptionValues("ezserver");
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
