package colocoloc.megafonremainders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by colocoloc on 11.12.2015.
 */
public class SmsListener extends BroadcastReceiver {

    private OnBroadcastReceived mOnBroadcastReceived;

    public void setOnBroadcastReceived(OnBroadcastReceived listener) {
        mOnBroadcastReceived = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("REMAINDERS", "onReceive");
            Bundle bundle = intent.getExtras();
            SmsMessage[] msgs = null;
            String msg_from;
            if (bundle != null) {
                try {
                    Object[] pdus = (Object[])bundle.get("pdus");
                    msgs = new SmsMessage[pdus.length];
                    for (int i = 0; i < msgs.length; i++) {
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        msg_from = msgs[i].getOriginatingAddress();
                        String msgBody = msgs[i].getMessageBody();
                        Log.v("REMAINDERS", msg_from);
                        Log.v("REMAINDERS", msgBody);
                        if (msg_from.equals("MegaFon") && msgBody.contains("Пароль для доступа в Личный кабинет")) {
                            //  Покажем новый пароль и подсказку.
                            String passw = msgBody.substring(msgBody.indexOf("ет") + 2);
                            passw = passw.split(",")[0];
                            Toast.makeText(context, "Ваш пароль был обновлён.\nНовый пароль: " + passw, Toast.LENGTH_LONG).show();
                            View view = View.inflate(context, R.layout.activity_main, null);
                            EditText etPassword = (EditText)view.findViewById(R.id.etPassw);
                            Log.v("REMAINDERS", "etPassword = " + etPassword.getText());
                            etPassword.setText(passw);
                            return;
                        }
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
    }
}
