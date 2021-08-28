package colocoloc.megafonremainders;

import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.Telephony;
import android.support.v7.app.NotificationCompat;
import android.telephony.SmsMessage;
import java.text.DateFormat;

import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.json.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class mainActivity extends Activity implements DialogCloseListener {

    InterstitialAd mInterstitialAd;
    private boolean rememberData;
    private String captcha;
    private String session;
    public String defaultSmsApp;
    private Boolean bPasswordVisible = false;

    public void saveSettings(Map<String, String> params) {
        if (params.isEmpty())
            throw new NullPointerException("no params specified for saveSettings!");

        SharedPreferences settings = getSharedPreferences("megafonremainders", 0);
        SharedPreferences.Editor editor = settings.edit();

        for (Map.Entry<String, String> set : params.entrySet()) {
            editor.putString(set.getKey(), set.getValue());
            Log.v("SAVESETTINGS", "key = " + set.getKey() + "; val = " + set.getValue());
        }

        editor.apply();
    }

    public String readSettingsValue(String key) {
        if (key.isEmpty())
            throw new NullPointerException("no key specified for readSettingsValue!");

        SharedPreferences settings = getSharedPreferences("megafonremainders", 0);
        String value = settings.getString(key, null);
        Log.v("READSETTINGSVAL", "key = " + key + "; val = " + value);

        if (value == null)
            return null;

        return value;
    }

    public String readSettingsValue(String key, String defValue) {
        if (key == null || defValue == null)
            throw new NullPointerException("no key and/or value specified for readSettingsValue!");

        SharedPreferences settings = getSharedPreferences("megafonremainders", 0);

        return settings.getString(key, defValue);
    }

    public void removeSettingsValue(String key) {
        if (key.isEmpty())
            throw new NullPointerException("no key value specified for removeSettingsValue!");

        SharedPreferences settings = getSharedPreferences("megafonremainders", 0);
        SharedPreferences.Editor editor = settings.edit();

        try {
            editor.remove(key);
        }finally {
            editor.apply();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Рекламный модуль.
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-3272387314575556/4284034617");

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
            }
        });

        requestNewInterstitial();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        final EditText etPhonenum = (EditText) findViewById(R.id.etPhonenum);
        final EditText etPassw = (EditText) findViewById(R.id.etPassw);
        final CheckBox chkRemember = (CheckBox) findViewById(R.id.chkRemember);

        String srememberData = readSettingsValue("rememberdata", "false");
        Log.v("REMEMBERDATA", srememberData);
        rememberData = Boolean.valueOf(srememberData);

        if (rememberData) {
            etPhonenum.setText(readSettingsValue("phonenum", ""));
            etPassw.setText(readSettingsValue("passw", ""));
            chkRemember.setChecked(rememberData);
        }

        //  Присвоим кнопке "показать рекламу" (#tvShowAd) обработчик нажатия на кнопку (onClick).
        final TextView tvShowAd = (TextView) findViewById(R.id.tvShowAd);
        tvShowAd.setOnClickListener(new TextView.OnClickListener() {
            public void onClick(View v) {
                if (mInterstitialAd.isLoaded())
                    mInterstitialAd.show();
            }
        });

        /*Button btnTest = (Button)findViewById(R.id.btnTest);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("SMS", "Change sms in progress.");
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                    intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                            getPackageName());
                    startActivity(intent);
                }
            }
        });
        */

        final ImageView ivGlazik = (ImageView)findViewById(R.id.ivGlazik);
        ivGlazik.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  При нажатии на эту пиктограмму показываем или скрываем пароль.
                if (bPasswordVisible == false) {
                    //  Показывыаем пароль.
                    ivGlazik.setImageResource(R.drawable.glazik1);
                    etPassw.setTransformationMethod(null);
                    bPasswordVisible = true;
                }else{
                    ivGlazik.setImageResource(R.drawable.glazik0);
                    etPassw.setTransformationMethod(new PasswordTransformationMethod());
                    bPasswordVisible = false;
                }
            }
        });

        //  Присвоим кнопке входа (#btnLogin) обработчик события нажатия на кнопку (onClick).
        final Button button = (Button) findViewById(R.id.btnLogin);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //  При нажатии на кнопку отсылаем запрос.
                ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetworkInfo = connManager.getActiveNetworkInfo();
                boolean networkAvailable = activeNetworkInfo != null && activeNetworkInfo.isConnected();
                String phone_number = etPhonenum.getText().toString();
                String password_string = etPassw.getText().toString();

                // Если интернета нет - ошибка.
                if (!networkAvailable) {
                    DialogFragment dialog = new DialogNoInternet();
                    dialog.show(getFragmentManager(), "nonetwork");
                    return;
                }
                // Если не заполнено хотя бы одно поле - ошибка.
                if (phone_number.length() == 0 || password_string.length() == 0) {
                    Toast.makeText(getApplicationContext(), R.string.megafon_emptyinput, Toast.LENGTH_LONG).show();
                    return;
                }

                // Сохраняем данные, если такое необходимо.
                if (rememberData) {
                    Map params = new HashMap();

                    params.put("phonenum", phone_number);
                    params.put("passw", password_string);
                    params.put("rememberdata", "true");

                    saveSettings(params);
                }

                // Всё ок. Логинимся и показываем остатки.
                if (captcha != null) {
                    AsyncLoginMegafon asyncLoginMegafon = new AsyncLoginMegafon(mainActivity.this, captcha, session);
                    asyncLoginMegafon.execute(phone_number, password_string);
                    captcha = null;
                    session = null;
                }else{
                    AsyncLoginMegafon asyncLoginMegafon = new AsyncLoginMegafon(mainActivity.this);
                    asyncLoginMegafon.execute(phone_number, password_string);
                };
            }
        });

        chkRemember.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                rememberData = isChecked;

                Map params = new HashMap();

                params.put("rememberdata", Boolean.toString(rememberData));
                Log.v("REMEMBERDATA", "remember = " + Boolean.toString(rememberData));
                if (rememberData) {
                    params.put("phonenum", etPhonenum.getText().toString());
                    params.put("passw", etPassw.getText().toString());
                    Log.v("SETTINGS", "writing phonenum and passw to storage.");
                } else {
                    removeSettingsValue("phonenum");
                    removeSettingsValue("passw");
                    Log.v("SETTINGS", "removed phonenum and passw from storage.");
                }
                saveSettings(params);
            }
        });

        final TextView tvPasswHelp = (TextView) findViewById(R.id.tvPasswHelp);
        tvPasswHelp.setOnClickListener(new TextView.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogHelpPassword dialogHelpPassword = new DialogHelpPassword();
                dialogHelpPassword.setPhonenum(etPhonenum.getText().toString());
                dialogHelpPassword.setContext(getApplicationContext());
                dialogHelpPassword.show(getFragmentManager(), "passwhelp");
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(getApplicationContext());
        }
    }

    @Override
    public void onDestroy() {
        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, defaultSmsApp);
        startActivity(intent);

        super.onDestroy();
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();

        mInterstitialAd.loadAd(adRequest);
    }

    @Override
    public void handleDialogClose(DialogInterface dialog, View view, String sessionId) {

    }

    class AsyncLoginMegafon extends AsyncTask<String, String, String> {

        final private Context context;
        private ProgressDialog progressDialog;
        private String result;
        private boolean error;
        private boolean captcha;
        private String captcha_v;
        private boolean blocked;
        private String session = "android";
        private String session_web = "web";
        private String error_msg = null;
        private String phone_number;
        private String password;

        public AsyncLoginMegafon(Context context) {
            this.context = context;
            this.error = false;
            this.captcha = false;
            this.blocked = false;
            this.error_msg = null;
        }

        public AsyncLoginMegafon(Context context, String captcha, String session) {
            this.context = context;
            this.captcha_v = captcha;
            this.error = false;
            this.captcha = false;
            this.blocked = false;
            this.session = session;
            this.error_msg = null;
        }

        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(context.getString(R.string.megafon_loading));
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }

        protected String doInBackground(String... params) {
            loginMegafon(params[0], params[1]);
            return result;
        }

        private Bitmap getCaptcha() {
            try {
                Response response_captcha = Jsoup.connect("https://api.megafon.ru/mlk/auth/captcha")
                        .userAgent("MLK Android Phone 1.2.1")
                        .cookie("JSESSIONID", session)
                        .ignoreContentType(true)
                        .method(Connection.Method.GET)
                        .execute();

                Bitmap bm = BitmapFactory.decodeByteArray(response_captcha.bodyAsBytes(), 0, response_captcha.bodyAsBytes().length);

                return bm;
            }catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        private String getOptionStartDate(String optionName) {
            Response response_options = null;
            Response response_login = null;
            String options_internet_el = new String();
            try {
                Response response_prelogin = Jsoup.connect("https://lk.megafon.ru/login/?noredirect")
                        .method(Connection.Method.GET)
                        .execute();
                Document doc_login = response_prelogin.parse();
                String CSRF_param = doc_login.select("body > script:eq(4)").html();
                CSRF_param = CSRF_param.substring(CSRF_param.indexOf("AM = ") + 6);
                CSRF_param = CSRF_param.substring(0, CSRF_param.indexOf("\","));
                Log.v("OPTIONDATE", "CSRF = " + CSRF_param);
                response_login = Jsoup.connect("https://lk.megafon.ru/dologin/?noredirect")
                        .cookies(response_prelogin.cookies())
                        .data("CSRF", CSRF_param)
                        .data("j_username", phone_number)
                        .data("j_password", password)
                        .method(Connection.Method.POST)
                        .execute();

                Map<String, String> cookies = response_login.cookies();
                session_web = response_login.cookie("JSESSIONID");

                Log.v("OPTIONDATE", "JSESSIONID = " + session_web);
                response_options = Jsoup.connect("https://lk.megafon.ru/options/?noredirect")
                        .followRedirects(false)
                        .cookies(cookies)
                        .method(Connection.Method.GET)
                        .execute();

                Document options_doc = response_options.parse();

                Log.v("OPTIONDATE", "title = " + options_doc.select("title").text().trim());

                //  Ищем в документе нужные нам данные, содержащие дату подключения.
                Elements options_elements = options_doc.select("body > div > div > div.private-office-content > div > div.private-office-td.private-office-content-left.private-office-768 > div.main-pades > div > div.ui-tabBox > div > div.ui-tabBox-contentWrapper.ui-tabBox-contentWrapperActive > div > div > div > div > div.gadget-options-td.gadget-options-name.gadget-options-mobile");

                for (Element el : options_elements)
                    if (el.select("a").text().trim().equals(optionName))
                        options_internet_el = el.parent().select("div.gadget-options-td.gadget-options-button-box.gadget-options-mobile > p").first().text();

                Log.v("OPTIONDATE", "options_internet_el = " + options_internet_el);
            } catch (Exception e) {
                error = true;
                e.printStackTrace();
            }finally {
                return options_internet_el;
            }
        }

        private JSONObject getPaidOptionInfo(String optionName) {
            try {
                Response response_optionsactive = Jsoup.connect("https://api.megafon.ru/mlk/api/options/list/current")
                        .ignoreContentType(true)
                        .userAgent("MLK Android Phone 1.2.1")
                        .cookie("JSESSIONID", session)
                        .method(Connection.Method.GET)
                        .execute();

                JSONObject options = new JSONObject(response_optionsactive.body());
                JSONArray options_paid = options.getJSONArray("paid");
                int options_length = options_paid.length();
                for (int i = 0; i < options_length; i++) {
                    JSONObject option = options_paid.getJSONObject(i);
                    if (option.getString("optionName").startsWith(optionName)) {
                        Log.v("OPTIONINFO", option.toString());
                        return option;
                    }
                }

                return null;
            }catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        private String getMonthName(String month) {
            month = month.toLowerCase();
            return month.replace("january", "января").replace("february", "февраля").replace("march", "марта").replace("april", "апреля").replace("may", "мая").replace("june", "июня").replace("july", "июля").replace("august", "августа").replace("september", "сентября").replace("october", "октября").replace("november", "ноября").replace("december", "декабря");
        }

        private void getRemainders() {
            Response response_remainders = null;
            try {
                response_remainders = Jsoup.connect("https://api.megafon.ru/mlk/api/options/remainders")
                        .ignoreContentType(true)
                        .userAgent("MLK Android Phone 1.2.1")
                        .cookie("JSESSIONID", session)
                        .method(Connection.Method.GET)
                        .execute();

                JSONObject remainders_models = new JSONObject(response_remainders.body());
                JSONArray remainders = remainders_models.getJSONArray("models");
                JSONObject remainders_1 = new JSONObject();
                for (int i = 0; i < remainders.length(); i++) {
                    remainders_1 = remainders.getJSONObject(i);
                    if (remainders_1.getString("optionsRemaindersType").equals("OPTION") &&
                            remainders_1.has("groupName") &&
                            remainders_1.getString("groupName").equals("Интернет"))
                        break;
                }
                String result;

                Log.v("REMAINDERS", remainders_1.toString());
                Log.v("REMAINDERS", "name = " + remainders_1.getString("name"));
                result = remainders_1.getString("name");
                Boolean IsMonthly = false;
                for (int i = 0; i < remainders_1.getJSONArray("remainders").length(); i++) {
                    JSONObject rem = remainders_1.getJSONArray("remainders").getJSONObject(i);
                    Log.v("REMAINDERS", "remainders #" + i + " = " + rem.toString());
                    if (rem.has("total"))
                        result = result + "\n" + rem.getString("name") + ": " + rem.get("available") + rem.getString("unit") + " из " + rem.get("total") + rem.getString("unit");
                    else
                        result = result + "\n" + rem.getString("name") + ": " + rem.get("available");
                }

                JSONObject internet_option_info = getPaidOptionInfo(remainders_1.getString("name"));
                if (internet_option_info == null) {
                    this.result = result + "\nдату начала получить не удалось.";
                    return;
                }
                Log.v("PAIDOPTION", internet_option_info.toString());
                if (internet_option_info.getString("shortDescription").contains("в сутки"))
                    IsMonthly = false;
                else
                    IsMonthly = true;

                if (IsMonthly) {
                    //  В месяц.
                    //  Показываем дату начала оказания услуги и дату прекращения оказания услуги.
                    String activeSince = getOptionStartDate(remainders_1.getString("name"));
                    if (activeSince == null || activeSince.isEmpty() || activeSince.length() < 18) {
                        this.result = result + "\nдату начала получить\nне удалось.";
                        return;
                    }

                    activeSince = activeSince.substring(18);
                    activeSince = activeSince.substring(0, activeSince.indexOf(" г."));
                    String unactiveDate = activeSince.replace("января", "февраля").replace("февраля", "марта").replace("марта", "апреля").replace("апреля", "мая").replace("мая", "июня").replace("июня", "июля").replace("июля", "августа").replace("августа", "сентября").replace("сентября", "октября").replace("октября", "ноября").replace("ноября", "декабря").replace("декабря", "января");
                    if (activeSince.contains("декабря") && unactiveDate.contains("января")) {
                        //  Надо поменять ещё и год!
                        String[] a = unactiveDate.split(" ");
                        int y = Integer.parseInt(a[a.length - 1]);
                        unactiveDate = unactiveDate.replace(a[a.length - 1], Integer.toString(y + 1));
                    }

                    result = result + "\nс " + activeSince + "\n" + "по " + unactiveDate;
                }else {
                    //  В сутки.
                    DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");
                    Date date = new Date();
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    calendar.add(Calendar.DATE, 1);
                    result = result + "\nс " + getMonthName(dateFormat.format(date)) + "\nпо " + getMonthName(dateFormat.format(calendar.getTime()));
                }

                this.result = result;
            } catch (Exception e) {
                e.printStackTrace();
                Looper.prepare();
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        private void loginMegafon(String phone_number, String password) {
            this.phone_number = phone_number;
            this.password = password;
            try {
                // v2 using API.
                if (captcha_v != null) {
                    Log.v("LOGIN", "login with captcha (" + captcha_v + ") session = " + session);
                    Response response_login = Jsoup.connect("https://api.megafon.ru/mlk/login")
                            .ignoreHttpErrors(true)
                            .ignoreContentType(true)
                            .userAgent("MLK Android Phone 1.2.1")
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .data("login", phone_number)
                            .data("password", password)
                            .data("captcha", this.captcha_v)
                            .cookie("JSESSIONID", session)
                            .method(Connection.Method.POST)
                            .execute();

                    Log.v("LOGIN", response_login.body());

                    JSONObject json = new JSONObject(response_login.body());

                    if (json.has("msisdn")) {
                        // Вход успешен. Можно выполнить запрос на получение остатков траффика.
                        session_web = json.getString("widgetKey");
                        session = response_login.cookie("JSESSIONID");

                        getRemainders();
                    }

                    if (json.has("message")) {

                        if (json.getString("message").contains("телефона или пароль")) {
                            captcha = false;
                            error = true;
                            blocked = false;
                            this.result = null;
                            return;
                        }

                        if (json.getString("message").contains("кода с картинки")) {
                            session = response_login.cookie("JSESSIONID");
                            captcha = true;
                            error = false;
                            this.result = null;
                            return;
                        }

                        if (json.getString("message").contains("неверно") && json.getString("message").contains("Код")) {
                            session = response_login.cookie("JSESSIONID");
                            captcha = true;
                            error = true;
                            this.result = null;
                            return;
                        }
                        if (json.getString("message").contains("заблокирован")) {
                            captcha = false;
                            error = false;
                            blocked = true;
                            this.result = null;
                            return;
                        }
                    }
                    return;
                }else {
                    Response response_login = Jsoup.connect("https://api.megafon.ru/mlk/login")
                            .ignoreHttpErrors(true)
                            .ignoreContentType(true)
                            .userAgent("MLK Android Phone 1.2.1")
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .data("login", phone_number)
                            .data("password", password)
                            .method(Connection.Method.POST)
                            .execute();

                    Log.v("LOGIN", response_login.body());

                    JSONObject json = new JSONObject(response_login.body());

                    if (json.has("msisdn")) {
                        // Вход успешен. Можно выполнить запрос на получение остатков траффика.
                        session_web = json.getString("widgetKey");
                        session = response_login.cookie("JSESSIONID");

                        getRemainders();
                    }

                    if (json.has("message")) {

                        if (json.getString("message").contains("телефона или пароль")) {
                            captcha = false;
                            error = true;
                            blocked = false;
                            this.result = null;
                            return;
                        }

                        if (json.getString("message").contains("кода с картинки")) {
                            session = response_login.cookie("JSESSIONID");
                            captcha = true;
                            error = false;
                            this.result = null;
                            return;
                        }

                        if (json.getString("message").contains("неверно") && json.getString("message").contains("Код")) {
                            session = response_login.cookie("JSESSIONID");
                            captcha = true;
                            error = true;
                            this.result = null;
                            return;
                        }
                        if (json.getString("message").contains("заблокирован")) {
                            captcha = false;
                            error = false;
                            blocked = true;
                            this.result = null;
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                error_msg = e.getMessage();
                return;
            }finally {
                checkResult();
            }
        }

        private void checkResult() {
            Log.v("POSTEXECUTE", "result = " + result);
            Log.v("POSTEXECUTE", "error = " + Boolean.toString(error));
            Log.v("POSTEXECUTE", "captcha = " + Boolean.toString(captcha));
            Log.v("POSTEXECUTE", "blocked = " + Boolean.toString(blocked));
            Log.v("POSTEXECUTE", "error_msg = " + error_msg);

            if (result == null) {
                DialogError dialog = new DialogError();
                if (error && captcha == false) {
                    Log.v("ERROR", "error = true & captcha = false (wrong login/pw)");
                    dialog.show(getFragmentManager(), "errorloginpw", getString(R.string.megafon_error_loginw));
                }else if (captcha && error == false) {
                    DialogCaptcha dialog_ = new DialogCaptcha();
                    dialog_.show(getFragmentManager(), "captcha", context, getCaptcha(), session);
                    dialog_.setOnDialogDismissedListener(new OnDialogDismissed() {
                        @Override
                        public void action(View view, Context context) {
                            EditText etCaptcha = (EditText)view.findViewById(R.id.etCaptcha);

                            Log.v("DIALOG", "session = " + session);
                            Log.v("DIALOG", "captcha = " + etCaptcha.getText().toString());

                            if (etCaptcha.getText().toString().length() == 0) {
                                Toast.makeText(getApplicationContext(), getString(R.string.megafon_captcha_err), Toast.LENGTH_LONG).show();
                                return;
                            }

                            captcha_v = etCaptcha.getText().toString();
                            loginMegafon(phone_number, password);
                        }
                    });
                }else if (blocked)
                    dialog.show(getFragmentManager(), "errorblocked", getString(R.string.megafon_error_blocked));
                else if (error && captcha == true) {
                    Log.v("ERROR", "error = true & captcha = true (wrong captcha)");
                    dialog.show(getFragmentManager(), "errorcaptcha", getString(R.string.megafon_captcha_err));
                }else
                    dialog.show(getFragmentManager(), "errorlogin", error_msg);
            }else{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView tvRemainders = (TextView) findViewById(R.id.tvRemainders);
                        tvRemainders.setText(result);
                    }
                });
            }
        }

        protected void onPostExecute(String result) {
            progressDialog.dismiss();
        }
    }
}