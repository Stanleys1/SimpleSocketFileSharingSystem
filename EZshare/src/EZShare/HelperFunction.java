package EZShare;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

/**
 * HelperFunction class that provide useful function
 * to be used on other classes
 * test pull
 *
 */
public class HelperFunction {
	
	/**
	 * check if the string is integer
	 * @param s string
	 * @return true if integer, false if not integer
	 */
	public static boolean IsInteger(String s){
		if(s.matches("^[0-9]+$")){
			return true;
		}
		return false;
		
	}
	
	/**
	 * check if the string is an ip address
	 * @param s
	 * @return
	 */
	public static boolean IsIP(String s){
		
		try {
			InetAddress.getByName(s);
			return true;
		} catch (UnknownHostException e) {
			return false;
		}
		
		
	}
	
	/**
	 * check if it is valid uri
	 * @param string
	 * @return true or false
	 */
	public static boolean isURI(String s){
		try {
			URI uri = new URI(s);
			return uri.isAbsolute();
		} catch (URISyntaxException e) {
			return false;
		}
	}
	
	/**
	 * check if the string is a file scheme
	 * @param s
	 * @return true if it is a file scheme, false otherwise
	 */
	public static boolean isFileScheme(String s){
		try {
			URI u = new URI(s);
			//boolean isWeb = "http".equalsIgnoreCase(u.getScheme())
			   // || "https".equalsIgnoreCase(u.getScheme());
			File file = new File(u); 
			return file.exists();
			
			//return !isWeb;
		} catch (URISyntaxException e) {
			return false;
		} catch (IllegalArgumentException e){
			return false;
		}
	}
	/**
	 * check if the string is a file name,beginning with 'file'
	 * @param s
	 * @return true if it is a file name, false otherwise
	 */
	public static boolean isFileName(String s){
		try {
			File file = new File(s); 
			if(s.substring(0, 4).toLowerCase().equals("file")){
				return true;
			}
			return false;
			//return !isWeb;
		} catch (Exception e){
			return false;
		}
	}
	
	/**
	 * get the filesize of a URI file scheme string
	 * @param s 
	 * @return the file size length
	 */
	public static long fileSize(String s){
		try {
			URI u = new URI(s);
			//boolean isWeb = "http".equalsIgnoreCase(u.getScheme())
			   // || "https".equalsIgnoreCase(u.getScheme());
			File file = new File(u); 
			return file.length();
			
			//return !isWeb;
		} catch (URISyntaxException e) {
			return 0;
		}
		
		
	}
	
	/**
	 * all string must pass through this function
	 * @param s string
	 * @return trimmed handled string
	 */
	public static String handleString(String s){
		
		if(s != null){
			String string = s.replace("\\0", "");
			return string.trim();
		}
		else return null;
		
	}
	  /**
		 * get the directory for fetching file to client  
		 * @param String r- uri in argument
		 * @return the directory where the downloaded file is stored
		 */
	public static String getFileName(String f) {
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
