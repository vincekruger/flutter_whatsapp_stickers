package io.github.vincekruger.whatsapp_stickers;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;

public class StickerPackActivity implements PluginRegistry.ActivityResultListener {
    private final String TAG = "StickerPackActivity";

    private static final String EXTRA_STICKER_PACK_ID = "sticker_pack_id";
    private static final String EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority";
    private static final String EXTRA_STICKER_PACK_NAME = "sticker_pack_name";

    static final int ADD_PACK = 200;

    private MethodChannel.Result result;

    StickerPackActivity(MethodChannel.Result result) {
        this.result = result;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == ADD_PACK) {
            if (resultCode == Activity.RESULT_CANCELED) {
                if (intent != null) {
                    final String validationError = intent.getStringExtra("validation_error");
                    if (validationError != null) {
                        result.error("Validation failed:" + validationError, null, null);
                        Log.e(TAG, "Validation failed:" + validationError);
                        return false;
                    }
                }

                result.success(false);
                return false;
            }

            result.success(true);
        }

        return true;
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
