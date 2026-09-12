package com.nso;

import com.nso.shared.util.FileContentSignature;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileContentSignatureTest {

    @Test
    void acceptsMatchingPngContentAndLeavesTheStreamReadable() throws Exception {
        byte[] content = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

        try (InputStream verified = FileContentSignature.verify("png", new ByteArrayInputStream(content))) {
            assertThat(verified.readAllBytes()).containsExactly(content);
        }
    }

    @Test
    void rejectsContentThatDoesNotMatchTheClaimedExtension() {
        assertThatThrownBy(() -> FileContentSignature.verify(
                "pdf",
                new ByteArrayInputStream(new byte[]{(byte) 0x89, 'P', 'N', 'G'})))
                .isInstanceOf(IOException.class)
                .hasMessage("文件内容与扩展名不匹配");
    }
}
