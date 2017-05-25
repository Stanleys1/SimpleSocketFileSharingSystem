package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ServerSubscribeClient extends Thread implements ServerSubscribe{
	private int port;
	private String host;
	private int resultSize = 0;
	private Socket s = null;
	private SSLSocket secureSocket= null;
	private boolean finished = false;
	private boolean debug;
	private boolean security;
	private Resource template;
	private String id;
	private JSONParser parser = new JSONParser();
	private DataOutputStream out;
	
	ServerSubscribeClient(String server,boolean debug, Resource template,String id
			,DataOutputStream out,boolean security){
		String[] tokens = server.split(":");
		this.host = tokens[0];
		if(HelperFunction.IsInteger(tokens[1])){
			this.port = Integer.parseInt(tokens[1]);
		}else{
			System.out.println("wrong port format given");
			this.finished = true;
		}
		this.debug = debug;
		this.template = template;
		this.id = id;
		this.out = out;
		this.security = security;
	}
	
	@Override
	public void run(){
		try {
			
			DataInputStream in;
			DataOutputStream outputStream;
			if(security){
				SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
				this.secureSocket = (SSLSocket) sslsocketfactory.createSocket(host,port);
				in = new DataInputStream(secureSocket.getInputStream());
				outputStream = new DataOutputStream(secureSocket.getOutputStream());
			}else{
				s = new Socket(this.host,this.port);
				in = new DataInputStream( s.getInputStream());
			    outputStream =new DataOutputStream( s.getOutputStream());
			}
			
			
		    
		    String message = generate_message().toJSONString();
		    
		    if(debug){
		    	System.out.println("Subscribe Relay id "+ id+" to "+host+":"+port
		    			+" SENT:"+message); 
		    }
		    
		    outputStream.writeUTF(message);
		    
		    
		    //get initial response;
			System.out.println("here");
		    JSONObject response = (JSONObject) parser.parse(in.readUTF());
		    System.out.println(response.toJSONString());
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
			e.printStackTrace();
		} catch (IOException e) {
			finished = true;
			e.printStackTrace();
		} catch (NullPointerException e){
			finished = true;
			e.printStackTrace();
		} catch (ParseException e) {
			finished = true;
			e.printStackTrace();
		}
	}
	
	
	@Override
	public int getResultSize() {
		return this.resultSize;
	}

	@Override
	public void stopThread() {
		try {
			
			if(this.s != null){
				s.close();
			}else{
				this.secureSocket.close();
				this.finished = true;
			}
			
		} catch (IOException e) {
			//closed socket will always throw exception due to read block
			//thus ignore exception
			this.finished = true;
			e.printStackTrace();
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
