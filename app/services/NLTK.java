package services;

import play.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class NLTK {
    public static String nltkRequest(String value) {
        HttpURLConnection hConnection = null;
        PrintStream ps = null;
        InputStream is = null;
        BufferedReader in = null;

        try{
            URL url = new URL("http://text-processing.com/api/sentiment/");
            hConnection = (HttpURLConnection)url.openConnection();
            hConnection.setInstanceFollowRedirects(false);
            hConnection.setDoOutput(true);
            hConnection.setRequestMethod("POST");
            ps = new PrintStream(hConnection.getOutputStream());

            //인코딩된 uid, pwd 로 문자열 replace
            ps.print(String.format("text=\"%s\"", value));

            if((is = hConnection.getInputStream()) != null)
            {
                in = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
                String readLine;

                while((readLine=in.readLine()) != null)
                {
                    return readLine;
                }
            }
            else {
                return null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            //스트림 닫기
            hConnection.disconnect();
            ps.close();
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
        return null;
    }
}
