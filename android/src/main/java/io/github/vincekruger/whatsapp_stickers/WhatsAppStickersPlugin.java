package io.github.vincekruger.whatsapp_stickers;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static io.github.vincekruger.whatsapp_stickers.StickerPackActivity.ADD_PACK;

/**
 * WhatsAppStickersPlugin
 */
public class WhatsAppStickersPlugin implements MethodCallHandler {
    private final String TAG = "WhatsAppStickersPlugin";
    private final Registrar registrar;

    private WhatsAppStickersPlugin(Registrar registrar) {
        this.registrar = registrar;
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "io.github.vincekruger/whatsapp_stickers");
        channel.setMethodCallHandler(new WhatsAppStickersPlugin(registrar));
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
            case "isStickPackInstalled": {
                String stickerPackIdentifier = call.argument("identifier");
                final boolean isInstalled = WhitelistCheck.isWhitelisted(registrar.context(), stickerPackIdentifier);
                result.success(isInstalled);
                break;
            }
            case "addStickerPack": {
                StickerPackActivity stickerPackActivity = new StickerPackActivity(result);
                registrar.addActivityResultListener(stickerPackActivity);

                String whatsAppPackage = call.argument("package");
                String stickerPackIdentifier = call.argument("identifier");
                String stickerPackName = call.argument("name");

                Intent intent = StickerPackActivity.createIntentToAddStickerPack(getContentProviderAuthority(registrar.context()), stickerPackIdentifier, stickerPackName);
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
}
