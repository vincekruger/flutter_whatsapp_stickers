// Copyright 2021 Vince Kruger. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

import 'dart:async';
import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

typedef Future<void> MessageHandler(StickerPackResult action, bool status,
    {String error});

enum StickerPackResult {
  SUCCESS,
  ADD_SUCCESSFUL,
  ALREADY_ADDED,
  CANCELLED,
  ERROR,
  UNKNOWN,
}

enum WhatsAppPackage {
  Consumer,
  Business,
}

/// Implementation of the WhatsApp Stickers API for Flutter.
class WhatsAppStickers {
  static const consumerWhatsAppPackageName = 'com.whatsapp';
  static const businessWhatsAppPackageName = 'com.whatsapp.w4b';

  static const MethodChannel _channel =
      const MethodChannel('io.github.vincekruger/whatsapp_stickers');
  MessageHandler? _addStickerPackListener;

  /// Get the platform version
  static Future<String> get platformVersion async {
    return await _channel.invokeMethod('getPlatformVersion');
  }

  /// Check if WhatsApp is installed
  /// This will check both the comsumer and business packages
  static Future<bool> get isWhatsAppInstalled async {
    return await _channel.invokeMethod("isWhatsAppInstalled");
  }

  /// Check if the WhatsApp consumer package is installed
  static Future<bool> get isWhatsAppConsumerAppInstalled async {
    return await _channel.invokeMethod("isWhatsAppConsumerAppInstalled");
  }

  /// Check if the WhatsApp business package is installed
  static Future<bool> get isWhatsAppSmbAppInstalled async {
    return await _channel.invokeMethod("isWhatsAppSmbAppInstalled");
  }

  /// Launch WhatsApp
  static void launchWhatsApp() {
    _channel.invokeMethod("launchWhatsApp");
  }

  /// Check if a sticker pack is installed on WhatsApp
  ///
  /// [stickerPackIdentifier] The sticker pack identifier
  Future<bool> isStickerPackInstalled(String stickerPackIdentifier) async {
    final bool result = await _channel.invokeMethod(
        "isStickerPackInstalled", {"identifier": stickerPackIdentifier});
    return result;
  }

  /// Updated sticker packs
  ///
  /// [stickerPackIdentifier] The sticker pack identider
  void updatedStickerPacks(String stickerPackIdentifier) {
    _channel.invokeMethod("updatedStickerPackContentsFile",
        {"identifier": stickerPackIdentifier});
  }

  /// Add a sticker pack to whatsapp.
  ///
  /// [packageName] The WhatsApp package name.
  /// [stickerPackIdentifier] The sticker pack identider
  /// [stickerPackName] The sticker pack name
  /// [listener] Sets up [MessageHandler] function for incoming events.
  void addStickerPack({
    WhatsAppPackage packageName = WhatsAppPackage.Consumer,
    @required String? stickerPackIdentifier,
    @required String? stickerPackName,
    MessageHandler? listener,
  }) {
    String packageString;
    switch (packageName) {
      case WhatsAppPackage.Consumer:
        packageString = consumerWhatsAppPackageName;
        break;
      case WhatsAppPackage.Business:
        packageString = businessWhatsAppPackageName;
        break;
    }

    _addStickerPackListener = listener;
    _channel.setMethodCallHandler(_handleMethod);
    _channel.invokeMethod("addStickerPack", {
      "package": packageString,
      "identifier": stickerPackIdentifier,
      "name": stickerPackName,
    });
  }

  Future<dynamic> _handleMethod(MethodCall call) async {
    switch (call.method) {
      case "onSuccess":
        String action = call.arguments['action'];
        bool result = call.arguments['result'];
        switch (action) {
          case 'success':
            _addStickerPackListener!(StickerPackResult.SUCCESS, result);
            break;
          case 'add_successful':
            _addStickerPackListener!(StickerPackResult.ADD_SUCCESSFUL, result);
            break;
          case 'already_added':
            _addStickerPackListener!(StickerPackResult.ALREADY_ADDED, result);
            break;
          case 'cancelled':
            _addStickerPackListener!(StickerPackResult.CANCELLED, result);
            break;
          default:
            _addStickerPackListener!(StickerPackResult.UNKNOWN, result);
        }
        return null;
      case "onError":
        bool result = call.arguments['result'];
        String error = call.arguments['error'] ?? null;
        _addStickerPackListener!(StickerPackResult.ERROR, result, error: error);
        return null;
      default:
        throw UnsupportedError("Unrecognized activity handler");
    }
  }
}
