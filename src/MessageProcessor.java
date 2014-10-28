import org.json.simple.JSONArray;


public interface MessageProcessor {
	JSONArray process(String type, String author, String space, JSONArray parameter, String recipient);
}
