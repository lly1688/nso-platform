package com.nso.shared.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

// 文件内容签名校验工具。
public final class FileContentSignature {

    // 签名检测所需的头部字节数
    private static final int HEADER_SIZE = 64;

    private FileContentSignature() {
    }

    // 校验文件扩展名与内容签名是否匹配。
    public static InputStream verify(String extension, InputStream source) throws IOException {
        if (source == null) {
            throw new IOException("文件内容不能为空");
        }
        PushbackInputStream input = new PushbackInputStream(source, HEADER_SIZE);
        byte[] header = input.readNBytes(HEADER_SIZE);
        input.unread(header);
        if (!matches(extension == null ? "" : extension.toLowerCase(Locale.ROOT), header)) {
            throw new IOException("文件内容与扩展名不匹配");
        }
        return input;
    }

    private static boolean matches(String extension, byte[] header) {
        return switch (extension) {
            case "pdf" -> startsWith(header, "%PDF-".getBytes(StandardCharsets.US_ASCII));
            case "png" -> startsWith(header, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
            case "jpg", "jpeg" -> startsWith(header, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "webp" -> startsWith(header, "RIFF".getBytes(StandardCharsets.US_ASCII))
                    && containsAt(header, 8, "WEBP".getBytes(StandardCharsets.US_ASCII));
            case "zip", "xlsx", "docx" -> zip(header);
            case "xls", "doc" -> startsWith(header,
                    new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1});
            case "dwg" -> startsWith(header, "AC".getBytes(StandardCharsets.US_ASCII));
            case "dxf" -> normalizedText(header).startsWith("0") && normalizedText(header).contains("SECTION");
            case "step", "stp" -> normalizedText(header).startsWith("ISO-10303-21");
            default -> true;
        };
    }

    private static boolean zip(byte[] header) {
        return startsWith(header, new byte[]{'P', 'K', 0x03, 0x04})
                || startsWith(header, new byte[]{'P', 'K', 0x05, 0x06})
                || startsWith(header, new byte[]{'P', 'K', 0x07, 0x08});
    }

    private static boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (value[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsAt(byte[] value, int offset, byte[] expected) {
        if (value.length < offset + expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if (value[offset + index] != expected[index]) {
                return false;
            }
        }
        return true;
    }

    private static String normalizedText(byte[] value) {
        return new String(value, StandardCharsets.US_ASCII)
                .replace("\uFEFF", "")
                .stripLeading()
                .replace('\r', '\n');
    }
}
