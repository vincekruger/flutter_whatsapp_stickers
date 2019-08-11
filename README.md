# WhatsApp Stickers plugin for Flutter

Note: This plugin is still under development and for now there is only support for Android. Feedback and Pull Requests welcome!

## Getting Started

Add `flutter_whatsapp_stickers` as a [dependency in your pubspec.yaml file](https://flutter.io/platform-plugins/).

This project is a starting point for a Flutter
[plug-in package](https://flutter.dev/developing-packages/),
a specialized package that includes platform-specific implementation code for
Android and/or iOS.

For help getting started with Flutter, view our 
[online documentation](https://flutter.dev/docs), which offers tutorials, 
samples, guidance on mobile development, and a full API reference.

### Android

Android the following to your app's gradle file

```
android {
    aaptOptions {
        noCompress "webp"
    }
}
```

If you are using assets that are not bundled in your build, then you need to add the following to your Manifest file.
Note that you cannot use both.  It's either non assets or you should package the stickers in the app.

```xml
<meta-data android:name="NonAssetContentProvider" android:value="true" />
```

Here is an example of how to create a custom stickers_packs.json file.


```dart
void createLocalFile() async {
  String dir = (await getApplicationDocumentsDirectory()).path;
  Directory stickersDirectory = Directory("$dir/sticker_packs");
  if (!await stickersDirectory.exists()) {
    await stickersDirectory.create();
  }

  File jsonFile = File('$dir/sticker_packs/sticker_packs.json');
  String content = jsonEncode(stickerPacks);
  jsonFile.writeAsStringSync(content);
}
```

### Example

Check if WhatsApp is installed.

```dart
bool whatsAppInstalled = await WhatsAppStickers.isWhatsAppInstalled;
```

Check if WhatsApp consumer app is installed

```dart
bool whatsAppConsumerAppInstalled = await WhatsAppStickers.isWhatsAppConsumerAppInstalled;
```

Check if WhatsApp business app is installed


```dart
bool whatsAppSmbAppInstalled = await WhatsAppStickers.isWhatsAppSmbAppInstalled;
```

Check if is a sticker pack is installed.

``` dart
_stickerPackInstalled = await WhatsAppStickers().isStickerPackInstalled(_stickerPackIdentifier);
```

Add a sticker pack to WhatsApp

```dart
WhatsAppStickers().addStickerPack(
  packageName: WhatsAppStickers.consumerWhatsAppPackageName,
  stickerPackIdentifier: _stickerPackIdentifier,
  stickerPackName: _stickerPackName,
);
```

