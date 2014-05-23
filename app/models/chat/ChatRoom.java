package models.chat;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import controllers.Chat;
import kr.co.shineware.nlp.komoran.core.MorphologyAnalyzer;
import kr.co.shineware.util.common.model.Pair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import play.Logger;
import play.libs.Akka;
import play.libs.F;
import play.libs.Json;
import play.mvc.WebSocket;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
            komoran(talk);
            twitter(talk);

        } else if(message instanceof Quit)  {

            // Received a Quit message
            Quit quit = (Quit)message;

            members.remove(quit.username);

            notifyAll("quit", quit.username, "퇴장하였습니다");

        } else {
            unhandled(message);
        }

    }

    private void twitter(Talk talk) {
        List<String> twits = new ArrayList<String>();

        ObjectNode event = Json.newObject();
        event.put("kind", "twitter");

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey("1tj6RfxgtAeMRXJBPVqObA")
                .setOAuthConsumerSecret("jyj5jbVtj2yThy6lk5UbD7DpOXs1NLGxyju5rOn2DY")
                .setOAuthAccessToken("1689174836-UQ3T7Gb5lJVlYlAVxLPl6H8t4jgD3c97DfBwLJj")
                .setOAuthAccessTokenSecret("SVmUs1lBElCwR3NahWIhP0iOkhIeT4xqGTAuNYhYgoU");
        TwitterFactory tf = new TwitterFactory(cb.build());
        Twitter twitter = tf.getInstance();

        Query query = new Query(talk.text);
        QueryResult queryResult = null;
        try {
            queryResult = twitter.search(query);
        } catch (TwitterException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        for (Status status : queryResult.getTweets()) {
            Logger.debug("@" + status.getUser().getScreenName() + ":" + status.getText());
            twits.add("@" + status.getUser().getScreenName() + ":" + status.getText());
        }

        for(WebSocket.Out<JsonNode> channel: members.values()) {
            channel.write(event);
            event.put("message", "@" + twits);
        }
    }

    private void komoran(Talk talk) {
        String text = "";
        List<List<Pair<String,String>>> result = Chat.analyzer.analyze(talk.text);

        for (List<Pair<String, String>> eojeolResult : result) {
            for (Pair<String, String> wordMorph : eojeolResult) {
                Logger.debug(wordMorph.toString());
                text+=wordMorph.toString();
            }
        }

        for(WebSocket.Out<JsonNode> channel: members.values()) {
            ObjectNode event = Json.newObject();
            event.put("kind", "komoran");
            event.put("message", text);

            channel.write(event);
        }
    }

    private void analysisMessage(Talk talk) {
        String message = "";
        Boolean video = false;

        if(talk.text.contains("아르바이트")) {
            message = "part-time-job";
        }
        else if(talk.text.contains("랄라스윗") || talk.text.contains("제이래빗") || talk.text.contains("소란")) {
            message = "singer";
        }
        else if(talk.text.contains("미국")) {
            message = "usa-1";
        }
        else if(talk.text.contains("뉴욕") && talk.text.contains("LA")) {
            message = "usa-2";
        }
        else if(talk.text.contains("소녀시대") && talk.text.contains("콘서트")) {
            message = "girls-generation";
            video = true;
        }
        else if(talk.text.contains("워싱턴 DC") && talk.text.contains("백악관")) {
            message = "usa-3";
        }
        else if(talk.text.contains("유럽") && talk.text.contains("15박 16일")) {
            message = "europe-1";
        }
        else if(talk.text.contains("대관람차") && talk.text.contains("빅벤")) {
            message = "europe-2";
        }
        else if(talk.text.contains("오스트리아") && talk.text.contains("쉔부른")) {
            message = "europe-3";
        }
        else if(talk.text.contains("프랑스") && talk.text.contains("루브르")) {
            message = "europe-4";
        }
        else if(talk.text.contains("스위스") && talk.text.contains("융프라우")) {
            message = "europe-5";
        }
        else if(talk.text.contains("코스북")) {
            message = "europe-6";
        }
        else {
            return;
        }

        for(WebSocket.Out<JsonNode> channel: members.values()) {
            ObjectNode event = Json.newObject();
            event.put("kind", "alert");
            event.put("user", talk.username);
            event.put("message", message);
            event.put("video", video);

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
