package EZShare;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * 
 * Resource class
 *
 */
public class Resource {
	private String name;
	private String description;
	private String[] tags;
	private String uri;
	private String channel;
	private String owner;
	private String ezserver;
	
	/**
	 * constructor for resource
	 * @param name name of resource
	 * @param tags tags inside the resource
	 * @param description resource description
	 * @param uri uri of the resource
	 * @param channel channel that the resource will be stored in
	 * @param owner owner of the resource
	 * @param ezserver the server the resource is in
	 */
	public Resource(String name, String[] tags,String description,
			String uri, String channel,String owner,String ezserver){
		this.name= name;
		this.tags = tags;
		this.description= description;
		this.uri = uri;
		this.channel = channel;
		this.owner = owner;
		this.ezserver = ezserver;
		
	}
	
	/**
	 * get name of the resource
	 * @return name
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * get description of the resource
	 * @return description
	 */
	public String getDescription(){
		return description;
	}
	
	/**
	 * get tags of the resource
	 * @return tags
	 */
	public String[] getTags(){
		return tags;
	}
	
	/**
	 * get uri of the resource
	 * @return uri
	 */
	public String getUri(){
		return uri;
	}
	
	/**
	 * get the channel name of the resource
	 * @return channel
	 */
	public String getChannel(){
		return channel;
	}
	
	/**
	 * get the owner name of the resource
	 * @return owner
	 */
	public String getOwner(){
		return owner;
	}
	
	/**
	 * get the ezserver that the resource is in
	 * @return ezserver
	 */
	public String getEzserver(){
		return ezserver;
	}
	
	/**
	 * create a JSON object of the resource
	 * @return object the json object of the resource class
	 */
	public JSONObject getJSON(){
		JSONObject inside = new JSONObject();
		inside.put("name", name);
		inside.put("description",description);
		
		//tags
		JSONArray arraytags = new JSONArray();
		for(int i = 0 ; i<tags.length;i++){
			arraytags.add(tags[i]);
		}
		inside.put("tags", arraytags );
		
		inside.put("uri", uri);
		inside.put("channel", channel);
		inside.put("owner", owner);
		inside.put("ezserver", ezserver);
		
		return inside;
		
	}
	
	/**
	 * check if a template matches the resource 
	 * @param template
	 * @return true if it matches, false otherwise
	 */
	public boolean match_template(Resource template){
		
		/*if(!template.getChannel().equals("")) {
			if(!template.getChannel().equals(channel)){
				return false;
			}
		}
		*/
		
		if(!template.getChannel().equals(channel)){
			return false;
		}
		if(!template.getOwner().equals("")){
			if(!template.getOwner().equals(owner)){
				return false;
			}
		}
		boolean found;
		if(template.getTags().length!= 0){
			for(int i = 0; i<template.getTags().length ;i++){
				found = false;
				for(int j = 0 ; j < tags.length;j++){
					if(template.getTags()[i].equals(tags[j])){
						found = true;
						break;
					}
				}
				if(!found){
					return false;
				}
			}
		}
		
		if(!template.getUri().equals("")){
			if(!template.getUri().equals(uri)){
				return false;
			}
		}
		
		if(!template.getName().equals("")){
			if( name.contains(template.getName())){
				return true;
			}
		}
		
		if(!template.getDescription().equals("")){
			if(description.contains(template.getDescription())){
				return true;
			}
		}
		
		if(template.getName().equals("")&&template.getName().equals("")){
			return true;
		}
		
		return false;
	}
	
	/**
	 * get a resource file with ezserver included
	 * @param hostname hostname of the server
	 * @param port port of the server
	 * @return resource new resource file
	 */
	public Resource getResourceWithServer(String hostname, int port){
		this.ezserver= hostname+":"+port;
		return this;
	}
	
	public Resource getStarOwner(){
		
		if(!this.owner.equals("")) {
			return new Resource(name, tags,description,uri,channel,"*",ezserver);
		} else {
			return this;
		}
		
	}
	
}
