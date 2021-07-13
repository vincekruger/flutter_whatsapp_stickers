/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.github.vincekruger.whatsapp_stickers;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.CancellationSignal;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import io.flutter.util.PathUtils;

public class StickerContentProvider extends ContentProvider {

    /**
     * Do not change the strings listed below, as these are used by WhatsApp. And
     * changing these will break the interface between sticker app and WhatsApp.
     */
    public static final String STICKER_PACK_IDENTIFIER_IN_QUERY = "sticker_pack_identifier";
    public static final String STICKER_PACK_NAME_IN_QUERY = "sticker_pack_name";
    public static final String STICKER_PACK_PUBLISHER_IN_QUERY = "sticker_pack_publisher";
    public static final String STICKER_PACK_ICON_IN_QUERY = "sticker_pack_icon";
    public static final String ANDROID_APP_DOWNLOAD_LINK_IN_QUERY = "android_play_store_link";
    public static final String IOS_APP_DOWNLOAD_LINK_IN_QUERY = "ios_app_download_link";
    public static final String PUBLISHER_EMAIL = "sticker_pack_publisher_email";
    public static final String PUBLISHER_WEBSITE = "sticker_pack_publisher_website";
    public static final String PRIVACY_POLICY_WEBSITE = "sticker_pack_privacy_policy_website";
    public static final String LICENSE_AGREEMENT_WEBSITE = "sticker_pack_license_agreement_website";
    public static final String IMAGE_DATA_VERSION = "image_data_version";
    public static final String AVOID_CACHE = "whatsapp_will_not_cache_stickers";
    public static final String ANIMATED_STICKER_PACK = "animated_sticker_pack";

    public static final String STICKER_FILE_NAME_IN_QUERY = "sticker_file_name";
    public static final String STICKER_FILE_EMOJI_IN_QUERY = "sticker_emoji";
    public static final String CONTENT_PATH = "sticker_packs/";
    public static final String CONTENT_FILE_NAME = BuildConfig.STICKER_PACK_FILE;

    private boolean nonAssetContentProvider = false;
    private String contentPath;

    /**
     * Do not change the values in the UriMatcher because otherwise, WhatsApp will
     * not be able to fetch the stickers from the ContentProvider.
     */
    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    private static final String METADATA = "metadata";
    private static final int METADATA_CODE = 1;

    private static final int METADATA_CODE_FOR_SINGLE_PACK = 2;

    static final String STICKERS = "stickers";
    private static final int STICKERS_CODE = 3;

    static final String STICKERS_ASSET = "stickers_asset";
    private static final int STICKERS_ASSET_CODE = 4;

    private static final int STICKER_PACK_TRAY_ICON_CODE = 5;

    private List<StickerPack> stickerPackList;

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        final String authority = WhatsAppStickersPlugin.getContentProviderAuthority(context);
        if (!authority.startsWith(Objects.requireNonNull(getContext()).getPackageName())) {
            throw new IllegalStateException(
                    "your authority (" + authority + ") for the content provider should start with your package name: "
                            + getContext().getPackageName());
        }

        // Check meta-data
        try {
            final ApplicationInfo ai = getContext().getPackageManager().getApplicationInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            final Bundle bundle = ai.metaData;
            nonAssetContentProvider = bundle.getBoolean("NonAssetContentProvider");
        } catch (PackageManager.NameNotFoundException | NullPointerException e) {
            nonAssetContentProvider = false;
        }

        // Set sticker packs path
        contentPath = nonAssetContentProvider ? PathUtils.getDataDirectory(getContext()) + "/" + CONTENT_PATH
                : "flutter_assets/" + CONTENT_PATH;

        // The call to get the metadata for the sticker packs.
        MATCHER.addURI(authority, METADATA, METADATA_CODE);

        // The call to get the metadata for single sticker pack. * represent the
        // identifier
        MATCHER.addURI(authority, METADATA + "/*", METADATA_CODE_FOR_SINGLE_PACK);

        // Gets the list of stickers for a sticker pack, * represents the identifier.
        MATCHER.addURI(authority, STICKERS + "/*", STICKERS_CODE);

        // Gets the an asset from a sticker pack
        MATCHER.addURI(authority, STICKERS_ASSET + "/*/*", STICKERS_ASSET_CODE);

