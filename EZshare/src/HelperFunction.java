import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

/**
 * HelperFunction class that provide useful function
 * to be used on other classes
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
		if(s.equals("*")){
			s = "";
			return s;
		}
		if(s != null){
			String string = s.replace("\\0", "");
			return string.trim();
		}
		else return null;
		
	}
	

}
