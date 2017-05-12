package EZShare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.cli.ParseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Service extends Thread{
	private Socket clientSocket;
	private DataInputStream in = null;
	private DataOutputStream out = null;
	private String name;
	private String uri;
	private EzServer server;
	private JSONArray tags;
	private String[] tagsString;
	private String description;
	private String channel;
	private String ezserver;
	private String owner;
	private String secret;
	private String command;
	private boolean debug;
	private boolean finished;
	
	private String fileName;
	private String shareFileName;
	
	private HashMap<String, ServerSubscribeResponse> subscribeIDs;
	private int resultSize = 0;
	private boolean notadded = false;
	
	public Service(){
		super();
	}
	
	public Service(Socket clientSocket, EzServer server,boolean debug){
		this.clientSocket = clientSocket;
		this.server = server;
		this.debug = debug;
		this.finished= false;
		this.start();
	}
	
	/**
	 * service's run method 
	 * 
	 * @param 
	 * @return  
	 */
	public void run(){
		try{
			in = new DataInputStream(clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
			
			//as long it is not finished
			//keep reading and sending (for asynchronous connection)
			while(!finished){
				String input = in.readUTF();
				if(debug){
					System.out.println("RECEIVED:"+ input);
				}
				ArrayList<String> output = JSONOperator(input);
				
				if(output.size()>0){
					for(int i = 0 ; i<output.size();i++){
						if(debug){
							System.out.println("SENT:"+ output.get(i));
						}
						out.writeUTF(output.get(i));
					}
				}
			}
			
			//if command is fetch, send the file
			if(command.equals("FETCH")) {
				System.out.println("________"+ fileName);	
				if(resultSize != 0) {
					URI u = new URI(uri);	
					File f = new File(u);
					if(f.exists()) {
					// Send this back to client so that they know what the file is.
					JSONObject trigger = new JSONObject();
					trigger.put("command_name", "SENDING_FILE");
					trigger.put("file_name",fileName);
					trigger.put("file_size",f.length());
					try {
						// Start sending file
						RandomAccessFile byteFile = new RandomAccessFile(f,"r");
						byte[] sendingBuffer = new byte[1024*1024];
						int num;
						// While there are still bytes to send..
						while((num = byteFile.read(sendingBuffer)) > 0){
							System.out.println(num);
							
							  out.write(Arrays.copyOf(sendingBuffer, num));
						}
						byteFile.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
				} else {
					// Throw an error here..
				} }
				
			}
			
			in.close();
			out.close();
			
			
		}catch(IOException e){
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (org.json.simple.parser.ParseException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} finally{
			try{
				clientSocket.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		
	}
	

	/**
	 * dealing with  commands from client 
	 * parse client's message to one JsonObject then choose different routes according to different commands
	 * @param message from the client
	 * @return response message for the command 
	 */
		private ArrayList<String> JSONOperator(String js) 
				throws ParseException, org.json.simple.parser.ParseException {
			
			ArrayList<String> response = new ArrayList<String>();
			JSONObject obj;
			JSONObject rcs;
			JSONObject rcsTemplate;
			JSONArray sl;
			JSONParser parser = new JSONParser();
			obj = (JSONObject)parser.parse(js);
			try{
				command =(String)obj.get("command");
			}catch(NullPointerException e){
				response.add(generate_error_message("missing command"));
				this.finished = true;
				return response;
			}catch(ClassCastException e){
				response.add(generate_error_message("missing command"));
				this.finished = true;
				return response;
			}
			//dealing with query command
			if(command.equals("QUERY")){
				boolean relay = false;
				try{
					relay = (boolean)obj.get("relay");
					rcsTemplate = (JSONObject)obj.get("resourceTemplate");
				}catch(NullPointerException e){
					response.add(generate_error_message("missing resourceTemplate"));
					this.finished = true;
					return response ;
				}catch(ClassCastException e){
					response.add(generate_error_message("missing resourceTemplate"));
					this.finished = true;
					return response ;
				}
				
				if(!getResource(rcsTemplate)){
					response.add(generate_error_message("invalid resourceTemplate"));
					this.finished = true;
					return response ;
				}
				response = query(relay);
				this.finished = true;
				return response;
			}
			
			
			
			//deal with subscribe
			if(command.equals("SUBSCRIBE")){
				boolean relay = false;
				String id = "";
				try{
					relay = (boolean)obj.get("relay");
					id = (String)obj.get("id");
					rcsTemplate = (JSONObject)obj.get("resourceTemplate");
				}catch(NullPointerException e){
					response.add(generate_error_message("missing resourceTemplate"));
					return response ;
				}catch(ClassCastException e){
					response.add(generate_error_message("missing resourceTemplate"));
					return response ;
				}
				if(!getResource(rcsTemplate)){
					response.add(generate_error_message("invalid resourceTemplate"));
					return response ;
				}
				response = subscribe(relay,id);
				return response;
			}
			
			//deal with terminate
			if(command.equals("TERMINATE")){
				finished = true;
				response = terminate();
				return response;
			}
			
			//deal with unsubscribe
			if(command.equals("UNSUBSCRIBE")){
				String id= "";
				try{
					id = (String)obj.get("id");
				}catch(NullPointerException e){
					response.add(generate_error_message("missing resourceTemplate"));
					return response ;
				}catch(ClassCastException e){
					response.add(generate_error_message("missing resourceTemplate"));
					return response ;
				}
				response = unsubscribe(id);
				return response;
			}
			
			
			
			//dealing with exchange command
			if(command.equals("EXCHANGE")){
				sl = new JSONArray();
				try{
					sl = (JSONArray)obj.get("serverList");
				}catch(NullPointerException e){
					response.add(generate_error_message("missing or invalid server list"));
					this.finished= true;
					return response;
				}catch(ClassCastException e){
					response.add(generate_error_message("missing or invalid server list"));
					this.finished= true;
					return response;
				}
				
				String[] serverList = getServerList(sl);
				if(serverList.length==0){
					response.add(generate_error_message("missing server list"));
					this.finished= true;
					return response;
				}
				
				
				response = exchange(serverList);
				this.finished= true;
				return response;
				
			}
			//deal with remove,publish,share commands, 
			//these three commands share similar pre-check process
			if(command.equals("REMOVE")||command.equals("PUBLISH")||command.equals("SHARE")){
				try{
					rcs = (JSONObject)obj.get("resource");
					//System.out.println(rcs.toJSONString());
				}catch(NullPointerException e){
					response.add( generate_error_message("missing resource"));
					this.finished= true;
					return response;
				}catch(ClassCastException e){
					response.add( generate_error_message("missing resource"));
					this.finished= true;
					return response;
				}
				//URI must be absolute,owner cannot be * and correct resource field
				if(!getResource(rcs)||owner.equals("*")){
					response.add( generate_error_message("invalid resource"));
					this.finished= true;
					return response;
				}
				
				switch (command){
				case "REMOVE":
					if(!HelperFunction.isURI(uri)){
						response.add( generate_error_message("invalid resource"));
						this.finished= true;
						return response;
					}
					
					response = remove();
					break;
				case "PUBLISH":
					if(!HelperFunction.isURI(uri) || HelperFunction.isFileName(uri)){
						response.add( generate_error_message("invalid resource"));
						this.finished= true;
						return response;
					}
					response = publish();
					break;
				case "SHARE":
					if(!HelperFunction.isFileScheme(uri)){
						response.add( generate_error_message("invalid resource"));
						this.finished= true;
						return response;
					}
					// check if secret value is given
					secret =(String)obj.get("secret");
					if(secret.equals("")){
						response.add( generate_error_message("missing resource and/or secret"));
						this.finished= true;
						return response;
					}		
					// check if secret value is same as server secret
					if(!secret.equals(server.getSecret())){
						response.add( generate_error_message("incorrect secret"));
						this.finished= true;
						return response;
					}
					response = share();
					break;
				default: break;
				}
				this.finished= true;
				return response;
			}
			//dealing with fetch command
			if(command.equals("FETCH")){
				try{
					rcsTemplate = (JSONObject)obj.get("resourceTemplate");
				}catch(NullPointerException e){
					response.add( generate_error_message("missing resourceTemplate"));
					this.finished= true;
					return response;
				}catch(ClassCastException e){
					response.add( generate_error_message("missing resourceTemplate"));
					this.finished= true;
					return response;
				}
				
				
				if(!getResource(rcsTemplate)){
					response.add( generate_error_message("invalid resourceTemplate"));
					this.finished= true;
					return response;
				}
				
				if(channel.equals("")){
					response.add( generate_error_message("invalid channel"));
					this.finished= true;
					return response;
				}
				// check if it is a file
				if(!HelperFunction.isFileScheme(uri)){
					response.add( generate_error_message("invalid File"));
					this.finished= true;
					return response;
				}
				response = fetch();
				this.finished= true;
				return response;
				
			}
			response.add( generate_error_message("invalid command"));
			this.finished= true;
			return response;
			
		}
		
		
		/*
		 * publish command has three possible choices
		 * 1.resource is new, just add it into server resources
		 * 2.need to be overrided:there already is one resource with the same primary key(channel, owner,uri),
		 * 3.not allowed to be published:there is one resource having same channel and uri but different owner 
		 */
		private ArrayList<String> publish(){
			Resource publish_resource=new Resource(name, tagsString, description,
					 uri,  channel, owner, ezserver);
			ArrayList<String> response = new ArrayList<String>();
			boolean hasPublished=false;
			//get a synchronize lock
			synchronized(server.getResource()){
				for (int i=0;i<server.getResource().size();i++){
					if(hasPublished){
						break;
					}
					if(!publish_notAllowed(server.getResource().get(i), uri,owner,channel)){
						response.add( generate_error_message("cannot publish resource"));
						return response;
					}
					if(samePrimaryKey(server.getResource().get(i),uri,owner,channel)){
						server.getResource().set(i, publish_resource);
						hasPublished=true;
					}
				}
				if(!hasPublished){
					server.getResource().add(publish_resource);
				}
				//sleep 1 second
				try{
					sleep(1000);
				}catch(InterruptedException e){
					e.printStackTrace();
				}
				
			}
			
			this.server.notifyThreads(publish_resource);
			//new resource is added into server resourceArray
			response.add(generate_success_message());
			return response;
		}
		/*
		 * same uri,channel and owner, need to override such resource
		 */
		private boolean samePrimaryKey(Resource resource, String uri, String owner, String channel) {
			if(resource.getUri().equals(uri)&&resource.getChannel().equals(channel)
					&&resource.getOwner().equals(owner)){
				return true;
			}
			return false;
		}

		/*
		 * same uri and channel but different owner is not allowed 
		 */
		private boolean publish_notAllowed(Resource resource, String uri, String owner, String channel) {
			
			if(resource.getUri().equals(uri)&&resource.getChannel().equals(channel)
					&&!resource.getOwner().equals(owner)){
				return false;
			}
			return true;
		}
		
		/*
		 * Rules applied are same as Publish
		 * Additional rules: secret, file scheme    
		 */

		private ArrayList<String> share(){
			Resource share_resource=new Resource(name, tagsString, description,
					 uri,  channel, owner, ezserver);
			ArrayList<String> response = new ArrayList<String>();
			boolean hasShared=false;
			
			shareFileName = HelperFunction.getFileName(uri);
			System.out.println("________"+ shareFileName);
			synchronized(server.getResource()){
				for (int i=0;i<server.getResource().size();i++){
					if(hasShared){
						break;
					}
					
					// same resource with different owners is not allowed to share
					if(!publish_notAllowed(server.getResource().get(i), uri,owner,channel)){
						response.add( generate_error_message("cannot share resource"));
						return response;
					}
					if(samePrimaryKey(server.getResource().get(i),uri,owner,channel)){
						server.getResource().set(i, share_resource); // overwrite
						hasShared=true;
					}
				}
				//new resource is added into server resourceArray
				if(!hasShared){
					server.getResource().add(share_resource);
				}
			}
			//sleep 1 second
			try{
				sleep(1000);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			response.add(generate_success_message());
			this.server.notifyThreads(share_resource);
			return response;
		}
		/*
		 * remove command has two possible choices
		 * 1.normally remove:there is one resource with same primary key
		 * 2.not such a resource in server resources, response error message
		 * notes:get the index of targeted resource in "For", then remove it after the "For"
		 */
		private ArrayList<String> remove(){
			ArrayList<String> response = new ArrayList<String>();
			boolean resource_need_Removed=false;
			int index_remove=0;
			synchronized(server.getResource()){
				for (int i=0;i<server.getResource().size();i++){
					if(samePrimaryKey(server.getResource().get(i),uri,owner,channel)){
						index_remove=i;
						resource_need_Removed=true;
						break;
					}
				}
				if(!resource_need_Removed){
					response.add(generate_error_message( "cannot remove resource"));
					return response;
				}
				server.getResource().remove(index_remove);
			}
			//sleep 1 seconds
			try{
				sleep(1000);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			response.add(generate_success_message());
			return response;
		}
		/**
		 * dealing with exchange command 
		 * add servers' information to the server's records for exchanging with other servers per certain time
		 * @param 
		 * @return response message for one exchange command 
		 */
		private ArrayList<String> exchange(String[] serverlist){
			ArrayList<String> response = new ArrayList<String>();
			boolean duplicate = false;
			boolean isThisServer = false;
			String currentServer = server.getHostName()+":"+server.getPort();
			String currentServer2 = "localhost"+":"+server.getPort();
			String currentServerIP = server.getHostIP()+":"+server.getPort();
			synchronized(server.getServerRecord()){
				ArrayList<String> records = server.getServerRecord();
				for( int i = 0 ;i < serverlist.length;i++){
					duplicate = false;
					isThisServer = false;
					for(int j = 0;j<records.size();j++){
						if(serverlist[i].equals(records.get(j))){
							duplicate = true;
						}
					}
					if(currentServer.equals(serverlist[i])){
						isThisServer = true;
					}
					if(currentServer2.equals(serverlist[i])){
						isThisServer = true;
						}
					if(currentServerIP.equals(serverlist[i])){
						isThisServer = true;
						}
					if(!duplicate && !isThisServer){
						records.add(serverlist[i]);
						}
					}
				//sleep 1 seconds
				try{
					sleep(1000);
					}catch(InterruptedException e){
					e.printStackTrace();
					}				
				}
			response.add(generate_success_message());
			return response;
		}
		
		/**
		 * dealing with fetch command 
		 * traverse the server's resource list 
		 * then find target resource according to the resource template
		 * @param 
		 * @return response message for one fetch command 
		 */
		private ArrayList<String> fetch(){
			
			ArrayList<String> response = new ArrayList<String>();
			
			JSONObject resource;
			response.add(this.generate_success_message());
			//the resource template 
			Resource template=new Resource(name, tagsString, description,
					 uri,  channel, owner, ezserver);
			// traverse the server's resource list and
			// find target resources according to the resource template
			synchronized(server.getResource()){
				ArrayList<Resource> resources = server.getResource();
				for(int i = 0 ; i< resources.size();i++){
					if(resources.get(i).match_template(template)){
						resource = resources.get(i).getResourceWithServer
								(server.getHostName(),server.getPort()).getStarOwner().getJSON();
						
						fileName = HelperFunction.getFileName(resources.get(i).getUri());
						
						
						resource.put("resourceSize", HelperFunction.fileSize(uri));
						response.add(resource.toJSONString());
						resultSize ++;
						}
					}
				}
			//sleep 1 seconds
			try{
				sleep(1000);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			JSONObject result = new JSONObject();
			//add the information of file's size 
			result.put("resultSize", resultSize);
			response.add(result.toJSONString());
			
			return response;
			
			
		}
		
		/**
		 * dealing with query command 
		 * @param boolean query¡ªrelay, 
		 *  if true, need to query the server in server records, otherwise not need
		 * @return response message for one query command 
		 */
		private ArrayList<String> query(boolean relay){
			ArrayList<String> response = new ArrayList<String>();
			
			response.add(this.generate_success_message());
			
			JSONObject resource;
			
			//create template from the client arguments
			Resource template=new Resource(name, tagsString, description,
					 uri,  channel, owner, ezserver);
			synchronized(server.getResource()){
				ArrayList<Resource> resources = server.getResource();
				for(int i = 0 ; i< resources.size();i++){
					//if resource match template, add to the query message
					if(resources.get(i).match_template(template)){
						resource = resources.get(i).getResourceWithServer
								(server.getHostName(),server.getPort()).getStarOwner().getJSON();
						
						response.add(resource.toJSONString());
						resultSize ++;
						
					}
				}
			}
			//if there is a need for query relay
			if(relay){
				synchronized(server.getServerRecord()){
					//get serverRecords
					ArrayList<String> records =this.server.getServerRecord();
					
					//for all servers in serverRecords
					for(int i=0 ; i<records.size();i++){
						
						String serverString[] = records.get(i).split(":");
						String serverHost = serverString[0];
						int serverPort = Integer.parseInt(serverString[1]);
						//build a argument for client
						String serverArgument = "-query -host "+serverHost+" -port "+ serverPort;
						StringBuilder b = new StringBuilder();
						b.append(serverArgument);
						//add respective fields if its not empty
						if(!name.equals("")){
							b.append(" -name "+name);
						}
						if(!uri.equals("")){
							b.append(" -uri "+ uri);
						}
						if(!description.equals("")){
							b.append(" -description "+ description);
						}
						if(tagsString.length!=0){
							b.append(" -tags");
							for(int j = 0 ; j<this.tagsString.length;j++){
								b.append(" "+tagsString[j]);
							}
						}
						//add the arguments into the client
						try{
							//set relay argument to false
							EzClient c = new EzClient(b.toString().split(" "),false,true);
							ArrayList<String> serverResponse =c.run();
							
							for(int k = 1 ; k<serverResponse.size()-1;k++){
								response.add(serverResponse.get(k));
								resultSize++;
							}
						}catch(IOException e){
							//log if there is a failure in connection
							System.out.println("failed to connect to"+records.get(i));
						}catch(NullPointerException e ){
							System.out.println("failed to connect to"+records.get(i));
						}
					}
				}
			}
			//sleep 1 seconds
			try{
				sleep(1000);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
			//add resultsize to the query message
			JSONObject result = new JSONObject();
			result.put("resultSize", resultSize);
			response.add(result.toJSONString());
			
			return response;
		}
		
		//TODO
		//RELAYYYYYY AND SUBSCRIBE
		private ArrayList<String> subscribe(boolean relay, String id){
			
			ArrayList<String> response = new ArrayList<String>();
			JSONObject initialResponse = new JSONObject();
			
			//create list of threads to handle the subscription
			if(this.subscribeIDs == null){
				this.subscribeIDs = new HashMap<String,ServerSubscribeResponse>();
			}else{
				//reject if id already present
				if(this.subscribeIDs.containsKey(id)){
					response.add(this.generate_error_message("existing id"));
					return response;
				}
			}
			//create initial response for the subscribe
			initialResponse.put("response","success");
			initialResponse.put("id", id);
			
			//send that initial response to the client to start subscription
			try {
				out.writeUTF(initialResponse.toJSONString());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			//create template from the client arguments
			Resource template=new Resource(name, tagsString, description,
					 uri,  channel, owner, ezserver);
			
			if(!notadded){
				this.server.addThread(this);
				notadded = true;
			}
			
			
			//create response thread for the subscription for the template
			ServerSubscribeResponse s = new ServerSubscribeResponse(this.server,this.out,template);
			s.start();
			
			//put the thread into the hashmap with the id
			this.subscribeIDs.put(id, s );
			
			return response;
		}
		
		
		//function for unsubscribe
		private ArrayList<String> unsubscribe(String id){
			
			ArrayList<String> response = new ArrayList<String>();
			
			//if there is no subscription with that id return error message
			if(!this.subscribeIDs.containsKey(id)){
				response.add(this.generate_error_message("no matching id found"));
				return response;
			}else{
				//else stop thread subscription
				this.resultSize+= this.subscribeIDs.get(id).getResultSize();
				this.subscribeIDs.get(id).stopThread();
				//remove from hashmap
				this.subscribeIDs.remove(id);
			}
			
			response.add(this.generate_success_message());
			return response;
		}
		
		
		//terminate function
		private ArrayList<String> terminate(){
			
			ArrayList<String> response = new ArrayList<String>();
			
			//iterate the hashmap
			Iterator iter = this.subscribeIDs.entrySet().iterator();
			while(iter.hasNext()){
				Map.Entry pair = (Map.Entry)iter.next();
				//get the thread
		        ServerSubscribeResponse res = (ServerSubscribeResponse) pair.getValue();
		        //add the resultSize for each thread
		        this.resultSize+= res.getResultSize();
		        //stop each thread
		        res.stopThread();
			}
			
			response.add(this.generate_success_message());
			//add resultsize to the query message
			JSONObject result = new JSONObject();
			result.put("resultSize", resultSize);
			response.add(result.toJSONString());
			
			//remove this thread from the list in the server
			this.server.removeThread(this);
			
			return response;
		}

		/**
		 * generate response error message 
		 * @param detailed error information
		 * @return an error message
		 */
		private String generate_error_message(String s){
			JSONObject msg = new JSONObject();
			msg.put("response", "error");
			msg.put("errorMessage", s);
			return msg.toJSONString();
		}
		/*
		 * generic success response message
		 */
		private String generate_success_message(){
			JSONObject msg = new JSONObject();
			msg.put("response", "success");
			return msg.toJSONString();
		}
		/**
		 * parse serverlist storing in one JsonArray sent by client and get servers list for exchange command 
		 * @param one JSONArray containing the content of servers list
		 * @return StringArray storing server list for exchange command
		 */
		private String[] getServerList(JSONArray sl) {
			ArrayList<String> serverlist = new ArrayList<String>();
			String[] error = {};
			try{
				for(int i = 0 ; i< sl.size();i++){
					JSONObject host = (JSONObject) sl.get(i);
					String hostname = (String) host.get("hostname");
					long port = (long) host.get("port");
					StringBuilder b = new StringBuilder();

					try {
					if(hostname.equals("localhost")) {
						InetAddress host2 = InetAddress.getLocalHost();
						hostname = host2.getHostAddress();
						}
					}
					catch(UnknownHostException e){
						System.out.println("can't identify host name");
						e.printStackTrace();
						System.exit(0);
					}
					
					b.append(hostname);
					b.append(":");
					b.append(port);
					System.out.println(b.toString());
					serverlist.add(b.toString());
				}
			}catch(NullPointerException e){
				return error;
			}catch(ClassCastException e){
				return error;
			}
			
			String[] success = new String[serverlist.size()];
			serverlist.toArray(success);
			
			return success;
		}
		
		/**
		 * parse JsonObject sent by client and get resource 's variables 
		 * @param one JSONObject containing the content of one resource or resource template
		 * @return false if it catches an exception, otherwise true
		 */
		private boolean getResource(JSONObject rcs){
			try{
				uri = (String) rcs.get("uri");
				tags = (JSONArray)rcs.get("tags");
				name = (String) rcs.get("name");
				description = (String)rcs.get("description");
				channel = (String)rcs.get("channel");
				owner = (String)rcs.get("owner");
				ezserver = (String)rcs.get("ezserver");
				
			}catch(NullPointerException e){
				return false;
			}catch(ClassCastException e){
				return false;
			}
			
			uri = HelperFunction.handleString(uri);
			name = HelperFunction.handleString(name);
			owner =HelperFunction.handleString(owner);
			channel =HelperFunction.handleString(channel);
			description = HelperFunction.handleString(description);
			ezserver = HelperFunction.handleString(ezserver);
			ArrayList<String> taglist = new ArrayList<String>();
			
			for(int i = 0 ; i< tags.size();i++){
				taglist.add(HelperFunction.handleString((String)tags.get(i)));
			}
			tagsString = new String[tags.size()];
			taglist.toArray(tagsString);
			
			return true;
		}
		
		
		
		public void notifySender(Resource rcs){
			Iterator iter = this.subscribeIDs.entrySet().iterator();
			while(iter.hasNext()){
				Map.Entry pair = (Map.Entry)iter.next();
				//get the thread
		        ServerSubscribeResponse res = (ServerSubscribeResponse) pair.getValue();
		        
		        res.checkResource(rcs);
			}
		}
		
}
