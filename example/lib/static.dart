import 'package:flutter/material.dart';
import 'package:flutter_whatsapp_stickers/flutter_whatsapp_stickers.dart';
import 'package:whatsapp_stickers_example/utils.dart';

class StaticContent extends StatefulWidget {
  @override
  _StaticContentState createState() => _StaticContentState();
}

class _StaticContentState extends State<StaticContent> {
  final WhatsAppStickers _waStickers = WhatsAppStickers();

  final String _stickerPackIdentifier1 = "1";
  final String _stickerPackName1 = "Cuppy";

  final String _stickerPackIdentifier2 = "2";
  final String _stickerPackName2 = "Doge The Dog";

  bool _stickerPackInstalled1 = false;
  bool _stickerPackInstalled2 = false;

  @override
  void initState() {
    super.initState();
    checkInstallationStatuses();
  }

  void checkInstallationStatuses() async {
    bool installed1 =
        await _waStickers.isStickerPackInstalled(_stickerPackIdentifier1);
    bool installed2 =
        await _waStickers.isStickerPackInstalled(_stickerPackIdentifier2);
    setState(() {
      _stickerPackInstalled1 = installed1;
      _stickerPackInstalled2 = installed2;
    });
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.symmetric(vertical: 40),
      children: <Widget>[
        stickerPack(
            _stickerPackName1, _stickerPackIdentifier1, _stickerPackInstalled1),
        Divider(height: 40),
        stickerPack(
            _stickerPackName2, _stickerPackIdentifier2, _stickerPackInstalled2),
      ],
    );
  }

  Widget stickerPack(String name, String identifier, bool installed) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: <Widget>[
        Text("Sticker Pack Name: $name"),
        Text("Sticker Pack Identifier: $identifier"),
        SizedBox(height: 10),
        RaisedButton(
          child: Text("Add $name"),
          onPressed: () async {
            _waStickers.addStickerPack(
              packageName: WhatsAppPackage.Consumer,
              stickerPackIdentifier: identifier,
              stickerPackName: name,
              listener: (action, result, {error}) => processResponse(
                action: action,
                result: result,
                error: error,
                successCallback: checkInstallationStatuses,
                context: context,
              ),
            );
          },
        ),
        RaisedButton(
          child: Text("Check if Sticker Pack Status"),
          onPressed: () => checkInstallationStatuses(),
        ),
        Text(installed ? "Yes... it's installed" : "No! Install it"),
      ],
    );
  }
}
