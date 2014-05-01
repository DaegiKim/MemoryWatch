package controllers;

import models.chat.ChatRoom;
import org.codehaus.jackson.JsonNode;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import views.html.chat.chatRoom;
import views.html.chat.index;

public class Chat extends Controller {
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
