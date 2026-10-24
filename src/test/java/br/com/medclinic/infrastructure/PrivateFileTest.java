package br.com.medclinic.infrastructure;

import static org.assertj.core.api.Assertions.*;

import br.com.medclinic.application.PrivateFiles;
import br.com.medclinic.domain.RuleViolation;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PrivateFileTest {
    @TempDir
    Path directory;

    static byte[] png() throws IOException {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        return output.toByteArray();
    }

    @Test
    void acceptsRealImageAndRejectsEmptyOversizeAndDisguisedContent() throws IOException {
        PrivateFiles files = new PrivateFileAdapter(directory, bytes -> {});
        byte[] png = png();
        var prepared = files.prepare(png, "image/png");
        assertThat(files.load(prepared.storageKey())).isEqualTo(png);
        assertThatThrownBy(() -> files.prepare(new byte[0], "image/png")).isInstanceOf(RuleViolation.class);
        assertThatThrownBy(() -> files.prepare(new byte[10 * 1024 * 1024 + 1], "image/png"))
                .isInstanceOf(RuleViolation.class);
        assertThatThrownBy(() -> files.prepare(png, "application/pdf")).isInstanceOf(RuleViolation.class);
    }

    @Test
    void scanFailureAndUnavailableScannerNeverStoreFile() throws IOException {
        var unsafe = new PrivateFileAdapter(directory, bytes -> {
            throw new IOException("Refused");
        });
        assertThatThrownBy(() -> unsafe.prepare(png(), "image/png")).isInstanceOf(RuleViolation.class);
        try (var entries = java.nio.file.Files.list(directory)) {
            assertThat(entries.count()).isZero();
        }
    }
}
