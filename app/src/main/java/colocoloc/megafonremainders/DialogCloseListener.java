package colocoloc.megafonremainders;

import android.content.DialogInterface;
import android.view.View;

/**
 * Created by colocoloc on 26.11.2015.
 */
public interface DialogCloseListener {
    void handleDialogClose(DialogInterface dialog, View view, String session);
}