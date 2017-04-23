
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javax.net.ServerSocketFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;




//ARGUMENTS:
//-port 8000 : listen at port 8000 (NEEDED FOR IT TO RUN)
//-exchangeinterval 100: change the exchange timer to 100s. Change to sth small if u want to test
//-secret : set ur own secret, if not provided will generate 25-35 long random word
//-debug: service will print all message sent
//-connectionintervallimit: change the connection interval for each ips
//-advertisedname: NOT DONE


public class EzServer implements Runnable {
	
	//records of all server records to be exchanged
	private ArrayList<String> serverRecords;
	
	//default exchange time
	public static final int DEFAULTTIME = 600000;
	
	//default connection interval time
	public static final int DEFAULTINTERVAL = 1;
	private int exchangetime;
	private int intervaltime;
	private ArrayList<Resource> resources;
	private Timer timer;
	private int port;
	private String secret;
	private ServerSocket listen;
	private Options options;
	private boolean debug = false;
	private String[] args;
	private ArrayList<String> blockedIP;
	private HelpFormatter formatter;
	
	public static void main (String[] args){
		//args must be present
		if(args.length==0){
			System.out.println("please enter at least one command (port)");
			System.exit(0);
		}
		//commands must start with -
		if(!args[0].startsWith("-")){
			System.out.println("commands for server must start with '-'");
			System.exit(0);
		}
		//create server
		EzServer server = new EzServer(args);
		//run server
		server.run();
	}
	
	/**
	 * create new server
	 * @param args the arguments given to the server
	 */
	public EzServer(String[] args){
		//create options
		this.options= generateOptions();
		resources = new ArrayList<Resource>();
		this.args = args;
		serverRecords = new ArrayList<String> ();
		blockedIP = new ArrayList<String>();
		
		this.formatter = new HelpFormatter();
		this.formatter.printHelp("help", options);
		
		//For testing purposes
		//serverRecords.add("localhost:8000");
		//serverRecords.add("localhost:10000");
		
		timer = new Timer();
		
	}
	
	//create all the options for argument handling
	private Options generateOptions(){
		ArrayList<Option> op= new ArrayList<Option>();
		
		op.add(new Option("advertisedhostname",true,"change the advertised hostname"));
		op.add(new Option("connectionintervallimit", true, "change the connection interval for each IP"));
		op.add(new Option("exchangeinterval", true, "change the interval for syncing between servers"));
		op.add(new Option("port",true,"portnumber for the server to listen in"));
		op.add(new Option("secret",true,"set the secret for server. Default will be randomised 25-35 long random string"));
		op.add(new Option("debug",false,"debug mode"));
		op.add(new Option("help",false,"get help on all arguments"));
		
		Options options = new Options();
		
		
		for(int i = 0 ; i < op.size();i++){
			options.addOption(op.get(i));
		}
		return options;
		
	}
	
