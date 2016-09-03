package com.vk.vktestapp;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class VKAPI {
    // accoint содержит access_token и user_id,
    // так же два метода сохранить и загрузить идентификатор в SharedPreferences
    Account account = new Account();

    // в конструкторе восстанавливаем параметры access_token и user_id
    public VKAPI(Context context) {
        account.restore(context);
       // System.out.println(account.access_token + " " + account.user_id);
    }

    // Проверка токена выполняется по запросу информации профиля залогиненного юзера
    // возвращает
    // true - ok
    // false - fail, по коду ошибки error_code
    // метод нужно переделать под универсальный, посылать в него имя VK API и парсить ответ в JSON
    boolean checkToken(String accessToken, long user_id) {
        // инициализируем статус http запроса чем-нибудь
        Integer status = -1;

        // код http ответа 200 = ок
        final int STATUS_OK = 200;

        StringBuffer response = null;
        try {
            response = new StringBuffer("");
// пример формирования строки запроса
//            String urlString = "https://api.vk.com/method/newsfeed.get?" +
//                    "access_token=88fb6bdc45a153385f7a29e8d730dff2f31276135d01f799915f3d7123429bac64303aa5e33ec5747e8d3" +
//                    "start_from=0&" +
//                    "count=2&" +
//                    "display=page&" +
//                    "version=5.53";
//
            // строка запроса по https
            String urlString = "https://api.vk.com/method/account.getProfileInfo?" +
                    "access_token=" + accessToken + "&" +
                    "user_id=" + user_id + "&" +
                    "v=5.53";

            try {
                // старый доступ через HttpResponse указан как deprecated, поэтому на HttpURLConnection
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoInput(true);
                connection.connect();

                InputStream inputStream = connection.getInputStream();
                status = connection.getResponseCode();

                // если ответ пришел нормальный, то читаем ответ в response = new StringBuffer("");

                if (status == STATUS_OK) {
                    String line = "";
                    BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
                    while ((line = rd.readLine()) != null) {
                        response.append(line);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();

        }
   //     System.out.println(response);
  //      System.out.println(status);
        String stringResponse = response.toString();

        // если в ответе есть строка error_code, то токен неправильный
        // здесь же можно вынуть код ответа от ВК, у них всё коды ответа приведены
        if (status == STATUS_OK && stringResponse.contains("error_code")) {
            return false;
        } else {
            return true;
        }
    }

    // ВК авторизация через webView, передаю view на отображение веб-формы авторизации webView
    void VKAuth(View view) {
        WebView wv = (WebView) view;
        WebSettings webSettings = wv.getSettings();
        // включаем обработку ява-скриптов
        webSettings.setJavaScriptEnabled(true);
        // чистим кеш
        wv.clearCache(true);
        // отключаем горизонтальный скролл-бар, бо некрасиво
        wv.setHorizontalScrollBarEnabled(false);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        // обработчик события окончания загрузки и затем парсинг
        wv.setWebViewClient(new VkWebViewClient());
        // всё на константах, по идее нужно разнести по переменным
        // client_id - моя зареганная прога в вк, как standalone приложение
        // доступ к wall,offline,friends
        // в ответ приходит токен
        wv.loadUrl("https://oauth.vk.com/authorize?" +
                "client_id=5602791&" +
                "scope=wall,offline,friends&" +
                "redirect_uri=https://oauth.vk.com/blank.html&" +
                "display=mobile&" +
                "v=5.53&" +
                "response_type=token&" +
                "revoke=1");
    }

    public class VkWebViewClient extends WebViewClient {
        public VkWebViewClient() {
        }

        // user_id и access_token приходят в ответ авторизации через webView
        @Override
        public void onPageFinished(WebView view, String url) {
            Log.d("onPageFinished", url);
            if (url.contains("oauth.vk.com/blank.html#")) {
                if (url.contains("error")) {
                    // Error
                } else {
                    String accessTokenUserID = url.substring(url.indexOf("#") + 1);
                    String accessToken = accessTokenUserID.substring(accessTokenUserID.indexOf('=') + 1, accessTokenUserID.indexOf('&'));

                    // как-то замороченно выделяю access_token и user_id
                    String userID = accessTokenUserID.substring(accessTokenUserID.indexOf('&') + 1);
                    userID = userID.substring(userID.indexOf("user_id"));
                    userID = userID.substring(userID.indexOf('=') + 1);


                    account.access_token = accessToken;
                    account.user_id = Long.parseLong(userID);
                    // сохраняю в sharedPreferences
                    account.save(view.getContext());

                //    System.out.println(accessToken + " " + userID);
                }
            }
        }
    }

}

