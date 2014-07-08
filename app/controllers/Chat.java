package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import kr.co.shineware.nlp.komoran.core.MorphologyAnalyzer;
import models.chat.ChatRoom;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import services.NLTK;
import views.html.chat.chatRoom;
import views.html.chat.index;

public class Chat extends Controller {
    public static MorphologyAnalyzer analyzer = new MorphologyAnalyzer("data/");

    /**
     * Display the home page.
     */
    public static Result index() {
        return ok(index.render());
    }

    /**
     * Display the chat room.
     */
    public static Result chatRoom(String username) {
        Logger.debug("@@채팅방입장");

        return ok(chatRoom.render(username));
    }

    public static Result nltk(String text) {
        String result = NLTK.nltkRequest(text);

        return ok(Json.toJson(result));
    }

    /**
     * Handle the chat websocket.
     */
    public static WebSocket<JsonNode> chat(final String username) {
        Logger.debug("@@@채팅방입장");
        return new WebSocket<JsonNode>() {

            // Called when the Websocket Handshake is done.
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){

                // Join the chat room.
                try {
                    ChatRoom.join(username, in, out);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
    }
}
