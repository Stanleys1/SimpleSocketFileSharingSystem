package EZShare;


import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
//-advertisedname: change the advertised host name (NOT DNS-RESOLVABLE)


public class EzServer implements Runnable {
	
	//records of all server records to be exchanged
	private ArrayList<String> serverRecords;
	//record secure server records
	private ArrayList<String> secureServerRecords;
	//record unsecure server records
	private ArrayList<String> unSecureServerRecords;
	//default exchange time
	public static final int DEFAULTTIME = 600000;
	
	//default connection interval time
	public static final int DEFAULTINTERVAL = 1000;
	
	public static final int DEFAULTSECRETLENGTH = 25;
	
	public static final int DEFAULTPORT = 3780;
	public static final int DEFAULTSECUREPORT=3781;
	public static final int DEFAULTVARIABLELENGTH = 10;
	private int exchangetime;
	private int intervaltime;
	private ArrayList<Resource> resources;
	private Timer timer;
	private int port;
	private int securePort;
	private String host;
	private String secret;
	private ServerSocket listen;
	private Options options;
	private boolean debug = false;
	private String[] args;
	private HashMap<String,Date> blockedIP;
	private HashMap<String,Date> blockedSecureIP;
	private HelpFormatter formatter;
	private Integer numberOfThread=0;
	private ArrayList<Service> subscribeThreads;
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
		secureServerRecords= new ArrayList<String> ();
		unSecureServerRecords= new ArrayList<String> ();
		blockedIP = new HashMap<String,Date>();
		blockedSecureIP= new HashMap<String,Date>();
		this.formatter = new HelpFormatter();
		
		//For testing purposes
		//serverRecords.add("localhost:8000");
		//serverRecords.add("localhost:10000");
		
		timer = new Timer();
		this.subscribeThreads = new ArrayList<Service>();
		
	}
	
	/**
	 * add the thread into sub threads
	 * @param s
	 */
	public void addSubThread(Service s){
		subscribeThreads.add(s);
	}
	
	/**
	 * remove the threads from the list
	 * @param s
	 */
	public void removeThread(Service s){
		for(int i = 0 ;i <this.subscribeThreads.size();i++){
			if(this.subscribeThreads.get(i).equals(s)){
				this.subscribeThreads.remove(s);
				break;
			}
		}
	}
	
	/**
	 * notify all threads that changes in resource
	 * when published or shared
	 * @param rcs
	 */
	public void notifyThreads_Resource(Resource rcs){
		//System.out.println(this.subscribeThreads.size());
		for(int i = 0 ; i<this.subscribeThreads.size();i++){
			Service s = this.subscribeThreads.get(i);
			s.notifySender(rcs);
		}
	}
	
	public void notifyThreads_Server(ArrayList<String> servers ){
		for(int i = 0 ; i<this.subscribeThreads.size();i++){
			Service s = this.subscribeThreads.get(i);
			s.checkServer(servers);
		}
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
		op.add(new Option("sport",true,"flag for secure connection"));
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
		return host;
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
		int stringLength = DEFAULTSECRETLENGTH + random.nextInt(DEFAULTVARIABLELENGTH);
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
			System.out.println("Starting the EZShare Server");
			cmd = parser.parse(options, args);
			
			if(cmd.hasOption("help")){
				this.formatter.printHelp("Help", options);
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
				port = DEFAULTPORT;
			}
			//check the secure port number for server to listen in
			if(cmd.hasOption("sport")){
				String portString = cmd.getOptionValue("sport");
				if(HelperFunction.IsInteger(portString)){
					this.securePort = Integer.parseInt(portString);
					System.out.println("secure port = " + securePort);
				}else{
					System.out.println("secure port number is not a number, initialization failed");
					System.exit(0);
				}
			}else{
				securePort = DEFAULTSECUREPORT;
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
					this.intervaltime = Integer.parseInt(buffer)*1000;
				}
				else{
					System.out.println("interval time not an integer, using default time");
					this.intervaltime = DEFAULTINTERVAL;
				}
			}else{
				this.intervaltime = DEFAULTINTERVAL;
			}
			
			System.out.println("connectionintervallimit = "+ this.intervaltime+"ms");
			
			//get advertised host name
			if(cmd.hasOption("advertisedhostname")){
				host = cmd.getOptionValue("advertisedhostname");
				
			}else{
				try {
					host = InetAddress.getLocalHost().getHostName();
				} catch (UnknownHostException e) {
					System.out.println("host not found");
					host = "";
				}
			}
			System.out.println("using advertisedhostname:"+ host);
			
			
		}catch(ParseException exception){
			System.out.println("Parse error:");
			exception.printStackTrace();
		}
		
		//schedule sync server according to the timer
		timer.scheduleAtFixedRate(new SyncServer(this),exchangetime, exchangetime);
		
		EzServerUnSecure ezServerUnSecure=new EzServerUnSecure(this);
		EzServerSecure ezServerSecure=new EzServerSecure(this);
		ezServerUnSecure.start();
		ezServerSecure.start();
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
	 * get the record of servers
	 * @return serverRecords
	 */
	protected ArrayList<String> getSecureServerRecord(){
		return secureServerRecords;
	}
	/**
	 * get the record of servers
	 * @return serverRecords
	 */
	protected ArrayList<String> getUnSecureServerRecord(){
		return unSecureServerRecords;
	}
	/**
	 * get the record of servers
	 * @return serverRecords
	 */
	protected HashMap<String,Date> getBlockedIP(){
		return blockedIP;
	}
	/**
	 * get the record of servers
	 * @return serverRecords
	 */
	protected HashMap<String,Date> getBlockedSecureIP(){
		return blockedSecureIP;
	}
	/**
	 * get the record of servers
	 * @return serverRecords
	 */
	protected int getIntervalTime(){
		return intervaltime;
	}
	
	protected Integer getNumberOfThread(){
		return numberOfThread;
	}
	
	protected boolean getDebug(){
		return debug;
	}
	
	protected int getSecurePort(){
		return securePort;
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
		    synchronized(server.getServerRecord()){
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
						EzClient c = new EzClient (arguments,false,true);
						ArrayList<String> response = c.run();
						for(int i = 0; i<response.size();i++){
							System.out.println(response.get(i));
						}
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
}
