package com.vk.vktestapp;

import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.util.VKUtil;

import static android.Manifest.permission.INTERNET;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {
    VKApi api;

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_INTERNET = 1;

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mLoginView;
    private EditText mPasswordView;
    ImageView imageRotate;
    int animationStatus = 0; // статус анимации 0-баунс, 1 - зум
    private VKAccessToken access_token;
    VKAPI vk;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // по идее надо проверять токен и пропускать окно с логином-паролем

        // показываем основную активность
        setContentView(R.layout.activity_main);

        // эта часть запускает часть из VK SDK, но я её не использовал
        new Application();

        // часть VK SDK, не использовал
        String[] fingerprints = VKUtil.getCertificateFingerprint(this, this.getPackageName());

        // при старте не показываем клавиатуру, осталось с проекта где нужно было вводить логин-пароль
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Мой велосипед-гуглеж по http апи вк, через HttpURLConnection и WebView для авторизации
        vk = new VKAPI(this.getApplicationContext());

        // старт непрерывной анимация для шевелонки пёсика
        bounceDogee();

        // назначаем слушателя кнопки входа/авторизации
        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        if (mSignInButton != null) {
            mSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    // крутим пёсика
                    animationStatus = 1;
                    // запускаю асинхронное задание, а то нельзя к сети из GUI обращаться
                    mAuthTask = new UserLoginTask();
                    mAuthTask.execute((Void) null);

                }
            });
        }
    }

    // циклическая анимация пёсика
    private void bounceDogee() {
        imageRotate = (ImageView) findViewById(R.id.vkDogeeImageView);
        if (imageRotate != null) {
            imageRotate.setVisibility(View.VISIBLE);
        }

        Animation anim = AnimationUtils.loadAnimation(this, R.anim.rotatebounce);

        anim.setAnimationListener(new Animation.AnimationListener() {

            // по окончанию цикла анимации перезапуск циклично
            @Override
            public void onAnimationEnd(Animation arg0) {
                Animation anim = AnimationUtils.loadAnimation(LoginActivity.this, R.anim.rotatebounce);
                // две предустановки анимации
                switch (animationStatus) {
                    case 0: //
                        break;
                    case 1: // анимация вращения при animationStatus = 1
                        anim = AnimationUtils.loadAnimation(LoginActivity.this, R.anim.spin);
                        break;

                    default: // по умолчанию - качание
                        anim = AnimationUtils.loadAnimation(LoginActivity.this, R.anim.rotatebounce);
                }

                anim.setAnimationListener(this);
                // перезапуск анимации с новым указанием анимации
                imageRotate.startAnimation(anim);
            }

            @Override
            public void onAnimationRepeat(Animation arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onAnimationStart(Animation arg0) {
                // TODO Auto-generated method stub

            }

        });

        imageRotate.startAnimation(anim);
    }

    private void populateAutoComplete() {
        if (!mayRequestInternet()) {
            return;
        }

    }

    // код под вопросом, не проверял
    private boolean mayRequestInternet() {
        // если андроид ниже 6 версии, то разрешения в манифесте при установке
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        // если проверка возвращает ОК
        if (checkSelfPermission(INTERNET) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        // окошко-запрос разрешения на доступ в интернеты эти ваши
        if (shouldShowRequestPermissionRationale(INTERNET)) {
            Snackbar.make(mLoginView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{INTERNET}, REQUEST_INTERNET);
                        }
                    });
        } else {
            requestPermissions(new String[]{INTERNET}, REQUEST_INTERNET);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_INTERNET) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            // результат проверки токена возвращается в виде булева в onPostExecute
            return vk.checkToken(vk.account.access_token, vk.account.user_id);
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            //showProgress(false);
            // если проверка токена прошла успешно
            if (success) {
                System.out.println("ok");
                // вот тут мы залогинились и можно вызывать список тем со стены и вообще что-то делать
            } else {
                // вызов авторизации в webView
                View webViewVK = findViewById(R.id.webView1);
                vk.VKAuth(webViewVK);
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
//            showProgress(false);
        }
    }

}

