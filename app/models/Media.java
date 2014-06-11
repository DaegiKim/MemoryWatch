package models;

import com.google.common.io.Files;
import com.mongodb.BasicDBObject;
import net.vz.mongodb.jackson.JacksonDBCollection;
import net.vz.mongodb.jackson.ObjectId;
import net.vz.mongodb.jackson.Id;
import play.modules.mongodb.jackson.MongoDB;
import play.mvc.Result;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static play.mvc.Results.ok;

public class Media {
    private static JacksonDBCollection<Media, String> coll = MongoDB.getCollection("media", Media.class, String.class);

    @Id
    @ObjectId
    public String id;              //MongoDB Object ID
    public String keyword;         //키워드
    public byte[] contents;        //컨텐츠

    public static Media createMedia(Media m, File f) {
        Media media = new Media();
        media.keyword = m.keyword;
        try {
            media.contents = Files.toByteArray(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Media.coll.save(media);

        return media;
    }

    public static List<Media> all() {
        return Media.coll.find().toArray();
    }

    public static Media findById(String id) {
        Media media = Media.coll.findOneById(id);
        return media;
    }
}
