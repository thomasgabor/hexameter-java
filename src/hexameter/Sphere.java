package hexameter;

public interface Sphere extends Spherical {
	MessageProcessor build(MessageProcessor continuation, String direction);
}
