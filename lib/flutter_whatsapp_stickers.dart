import 'dart:async';

import 'package:flutter/services.dart';

class WhatsAppStickers {
  static const consumerWhatsAppPackageName = 'com.whatsapp';
  static const businessWhatsAppPackageName = 'com.whatsapp.w4b';

  static const MethodChannel _channel = const MethodChannel('io.github.vincekruger/whatsapp_stickers');

  static Future<String> get platformVersion async {
    return await _channel.invokeMethod('getPlatformVersion');
  }

  static Future<bool> get isWhatsAppInstalled async {
    return await _channel.invokeMethod("isWhatsAppInstalled");
  }

  static Future<bool> get isWhatsAppConsumerAppInstalled async {
    return await _channel.invokeMethod("isWhatsAppConsumerAppInstalled");
  }
  
  static Future<bool> get isWhatsAppSmbAppInstalled async {
    return await _channel.invokeMethod("isWhatsAppSmbAppInstalled");
  }

  Future<bool> isStickerPackInstalled(String stickerPackIdentifier) async {
    final bool result = await _channel.invokeMethod("isStickPackInstalled", {"identifier": stickerPackIdentifier});
    return result;
  }

  Future<bool> addStickerPack({String packageName, String stickerPackIdentifier, String stickerPackName}) async {
    return await _channel.invokeMethod("addStickerPack", {
      "package": packageName,
      "identifier": stickerPackIdentifier,
      "name": stickerPackName,
    });
  }
}
