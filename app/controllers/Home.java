package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.home.index;
import views.html.home.upload;

public class Home extends Controller {
    public static Result index() {
        return ok(index.render());
    }

    public static Result upload() {
        return ok(upload.render());
    }
}
