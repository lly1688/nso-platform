package com.nso.business.file.service;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

// 下载完成后校验文件内容摘要的输入流。
public final class IntegrityCheckingInputStream extends FilterInputStream {
    // 上传时计算并持久化的 SHA-256；下载完成后用它作为预期摘要进行比对。
    private final String expectedSha256;
    // 随每次读取累计实际下载内容的 SHA-256。
    private final MessageDigest digest;
    // 仅在底层流返回 EOF 后为 true，表示全部文件内容都已参与摘要计算。
    private boolean endOfStream;
    // 防止多次触发校验时重复结算 MessageDigest 或重复抛出校验结果。
    private boolean verified;

    public IntegrityCheckingInputStream(InputStream input, String expectedSha256) throws IOException {
        super(input);
        // 空白摘要兼容未保存摘要的历史文件，完整读取后不执行内容比对。
        this.expectedSha256 = expectedSha256;
        try {
            // 为当前下载流创建独立的摘要器，避免共享可变的计算状态。
            this.digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception exception) {
            throw new IOException("无法初始化文件完整性校验", exception);
        }
    }

    @Override
    public int read() throws IOException {
        int value = super.read();
        if (value >= 0) {
            // 单字节读取也必须计入实际内容摘要。
            digest.update((byte) value);
        } else {
            // 读到 EOF 说明文件已完整读取，此时才可安全校验摘要。
            endOfStream = true;
            verify();
        }
        return value;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int count = super.read(buffer, offset, length);
        if (count > 0) {
            // 仅将本次实际读取的字节区间计入摘要。
            digest.update(buffer, offset, count);
        } else if (count < 0) {
            // 批量读取同样在 EOF 时触发完整文件校验。
            endOfStream = true;
            verify();
        }
        return count;
    }

    @Override
    public void close() throws IOException {
        try {
            // 未读完即关闭时不能用不完整内容校验，避免将部分文件误判为完整文件。
            if (endOfStream) {
                verify();
            }
        } finally {
            super.close();
        }
    }

    private void verify() throws IOException {
        // 已校验过或没有预期摘要时不再结算摘要器。
        if (verified || expectedSha256 == null || expectedSha256.isBlank()) {
            verified = true;
            return;
        }
        // 先标记完成，保证重复读取 EOF 或 close 不会重复调用 digest()。
        verified = true;
        String actual = HexFormat.of().formatHex(digest.digest());
        // 以不区分大小写的十六进制形式比较上传摘要与下载实际摘要。
        if (!expectedSha256.equalsIgnoreCase(actual)) {
            throw new IOException("文件完整性校验失败");
        }
    }
}
