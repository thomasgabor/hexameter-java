package hexameter;
import org.json.simple.JSONArray;


public interface MessageSink {
	boolean process(String type, String author, String space, JSONArray parameter, String recipient);
}