	/**
	 * get the hostname of the server
	 * @return hostname
	 */
	public String getHostName(){
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	/**
	 * get the host ip of the server
	 * @return host ip
	 */
	public String getHostIP(){
		try{
			return InetAddress.getLocalHost().getHostAddress();
		}catch(UnknownHostException e){
			e.printStackTrace();
		}
		return "";
	}
	
	/**
	 * get the port number of the server
	 * @return port
	 */
	public int getPort(){
		return this.port;
	}
	
	/**
	 * return the secret of the server
	 * @return secret
	 */
	public String getSecret() {
		return this.secret;
	}
	
	
	/**
	 * generate random secret
	 * @return secret random secret between 25-35 letters long
	 */
	private String generate_random_secret(){
		Random random = new Random();
		int stringLength = 25 + (int)(Math.random()*10);
		System.out.println("secret length = "+ stringLength);
		//possible chars in the secret
		char[] possibleChar ="abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
		StringBuilder secret = new StringBuilder();
		for(int i =0; i<stringLength;i++){
			char c = possibleChar[random.nextInt(possibleChar.length)];
			secret.append(c);
		}
		return secret.toString();
		
	}
	
	
	
	@Override
	public void run() {
		CommandLine cmd;
		CommandLineParser parser = new DefaultParser();
		try{
			//parse args
			cmd = parser.parse(options, args);
			
			if(cmd.hasOption("help")){
				System.exit(0);
			}
			//Setting exchange interval
			if(cmd.hasOption("exchangeinterval")){
				String timeString = cmd.getOptionValue("exchangeinterval");
				if(HelperFunction.IsInteger(timeString)){
					this.exchangetime = Integer.parseInt(timeString)*1000;
				}else{
					System.out.println("exchange interval is not integer,using default timer");
					this.exchangetime = DEFAULTTIME;
				}
			}else{
				this.exchangetime = DEFAULTTIME;
			}
			
			System.out.println("exchange interval time = " +exchangetime/1000+ "s");
			
			//check if debug is on
			if(cmd.hasOption("debug")){
				debug = true;
			}
			System.out.println("debug = "+ debug);
			
			//check the port number for server to listen in
			if(cmd.hasOption("port")){
				String portString = cmd.getOptionValue("port");
				if(HelperFunction.IsInteger(portString)){
					this.port = Integer.parseInt(portString);
					System.out.println("port = " + port);
				}else{
					System.out.println("port number is not a number, initialization failed");
					System.exit(0);
				}
			}else{
				System.out.println("no port value is given");
				System.exit(0);
			}
			
			//get the secret
			if(cmd.hasOption("secret")){
				this.secret = cmd.getOptionValue("secret");
			}else{
				this.secret = generate_random_secret();
			}
			System.out.println("this server secret = " + this.secret);
			
			
			//get the connectioninterval limit
			if(cmd.hasOption("connectionintervallimit")){
				String buffer =  cmd.getOptionValue("connectionintervallimit");
				if(HelperFunction.IsInteger(buffer)){
					this.intervaltime = Integer.parseInt(buffer);
				}
				else{
					System.out.println("interval time not an integer, using default time");
					this.intervaltime = DEFAULTINTERVAL;
				}
			}else{
				this.intervaltime = DEFAULTINTERVAL;
			}
			
			System.out.println("connectionintervallimit = "+ this.intervaltime);
			
			//get advertised host name
			if(cmd.hasOption("advertisedhostname")){
				//TODO
			}
			
			
			
		}catch(ParseException exception){
			System.out.println("Parse error:");
			exception.printStackTrace();
		}
		
		//schedule sync server according to the timer
		timer.scheduleAtFixedRate(new SyncServer(this),exchangetime, exchangetime);
		
		ServerSocketFactory factory = ServerSocketFactory.getDefault();
		
		
		try {
			//listen
			listen = factory.createServerSocket(port);
			
			System.out.println("Server connected on port " + listen.getLocalPort());
			int numberOfThreads = 0;
			boolean blocked = false;
			while(true){
				System.out.println("listening for connection");
				Socket clientSocket = listen.accept();
				blocked = false;
				
				
				//get incoming ip
				String incomingIP = clientSocket.getInetAddress().toString();
				System.out.println(incomingIP);
				
				//if incoming ip inside blocked ip list, reject connection
				for(int i = 0 ; i<this.blockedIP.size(); i++){
					if(this.blockedIP.get(i).equals(incomingIP)){
						blocked = true;
					}
				}
				
				//reject connection if blocked
				if(blocked){
					clientSocket.close();
				}else{
					//else do service on the connection
					numberOfThreads ++;
				
					System.out.println("threads +"+ numberOfThreads+"created");
					
					//add the current ip to the blocked list
					this.blockedIP.add(incomingIP);
					
					//create new timer for that ip
					//this will create 1 thread for every ip connection
					Timer t = new Timer();
					t.schedule(new IPTimer(incomingIP,this.blockedIP), this.intervaltime*1000);
				
					Service s = new Service(clientSocket,this,debug);
				}
				
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try {
				listen.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
	}
	
	
	/**
	 * get the resource list of the server
	 * @return resources
	 */
	protected ArrayList<Resource> getResource(){
		return resources;
	}
	
	/**
	 * get the record of servers
	 * @return serverRecords
	 */
	protected ArrayList<String> getServerRecord(){
		return serverRecords;
	}
	
	
	
	
	/**
	 * class of sync server that do the handling
	 * for every time the exchange timer finishes
	 * extends TimerTask
	 */
	class SyncServer extends TimerTask{
		private EzServer server;
		private Random rand;
		
		/**
		 * constructor
		 * @param server the server that the timer needs to work in
		 */
		public SyncServer(EzServer server){
			this.server = server;
			rand = new Random();
			
		}
		
		public void run(){
		
			ArrayList<String> records =server.getServerRecord();
			int record_size = records.size();
			
			//if record_size not empty
			if(record_size!= 0){
				
				//get a random server to be checked
				int servernumber = rand.nextInt(record_size);
				
				//get the address
				String address = records.get(servernumber);
				System.out.println("syncing with "+address);
				String[] addr = address.split(":");
				StringBuilder b = new StringBuilder();
				b.append(records.get(0));
				for(int i = 1 ; i<records.size();i++){
					b.append(",");
					b.append(records.get(i));
				}
				//System.out.println(b.toString());
				
				//create argument for client
				String argument = "-exchange -port "+addr[1]+" -host "+addr[0]+" -debug "+"-servers "+b;
				//System.out.println(argument);
				
				
				String[] arguments = argument.split(" ");
				try{
					//create client to run and get the response
					EzClient c = new EzClient (arguments,false);
					String response = c.run();
					System.out.println(response);
				}catch(NullPointerException e){
					//if any exception found during connection, remove the address
					System.out.println("null pointer found");
					remove_addr(address,records);
					
				}catch(IOException e){
					System.out.println("IO problems found");
					remove_addr(address,records);
				}
					
			}
			
		}
		
		
		//function to remove the address from the recordlist
		private void remove_addr(String address,ArrayList<String>addrs){
			for(int i = 0; i<addrs.size();i++){
				if(addrs.get(i).equals(address)){
					addrs.remove(i);
				}
			}
		}
		
		
		
	}
	
	
	/**
	 * IP timer to delete the blocked ip from the list
	 *
	 */
	class IPTimer extends TimerTask{
		private ArrayList<String> blockedIp;
		private String ip;
		
		public IPTimer(String ip, ArrayList<String> blockedIP){
			this.ip = ip;
			this.blockedIp = blockedIP;
		}

		@Override
		public void run() {
			for(int i = 0 ; i < blockedIp.size(); i++){
				if(blockedIp.get(i).equals(ip)){
					blockedIp.remove(i);
				}
			}
		}
		
	}
	
	
	

	
	
	
	

}