        return true;
    }

    @Override
    public MatrixCursor query(@NonNull final Uri uri, @Nullable final String[] projection, final String selection,
            final String[] selectionArgs, final String sortOrder) {
        final int code = MATCHER.match(uri);
        if (code == METADATA_CODE) {
            return getPackForAllStickerPacks(uri);
        } else if (code == METADATA_CODE_FOR_SINGLE_PACK) {
            return getCursorForSingleStickerPack(uri);
        } else if (code == STICKERS_CODE) {
            return getStickersForAStickerPack(uri);
        } else {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Nullable
    @Override
    public AssetFileDescriptor openAssetFile(@NonNull final Uri uri, @NonNull final String mode) {
        final int matchCode = MATCHER.match(uri);
        if (matchCode == STICKERS_ASSET_CODE || matchCode == STICKER_PACK_TRAY_ICON_CODE) {
            return getImageAsset(uri);
        }
        return null;
    }

    @Override
    public String getType(@NonNull final Uri uri) {
        final Context context = getContext();
        assert context != null;

        final int matchCode = MATCHER.match(uri);
        switch (matchCode) {
            case METADATA_CODE:
                return "vnd.android.cursor.dir/vnd." + WhatsAppStickersPlugin.getContentProviderAuthority(context) + "."
                        + METADATA;
            case METADATA_CODE_FOR_SINGLE_PACK:
                return "vnd.android.cursor.item/vnd." + WhatsAppStickersPlugin.getContentProviderAuthority(context)
                        + "." + METADATA;
            case STICKERS_CODE:
                return "vnd.android.cursor.dir/vnd." + WhatsAppStickersPlugin.getContentProviderAuthority(context) + "."
                        + STICKERS;
            case STICKERS_ASSET_CODE:
                final List<String> pathSegments = uri.getPathSegments();

                if (pathSegments.size() != 3) {
                    throw new IllegalArgumentException("path segments should be 3, uri is: " + uri);
                }

                final String fileName = pathSegments.get(pathSegments.size() - 1);
                final String extension = fileName.substring(fileName.lastIndexOf("."));

                return extension.equals(".png") ? "image/png" : "image/webp";
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    private synchronized void readContentFile(@NonNull final Context context) {
        if (nonAssetContentProvider) {
            final File file = new File(contentPath + CONTENT_FILE_NAME);
            try (InputStream contentsInputStream = new FileInputStream(file)) {
                stickerPackList = ContentFileParser.parseStickerPacks(contentsInputStream);
            } catch (IOException | IllegalStateException e) {
                throw new RuntimeException(CONTENT_FILE_NAME + " file has some issues: " + e.getMessage(), e);
            }
        } else {
            try (InputStream contentsInputStream = context.getAssets().open(contentPath + CONTENT_FILE_NAME)) {
                stickerPackList = ContentFileParser.parseStickerPacks(contentsInputStream);
            } catch (IOException | IllegalStateException e) {
                throw new RuntimeException(CONTENT_FILE_NAME + " file has some issues: " + e.getMessage(), e);
            }
        }
    }

    private List<StickerPack> getStickerPackList() {
        if (stickerPackList == null || nonAssetContentProvider) {
            readContentFile(Objects.requireNonNull(getContext()));
        }
        return stickerPackList;
    }

    private MatrixCursor getPackForAllStickerPacks(@NonNull final Uri uri) {
        return getStickerPackInfo(uri, getStickerPackList());
    }

    private MatrixCursor getCursorForSingleStickerPack(@NonNull final Uri uri) {
        final String identifier = uri.getLastPathSegment();
        for (final StickerPack stickerPack : getStickerPackList()) {
            if (identifier.equals(stickerPack.identifier)) {
                return getStickerPackInfo(uri, Collections.singletonList(stickerPack));
            }
        }

        return getStickerPackInfo(uri, new ArrayList<StickerPack>());
    }

    @NonNull
    private MatrixCursor getStickerPackInfo(@NonNull final Uri uri, @NonNull final List<StickerPack> stickerPackList) {
        final MatrixCursor cursor = new MatrixCursor(new String[] { STICKER_PACK_IDENTIFIER_IN_QUERY,
                STICKER_PACK_NAME_IN_QUERY, STICKER_PACK_PUBLISHER_IN_QUERY, STICKER_PACK_ICON_IN_QUERY,
                ANDROID_APP_DOWNLOAD_LINK_IN_QUERY, IOS_APP_DOWNLOAD_LINK_IN_QUERY, PUBLISHER_EMAIL, PUBLISHER_WEBSITE,
                PRIVACY_POLICY_WEBSITE, LICENSE_AGREEMENT_WEBSITE, IMAGE_DATA_VERSION, AVOID_CACHE, ANIMATED_STICKER_PACK, });

        for (final StickerPack stickerPack : stickerPackList) {
            final MatrixCursor.RowBuilder builder = cursor.newRow();
            builder.add(stickerPack.identifier);
            builder.add(stickerPack.name);
            builder.add(stickerPack.publisher);
            builder.add(stickerPack.trayImageFile);
            builder.add(stickerPack.androidPlayStoreLink);
            builder.add(stickerPack.iosAppStoreLink);
            builder.add(stickerPack.publisherEmail);
            builder.add(stickerPack.publisherWebsite);
            builder.add(stickerPack.privacyPolicyWebsite);
            builder.add(stickerPack.licenseAgreementWebsite);
            builder.add(stickerPack.imageDataVersion);
            builder.add(stickerPack.avoidCache ? 1 : 0);
            builder.add(stickerPack.animatedStickerPack ? 1 : 0);
        }

        cursor.setNotificationUri(Objects.requireNonNull(getContext()).getContentResolver(), uri);
        return cursor;
    }

    @NonNull
    private MatrixCursor getStickersForAStickerPack(@NonNull final Uri uri) {
        final String identifier = uri.getLastPathSegment();
        final MatrixCursor cursor = new MatrixCursor(
                new String[] { STICKER_FILE_NAME_IN_QUERY, STICKER_FILE_EMOJI_IN_QUERY });

        for (final StickerPack stickerPack : getStickerPackList()) {
            if (identifier.equals(stickerPack.identifier)) {
                for (final Sticker sticker : stickerPack.getStickers()) {
                    cursor.addRow(new Object[] { sticker.imageFileName, TextUtils.join(",", sticker.emojis) });
                }
            }
        }

        cursor.setNotificationUri(Objects.requireNonNull(getContext()).getContentResolver(), uri);
        return cursor;
    }

    private AssetFileDescriptor getImageAsset(final Uri uri) throws IllegalArgumentException {
        final AssetManager am = Objects.requireNonNull(getContext()).getAssets();
        final List<String> pathSegments = uri.getPathSegments();

        if (pathSegments.size() != 3) {
            throw new IllegalArgumentException("path segments should be 3, uri is: " + uri);
        }

        final String fileName = pathSegments.get(pathSegments.size() - 1);
        final String identifier = pathSegments.get(pathSegments.size() - 2);

        if (TextUtils.isEmpty(identifier)) {
            throw new IllegalArgumentException("identifier is empty, uri: " + uri);
        }

        if (TextUtils.isEmpty(fileName)) {
            throw new IllegalArgumentException("file name is empty, uri: " + uri);
        }

        // making sure the file that is trying to be fetched is in the list of stickers.
        for (final StickerPack stickerPack : getStickerPackList()) {
            if (identifier.equals(stickerPack.identifier)) {
                if (fileName.equals(stickerPack.trayImageFile)) {
                    return fetchFile(uri, am, fileName, identifier);
                } else {
                    for (final Sticker sticker : stickerPack.getStickers()) {
                        if (fileName.equals(sticker.imageFileName)) {
                            return fetchFile(uri, am, fileName, identifier);
                        }
                    }
                }
            }
        }
        return null;
    }

    private AssetFileDescriptor fetchFile(@NonNull final Uri uri, @NonNull final AssetManager am,
            @NonNull final String fileName, @NonNull final String identifier) {
        return nonAssetContentProvider ? fetchNonAssetFile(uri, fileName, identifier)
                : fetchAssetFile(uri, am, fileName, identifier);
    }

    private AssetFileDescriptor fetchNonAssetFile(final Uri uri, final String fileName, final String identifier) {
        try {
            final File file = new File(contentPath + identifier, fileName);
            return new AssetFileDescriptor(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY), 0,
                    AssetFileDescriptor.UNKNOWN_LENGTH);
        } catch (final IOException e) {
            Log.e(Objects.requireNonNull(getContext()).getPackageName(),
                    "IOException when getting asset file, uri:" + uri, e);
            return null;
        }
    }

    private AssetFileDescriptor fetchAssetFile(@NonNull final Uri uri, @NonNull final AssetManager am,
            @NonNull final String fileName, @NonNull final String identifier) {
        try {
            return am.openFd(contentPath + identifier + "/" + fileName);
        } catch (final IOException e) {
            Log.e(Objects.requireNonNull(getContext()).getPackageName(),
                    "IOException when getting asset file, uri:" + uri, e);
            return null;
        }
    }

    @Override
    public boolean refresh(final Uri uri, final Bundle args, final CancellationSignal cancellationSignal) {
        return true;
    }

    @Override
    public int delete(@NonNull final Uri uri, @NonNull final String selection, final String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Uri insert(@NonNull final Uri uri, final ContentValues values) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int update(@NonNull final Uri uri, final ContentValues values, final String selection,
            final String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported");
    }

    public static String convertStreamToString(final InputStream is) throws Exception {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        final StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile(final String filePath) throws Exception {
        final File fl = new File(filePath);
        final FileInputStream fin = new FileInputStream(fl);
        final String ret = convertStreamToString(fin);
        // Make sure you close all streams.
        fin.close();
        return ret;
    }
}
