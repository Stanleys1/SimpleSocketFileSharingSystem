package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ServerSubscribeClient extends Thread implements ServerSubscribe{
	private int port;
	private String host;
	private int resultSize = 0;
	private Socket s;
	private boolean finished = false;
	private boolean debug;
	private Resource template;
	private String id;
	private JSONParser parser = new JSONParser();
	private DataOutputStream out;
	
	ServerSubscribeClient(String server,boolean debug, Resource template,String id
			,DataOutputStream out){
		String[] tokens = server.split(":");
		this.host = tokens[0];
		if(HelperFunction.IsInteger(tokens[1])){
			this.port = Integer.parseInt(tokens[1]);
		}else{
			System.out.println("wrong port format given");
			this.finished = true;
		}
		this.s = null;
		this.debug = debug;
		this.template = template;
		this.id = id;
		this.out = out;
	}
	
	@Override
	public void run(){
		try {
			s = new Socket(this.host,this.port);
			
			DataInputStream in = new DataInputStream( s.getInputStream());
		    DataOutputStream outputStream =new DataOutputStream( s.getOutputStream());
		    
		    String message = generate_message().toJSONString();
		    
		    if(debug){
		    	System.out.println("Subscribe Relay id "+ id+" to "+host+":"+port
		    			+" SENT:"+message); 
		    }
		    
		    outputStream.writeUTF(message);
		    
		    
		    //get initial response;
				
		    JSONObject response = (JSONObject) parser.parse(in.readUTF());
		    	//if response succeeds
		    if(response.get("response").equals("success")){
		    	if(debug){	
		    		System.out.println(response.toJSONString());
		    	}
		    	while(!finished){
		    		JSONObject next = null;
					try {
						next = (JSONObject) parser.parse(in.readUTF());
						
					} catch (ParseException e) {
						e.printStackTrace();
					}
					if(!next.containsKey("response")&&!next.containsKey("resultSize")){
						String passing = next.toJSONString();
						if(debug){
							System.out.println("get this subscribe from "+host+":"+port);
							System.out.println("SENT:"+passing);
						}
						this.out.writeUTF(passing);
						this.resultSize++;
					}
		    	}
				
			}
		} catch (UnknownHostException e) {
			finished = true;
		} catch (IOException e) {
			finished = true;
		} catch (NullPointerException e){
			finished = true;
		} catch (ParseException e) {
			finished = true;
		}
	}
	
	
	@Override
	public int getResultSize() {
		return this.resultSize;
	}

	@Override
	public void stopThread() {
		try {
			this.finished = true;
			s.close();
		} catch (IOException e) {
			//closed socket will always throw exception due to read block
			//thus ignore exception
			this.finished = true;
		}
	}
	
	private JSONObject generate_message(){
		JSONObject message = new JSONObject();
		message.put("command", "SUBSCRIBE");
		message.put("resourceTemplate", template.getJSON());
		message.put("relay", false);
		message.put("id", this.id);
		return message;
	}
	
	

}
