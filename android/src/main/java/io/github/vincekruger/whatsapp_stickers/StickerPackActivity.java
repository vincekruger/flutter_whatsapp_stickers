package io.github.vincekruger.whatsapp_stickers;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.util.Log;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;

public class StickerPackActivity implements PluginRegistry.ActivityResultListener {
    private final String TAG = "StickerPackActivity";

    public static final String ACTION_STICKER_PACK_RESULT = "io.github.vincekruger/whatsapp_stickers/result";
    public static final String ACTION_STICKER_PACK_ERROR = "io.github.vincekruger/whatsapp_stickers/error";
    public static final String EXTRA_STICKER_PACK_ACTION = "action";
    public static final String EXTRA_STICKER_PACK_RESULT = "result";
    public static final String EXTRA_STICKER_PACK_ERROR = "error";

    private static final String EXTRA_STICKER_PACK_ID = "sticker_pack_id";
    private static final String EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority";
    private static final String EXTRA_STICKER_PACK_NAME = "sticker_pack_name";

    static final int ADD_PACK = 200;

    private Context context;

    StickerPackActivity(@NonNull Context context) {
        this.context = context;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADD_PACK) {
            // Create the intent
            if (resultCode == Activity.RESULT_CANCELED) {
                if (data != null) {
                    final String validationError = data.getStringExtra("validation_error");
                    if (validationError != null) {
                        this.broadcastResult(ACTION_STICKER_PACK_ERROR, "failed", true, validationError);
                    }
                }
                else {
                    this.broadcastResult(ACTION_STICKER_PACK_RESULT, "cancelled", true, null);
                }
            }
            else if(resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Bundle bundle = data.getExtras();
                    if(bundle.containsKey("add_successful")){
                        this.broadcastResult(ACTION_STICKER_PACK_RESULT, "add_successful", bundle.getBoolean("add_successful"), null);
                    }
                    else if(bundle.containsKey("already_added")){
                        this.broadcastResult(ACTION_STICKER_PACK_RESULT, "already_added", bundle.getBoolean("already_added"), null);
                    }
                    else {
                        this.broadcastResult(ACTION_STICKER_PACK_RESULT, "success", true, null);
                    }
                }
                else {
                    this.broadcastResult(ACTION_STICKER_PACK_RESULT, "success", true, null);
                }
            }
            else {
                this.broadcastResult(ACTION_STICKER_PACK_RESULT, "unkonwn", true, null);
            }
        }

        return true;
    }

    private void broadcastResult(String intentAction, String action, Boolean result, String error){
        Intent flutterIntent = new Intent(intentAction);
        flutterIntent.putExtra(EXTRA_STICKER_PACK_ACTION, action);
        flutterIntent.putExtra(EXTRA_STICKER_PACK_RESULT, result);
        flutterIntent.putExtra(EXTRA_STICKER_PACK_ERROR, error);
        LocalBroadcastManager.getInstance(this.context).sendBroadcast(flutterIntent);
    }

    static Intent createIntentToAddStickerPack(String authority, String identifier, String stickerPackName) {
        Intent intent = new Intent();
        intent.setAction("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
        intent.putExtra(EXTRA_STICKER_PACK_ID, identifier);
        intent.putExtra(EXTRA_STICKER_PACK_AUTHORITY, authority);
        intent.putExtra(EXTRA_STICKER_PACK_NAME, stickerPackName);
        return intent;
    }
}
