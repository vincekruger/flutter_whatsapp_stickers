import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_whatsapp_stickers/flutter_whatsapp_stickers.dart';

class WhatsAppInformation extends StatefulWidget {
  @override
  _WhatsAppInformationState createState() => _WhatsAppInformationState();
}

class _WhatsAppInformationState extends State<WhatsAppInformation> {
  String _platformVersion = 'Unknown';
  bool _whatsAppInstalled = false;
  bool _whatsAppConsumerAppInstalled = false;
  bool _whatsAppSmbAppInstalled = false;

  @override
  void initState() {
    super.initState();
    init();
  }

  Future<void> init() async {
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
    bool whatsAppConsumerAppInstalled =
        await WhatsAppStickers.isWhatsAppConsumerAppInstalled;
    bool whatsAppSmbAppInstalled =
        await WhatsAppStickers.isWhatsAppSmbAppInstalled;

    setState(() {
      _platformVersion = platformVersion;
      _whatsAppInstalled = whatsAppInstalled;
      _whatsAppConsumerAppInstalled = whatsAppConsumerAppInstalled;
      _whatsAppSmbAppInstalled = whatsAppSmbAppInstalled;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 40),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: <Widget>[
          Text('Running on: $_platformVersion'),
          Text("WhatsApp Installed: $_whatsAppInstalled"),
          Text("WhatsApp Consumer Installed: $_whatsAppConsumerAppInstalled"),
          Text("WhatsApp Business Installed: $_whatsAppSmbAppInstalled"),
        ],
      ),
    );
  }
}
