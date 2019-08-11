import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_whatsapp_stickers/whatsapp_stickers.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final String _stickerPackIdentifier = "1";
  final String _stickerPackName = "Cuppy";

  String _platformVersion = 'Unknown';

  bool _whatsAppInstalled = false;
  bool _whatsAppConsumerAppInstalled = false;
  bool _whatsAppSmbAppInstalled = false;
  bool _stickerPackInstalled = false;

  final WhatsAppStickers _waStickers = WhatsAppStickers();

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;

    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await WhatsAppStickers.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    bool whatsAppInstalled = await WhatsAppStickers.isWhatsAppInstalled;
    bool whatsAppConsumerAppInstalled = await WhatsAppStickers.isWhatsAppConsumerAppInstalled;
    bool whatsAppSmbAppInstalled = await WhatsAppStickers.isWhatsAppSmbAppInstalled;

    _stickerPackInstalled = await _waStickers.isStickerPackInstalled(_stickerPackIdentifier);

    setState(() {
      _platformVersion = platformVersion;
      _whatsAppInstalled = whatsAppInstalled;
      _whatsAppConsumerAppInstalled = whatsAppConsumerAppInstalled;
      _whatsAppSmbAppInstalled = whatsAppSmbAppInstalled;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('WhatsApp Sticker Pack'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: <Widget>[
              Text('Running on: $_platformVersion'),
              Text("WhatsApp Installed: $_whatsAppInstalled"),
              Text("WhatsApp Consumer Installed: $_whatsAppConsumerAppInstalled"),
              Text("WhatsApp Business Installed: $_whatsAppSmbAppInstalled"),
              Padding(
                padding: const EdgeInsets.only(top: 10),
                child: Text("Sticker Pack Name: $_stickerPackName"),
              ),
              Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: Text("Sticker Pack Identifier: $_stickerPackIdentifier"),
              ),
              RaisedButton(
                onPressed: () async {
                  await _waStickers.addStickerPack(
                    packageName: WhatsAppStickers.consumerWhatsAppPackageName,
                    stickerPackIdentifier: _stickerPackIdentifier,
                    stickerPackName: _stickerPackName,
                  );
                  bool installed = await _waStickers.isStickerPackInstalled(_stickerPackIdentifier);
                  setState(() {
                    _stickerPackInstalled = installed;
                  });
                },
                child: Text("Add Sticker Pack"),
              ),
              RaisedButton(
                onPressed: () async {
                  bool installed = await _waStickers.isStickerPackInstalled(_stickerPackIdentifier);
                  setState(() {
                    _stickerPackInstalled = installed;
                  });
                },
                child: Text("Check if Sticker Pack is Installed"),
              ),
              Text(_stickerPackInstalled ? "Yes... it's installed" : "No! Install it"),
            ],
          ),
        ),
      ),
    );
  }
}