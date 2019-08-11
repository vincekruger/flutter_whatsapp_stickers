import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_whatsapp_stickers/whatsapp_stickers.dart';

void main() {
  const MethodChannel channel = MethodChannel('io.github.vincekruger/whatsapp_stickers');

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await WhatsAppStickers.platformVersion, '42');
  });
}
