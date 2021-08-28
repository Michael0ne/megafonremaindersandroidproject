package colocoloc.megafonremainders;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class DialogCaptcha extends DialogFragment {
    private Bitmap captcha;
    private Context context;
    private String session;
    private View dialog_view;

    private OnDialogDismissed mOnDialogDismissed;

    public void setOnDialogDismissedListener(OnDialogDismissed listener) {
        mOnDialogDismissed = listener;
    }

    public void show(FragmentManager fragmentManager, String tag) {
        super.show(fragmentManager, tag);
    }

    public void show(FragmentManager fragmentManager, String tag, Context context, Bitmap captchaImage, String sessionId) {
        this.captcha = captchaImage;
        this.context = context;
        this.session = sessionId;
        super.show(fragmentManager, tag);
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        try {
            if (this.captcha == null)
                throw new java.lang.NullPointerException("Captcha is null!");
        }catch (java.lang.NullPointerException e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final View view = View.inflate(context, R.layout.captcha_dialog, null);
            ImageView ivCaptcha = (ImageView) view.findViewById(R.id.ivCaptcha);
            ivCaptcha.setMinimumHeight(this.captcha.getHeight());
            ivCaptcha.setMinimumWidth(this.captcha.getWidth());
            ivCaptcha.setImageBitmap(this.captcha);
            builder.setView(view)
                    .setTitle(R.string.megafon_captcha_title)
                    .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            return;
                        }
                    });
            this.dialog_view = view;
            return builder.create();
    }

    public void onDismiss(DialogInterface dialog)
    {
        if (mOnDialogDismissed != null)
            mOnDialogDismissed.action(dialog_view, context);
    }
}
