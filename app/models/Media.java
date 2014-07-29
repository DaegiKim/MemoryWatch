package models;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import kr.co.shineware.util.common.model.Pair;
import models.chat.ChatRoom;
import net.vz.mongodb.jackson.JacksonDBCollection;
import net.vz.mongodb.jackson.ObjectId;
import net.vz.mongodb.jackson.Id;
import play.Logger;
import play.modules.mongodb.jackson.MongoDB;
import play.mvc.Http;
import services.Komoran;

import java.io.IOException;
import java.util.*;

public class Media {
    private static JacksonDBCollection<Media, String> coll = MongoDB.getCollection("media", Media.class, String.class);

    @Id
    @ObjectId
    public String id;               //MongoDB Object ID
    public List<String> keyword;    //키워드
    public String type;             //키워드

    public static boolean save(String keyword, Http.MultipartFormData.FilePart filePart) {
        DB db = coll.getDB();

        GridFS gridfs = new GridFS(db, "media");
        GridFSInputFile gfsFile = null;
        try {
            gfsFile = gridfs.createFile(filePart.getFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

        gfsFile.setFilename(filePart.getFilename());
        gfsFile.setMetaData(new BasicDBObject("tags", keyword.replace(" ", "").split(",")));

        String type = filePart.getContentType();
        gfsFile.setContentType(type);

        gfsFile.save();

        return true;
    }

    public static boolean remove(String id) {
        DB db = coll.getDB();

        GridFS gridfs = new GridFS(db, "media");
        gridfs.remove(new org.bson.types.ObjectId(id));

        return true;
    }

    public static List<Media> all() {
        List<Media> mediaList = new ArrayList<>();

        GridFS gfsPhoto = new GridFS(coll.getDB(), "media");
        com.mongodb.DBCursor fileList = gfsPhoto.getFileList();

        for(DBObject object : fileList) {
            Media media = new Media();
            media.id = object.get("_id").toString();
            DBObject metadata = (DBObject) object.get("metadata");
            media.keyword = (List<String>) metadata.get("tags");
            media.type = object.get("contentType").toString();

            mediaList.add(media);
        }
        return mediaList;
    }

    public static GridFSDBFile getFile(String id) {
        GridFS gfs = new GridFS(coll.getDB(), "media");
        GridFSDBFile file = gfs.find(new org.bson.types.ObjectId(id));

        return file;
    }

    public static Pair<List<Media>, Map<String, String>> findByTalk(ChatRoom.Talk talk) {
        Map<String, String> map = Komoran.analysis(talk);
        Logger.debug(map.toString());

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
        List<Media> mediaList = new ArrayList<>();
        //db.coll.find({a:{$in:[1,2,3,5]})
        GridFS gfs = new GridFS(coll.getDB(), "media");
        List<GridFSDBFile> gridFSDBFiles = gfs.find(new BasicDBObject("metadata.tags", keyword));


        for(GridFSDBFile gridFSDBFile : gridFSDBFiles) {
            Media media = new Media();
            media.id = gridFSDBFile.getId().toString();
            DBObject metadata = (DBObject) gridFSDBFile.get("metadata");
            media.keyword = (List<String>) metadata.get("tags");
            media.type = gridFSDBFile.getContentType();

            mediaList.add(media);
        }

        return mediaList;
    }
}
