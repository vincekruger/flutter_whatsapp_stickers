#import "WhatsAppStickersPlugin.h"
#import <whatsapp_stickers/whatsapp_stickers-Swift.h>

@implementation WhatsAppStickersPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftWhatsAppStickersPlugin registerWithRegistrar:registrar];
}
@end
