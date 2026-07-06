#import "ImageCacheManager.h"

@implementation ImageCacheManager

static NSCache<NSString*, UIImage*> *_imageCache = nil;

static ImageCacheManager *_instance = nil;

+ (id)instance
{
    if (!_instance) {
        _instance = [[ImageCacheManager alloc] init];
    }
    return _instance;
}

- (id)init
{
    if (!_instance) {
        _instance = [super init];
        _imageCache = [[NSCache alloc] init];
    }
    return _instance;
}

// Contract: completion is ALWAYS invoked exactly once, on the main queue.
// A nil image means the source could not be loaded — callers must handle it
// (fall back to a default icon) instead of silently dropping the map object.
- (void)getWithSource:(NSString * _Nonnull)source completion:(void (^ _Nonnull __strong)(UIImage * _Nullable __strong))completion {
    UIImage *cachedImage = [_imageCache objectForKey:source];
    if (cachedImage) {
        dispatch_async(dispatch_get_main_queue(), ^{
            completion(cachedImage);
        });
        return;
    }

    NSURL *url = [NSURL URLWithString:source];
    if (!url) {
        NSLog(@"[Yamap] Invalid image source URL: %@", source);
        dispatch_async(dispatch_get_main_queue(), ^{
            completion(nil);
        });
        return;
    }

    NSURLSessionDataTask *task = [[NSURLSession sharedSession] dataTaskWithURL:url completionHandler:^(NSData * _Nullable taskData, NSURLResponse * _Nullable response, NSError * _Nullable error) {
        UIImage *image = nil;

        if (error) {
            NSLog(@"[Yamap] Failed to fetch image data with error: %@", error);
        } else if (!taskData) {
            NSLog(@"[Yamap] Failed to load image data from URL: %@", source);
        } else {
            image = [UIImage imageWithData:taskData];
            if (!image) {
                NSLog(@"[Yamap] Failed to create image from loaded data: %@", source);
            }
        }

        if (image) {
            [_imageCache setObject:image forKey:source];
        }

        dispatch_async(dispatch_get_main_queue(), ^{
            completion(image);
        });
    }];
    [task resume];
}

@end
