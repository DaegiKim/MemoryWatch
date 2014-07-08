package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Media;
import models.Tweet;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;
import views.html.chat.partial.partial_contents;
import views.html.chat.partial.partial_tweets;
import views.html.home.index;
import views.html.home.media;
import views.html.home.upload;

import java.util.ArrayList;
import java.util.List;

public class Home extends Controller {
    static Form<Media> mediaForm = Form.form(Media.class);

    public static Result index() {
        return ok(index.render());
    }

    public static Result upload() {
        return ok(upload.render(mediaForm));
    }

    public static Result media() {
        return ok(media.render());
    }

    public static Result contents(String id) {
        final Media media = Media.findById(id);
        if(media == null) return notFound();
        return ok(media.contents);
    }

    public static Result partial_contents(String keyword) {
        List<Media> mediaList = Media.findByKeyword(keyword);
        return ok(partial_contents.render(mediaList));
    }

    public static Result partial_tweets(String keyword) {
        List<Tweet> tweets = new ArrayList<>();

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

        Query query = new Query(keyword);
        QueryResult queryResult = null;
        try {
            queryResult = twitter.search(query);
        } catch (TwitterException e) {
            e.printStackTrace();
        }

        for(twitter4j.Status status : queryResult.getTweets()) {
            Tweet tweet = new Tweet();
            tweet.setName(status.getUser().getScreenName());
            tweet.setText(status.getText());
            tweet.setCreatedAt(status.getCreatedAt().toString());
            tweet.setProfileURL(status.getUser().getProfileImageURL());

            MediaEntity[] mediaEntities = status.getMediaEntities();

            for(MediaEntity mediaEntity : mediaEntities) {
                play.Logger.debug(mediaEntity.getMediaURL());
            }

            tweets.add(tweet);
        }

        return ok(partial_tweets.render(tweets));
    }

    public static Result save() {
        Form<Media> filledForm = mediaForm.bindFromRequest();

        if(filledForm.hasErrors()) {
            return badRequest();
        } else {
            Http.MultipartFormData body = request().body().asMultipartFormData();
            Http.MultipartFormData.FilePart part = body.getFile("contents");

            if(part != null) {
                Media.createMedia(filledForm.get(), part.getFile());
            }
        }

        return redirect(routes.Home.media());
    }
}
