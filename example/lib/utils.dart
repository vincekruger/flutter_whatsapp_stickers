import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_whatsapp_stickers/flutter_whatsapp_stickers.dart';

Future<void> processResponse(
    {StickerPackResult action = StickerPackResult.UNKNOWN,
    bool? result,
    String error = "Error",
    BuildContext? context,
    Function? successCallback}) async {
  SnackBar? snackBar;

  switch (action) {
    case StickerPackResult.SUCCESS:
    case StickerPackResult.ADD_SUCCESSFUL:
    case StickerPackResult.ALREADY_ADDED:
      successCallback!();
      break;
    case StickerPackResult.CANCELLED:
      snackBar = SnackBar(content: Text('Cancelled Sticker Pack Install'));
      break;
    case StickerPackResult.ERROR:
      snackBar = SnackBar(content: Text(error));
      break;
    case StickerPackResult.UNKNOWN:
      snackBar = SnackBar(content: Text('Unkown Error - check the logs'));
      break;
    default:
      snackBar = SnackBar(content: Text('Unkown Error - check the logs'));
      break;
  }

  if (snackBar != null && context != null) {
    ScaffoldMessenger.of(context).showSnackBar(snackBar);
  }
}
