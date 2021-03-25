import 'package:flutter/material.dart';
import 'dynamic.dart';
import 'information.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  final String title = 'WhatsApp Stickers Demo Dynamic';

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: title,
      home: DefaultTabController(
        length: 2,
        child: Scaffold(
          appBar: AppBar(
            title: Text(title),
            bottom: TabBar(
              tabs: [
                Tab(text: "Information"),
                Tab(text: "Sticker Packs"),
              ],
            ),
          ),
          body: TabBarView(
            children: [
              WhatsAppInformation(),
              DynamicContent(),
            ],
          ),
        ),
      ),
    );
  }
}
