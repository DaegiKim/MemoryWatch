package controllers;

import com.mongodb.gridfs.GridFSDBFile;
import models.Media;
import play.Logger;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.html.home.index;
import views.html.home.media;
import views.html.home.upload;

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
        GridFSDBFile file = Media.getFile(id);

        return ok(file.getInputStream()).as(file.getContentType());
    }

    public static Result save() {
        Form<Media> filledForm = mediaForm.bindFromRequest();

        if(filledForm.hasErrors()) {
            return badRequest();
        } else {
            Http.MultipartFormData body = request().body().asMultipartFormData();
            Http.MultipartFormData.FilePart part = body.getFile("contents");

            if(part != null) {
                Media.save(filledForm.data().get("keyword"), part);
            }
        }

        return redirect(routes.Home.media());
    }

    public static Result delete(String id) {
        Logger.debug(id);
        Media.remove(id);

        return redirect(routes.Home.media());
    }
}
