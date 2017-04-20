
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
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;




//ARGUMENTS:
//-port 8000 : listen at port 8000 (NEEDED FOR IT TO RUN)
//-exchangeinterval 100: change the exchange timer to 100s. Change to sth small if u want to test
//-secret : set ur own secret, if not provided will generate 25-35 long random word
//-debug: service will print all message sent
//-connectionintervallimit: NOT DONE
//-advertisedname: NOT DONE


public class EzServer implements Runnable {
	private ArrayList<String> serverRecords;
	public static final int DEFAULTTIME = 600000;
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
		EzServer server = new EzServer(args);
		server.run();
	}
	
	public EzServer(String[] args){
		this.options= generateOptions();
		resources = new ArrayList<Resource>();
		this.args = args;
		serverRecords = new ArrayList<String> ();
		blockedIP = new ArrayList<String>();
		
		//For testing purposes
		//serverRecords.add("localhost:8000");
		//serverRecords.add("localhost:10000");
		
		timer = new Timer();
		
	}
	
	private Options generateOptions(){
		ArrayList<Option> op= new ArrayList<Option>();
		
		op.add(new Option("advertisedhostname",true,"hostname"));
		op.add(new Option("connectionintervallimit", true, "timelimit"));
		op.add(new Option("exchangeinterval", true, "exchangetime"));
		op.add(new Option("port",true,"portnumber"));
		op.add(new Option("secret",true,"secret"));
		op.add(new Option("debug",false,null));
		
		Options options = new Options();
		
		
		for(int i = 0 ; i < op.size();i++){
			options.addOption(op.get(i));
		}
		return options;
		
	}
	
	public String getHostName(){
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public String getHostIP(){
		try{
			return InetAddress.getLocalHost().getHostAddress();
		}catch(UnknownHostException e){
			e.printStackTrace();
		}
		return "";
	}
	
	public int getPort(){
		return this.port;
	}
	
	public String getSecret() {
		return this.secret;
	}
	
	
	private String generate_random_secret(){
		Random random = new Random();
		int stringLength = 25 + (int)(Math.random()*10);
		System.out.println("secret length = "+ stringLength);
		char[] possibleChar ="abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
		StringBuilder secret = new StringBuilder();
		for(int i =0; i<stringLength;i++){
			char c = possibleChar[random.nextInt(possibleChar.length)];
			secret.append(c);
		}
		return secret.toString();
		
	}
	
	//jason
	
	
	@Override
	public void run() {
		//jason
		//String message = generate_message(args).toJSONString();
		//Socket c = null;
		CommandLine cmd;
		CommandLineParser parser = new DefaultParser();
		try{
			cmd = parser.parse(options, args);
			
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
			
			if(cmd.hasOption("debug")){
				debug = true;
			}
			System.out.println("debug = "+ debug);
			
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
			
			if(cmd.hasOption("secret")){
				this.secret = cmd.getOptionValue("secret");
			}else{
				this.secret = generate_random_secret();
			}
			System.out.println("this server secret = " + this.secret);
			
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
			if(cmd.hasOption("advertisedhostname")){
				//TODO
			}
			
			
			
		}catch(ParseException exception){
			System.out.println("Parse error:");
			exception.printStackTrace();
		}
		
		
		timer.scheduleAtFixedRate(new SyncServer(this),exchangetime, exchangetime);
		
		ServerSocketFactory factory = ServerSocketFactory.getDefault();
		
		
		try {
			listen = factory.createServerSocket(port);
			
			System.out.println("Server connected on port " + listen.getLocalPort());
			int numberOfThreads = 0;
			boolean blocked = false;
			while(true){
				System.out.println("listening for connection");
				Socket clientSocket = listen.accept();
				blocked = false;
				
				String incomingIP = clientSocket.getInetAddress().toString();
				System.out.println(incomingIP);
				for(int i = 0 ; i<this.blockedIP.size(); i++){
					if(this.blockedIP.get(i).equals(incomingIP)){
						blocked = true;
					}
				}
				
				if(blocked){
					clientSocket.close();
				}else{
					numberOfThreads ++;
				
					System.out.println("threads +"+ numberOfThreads+"created");
					
					this.blockedIP.add(incomingIP);
					
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
	
	
	protected ArrayList<Resource> getResource(){
		return resources;
	}
	
	protected ArrayList<String> getServerRecord(){
		return serverRecords;
	}
	
	
	
	
	
	class SyncServer extends TimerTask{
		private EzServer server;
		private Random rand;
		
		
		public SyncServer(EzServer server){
			this.server = server;
			rand = new Random();
			
		}
		
		public void run(){
			ArrayList<String> records =server.getServerRecord();
			int record_size = records.size();
			if(record_size!= 0){
				int servernumber = rand.nextInt(record_size);
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
				String argument = "-exchange -port "+addr[1]+" -host "+addr[0]+" -debug "+"-servers "+b;
				//System.out.println(argument);
				String[] arguments = argument.split(" ");
				try{
					EzClient c = new EzClient (arguments,false);
					String response = c.run();
					System.out.println(response);
				}catch(NullPointerException e){
					System.out.println("null pointer found");
					remove_addr(address,records);
					
				}catch(IOException e){
					System.out.println("IO problems found");
					remove_addr(address,records);
				}
					
			}
			
		}
		
		public void remove_addr(String address,ArrayList<String>addrs){
			for(int i = 0; i<addrs.size();i++){
				if(addrs.get(i).equals(address)){
					addrs.remove(i);
				}
			}
		}
		
		
		
	}
	
	
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
