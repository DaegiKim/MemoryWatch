package models.chat;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.Chat;
import controllers.Home;
import controllers.routes;
import kr.co.shineware.util.common.model.Pair;
import models.Media;
import play.Logger;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.mvc.WebSocket;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import services.Komoran;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static akka.pattern.Patterns.ask;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A chat room is an Actor.
 */
public class ChatRoom extends UntypedActor {

    //채팅방
    static ActorRef defaultRoom = Akka.system().actorOf(new Props(ChatRoom.class));

    /**
     * 채팅방 입장
     */
    public static void join(final String username, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception{
        // Send the Join message to the room
        String result = (String)Await.result(ask(defaultRoom,new Join(username, out), 1000), Duration.create(1, SECONDS));

        if("OK".equals(result)) {

            // For each event received on the socket,
            in.onMessage(new F.Callback<JsonNode>() {
                public void invoke(JsonNode event) {

                    if(event.has("to")) {
                        defaultRoom.tell(new Message(event.get("to").asText(), username, event.get("text").asText()), null);
                    }
                    else {
                        // Send a Talk message to the room.
                        defaultRoom.tell(new Talk(username, event.get("text").asText()), null);
                    }

                }
            });

            // When the socket is closed.
            in.onClose(new F.Callback0() {
                public void invoke() {

                    // Send a Quit message to the room.
                    defaultRoom.tell(new Quit(username), null);

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
                getSender().tell("이미 사용중인 닉네임입니다", getSelf());
            } else {
                members.put(join.username, join.channel);
                notifyAll("join", join.username, "입장하였습니다");
                getSender().tell("OK", getSelf());
            }

        } else if(message instanceof Talk)  {

            // Received a Talk message
            Talk talk = (Talk)message;

            notifyAll("talk", talk.username, talk.text);
            response(talk);

        } else if(message instanceof Message) {

            Message m = (Message)message;
            notifyMessage(m);

        } else if(message instanceof Quit)  {

            // Received a Quit message
            Quit quit = (Quit)message;

            members.remove(quit.username);

            notifyAll("quit", quit.username, "퇴장하였습니다");

        } else {
            unhandled(message);
        }

    }

    private void response(Talk talk) {
        Pair<List<Media>, Map<String, String>> response = Media.findByTalk(talk);

        for(WebSocket.Out<JsonNode> channel: members.values()) {
            ObjectNode event = Json.newObject();
            event.put("kind", "response");

            ArrayNode mediaArrayNode = event.putArray("media");
            for(Media m : response.getFirst()) {
                ObjectNode media = Json.newObject();
                media.put("keyword", m.keyword);
                media.put("type", m.type);
                media.put("url", routes.Home.contents(m.id).url());

                mediaArrayNode.add(media);
            }
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

    public void notifyMessage(Message message) {
        WebSocket.Out<JsonNode> jsonNodeOut = members.get(message.to);

        ObjectNode event = Json.newObject();
        event.put("to", message.to);
        event.put("from", message.from);
        event.put("text", message.text);

        jsonNodeOut.write(event);
    }

        /**
         * 채팅방 입장
         */
    public static class Join {

        final String username;
        final WebSocket.Out<JsonNode> channel;

        public Join(String username, WebSocket.Out<JsonNode> channel) {
            this.username = username;
            this.channel = channel;
        }

    }

    /**
     * 대화 메시지
     */
    public static class Talk {

        public final String username;
        public final String text;

        public Talk(String username, String text) {
            this.username = username;
            this.text = text;
        }

    }

    /**
     * 채팅방 퇴장
     */
    public static class Quit {

        final String username;

        public Quit(String username) {
            this.username = username;
        }

    }

    public static class Message {
        final String to;
        final String from;
        final String text;

        public Message(String to, String from, String text) {
            this.to = to;
            this.from = from;
            this.text = text;
        }
    }

}
