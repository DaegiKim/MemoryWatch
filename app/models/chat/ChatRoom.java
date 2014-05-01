package models.chat;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.mvc.WebSocket;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.Map;

import static akka.pattern.Patterns.ask;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A chat room is an Actor.
 */
public class ChatRoom extends UntypedActor {

    //채팅방
    static ActorRef defaultRoom = Akka.system().actorOf(new Props(ChatRoom.class));

    //로봇 생성
    static {
        //new Robot(defaultRoom);
    }

    /**
     * Join the default room.
     */
    public static void join(final String username, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception{
        // Send the Join message to the room
        String result = (String)Await.result(ask(defaultRoom,new Join(username, out), 1000), Duration.create(1, SECONDS));

        if("OK".equals(result)) {

            // For each event received on the socket,
            in.onMessage(new F.Callback<JsonNode>() {
                public void invoke(JsonNode event) {

                    // Send a Talk message to the room.
                    defaultRoom.tell(new Talk(username, event.get("text").asText()));

                }
            });

            // When the socket is closed.
            in.onClose(new F.Callback0() {
                public void invoke() {

                    // Send a Quit message to the room.
                    defaultRoom.tell(new Quit(username));

                }
            });

        } else {

            // Cannot connect, create a Json error.
            ObjectNode error = Json.newObject();
            error.put("error", result);

            // Send the error to the socket.
            out.write(error);

        }

    }

    // Members of this room.
    Map<String, WebSocket.Out<JsonNode>> members = new HashMap<String, WebSocket.Out<JsonNode>>();

    public void onReceive(Object message) throws Exception {

        if(message instanceof Join) {

            // Received a Join message
            Join join = (Join)message;

            // Check if this username is free.
            if(members.containsKey(join.username)) {
                getSender().tell("이미 사용중인 닉네임입니다");
            } else {
                members.put(join.username, join.channel);
                notifyAll("join", join.username, "입장하였습니다");
                getSender().tell("OK");
            }

        } else if(message instanceof Talk)  {

            // Received a Talk message
            Talk talk = (Talk)message;

            notifyAll("talk", talk.username, talk.text);
            analysisMessage(talk);

        } else if(message instanceof Quit)  {

            // Received a Quit message
            Quit quit = (Quit)message;

            members.remove(quit.username);

            notifyAll("quit", quit.username, "퇴장하였습니다");

        } else {
            unhandled(message);
        }

    }

    private void analysisMessage(Talk talk) {
        String message = "";

        if(talk.text.contains("소녀시대") && talk.text.contains("콘서트")) {
            message = "girlsgeneration1";
        }
        else if(talk.text.contains("이소라") && talk.text.contains("콘서트")) {
            message = "leesora1";
        }
        else if(talk.text.contains("꽃보다") && talk.text.contains("할배")) {
            message = "flower1";
        }
        else if(talk.text.contains("싸이") && talk.text.contains("강남스타일")) {
            message = "psy1";
        }
        else {
            return;
        }

        for(WebSocket.Out<JsonNode> channel: members.values()) {
            ObjectNode event = Json.newObject();
            event.put("kind", "alert");
            event.put("user", talk.username);
            event.put("message", message);

            channel.write(event);
        }
    }

    // Send a Json event to all members
    public void notifyAll(String kind, String user, String text) {
        for(WebSocket.Out<JsonNode> channel: members.values()) {

            ObjectNode event = Json.newObject();
            event.put("kind", kind);
            event.put("user", user);
            event.put("message", text);

            ArrayNode m = event.putArray("members");
            for(String u: members.keySet()) {
                m.add(u);
            }

            channel.write(event);
        }
    }

    public static class Join {

        final String username;
        final WebSocket.Out<JsonNode> channel;

        public Join(String username, WebSocket.Out<JsonNode> channel) {
            this.username = username;
            this.channel = channel;
        }

    }

    public static class Talk {

        final String username;
        final String text;

        public Talk(String username, String text) {
            this.username = username;
            this.text = text;
        }

    }

    public static class Quit {

        final String username;

        public Quit(String username) {
            this.username = username;
        }

    }

}
