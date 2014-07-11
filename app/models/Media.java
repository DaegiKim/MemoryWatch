package models;

import com.google.common.io.Files;
import com.mongodb.BasicDBObject;
import controllers.Chat;
import kr.co.shineware.util.common.model.Pair;
import models.chat.ChatRoom;
import net.vz.mongodb.jackson.DBCursor;
import net.vz.mongodb.jackson.JacksonDBCollection;
import net.vz.mongodb.jackson.ObjectId;
import net.vz.mongodb.jackson.Id;
import play.Logger;
import play.modules.mongodb.jackson.MongoDB;
import play.mvc.Result;
import services.Komoran;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import static play.mvc.Results.ok;

public class Media {
    private static JacksonDBCollection<Media, String> coll = MongoDB.getCollection("media", Media.class, String.class);

    @Id
    @ObjectId
    public String id;              //MongoDB Object ID
    public String keyword;         //키워드
    public String type;         //키워드
    public byte[] contents;        //컨텐츠

    public static Media createMedia(Media m, File f) {
        Media media = new Media();
        media.keyword = m.keyword;

        try {
            String type = f.toURI().toURL().openConnection().getContentType();
            media.type = type;
            Logger.debug(type);
        } catch (IOException e) {
            e.printStackTrace();
        }

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

    public static Pair<List<Media>, Map<String, String>> findByTalk(ChatRoom.Talk talk) {
        Map<String, String> map = Komoran.analysis(talk);

        Map<String, Media> mediaHashMap = new HashMap<>();
        List<Media> mediaList = new ArrayList<>();

        for (String word : map.keySet()) {
            //각 단어로 미디어를 찾는다

            List<Media> list = findByKeyword(word);
            for(Media m : list) {
                mediaHashMap.put(m.id, m);
            }
        }

        //중복 제거
        mediaList.addAll(mediaHashMap.values());

        return new Pair<>(mediaList, map);
    }

    public static List<Media> findByKeyword(String keyword) {
        //db.coll.find({a:{$in:[1,2,3,5]})

        DBCursor<Media> cursor = Media.coll.find(new BasicDBObject("keyword", Pattern.compile(keyword)));
        return cursor.toArray();
    }
}
