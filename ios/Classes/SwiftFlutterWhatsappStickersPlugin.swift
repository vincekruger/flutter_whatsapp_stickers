import Flutter
import UIKit

public class SwiftFlutterWhatsappStickersPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "flutter_whatsapp_stickers", binaryMessenger: registrar.messenger())
    let instance = SwiftFlutterWhatsappStickersPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    result("iOS " + UIDevice.current.systemVersion)
  }
}
