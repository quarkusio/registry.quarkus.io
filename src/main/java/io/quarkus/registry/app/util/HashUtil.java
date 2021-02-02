package io.quarkus.registry.app.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;

public class HashUtil {

    public static String sha1(String content) throws IOException {
        return checksum(content, sha1Digest());
    }

    public static String md5(String content) throws IOException {
        return checksum(content, md5Digest());
    }

    private static String checksum(String content, MessageDigest digest) {
        digest.update(content.getBytes(StandardCharsets.UTF_8));
        return Hex.encodeHexString(digest.digest());
    }

    private static MessageDigest sha1Digest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static MessageDigest md5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
