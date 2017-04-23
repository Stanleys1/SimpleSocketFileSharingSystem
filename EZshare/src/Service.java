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

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
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
	
	private int resultSize2;
	private long fileSize;
	private String fileName;
	private String uri2;
	private String shareFileName;
	
	public Service(){
		super();
	}
	
	public Service(Socket clientSocket, EzServer server,boolean debug){
		this.clientSocket = clientSocket;
		this.server = server;
		this.debug = debug;
		this.start();
	}
	
	@Override
	public void run(){
		
		System.out.println("get connection with " + clientSocket.getInetAddress() );
		
		try{
			in = new DataInputStream(clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
			String input = in.readUTF();
			if(debug){
				System.out.println(input);
			}
			
			String output = JSONOperator(input);
			if(debug){
				System.out.println(output);
			}
			
			out.writeUTF(output);
			
			if(command.equals("FETCH")) {
				
				
				System.out.println("________"+ fileName);
				
				if(resultSize2 != 0) {
				
				File f = new File("server_files/"+fileName);
				if(f.exists()) {
					
					// Send this back to client so that they know what the file is.
					JSONObject trigger = new JSONObject();
					trigger.put("command_name", "SENDING_FILE");
					trigger.put("file_name",fileName);
					trigger.put("file_size",f.length());
					try {
						// Send trigger to client
						
						//out.writeUTF(trigger.toJSONString());
						
						//fetchMessage.append(trigger.toJSONString()+"\n");
						
						// Start sending file
						RandomAccessFile byteFile = new RandomAccessFile(f,"r");
						byte[] sendingBuffer = new byte[1024*1024];
						int num;
						// While there are still bytes to send..
						while((num = byteFile.read(sendingBuffer)) > 0){
							System.out.println(num);
							
							  out.write(Arrays.copyOf(sendingBuffer, num));
							 
							//fetchMessage.append(Arrays.copyOf(sendingBuffer, num)+"\n");
							
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
		}finally{
			try{
				clientSocket.close();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		
	}
	
	
	
	// Need to change,  JSON parsing
		private String JSONOperator(String js) throws ParseException, org.json.simple.parser.ParseException {
			
			JSONObject obj;
			JSONObject rcs;
			JSONObject rcsTemplate;
			JSONArray sl;
			String message = ""; //Jason
			JSONParser parser = new JSONParser();
			obj = (JSONObject)parser.parse(js);
			try{
				command =(String)obj.get("command");
			}catch(NullPointerException e){
				return generate_error_message("missing command");
			}catch(ClassCastException e){
				return generate_error_message("missing command");
			}
			/*
			if(command.equals("SHARE")){
				
				try{
					rcs = (JSONObject)obj.get("resource");
					
					System.out.println(rcs.toJSONString());
				}catch(NullPointerException e){
					return generate_error_message("missing resource");
				}catch(ClassCastException e){
					return generate_error_message("missing resource");
				}
				if(!getResource(rcs)){
					return generate_error_message("invalid resource");
				}
				
				// check if it is a file
				if(!HelperFunction.isFileScheme(uri)){
					return generate_error_message("invalid File");
				}
				
				
				if(owner.equals("*")){
					return generate_error_message("invalid resource");
				}
				
				// check if secret value is given
				secret =(String)obj.get("secret");
				if(secret.equals("")){
					return generate_error_message("missing resource and/or secret");
				}
				
				// check if secret value is same as server secret
				if(!secret.equals(server.getSecret())){
					return generate_error_message("incorrect secret");
				}
				
				message = share();
				return message;
			}
			*/
			
			/* need to compare with the resources
			 * on server
			 * 
			 */
			if(command.equals("QUERY")){
				boolean relay = false;
				try{
					relay = (boolean)obj.get("relay");
					//System.out.println("relay  = "+ relay);
					rcsTemplate = (JSONObject)obj.get("resourceTemplate");
					//System.out.println(rcsTemplate.toJSONString());
				}catch(NullPointerException e){
					return generate_error_message("missing resourceTemplate");
				}catch(ClassCastException e){
					return generate_error_message("missing resourceTemplate");
				}
				
				
				if(!getResource(rcsTemplate)){
					return generate_error_message("invalid resourceTemplate");
				}
				
				
				message = query(relay);
				return message;
				
			}
			
			if(command.equals("EXCHANGE")){
				
				sl = new JSONArray();
				try{
					sl = (JSONArray)obj.get("serverList");
					//System.out.println(sl.toJSONString());
				}catch(NullPointerException e){
					return generate_error_message("missing or invalid server list");
				}catch(ClassCastException e){
					return generate_error_message("missing or invalid server list");
				}
				
				String[] serverList = getServerList(sl);
				if(serverList.length==0){
					return generate_error_message("missing server list");
				}
				
				
				message = exchange(serverList);
				return message;
				
			}
			if(command.equals("REMOVE")||command.equals("PUBLISH")||command.equals("SHARE")){
				try{
					rcs = (JSONObject)obj.get("resource");
					//System.out.println(rcs.toJSONString());
				}catch(NullPointerException e){
					return generate_error_message("missing resource");
				}catch(ClassCastException e){
					return generate_error_message("missing resource");
				}
				//URI must be absolute,owner cannot be * and correct resource field
				if(!getResource(rcs)||owner.equals("*")){
					return generate_error_message("invalid resource");
				}
				switch (command){
				case "REMOVE":
					if(!HelperFunction.isURI(uri)){
						return generate_error_message("invalid resource");
					}
					message = remove();
					break;
				case "PUBLISH":
					if(!HelperFunction.isURI(uri)){
						return generate_error_message("invalid resource");
					}
					message = publish();
					break;
				case "SHARE":
					if(!HelperFunction.isFileScheme(uri)){
						return generate_error_message("invalid resource");
					}
					// check if secret value is given
					secret =(String)obj.get("secret");
					if(secret.equals("")){
						return generate_error_message("missing resource and/or secret");
					}		
					// check if secret value is same as server secret
					if(!secret.equals(server.getSecret())){
						return generate_error_message("incorrect secret");
					}
					message = share();
					break;
				default: break;
				}
				return message;
			}
			if(command.equals("FETCH")){
				try{
					rcsTemplate = (JSONObject)obj.get("resourceTemplate");
					//System.out.println(rcsTemplate.toJSONString());
				}catch(NullPointerException e){
					return generate_error_message("missing resourceTemplate");
				}catch(ClassCastException e){
					return generate_error_message("missing resourceTemplate");
				}
				
				
				if(!getResource(rcsTemplate)){
					return generate_error_message("invalid resourceTemplate");
				}
				
				if(channel.equals("")){
					return generate_error_message("invalid channel");
				}
				// check if it is a file
				if(!HelperFunction.isFileScheme(uri)){
					return generate_error_message("invalid File");
				}
				
				//rcsTemplate.put("resourceSize", "");
				//System.out.println(rcsTemplate.toJSONString());
				message = fetch();
				return message;
				
			}
			
			return generate_error_message("invalid command");
			
		}
		
		
		/*
		 * publish command has three possible choices
		 * 1.resource is new, just add it into server resources
		 * 2.need to be overrided:there already is one resource with the same primary key(channel, owner,uri),
		 * 3.not allowed to be published:there is one resource having same channel and uri but different owner 
		 */
		private String publish(){
			Resource publish_resource=new Resource(name, tagsString, description,
					 uri,  channel, owner, ezserver);
			boolean hasPublished=false;
			//get a synchronize lock
			synchronized(server){
				for (int i=0;i<server.getResource().size();i++){
					if(hasPublished){
						break;
					}
					if(!publish_notAllowed(server.getResource().get(i), uri,owner,channel)){
						return generate_error_message( "cannot publish resource");
					}
					if(samePrimaryKey(server.getResource().get(i),uri,owner,channel)){
						server.getResource().set(i, publish_resource);
						hasPublished=true;
					}
				}
				if(!hasPublished){
					server.getResource().add(publish_resource);
				}
				//sleep several seconds
				try{
					//test 
					System.out.println("100s cannot operate server");
					sleep(100000);
				}catch(InterruptedException e){
					e.printStackTrace();
				}
				
			}
			//new resource is added into server resourceArray
			return generate_success_message();
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

		private String share(){
			JSONObject obj;
			
			Resource share_resource=new Resource(name, tagsString, description,
					 uri,  channel, owner, ezserver);
			boolean hasShared=false;
			
			shareFileName = getFileName(uri);
			System.out.println("________"+ shareFileName);
			
			for (int i=0;i<server.getResource().size();i++){
				if(hasShared){
					break;
				}
				
				// same resource with different owners is not allowed to share
				if(!publish_notAllowed(server.getResource().get(i), uri,owner,channel)){
					return generate_error_message( "cannot share resource"); 
				}
				if(samePrimaryKey(server.getResource().get(i),uri,owner,channel)){
					server.getResource().set(i, share_resource); // overwrite
					hasShared=true;
				}
			}
			//new resource is added into server resourceArray
			if(!hasShared){
				server.getResource().add(share_resource);
				try {
					URI u = new URI(uri);
					
					File source = new File(u); 
					File dest = new File("server_files/"+shareFileName);
					//FileUtils.copyDirectory(source, dest);
					FileUtils.copyFile(source, dest);
					
				} catch (URISyntaxException e) {
					// error
					
				} catch (IOException e) {
				    e.printStackTrace();
				}
				
			}
			return generate_success_message();
			
			//return "share";
		}
		/*
		 * remove command has two possible choices
		 * 1.normally remove:there is one resource with same primary key
		 * 2.not such a resource in server resources, response error message
		 * notes:get the index of targeted resource in "For", then remove it after the "For"
		 */
		private String remove(){
			Resource publish_resource=new Resource(name, tagsString, description,
					 uri,  channel, owner, ezserver);
			boolean resource_need_Removed=false;
			int index_remove=0;
			synchronized(server){
				for (int i=0;i<server.getResource().size();i++){
					if(samePrimaryKey(server.getResource().get(i),uri,owner,channel)){
						index_remove=i;
						resource_need_Removed=true;
						break;
					}
				}
				if(!resource_need_Removed){
					return generate_error_message( "cannot remove resource");
				}
				server.getResource().remove(index_remove);
			}
			
			return generate_success_message();
		}
		private String exchange(String[] serverlist){
			ArrayList<String> records = server.getServerRecord();
			boolean duplicate = false;
			boolean isThisServer = false;
			String currentServer = server.getHostName()+":"+server.getPort();
			String currentServer2 = "localhost"+":"+server.getPort();
			String currentServerIP = server.getHostIP()+":"+server.getPort();
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
				//System.out.println(serverlist[i]);
			}
			return generate_success_message();
		}
		
		/*
		 * Using the same technique as Query
		 * Additional things: put resourceSize
		 */
		private String fetch(){
			
			resultSize2 = 0;
			JSONObject response = new JSONObject();
			StringBuilder fetchMessage = new StringBuilder();
			JSONObject resource;
			ArrayList<Resource> resources = server.getResource();
			
			Resource template=new Resource(name, tagsString, description,
					 uri,  channel, owner, ezserver);
			
			for(int i = 0 ; i< resources.size();i++){
				if(resources.get(i).match_template(template)){
					resource = resources.get(i).getResourceWithServer
							(server.getHostName(),server.getPort()).getJSON();
					
					fileName = getFileName(resources.get(i).getUri());
					
					response.put("response", "success");
					fetchMessage.append(response.toJSONString()+"\n");
					resource.put("resourceSize", HelperFunction.fileSize(uri));
					fetchMessage.append(resource+"\n");
					resultSize2 ++;
					
				}
			}
			
			JSONObject result = new JSONObject();
			
			result.put("resultSize", resultSize2);
			fetchMessage.append(result.toJSONString());
			
			return fetchMessage.toString();
			
			
		}
		
		//do query
		private String query(boolean relay){
			//result size of query
			int resultSize = 0;
			
			//build query message
			StringBuilder querymessage = new StringBuilder();
			
			//create response
			JSONObject response = new JSONObject();
			response.put("response", "success");
			querymessage.append(response.toJSONString()+"\n");
			JSONObject resource;
			ArrayList<Resource> resources = server.getResource();
			
			//create template from the client arguments
			Resource template=new Resource(name, tagsString, description,
					 uri,  channel, owner, ezserver);
			
			for(int i = 0 ; i< resources.size();i++){
				//if resource match template, add to the query message
				if(resources.get(i).match_template(template)){
					resource = resources.get(i).getResourceWithServer
							(server.getHostName(),server.getPort()).getJSON();
					querymessage.append(resource+"\n");
					//add result size
					resultSize ++;
					
				}
			}
			
			//if there is a need for query relay
			if(relay){
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
					
					//Owner and Channel are directly set to "" 
					//in the query relay since 
					//both fields are not provided to the argument for the client
					
					
					//System.out.println("relay to with args"+ b.toString());
					
					
					//add the arguments into the client
					try{
						//set relay argument to false
						EzClient c = new EzClient(b.toString().split(" "),false);
						String serverResponse =c.run();
						
						//get the response and add it to querymessage
						String[] serverRes= serverResponse.split("\n");
						for(int k = 1 ; k<serverRes.length-1;k++){
							querymessage.append(serverRes[k]+"\n");
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
			
			//add resultsize to the query message
			JSONObject result = new JSONObject();
			result.put("resultSize", resultSize);
			querymessage.append(result.toJSONString());
			
			return querymessage.toString();
		}

		
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
		private String[] getServerList(JSONArray sl) {
			ArrayList<String> serverlist = new ArrayList<String>();
			String[] error = {};
			try{
				for(int i = 0 ; i< sl.size();i++){
					JSONObject host = (JSONObject) sl.get(i);    // need to change, cannot identify
					String hostname = (String) host.get("hostname");
					long port = (long) host.get("port");
					//System.out.println("hostname: "+hostname+"port: "+port);
					StringBuilder b = new StringBuilder();
					
					/*
					 * if localhost, change hostname to ip address
					 */
					try {
					if(hostname.equals("localhost")) {
						//System.out.println("localhost");
						InetAddress host2 = InetAddress.getLocalHost();
						//System.out.println(host2);
						hostname = host2.getHostAddress();
						//System.out.println("localhost finish");
					} }
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
	

}
