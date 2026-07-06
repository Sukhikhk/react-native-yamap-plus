#ifndef ImageCacheManager_h
#define ImageCacheManager_h

@interface ImageCacheManager : NSObject;

+ (id _Nonnull) instance;

/// Always invokes `completion` exactly once on the main queue. `image` is nil
/// when the source failed to load — callers must fall back, not drop.
- (void)getWithSource: (NSString*_Nonnull) source completion:(void (NS_SWIFT_SENDABLE ^_Nonnull)(UIImage * _Nullable image))completion;

@end

#endif
