package io.github.vincekruger.whatsapp_stickers;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.NewIntentListener;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import static io.github.vincekruger.whatsapp_stickers.StickerPackActivity.ADD_PACK;
import java.util.HashMap;
import java.util.Map;

/**
 * WhatsAppStickersPlugin
 */
public class WhatsAppStickersPlugin extends BroadcastReceiver implements MethodCallHandler {
    private final String TAG = "WhatsAppStickersPlugin";
    private final Registrar registrar;
    private final MethodChannel channel;

    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "io.github.vincekruger/whatsapp_stickers");
        final WhatsAppStickersPlugin plugin = new WhatsAppStickersPlugin(registrar, channel);
        channel.setMethodCallHandler(plugin);
    }

    private WhatsAppStickersPlugin(Registrar registrar, MethodChannel channel) {
        this.registrar = registrar;
        this.channel = channel;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(StickerPackActivity.ACTION_STICKER_PACK_RESULT);
        intentFilter.addAction(StickerPackActivity.ACTION_STICKER_PACK_ERROR);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(registrar.context());
        manager.registerReceiver(this, intentFilter);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "getPlatformVersion":
                result.success("Android " + android.os.Build.VERSION.RELEASE);
                break;
            case "isWhatsAppInstalled": {
                result.success(WhitelistCheck.isWhatsAppInstalled(registrar.context()));
                break;
            }
            case "isWhatsAppConsumerAppInstalled": {
                result.success(WhitelistCheck.isWhatsAppConsumerAppInstalled(registrar.context().getPackageManager()));
                break;
            }
            case "isWhatsAppSmbAppInstalled": {
                result.success(WhitelistCheck.isWhatsAppSmbAppInstalled(registrar.context().getPackageManager()));
                break;
            }
            case "isStickerPackInstalled": {
                String stickerPackIdentifier = call.argument("identifier");
                final boolean installed = WhitelistCheck.isWhitelisted(registrar.context(), stickerPackIdentifier);
                result.success(installed);
                break;
            }
            case "addStickerPack": {
                StickerPackActivity stickerPackActivity = new StickerPackActivity(registrar.context());
                registrar.addActivityResultListener(stickerPackActivity);

                String whatsAppPackage = call.argument("package");
                String stickerPackIdentifier = call.argument("identifier");
                String stickerPackName = call.argument("name");

                Intent intent = StickerPackActivity.createIntentToAddStickerPack(
                    getContentProviderAuthority(registrar.context()), 
                    stickerPackIdentifier, 
                    stickerPackName
                );
                intent.setPackage(whatsAppPackage.isEmpty() ? WhitelistCheck.CONSUMER_WHATSAPP_PACKAGE_NAME : whatsAppPackage);

                try {
                    registrar.activity().startActivityForResult(intent, ADD_PACK);
                } catch (ActivityNotFoundException e) {
                    String errorMessage = "Sticker pack not added. If you'd like to add it, make sure you update to the latest version of WhatsApp.";
                    result.error(errorMessage, "failed", e);
                }
                break;
            }
            default:
                result.notImplemented();
                break;
        }
    }

    static String getContentProviderAuthority(Context context) {
        return context.getPackageName() + ".stickercontentprovider";
    }

    // BroadcastReceiver implementation.
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // No action, exit
        if (action == null) {
            return;
        }
        
        if (action.equals(StickerPackActivity.ACTION_STICKER_PACK_RESULT)) {
            // Success
            Map<String, Object> content = new HashMap<>();
            content.put("action", intent.getStringExtra(StickerPackActivity.EXTRA_STICKER_PACK_ACTION));
            content.put("result", intent.getBooleanExtra(StickerPackActivity.EXTRA_STICKER_PACK_RESULT, false));
            channel.invokeMethod("onSuccess", content);
        }
        else if (action.equals(StickerPackActivity.ACTION_STICKER_PACK_ERROR)) {
            // Error
            String error = intent.getStringExtra(StickerPackActivity.EXTRA_STICKER_PACK_ERROR);
            Map<String, Object> content = new HashMap<>();
            content.put("action", intent.getStringExtra(StickerPackActivity.EXTRA_STICKER_PACK_ACTION));
            content.put("result", intent.getBooleanExtra(StickerPackActivity.EXTRA_STICKER_PACK_RESULT, false));
            content.put("error", intent.getStringExtra(StickerPackActivity.EXTRA_STICKER_PACK_ERROR));
            channel.invokeMethod("onError", content);
        }
    }
}
