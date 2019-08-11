import Flutter
import UIKit

public class SwiftWhatsAppStickersPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "io.github.vincekruger/whatsapp_stickers", binaryMessenger: registrar.messenger())
    let instance = SwiftWhatsAppStickersPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    result("iOS " + UIDevice.current.systemVersion)
  }
}
