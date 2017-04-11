import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

public class HelperFunction {
	
	public static boolean IsInteger(String s){
		if(s.matches("^[0-9]+$")){
			return true;
		}
		return false;
		
	}
	
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
		}
	}
	
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
			String string = s.replaceAll("\0", "");
			return string.trim();
		}
		else return null;
		
	}
}
