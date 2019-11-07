package com.example.lfapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity {

    ListView mListView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // URL с нашими JSON-данными
        String strUrl = "https://lifehacker.ru/api/wp/v2/posts";

        // определяем задачу по загрузке
        // и запускаем ее с нашим url
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute(strUrl);

        // ссылаемся на ListView в activity_main
        mListView = findViewById(R.id.lv_posts);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, Object> hm = (HashMap<String, Object>) parent.getAdapter().getItem(position);
                String link = (String) hm.get("link");

                Intent intent = new Intent(MainActivity.this, webviewActivity.class);
                intent.putExtra("link", link);
                startActivity(intent);
            }
        });
    }

    /**
     * метод загрузки данных из url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        try {
            URL url = new URL(strUrl);

            // Создаем http соединение, соединяемся и считываем данные
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();
        } catch (Exception e) {
            Log.d("Exception while downloading url", e.toString());
        } finally {
            iStream.close();
        }

        return data;
    }

    /**
     * Асинхронно скачиваем json
     */
    private class DownloadTask extends AsyncTask<String, Integer, String> {
        String data = null;

        @Override
        protected String doInBackground(String... url) {
            try {
                data = downloadUrl(url[0]);

            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            // закончили в non-ui
            ListViewLoaderTask listViewLoaderTask = new ListViewLoaderTask();

            // начинаем парсить
            listViewLoaderTask.execute(result);
        }
    }

    /**
     * Асинхронно парсим данные и кидаем в listView
     */
    private class ListViewLoaderTask extends AsyncTask<String, Void, SimpleAdapter> {
        JSONArray jObject;

        // парсим в non-ui
        @Override
        protected SimpleAdapter doInBackground(String... strJson) {
            try {
                jObject = new JSONArray(strJson[0]);
                PostsJSONParser postsJsonParser = new PostsJSONParser();
                postsJsonParser.parse(jObject);
            } catch (Exception e) {
                Log.d("JSON Exception1", e.toString());
            }

            // Инстанцируем класс парсера
            PostsJSONParser postsJsonParser = new PostsJSONParser();

            // Список для сохранения
            List<HashMap<String, Object>> posts = null;

            try {
                // Получаем спарсеные данные в List (наш список)
                posts = postsJsonParser.parse(jObject);
            } catch (Exception e) {
                Log.d("Exception", e.toString());
            }

            // Ключи, которые используем в hashMap
            String[] from = {"thumb", "title"};

            // и айдишники, используемые в listView
            int[] to = {R.id.iv_thumb, R.id.tv_description};

            // задаем адаптер
            // и закидываем ключи в айдишники
            SimpleAdapter adapter = new SimpleAdapter(getBaseContext(), posts, R.layout.lv_layout, from, to);

            return adapter;
        }

        /**
         * doInBackground выполнен - займемся картинками
         */
        @Override
        protected void onPostExecute(SimpleAdapter adapter) {
            // Задаем адаптер для listview
            mListView.setAdapter(adapter);

            for (int i = 0; i < adapter.getCount(); i++) {
                HashMap<String, Object> hm = (HashMap<String, Object>) adapter.getItem(i);
                String imgUrl = (String) hm.get("thumb_path");
                ImageLoaderTask imageLoaderTask = new ImageLoaderTask();

                hm.put("thumb_path", imgUrl);
                hm.put("position", i);

                // запускаем ImageLoaderTask для скачивания
                // и актуализации картинок в listview
                imageLoaderTask.execute(hm);
            }
        }
    }

    /**
     * Асинхронно качаем картинки и помещаем в listView
     */
    private class ImageLoaderTask extends AsyncTask<HashMap<String, Object>, Void, HashMap<String, Object>> {
        @Override
        protected HashMap<String, Object> doInBackground(HashMap<String, Object>... hm) {

            InputStream iStream;
            String imgUrl = (String) hm[0].get("thumb_path");
            int position = (Integer) hm[0].get("position");

            URL url;
            try {
                url = new URL(imgUrl);

                // создаем соединение и подключаемся
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();

                // считываем данные
                iStream = urlConnection.getInputStream();

                // директория кеширования
                File cacheDirectory = getBaseContext().getCacheDir();

                // временно сохраняем картинку в кеш-дир
                File tmpFile = new File(cacheDirectory.getPath() + "/ocpl_" + position + ".png");

                // поток в кеш-файл
                FileOutputStream fOutStream = new FileOutputStream(tmpFile);

                // из потока в картинку
                Bitmap b = BitmapFactory.decodeStream(iStream);

                // пишем файл в темп (png)
                b.compress(Bitmap.CompressFormat.PNG, 100, fOutStream);

                // сбрасываем и закрываем поток
                fOutStream.flush();
                fOutStream.close();

                // создаем hashMap для передачи картинки
                // в listview, в соответствии с позицией
                HashMap<String, Object> hmBitmap = new HashMap<String, Object>();

                // сохраняем путь к картинке
                // и позицию картинки в listview
                hmBitmap.put("thumb", tmpFile.getPath());
                hmBitmap.put("position", position);

                // возвращаем объект с картинкой и позицией
                return hmBitmap;

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(HashMap<String, Object> result) {
            // теперь получаем путь и позицию
            String path = (String) result.get("thumb");
            int position = (Integer) result.get("position");

            // задаем адаптер
            SimpleAdapter adapter = (SimpleAdapter) mListView.getAdapter();

            // забираем объекты из hashMap
            // с соответствующей позицией в listview
            HashMap<String, Object> hm = (HashMap<String, Object>) adapter.getItem(position);

            // заменяем текущий путь (сейчас "заглушка" - res/drawable/lfpng.png)
            hm.put("thumb", path);

            // и сообщаем listView об изенении содержимого
            adapter.notifyDataSetChanged();
        }
    }
}