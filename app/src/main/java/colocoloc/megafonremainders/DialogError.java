package colocoloc.megafonremainders;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Created by colocoloc on 21.11.2015.
 */
public class DialogError extends DialogFragment {
    private String msg = null;

    public void show(FragmentManager fragmentManager, String tag) {
        this.msg = getString(R.string.megafon_error);
        super.show(fragmentManager, tag);
    }

    public void show(FragmentManager fragmentManager, String tag, String msg) {
        this.msg = msg;
        super.show(fragmentManager, tag);
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(msg)
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        return;
                    }
                });
        return builder.create();
    }
}
