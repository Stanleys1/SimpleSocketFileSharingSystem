import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

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
			System.out.println(input);
			
			String output = JSONOperator(input);
			
			//Jason
			System.out.println("testing"+output);
			
			out.writeUTF(output);
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
				System.out.println("command = "+command);
			}catch(NullPointerException e){
				return generate_error_message("missing command");
			}catch(ClassCastException e){
				return generate_error_message("missing command");
			}
			
			//secret = (String) obj.get("secret");
			
			
			//need error check here
			//if the object.get(resource) is not a jsonObject
			
			/*try {
				sl = new JSONArray();
				sl = (JSONArray)obj.get("serverList");
			} finally {
				message = meaningless();
			}
			
			//sl = new JSONArray();
			//sl = (JSONArray)obj.get("serverList");
			
			try {
				rt = (JSONObject)obj.get("resourceTemplate");
			} finally {
				message = meaningless();
			}
			
			//rt = (JSONObject)obj.get("resourceTemplate");
				
			try {
				rcs = (JSONObject)obj.get("resource");
				/*uri = (String) rcs.get("uri");
				System.out.println("uri = " + uri);
				name= (String) rcs.get("name");
				System.out.println("name = "+name);
				
				
				
				description = (String) rcs.get("description");
				channel = (String) rcs.get("channel");
				
				owner = (String) rcs.get("owner");
				System.out.println("owner = "+owner);
				
				ezserver = (String) rcs.get("ezserver");*/
			/*} finally {
				message = meaningless();
			}
			
			/*rcs = (JSONObject)obj.get("resource");*/
			
			/*uri = (String) rcs.get("uri");
			System.out.println("uri = " + uri);
			name= (String) rcs.get("name");
			System.out.println("name = "+name);
			
			//tags = (String) rcs.get("tags");
			//System.out.println("tags = "+tags);
			
			description = (String) rcs.get("description");
			channel = (String) rcs.get("channel");
			
			owner = (String) rcs.get("owner");
			System.out.println("owner = "+owner);
			
			ezserver = (String) rcs.get("ezserver"); 
			
			
			/*
			 * Stanley
			 * you need to pass it on to the functions
			 * like the one below
			 * each function will return a string that is the output message to the client
			 * and do error checking
			 */
			if(command.equals("PUBLISH")){
				
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
				if(!HelperFunction.isURI(uri)){
					return generate_error_message("invalid resource");
				}
				if(owner.equals("*")){
					return generate_error_message("invalid resource");
				}
				message = publish();
				return message;
			}
			
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
				
				secret =(String)obj.get("secret");
				if(secret.equals("")){
					return generate_error_message("incorrect secret");
				}
				
				message = share();
				return message;
			}
			
			/* need to compare with the resources
			 * on server
			 * 
			 */
			if(command.equals("QUERY")){
				boolean relay = false;
				try{
					relay = (boolean)obj.get("relay");
					System.out.println("relay  = "+ relay);
					rcsTemplate = (JSONObject)obj.get("resourceTemplate");
					System.out.println(rcsTemplate.toJSONString());
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
					System.out.println(sl.toJSONString());
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
			
			/* need to compare with the resources
			 * on server
			 * 
			 * 
			 */
			if(command.equals("REMOVE")){   
				
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
				
				
				message = remove();
				return message;
			}
			
			if(command.equals("FETCH")){
				try{
					rcsTemplate = (JSONObject)obj.get("resourceTemplate");
					System.out.println(rcsTemplate.toJSONString());
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
				
				rcsTemplate.put("resourceSize", "");
				System.out.println(rcsTemplate.toJSONString());
				message = fetch();
				return message;
				
			}
			
			return generate_error_message("invalid command");
			
		}
		
		private String meaningless() {
			return "meaningless";
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
			//new resource is added into server resourceArray
			if(!hasPublished){
				server.getResource().add(publish_resource);
			}
			return generate_success_message();
		}
		/*
		 * same uri,channel and owner, need to override such resource
		 */
		private boolean samePrimaryKey(Resource resource, String uri, String owner, String channel) {
			// TODO Auto-generated method stub
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
			// TODO Auto-generated method stub
			if(resource.getUri().equals(uri)&&resource.getChannel().equals(channel)
					&&!resource.getOwner().equals(owner)){
				return false;
			}
			return true;
		}

		private String share(){
			
			return "share";
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
			return generate_success_message();
		}
		private String exchange(String[] serverlist){
			ArrayList<String> records = server.getServerRecord();
			boolean duplicate = false;
			boolean isThisServer = false;
			String currentServer = server.getHostName()+":"+server.getPort();
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
				if(currentServerIP.equals(serverlist[i])){
					isThisServer = true;
				}
				if(!duplicate && !isThisServer){
					records.add(serverlist[i]);
				}
			}
			return generate_success_message();
		}
		
		
		private String fetch(){
			
			return "fetch";
		}
		private String query(boolean relay){
			int resultSize = 0;
			StringBuilder querymessage = new StringBuilder();
			JSONObject response = new JSONObject();
			response.put("response", "success");
			querymessage.append(response.toJSONString()+"\n");
			JSONObject resource;
			ArrayList<Resource> resources = server.getResource();
			
			Resource template=new Resource(name, tagsString, description,
					 uri,  channel, owner, ezserver);
			for(int i = 0 ; i< resources.size();i++){
				if(resources.get(i).match_template(template)){
					resource = resources.get(i).getResourceWithServer
							(server.getHostName(),server.getPort()).getJSON();
					querymessage.append(resource+"\n");
					resultSize ++;
					
				}
			}
			if(relay){
				ArrayList<String> records =this.server.getServerRecord();
				for(int i=0 ; i<records.size();i++){
					String serverString[] = records.get(i).split(":");
					String serverHost = serverString[0];
					int serverPort = Integer.parseInt(serverString[1]);
					String serverArgument = "-query -host "+serverHost+" -port "+ serverPort;
					StringBuilder b = new StringBuilder();
					b.append(serverArgument);
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
					
					System.out.println("relay to with args"+ b.toString());
					try{
						EzClient c = new EzClient(b.toString().split(" "),false);
						String serverResponse =c.run();
						String[] serverRes= serverResponse.split("\n");
						for(int k = 1 ; k<serverRes.length-1;k++){
							querymessage.append(serverRes[k]+"\n");
							resultSize++;
						}
					}catch(IOException e){
						System.out.println("failed to connect to"+records.get(i));
					}catch(NullPointerException e ){
						System.out.println("failed to connect to"+records.get(i));
					}
				}
			}
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
					System.out.println("hostname: "+hostname+"port: "+port);
					StringBuilder b = new StringBuilder();
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
	

}
