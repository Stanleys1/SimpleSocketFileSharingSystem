package EZShare;


import java.io.IOException;

public class Client {
	
	public static void main (String[] args){
		
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
