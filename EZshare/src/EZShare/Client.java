package EZShare;


import java.io.IOException;

public class Client {
	
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
			EzClient client = new EzClient(args,true,false);
			String response =client.run();

		}catch(NullPointerException e){
			System.out.println("null pointer in client found");
			e.printStackTrace();
		}catch(IOException e){
			System.out.println("IO problems in client found");
			e.printStackTrace();
		}
		
		
	}

}
