#import "FlutterWhatsappStickersPlugin.h"
#import <flutter_whatsapp_stickers/flutter_whatsapp_stickers-Swift.h>

@implementation FlutterWhatsappStickersPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterWhatsappStickersPlugin registerWithRegistrar:registrar];
}
@end
