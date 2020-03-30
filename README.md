# WhatsApp Stickers plugin for Flutter

[![pub package](https://img.shields.io/pub/v/flutter_whatsapp_stickers.svg)](https://pub.dartlang.org/packages/flutter_whatsapp_stickers)

Note: This plugin is still under development and for now, only Android is supported.
Feedback and Pull Requests welcome!

## Getting Started

Add [flutter_whatsapp_stickers](https://pub.dev/packages/flutter_whatsapp_stickers) as a [dependency in your pubspec.yaml file](https://flutter.io/platform-plugins/).

Check out the example directory for a sample app.

## Android Configuration

Add the following option to your `app\build.gradle` file.  This will prevent all WebP files from being compressed.

```
android {
    aaptOptions {
        noCompress "webp"
    }
}
```

### Packaged Assess

You need to include your assets in your pubspec.yaml file.

```yaml
flutter:
  assets:
    - sticker_packs/sticker_packs.json
    - sticker_packs/1/
```

### Dynamic Content

If you are using assets that are not bundled in your build, then you need to add the following to your Manifest file. Note that you cannot use both.

```xml
<meta-data android:name="NonAssetContentProvider" android:value="true" />
```

### Sticker Pack Contents File

To change the stickers packs file, add this Build Config Field to your `app\build.gradle` file.  The default is `sticker_packs.json`.

```
buildConfigField("String", "STICKER_PACK_FILE", "\"sticker_packs.json\"")
```

## iOS Integration

Currently, there is no iOS support.  Pull requests for this are more than welcome for this.

## Methods

Check if WhatsApp is installed.

```dart
bool whatsAppInstalled = await WhatsAppStickers.isWhatsAppInstalled;
```

Check if the WhatsApp Consumer package is installed

```dart
bool whatsAppConsumerAppInstalled = await WhatsAppStickers.isWhatsAppConsumerAppInstalled;
```

Check if the WhatsApp Business package app is installed

```dart
bool whatsAppSmbAppInstalled = await WhatsAppStickers.isWhatsAppSmbAppInstalled;
```

Check if a sticker pack is installed.

``` dart
_stickerPackInstalled = await WhatsAppStickers().isStickerPackInstalled(_stickerPackIdentifier);
```

Add a sticker pack to WhatsApp.

```dart
WhatsAppStickers().addStickerPack(
  packageName: WhatsAppPackage.Consumer,
  stickerPackIdentifier: _stickerPackIdentifier,
  stickerPackName: _stickerPackName,
  listener: _listener,
);
```

Updated Sticker Pack contents and notify listeners

```dart
WhatsAppStickers().updatedStickerPacks(_stickerPackIdentifier);
```

The Add Sticker Pack Listener

```dart
Future<void> _listener(StickerPackResult action, bool result, {String error}) async {
    // Do what you must here
}
```

Here is an example of how to create a custom `stickers_packs.json` file.

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