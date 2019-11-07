package com.example.lfapp;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** класс парсера JSON данных */
public class PostsJSONParser {

    // Получаем JSONObject и возвращаем List
    public List<HashMap<String, Object>> parse(JSONArray jObject) {

        // применяем getPosts к массиву объекта JSON
        // теперь каждый объект - это товар
        return getPosts(jObject);
    }

    private List<HashMap<String, Object>> getPosts(JSONArray jPosts) {
        int postCount = jPosts.length();

        List<HashMap<String, Object>> postList = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> post;

        // разбираем товары по одному и добавляем к объекту List
        for (int i = 0; i < postCount; i++) {
            try {
                // вызываем getPost и парсим, добавляем
                post = getPost((JSONObject) jPosts.get(i));

                postList.add(post);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return postList;
    }

    // Разбираем JSON-объект post
    private HashMap<String, Object> getPost(JSONObject jPost) {

        HashMap<String, Object> post = new HashMap<String, Object>();

        String thumb_path;
        String title;
        String link;

        try {
            title = jPost.getJSONObject("title").getString("rendered");
            thumb_path = jPost.getJSONObject("cat_cover").getJSONObject("sizes").getString("mobile");
            link = jPost.getString("link");

            if(thumb_path.equals("null")) {
                //pаглушка если у поста нет картинки
                thumb_path = "https://lifehacker.ru/wp-content/uploads/2017/08/Cover_Books_2_1502885178-310x155.jpg";
            }

            post.put("thumb", R.drawable.lfpng); //заглушка пока не загрузились картинки
            post.put("title", title);
            post.put("thumb_path", thumb_path);
            post.put("link", link);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return post;
    }
}