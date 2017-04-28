package EZShare;


public class Server {
	
	public static void main (String[] args){
		//create server
		EzServer server = new EzServer(args);
		//run server
		server.run();
	}
}
