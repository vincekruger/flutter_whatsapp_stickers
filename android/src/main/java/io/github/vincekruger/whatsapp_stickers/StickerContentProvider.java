/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.github.vincekruger.whatsapp_stickers;

import android.content.ContentProvider;
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
     * Do not change the strings listed below, as these are used by WhatsApp. And changing these will break the interface between sticker app and WhatsApp.
     */
    private static final String STICKER_PACK_IDENTIFIER_IN_QUERY = "sticker_pack_identifier";
    private static final String STICKER_PACK_NAME_IN_QUERY = "sticker_pack_name";
    private static final String STICKER_PACK_PUBLISHER_IN_QUERY = "sticker_pack_publisher";
    private static final String STICKER_PACK_ICON_IN_QUERY = "sticker_pack_icon";
    private static final String ANDROID_APP_DOWNLOAD_LINK_IN_QUERY = "android_play_store_link";
    private static final String IOS_APP_DOWNLOAD_LINK_IN_QUERY = "ios_app_download_link";
    private static final String PUBLISHER_EMAIL = "sticker_pack_publisher_email";
    private static final String PUBLISHER_WEBSITE = "sticker_pack_publisher_website";
    private static final String PRIVACY_POLICY_WEBSITE = "sticker_pack_privacy_policy_website";
    private static final String LICENSE_AGREEMENT_WEBSITE = "sticker_pack_license_agreement_website";

    private static final String STICKER_FILE_NAME_IN_QUERY = "sticker_file_name";
    private static final String STICKER_FILE_EMOJI_IN_QUERY = "sticker_emoji";
    private static final String CONTENT_PATH = "sticker_packs/";
    private static final String CONTENT_FILE_NAME = BuildConfig.STICKER_PACK_FILE;

    private boolean nonAssetContentProvider = false;
    private String contentPath;

    /**
     * Do not change the values in the UriMatcher because otherwise, WhatsApp will not be able to fetch the stickers from the ContentProvider.
     */
    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    private static final int METADATA_CODE = 1;
    private static final int STICKERS_CODE = 3;
    private static final int METADATA_CODE_FOR_SINGLE_PACK = 2;
    private static final int STICKERS_ASSET_CODE = 4;

    private static final String METADATA = "metadata";
    private static final String STICKERS = "stickers";
    private static final String STICKERS_ASSET = "stickers_asset";

    private List<StickerPack> stickerPackList;

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile(String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        final String authority = WhatsAppStickersPlugin.getContentProviderAuthority(context);
        if (!authority.startsWith(Objects.requireNonNull(getContext()).getPackageName())) {
            throw new IllegalStateException("your authority (" + authority + ") for the content provider should start with your package name: " + getContext().getPackageName());
        }

        // Check meta-data
        try {
            ApplicationInfo ai = getContext().getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            nonAssetContentProvider = bundle.getBoolean("NonAssetContentProvider");
        } catch (PackageManager.NameNotFoundException | NullPointerException e) {
            nonAssetContentProvider = false;
        }

        // Set sticker packs path
        contentPath = nonAssetContentProvider ? PathUtils.getDataDirectory(getContext()) + "/" + CONTENT_PATH : "flutter_assets/" + CONTENT_PATH;

        // The call to get the metadata for the sticker packs.
        MATCHER.addURI(authority, METADATA, METADATA_CODE);

        // The call to get the metadata for single sticker pack. * represent the identifier
        MATCHER.addURI(authority, METADATA + "/*", METADATA_CODE_FOR_SINGLE_PACK);

        // Gets the list of stickers for a sticker pack, * represents the identifier.
        MATCHER.addURI(authority, STICKERS + "/*", STICKERS_CODE);

        // Gets the an asset from a sticker pack
        MATCHER.addURI(authority, STICKERS_ASSET + "/*/*", STICKERS_ASSET_CODE);

        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri,@NonNull  String[] projection, String selection, String[] selectionArgs, String sortOrder) {
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

    @Override
    public String getType(@NonNull Uri uri) {
        final Context context = getContext();
        assert context != null;

        final int matchCode = MATCHER.match(uri);
        switch (matchCode) {
            case METADATA_CODE:
                return "vnd.android.cursor.dir/vnd." + WhatsAppStickersPlugin.getContentProviderAuthority(context) + "." + METADATA;
            case METADATA_CODE_FOR_SINGLE_PACK:
                return "vnd.android.cursor.item/vnd." + WhatsAppStickersPlugin.getContentProviderAuthority(context) + "." + METADATA;
            case STICKERS_CODE:
                return "vnd.android.cursor.dir/vnd." + WhatsAppStickersPlugin.getContentProviderAuthority(context) + "." + STICKERS;
            case STICKERS_ASSET_CODE:
                final List<String> pathSegments = uri.getPathSegments();

                if (pathSegments.size() != 3) {
                    throw new IllegalArgumentException("path segments should be 3, uri is: " + uri);
                }

                String fileName = pathSegments.get(pathSegments.size() - 1);
                String extension = fileName.substring(fileName.lastIndexOf("."));

                return extension.equals(".png") ? "image/png" : "image/webp";
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public AssetFileDescriptor openAssetFile(@NonNull Uri uri, @NonNull String mode) {
        return getImageAsset(uri);
    }

    private synchronized void readContentFile(@NonNull Context context) {
        if (nonAssetContentProvider) {
            File file = new File(contentPath + CONTENT_FILE_NAME);
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
        if (stickerPackList == null) {
            readContentFile(Objects.requireNonNull(getContext()));
        }
        return stickerPackList;
    }

    private Cursor getPackForAllStickerPacks(@NonNull Uri uri) {
        return getStickerPackInfo(uri, getStickerPackList());
    }

    private Cursor getCursorForSingleStickerPack(@NonNull Uri uri) {
        final String identifier = uri.getLastPathSegment();
        for (StickerPack stickerPack : getStickerPackList()) {
            if (identifier.equals(stickerPack.identifier)) {
                return getStickerPackInfo(uri, Collections.singletonList(stickerPack));
            }
        }

        return getStickerPackInfo(uri, new ArrayList<StickerPack>());
    }

    @NonNull
    private Cursor getStickerPackInfo(@NonNull Uri uri, @NonNull List<StickerPack> stickerPackList) {
        MatrixCursor cursor = new MatrixCursor(
                new String[]{
                        STICKER_PACK_IDENTIFIER_IN_QUERY,
                        STICKER_PACK_NAME_IN_QUERY,
                        STICKER_PACK_PUBLISHER_IN_QUERY,
                        STICKER_PACK_ICON_IN_QUERY,
                        ANDROID_APP_DOWNLOAD_LINK_IN_QUERY,
                        IOS_APP_DOWNLOAD_LINK_IN_QUERY,
                        PUBLISHER_EMAIL,
                        PUBLISHER_WEBSITE,
                        PRIVACY_POLICY_WEBSITE,
                        LICENSE_AGREEMENT_WEBSITE
                });

        for (StickerPack stickerPack : stickerPackList) {
            MatrixCursor.RowBuilder builder = cursor.newRow();
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
        }

        cursor.setNotificationUri(Objects.requireNonNull(getContext()).getContentResolver(), uri);
        return cursor;
    }

    @NonNull
    private Cursor getStickersForAStickerPack(@NonNull Uri uri) {
        final String identifier = uri.getLastPathSegment();
        MatrixCursor cursor = new MatrixCursor(new String[]{STICKER_FILE_NAME_IN_QUERY, STICKER_FILE_EMOJI_IN_QUERY});

        for (StickerPack stickerPack : getStickerPackList()) {
            if (identifier.equals(stickerPack.identifier)) {
                for (Sticker sticker : stickerPack.getStickers()) {
                    cursor.addRow(new Object[]{sticker.imageFileName, TextUtils.join(",", sticker.emojis)});
                }
            }
        }

        cursor.setNotificationUri(Objects.requireNonNull(getContext()).getContentResolver(), uri);
        return cursor;
    }

    private AssetFileDescriptor getImageAsset(Uri uri) throws IllegalArgumentException {
        final List<String> pathSegments = uri.getPathSegments();

        if (pathSegments.size() != 3) {
            throw new IllegalArgumentException("path segments should be 3, uri is: " + uri);
        }

        String fileName = pathSegments.get(pathSegments.size() - 1);
        final String identifier = pathSegments.get(pathSegments.size() - 2);

        if (TextUtils.isEmpty(identifier)) {
            throw new IllegalArgumentException("identifier is empty, uri: " + uri);
        }

        if (TextUtils.isEmpty(fileName)) {
            throw new IllegalArgumentException("file name is empty, uri: " + uri);
        }

        // making sure the file that is trying to be fetched is in the list of stickers.
        for (StickerPack stickerPack : getStickerPackList()) {
            if (identifier.equals(stickerPack.identifier)) {
                if (fileName.equals(stickerPack.trayImageFile)) {
                    return fetchFile(uri, fileName, identifier);
                } else {
                    for (Sticker sticker : stickerPack.getStickers()) {
                        if (fileName.equals(sticker.imageFileName)) {
                            return fetchFile(uri, fileName, identifier);
                        }
                    }
                }
            }
        }

        return null;
    }

    private AssetFileDescriptor fetchFile(@NonNull Uri uri, @NonNull String fileName, @NonNull String identifier) {
        return nonAssetContentProvider ?
                fetchNonAssetFile(uri, fileName, identifier) :
                fetchAssetFile(uri, Objects.requireNonNull(getContext()).getAssets(), fileName, identifier);
    }

    private AssetFileDescriptor fetchNonAssetFile(Uri uri, String fileName, String identifier) {
        try {
            final File file = new File(contentPath + identifier, fileName);
            return new AssetFileDescriptor(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY), 0, AssetFileDescriptor.UNKNOWN_LENGTH);
        } catch (IOException e) {
            Log.e(Objects.requireNonNull(getContext()).getPackageName(), "IOException when getting asset file, uri:" + uri, e);
            return null;
        }
    }

    private AssetFileDescriptor fetchAssetFile(@NonNull Uri uri, @NonNull AssetManager am, @NonNull String fileName, @NonNull String identifier) {
        try {
            return am.openFd(contentPath + identifier + "/" + fileName);
        } catch (IOException e) {
            Log.e(Objects.requireNonNull(getContext()).getPackageName(), "IOException when getting asset file, uri:" + uri, e);
            return null;
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @NonNull String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported");
    }
}
