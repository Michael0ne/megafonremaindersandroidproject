package colocoloc.megafonremainders;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.jsoup.Connection;
import org.jsoup.*;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Random;

/**
 * Created by colocoloc on 22.11.2015.
 */
public class DialogHelpPassword extends DialogFragment implements DialogCloseListener {
    public String phonenum = null;
    private Context context;

    public void setPhonenum(String val) {
        if (val == null)
            return;

        this.phonenum = val;
    }

    public void setContext(Context context_) {
        if (context_ == null)
            return;

        this.context = context_;
    }

    @Override
    public void handleDialogClose(DialogInterface dialog, View view, String sessionId) {
        EditText etCaptcha = (EditText)view.findViewById(R.id.etCaptcha);

        Log.v("DIALOG", "session = " + sessionId);
        Log.v("DIALOG", "captcha = " + etCaptcha.getText().toString());
    }

    public Dialog onCreateDialog(Bundle savedInstance) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.megafon_passw_help_title))
                .setMessage(getString(R.string.megafon_passw_help_text))
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });
/*
                .setNeutralButton(R.string.megafon_get_pass, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String phpsessid = null;
                        try {
                            if (phonenum == null)
                                throw new Exception("no phone specified!");

                            //  Упразднён (проверил в середине февраля 2016 г.). Получение пароля только с помощью команды USSD.

                            Connection.Response restorepass = Jsoup.connect("https://moscowsg.megafon.ru/ps/scc/php/get_password.php?CHANNEL=WWW")
                                    .execute();

                            if (restorepass.cookie("PHPSESSID") == null)
                                throw new Exception("phpsessid is null!");

                            phpsessid = restorepass.cookie("PHPSESSID");
                            Connection.Response captcha_response = Jsoup.connect("https://moscowsg.megafon.ru/ps/scc/php/cryptographp.php?PHPSESSID=" + phpsessid + "&ref=" + (100 + (int)(Math.random() * ((999 - 100) + 1))) + "&w=150")
                                    .cookie("PHPSESSID", phpsessid)
                                    .ignoreContentType(true)
                                    .execute();

                            Bitmap bm = BitmapFactory.decodeByteArray(captcha_response.bodyAsBytes(), 0, captcha_response.bodyAsBytes().length);

                            DialogCaptcha dialogCaptcha = new DialogCaptcha();
                            dialogCaptcha.show(getFragmentManager(), "captchapasswrestore", context, bm, phpsessid);
                            final String finalPhpsessid = phpsessid;
                            dialogCaptcha.setOnDialogDismissedListener(new OnDialogDismissed() {
                                @Override
                                public void action(View view, Context context) {
                                    // Получили ответ на капчу. Отправляем запрос на сервер и ждём смску...
                                    EditText etCaptcha = (EditText)view.findViewById(R.id.etCaptcha);
                                    Log.v("DIALOGCAPTCHA", "captcha = " + etCaptcha.getText().toString());

                                    try {
                                        Log.v("DIALOGCAPTCHA", "phpsessid = " + finalPhpsessid);
                                        Log.v("DIALOGCAPTCHA", "code = " + etCaptcha.getText().toString());
                                        Log.v("DIALOGCAPTCHA", "login = " + phonenum);
                                        Connection.Response restore_response = Jsoup.connect("https://moscowsg.megafon.ru/ps/scc/php/get_password.php?CHANNEL=WWW")
                                                .header("Content-Type", "application/x-www-form-urlencoded")
                                                .header("Host", "moscowsg.megafon.ru")
                                                .header("Origin", "https://moscowsg.megafon.ru")
                                                .header("Referer", "https://moscowsg.megafon.ru//ps/scc/?ULOGIN=" + phonenum + "&CHANNEL=WWW&P_USER_LANG_ID=1")
                                                .cookie("PHPSESSID", finalPhpsessid)
                                                .data("PHPSESSID", finalPhpsessid)
                                                .data("CODE", etCaptcha.getText().toString())
                                                .data("LOGIN", phonenum)
                                                .data("P_SEND_TYPE", "1")
                                                .data("P_USER_LANG_ID", "1")
                                                .method(Connection.Method.POST)
                                                .execute();

                                        Document restore_doc = restore_response.parse();
                                        if (restore_doc.select("error_id").text() != null) {
                                            String err_code = restore_doc.select("error_id").text().trim();
                                            String err_msg = restore_doc.select("error_message").text().trim();
                                            Log.v("DIALOGCAPTCHA", "html = " + restore_doc.outerHtml());
                                            switch (err_code) {
                                                case "-6":
                                                    //  Код неверен.
                                                    Toast.makeText(context, "Код с картинки неверен. Попробуйте снова.", Toast.LENGTH_LONG).show();
                                                    break;
                                                case "-5":
                                                    //  Код устарел.
                                                    Toast.makeText(context, "Код устарел. Попробуйте снова.", Toast.LENGTH_LONG).show();
                                                    break;
                                                case "51030277":
                                                    //  Превысили запрос на пароль.
                                                    Toast.makeText(context, "Запрос на пароль возможен не чаще, чем раз в 5 минут.", Toast.LENGTH_LONG).show();
                                                    break;
                                                default:
                                                    if (restore_doc.select("message").text() != null) {
                                                        //  Успешно. Ждём смс-ку.
                                                        Toast.makeText(context, restore_doc.select("message").text(), Toast.LENGTH_LONG).show();
                                                        return;
                                                    }
                                                    if (err_msg != null)
                                                        Toast.makeText(context, err_msg, Toast.LENGTH_LONG).show();
                                                    else
                                                        Toast.makeText(context, getString(R.string.megafon_error) + "\nКод: " + err_code, Toast.LENGTH_LONG).show();
                                                    break;
                                            }
                                            return;
                                        }

                                        return;
                                    }catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
*/
        return builder.create();
    }
}
