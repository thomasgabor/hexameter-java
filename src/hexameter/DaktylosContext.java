package hexameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.zeromq.ZMQ;
import org.json.simple.*;

public class DaktylosContext {

	protected int recvtries  = 100000; //magic number achieved through tests
	protected int defaultport = 55555;
	protected int socketcache = 10;

	protected String me;
	protected MessageProcessor processor;
	protected ZMQ.Context context;
	protected ZMQ.Socket respondingSocket;
	protected HashMap<String, ZMQ.Socket> talkingSockets = new HashMap<String, ZMQ.Socket>();
	protected ArrayList<String> recents = new ArrayList<String>();
	
	
	protected ZMQ.Socket getSocket(String target) {
		if ( this.socketcache > 0 ) {
			if ( this.talkingSockets.get("target") != null ) {
				return this.talkingSockets.get("target");
			}
			if ( this.recents.size() == this.socketcache ) {
				String oldestTarget = this.recents.get(0);
				if ( this.talkingSockets.containsKey(oldestTarget) ) {
					this.talkingSockets.get(oldestTarget).close();
					this.talkingSockets.remove(oldestTarget);
				}
				this.recents.remove(0);
			}
		}
		ZMQ.Socket socket = this.context.socket(ZMQ.DEALER);
		socket.connect(target);
		if ( this.socketcache > 0 && this.recents.size() < this.socketcache ) {
			this.talkingSockets.put(target, socket);
			this.recents.add(target);
		}
		return socket;
	}
	
	protected boolean multisend(ZMQ.Socket socket, String[] frames) {
		boolean lastSuccess = false;
		for ( int i = 0; i < frames.length; i++ ) {
			if ( i == frames.length - 1 ) {
				lastSuccess = socket.send(frames[i]);
			} else {
				lastSuccess = socket.sendMore(frames[i]);
			}
		}
		return lastSuccess;
	}
	
	protected List<String> multirecv(ZMQ.Socket socket, int recvOptions) {
		List<String> frames = new ArrayList<String>();
		frames.add(new String(socket.recv(recvOptions)));
		while ( socket.hasReceiveMore() ) {
			frames.add(new String(socket.recv()));
		};
		return frames;
	}
	
	protected List<String> multirecv(ZMQ.Socket socket) {
		return this.multirecv(socket, 0);
	}
	
	public DaktylosContext init(String name, MessageProcessor processor) {
		this.me = name;
		int port = name.matches("^(.*):(.*)$") ? new Integer(name.replaceAll("^(.*):", "")) : this.defaultport;
		this.processor = processor;
		this.context = ZMQ.context(1);
		this.respondingSocket = this.context.socket(ZMQ.ROUTER);
		this.respondingSocket.bind("tcp://*:" + Integer.toString(port));
		return this;
	}
	
	public DaktylosContext init(int port, MessageProcessor processor) {
		return this.init("localhost:" + Integer.toString(port), processor);
	}
	
	public void term() {
		this.respondingSocket.close();
		this.context.term();
	}
	
	public String me() {
		return this.me;
	}
	
	@SuppressWarnings("unchecked")
	public boolean message(String type, String recipient, String space, JSONArray parameter) {
		JSONObject obj = new JSONObject();
		obj.put("author", this.me());
		obj.put("recipient", recipient);
		obj.put("type", type);
		obj.put("space", space);
		obj.put("parameter", parameter);
		String msg = "json\n\n" + obj.toJSONString();
	    ZMQ.Socket socket = this.getSocket("tcp://" + recipient);
	    String[] frames = {"", msg};
	    return this.multisend(socket, frames);
	}
	
	public boolean respond(int tries) {
		@SuppressWarnings("unused")
		String src, del, msg = null;
		if ( tries == 0 ) {
			List<String> frames = this.multirecv(this.respondingSocket);
			if ( frames.size() >= 3 ) {
				src = frames.get(0);
				del = frames.get(1);
				msg = frames.get(2);
			}
		} else {
			for ( int i = 0; msg == null && i < tries ; i++ ) {
				List<String> frames = this.multirecv(this.respondingSocket, ZMQ.NOBLOCK);
				if ( frames.size() >= 3 ) {
					src = frames.get(0);
					del = frames.get(1);
					msg = frames.get(2);
				}
			}
		}
		if ( msg != null ) {
			Object object = JSONValue.parse(msg.replaceAll("^json\n\n", ""));
			JSONObject message = (JSONObject) object;
			JSONArray response = this.processor.process(
					(String) message.get("type"),
					(String) message.get("author"),
					(String) message.get("space"),
					(JSONArray) message.get("parameter"),
					(String) message.get("recipient")
			);
			if ( response != null ) {
				return this.message(
						"ack",
						(String) message.get("author"),
						(String) message.get("space"),
						response
					);
			}
			return true;
		} else {
			return false;
		}
	}
	
	public boolean respond() {
		return this.respond(this.recvtries);
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		//System.out.println(System.getProperty("java.class.path"));
		DaktylosContext context = new DaktylosContext().init(
				"localhost:88888",
				(type, author, space, parameter, recipient) -> {
				System.out.println("got " + type + "@" + space + " from " + author);
				JSONObject responseItem = new JSONObject();
				responseItem.put("a", 42);
				JSONArray response = new JSONArray();
				response.add(responseItem);
				return response;
		});
		context.respond(0);
	}

}
