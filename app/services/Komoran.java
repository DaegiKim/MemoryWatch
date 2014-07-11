package services;

import kr.co.shineware.nlp.komoran.core.MorphologyAnalyzer;
import kr.co.shineware.util.common.model.Pair;
import models.chat.ChatRoom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Komoran {
    public static MorphologyAnalyzer analyzer = new MorphologyAnalyzer("data/");

    public static Map<String, String> analysis(ChatRoom.Talk talk) {
        Map<String, String> map = new HashMap<>();

        List<List<Pair<String,String>>> result = analyzer.analyze(talk.text);


        for (List<Pair<String, String>> eojeolResult : result) {
            for (Pair<String, String> wordMorph : eojeolResult) {
                map.put(wordMorph.getFirst(), wordMorph.getSecond());
            }
        }

        return map;
    }
}
