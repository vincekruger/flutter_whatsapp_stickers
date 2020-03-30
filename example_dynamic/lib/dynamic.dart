import 'dart:io';
import 'dart:typed_data';
import 'dart:convert';
import 'dart:math';

import 'package:archive/archive.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import 'package:flutter_whatsapp_stickers/flutter_whatsapp_stickers.dart';
import 'package:whatsapp_stickers_example_dynamic/utils.dart';

int random() {
  var rng = new Random();
  return rng.nextInt(100);
}

class DynamicContent extends StatefulWidget {
  @override
  _DynamicContentState createState() => _DynamicContentState();
}

class _DynamicContentState extends State<DynamicContent> {
  final WhatsAppStickers _waStickers = WhatsAppStickers();

  final String _stickerPackIdentifier1 = "cats";
  final String _stickerPackName1 = "Cats";

  final String _stickerPackIdentifier2 = "dragon";
  final String _stickerPackName2 = "Dragon";

  bool _stickerPackInstalled1 = false;
  bool _stickerPackInstalled2 = false;

  Directory _applicationDirectory;
  Directory _stickerPacksDirectory;
  File _stickerPacksConfigFile;
  Map<String, dynamic> _stickerPacksConfig;
  List<dynamic> _storedStickerPacks;

  @override
  void initState() {
    super.initState();
    prepareFolderStructure();
    checkInstallationStatuses();
  }

  void prepareFolderStructure() async {
    _applicationDirectory = await getApplicationDocumentsDirectory();
    _stickerPacksDirectory =
        Directory("${_applicationDirectory.path}/sticker_packs");
    _stickerPacksConfigFile =
        File("${_stickerPacksDirectory.path}/sticker_packs.json");

    // Create the config file if it doesn't exist.
    if (!await _stickerPacksConfigFile.exists()) {
      _stickerPacksConfigFile.createSync(recursive: true);
      _stickerPacksConfig = {
        "android_play_store_link": "",
        "ios_app_store_link": "",
        "sticker_packs": [],
      };
      String contentsOfFile = jsonEncode(_stickerPacksConfig) + "\n";
      _stickerPacksConfigFile.writeAsStringSync(contentsOfFile, flush: true);
    }

    // Load sticker pack config
    _stickerPacksConfig =
        jsonDecode((await _stickerPacksConfigFile.readAsString()));
    _storedStickerPacks = _stickerPacksConfig['sticker_packs'];
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

  void unpackArchive(String identifier) async {
    ByteData fileData = await rootBundle.load("sticker_packs/$identifier.zip");
    final archive = ZipDecoder().decodeBytes(Uint8List.view(fileData.buffer));
    Directory packageDirectory =
        Directory("${_stickerPacksDirectory.path}/$identifier")
          ..create(recursive: true);

    for (final file in archive) {
      if (file.isFile) {
        List<int> data = file.content;
        File('${packageDirectory.path}/${file.name}')
          ..createSync(recursive: true)
          ..writeAsBytesSync(data);
      } else
        throw ("Invalid package!");
    }

    /// Read Package Contents
    File packageContentsFile = File("${packageDirectory.path}/config.json");
    Map<String, dynamic> packageContentsMap =
        jsonDecode(await packageContentsFile.readAsString());

    /// Add to global config
    _storedStickerPacks.removeWhere(
        (item) => item['identifier'] == packageContentsMap['identifier']);
    _storedStickerPacks.add(packageContentsMap);

    /// Update config file
    _stickerPacksConfig['sticker_packs'] = _storedStickerPacks;
    JsonEncoder encoder = new JsonEncoder.withIndent('  ');
    String contentsOfFile = encoder.convert(_stickerPacksConfig) + "\n";
    _stickerPacksConfigFile.deleteSync();
    _stickerPacksConfigFile.createSync(recursive: true);
    _stickerPacksConfigFile.writeAsStringSync(contentsOfFile, flush: true);

    _waStickers.updatedStickerPacks(identifier);
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
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.center,
          mainAxisSize: MainAxisSize.max,
          children: <Widget>[
            RaisedButton(
              child: Text('Unpack'),
              onPressed: () => unpackArchive(identifier),
            ),
            SizedBox(width: 10),
            RaisedButton(
              child: Text("Install"),
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
          ],
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
