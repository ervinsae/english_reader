#import <AppKit/AppKit.h>
#import <Foundation/Foundation.h>
#import <PDFKit/PDFKit.h>

static void PrintUsage(void) {
    fprintf(
        stderr,
        "usage:\n"
        "  pdf_book_renderer inspect <pdf-path>\n"
        "  pdf_book_renderer render <pdf-path> <page-index> <output-png> [max-dimension]\n"
    );
}

static PDFDocument *OpenDocument(NSString *path) {
    NSURL *url = [NSURL fileURLWithPath:path];
    return [[PDFDocument alloc] initWithURL:url];
}

static NSString *JSONStringForObject(id object) {
    NSError *error = nil;
    NSData *data = [NSJSONSerialization dataWithJSONObject:object options:NSJSONWritingPrettyPrinted error:&error];
    if (data == nil || error != nil) {
        fprintf(stderr, "failed to encode json: %s\n", error.localizedDescription.UTF8String);
        return nil;
    }
    return [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
}

static int InspectDocument(NSString *path) {
    PDFDocument *document = OpenDocument(path);
    if (document == nil) {
        fprintf(stderr, "failed to open pdf: %s\n", path.UTF8String);
        return 1;
    }

    NSMutableArray *pages = [NSMutableArray arrayWithCapacity:(NSUInteger)document.pageCount];
    for (NSInteger index = 0; index < document.pageCount; index++) {
        PDFPage *page = [document pageAtIndex:index];
        NSRect bounds = [page boundsForBox:kPDFDisplayBoxMediaBox];
        [pages addObject:@{
            @"index": @(index),
            @"label": page.label ?: @"",
            @"width": @(bounds.size.width),
            @"height": @(bounds.size.height),
        }];
    }

    NSDictionary *attributes = document.documentAttributes ?: @{};
    NSDictionary *payload = @{
        @"pageCount": @(document.pageCount),
        @"documentTitle": attributes[PDFDocumentTitleAttribute] ?: @"",
        @"pages": pages,
    };

    NSString *json = JSONStringForObject(payload);
    if (json == nil) {
        return 1;
    }
    printf("%s\n", json.UTF8String);
    return 0;
}

static int RenderPage(NSString *path, NSInteger pageIndex, NSString *outputPath, CGFloat maxDimension) {
    PDFDocument *document = OpenDocument(path);
    if (document == nil) {
        fprintf(stderr, "failed to open pdf: %s\n", path.UTF8String);
        return 1;
    }

    PDFPage *page = [document pageAtIndex:pageIndex];
    if (page == nil) {
        fprintf(stderr, "page index out of range: %ld\n", (long)pageIndex);
        return 1;
    }

    NSRect pageBounds = [page boundsForBox:kPDFDisplayBoxMediaBox];
    CGFloat longestSide = MAX(pageBounds.size.width, pageBounds.size.height);
    CGFloat scale = 1.0;
    if (maxDimension > 0.0 && longestSide > 0.0) {
        scale = maxDimension / longestSide;
    }
    if (scale <= 0.0) {
        scale = 1.0;
    }

    NSInteger pixelWidth = MAX((NSInteger)llround(pageBounds.size.width * scale), 1);
    NSInteger pixelHeight = MAX((NSInteger)llround(pageBounds.size.height * scale), 1);

    NSBitmapImageRep *bitmap = [[NSBitmapImageRep alloc]
        initWithBitmapDataPlanes:NULL
                      pixelsWide:pixelWidth
                      pixelsHigh:pixelHeight
                   bitsPerSample:8
                 samplesPerPixel:4
                        hasAlpha:YES
                        isPlanar:NO
                  colorSpaceName:NSDeviceRGBColorSpace
                     bytesPerRow:0
                    bitsPerPixel:0];
    if (bitmap == nil) {
        fprintf(stderr, "failed to allocate bitmap\n");
        return 1;
    }

    NSGraphicsContext *graphicsContext = [NSGraphicsContext graphicsContextWithBitmapImageRep:bitmap];
    [NSGraphicsContext saveGraphicsState];
    [NSGraphicsContext setCurrentContext:graphicsContext];

    CGContextRef cgContext = [graphicsContext CGContext];
    CGContextSetRGBFillColor(cgContext, 1.0, 1.0, 1.0, 1.0);
    CGContextFillRect(cgContext, CGRectMake(0.0, 0.0, pixelWidth, pixelHeight));
    CGContextScaleCTM(cgContext, scale, scale);
    [page drawWithBox:kPDFDisplayBoxMediaBox toContext:cgContext];

    [NSGraphicsContext restoreGraphicsState];

    NSData *pngData = [bitmap representationUsingType:NSBitmapImageFileTypePNG properties:@{}];
    if (pngData == nil) {
        fprintf(stderr, "failed to encode png\n");
        return 1;
    }

    NSString *directoryPath = [outputPath stringByDeletingLastPathComponent];
    NSError *directoryError = nil;
    [[NSFileManager defaultManager] createDirectoryAtPath:directoryPath
                              withIntermediateDirectories:YES
                                               attributes:nil
                                                    error:&directoryError];
    if (directoryError != nil) {
        fprintf(stderr, "failed to create output directory: %s\n", directoryError.localizedDescription.UTF8String);
        return 1;
    }

    if (![pngData writeToFile:outputPath atomically:YES]) {
        fprintf(stderr, "failed to write png: %s\n", outputPath.UTF8String);
        return 1;
    }

    NSDictionary *payload = @{
        @"pageIndex": @(pageIndex),
        @"path": outputPath,
        @"pixelWidth": @(pixelWidth),
        @"pixelHeight": @(pixelHeight),
        @"scale": @(scale),
    };
    NSString *json = JSONStringForObject(payload);
    if (json == nil) {
        return 1;
    }
    printf("%s\n", json.UTF8String);
    return 0;
}

int main(int argc, const char *argv[]) {
    @autoreleasepool {
        if (argc < 3) {
            PrintUsage();
            return 2;
        }

        NSString *command = [NSString stringWithUTF8String:argv[1]];
        if ([command isEqualToString:@"inspect"]) {
            return InspectDocument([NSString stringWithUTF8String:argv[2]]);
        }

        if ([command isEqualToString:@"render"]) {
            if (argc < 5) {
                PrintUsage();
                return 2;
            }
            CGFloat maxDimension = argc >= 6 ? [[NSString stringWithUTF8String:argv[5]] doubleValue] : 1600.0;
            return RenderPage(
                [NSString stringWithUTF8String:argv[2]],
                [[NSString stringWithUTF8String:argv[3]] integerValue],
                [NSString stringWithUTF8String:argv[4]],
                maxDimension
            );
        }

        PrintUsage();
        return 2;
    }
}
