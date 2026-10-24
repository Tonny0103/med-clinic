package br.com.medclinic.infrastructure;

import static br.com.medclinic.domain.Rules.require;

import br.com.medclinic.application.FileScanner;
import br.com.medclinic.application.PrivateFiles;
import br.com.medclinic.domain.RuleViolation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;

public class PrivateFileAdapter implements PrivateFiles {
    private final Path root;
    private final FileScanner scanner;

    public PrivateFileAdapter(Path root, FileScanner scanner) {
        this.root = root.toAbsolutePath().normalize();
        this.scanner = scanner;
        if (this.root.toString().contains("src/main/resources")
                || this.root.toString().contains("src\\main\\resources")) {
            throw new IllegalStateException("Arquivos precisam de diretório privado.");
        }
        try {
            Files.createDirectories(this.root);
            if (Files.getFileAttributeView(this.root, java.nio.file.attribute.PosixFileAttributeView.class) != null)
                Files.setPosixFilePermissions(this.root, PosixFilePermissions.fromString("rwx------"));
        } catch (IOException error) {
            throw new IllegalStateException("Diretório privado indisponível.");
        }
    }

    @Override
    public Prepared prepare(byte[] content, String declaredType) {
        require(
                "file",
                content != null && content.length > 0 && content.length <= 10 * 1024 * 1024,
                "Envie um arquivo não vazio de até 10 MiB.");
        String type = detect(content);
        require("file", type.equals(declaredType), "Tipo informado e conteúdo não correspondem. Use PDF, JPEG ou PNG.");
        try {
            scanner.scan(content);
            validateContent(content, type);
            String key = UUID.randomUUID().toString();
            Path target = path(key);
            try (FileChannel channel =
                    FileChannel.open(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(content);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            if (Files.getFileAttributeView(target, java.nio.file.attribute.PosixFileAttributeView.class) != null)
                Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rw-------"));
            return new Prepared(
                    key,
                    type,
                    content.length,
                    HexFormat.of()
                            .formatHex(MessageDigest.getInstance("SHA-256").digest(content)));
        } catch (IOException error) {
            throw new RuleViolation(
                    "file", "Arquivo recusado ou verificação/armazenamento indisponível. Tente novamente.");
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private String detect(byte[] bytes) {
        if (bytes.length >= 5
                && bytes[0] == '%'
                && bytes[1] == 'P'
                && bytes[2] == 'D'
                && bytes[3] == 'F'
                && bytes[4] == '-') return "application/pdf";
        if (bytes.length >= 8
                && bytes[0] == (byte) 0x89
                && bytes[1] == 'P'
                && bytes[2] == 'N'
                && bytes[3] == 'G'
                && bytes[4] == 13
                && bytes[5] == 10
                && bytes[6] == 26
                && bytes[7] == 10) return "image/png";
        if (bytes.length >= 3 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8 && bytes[2] == (byte) 0xff)
            return "image/jpeg";
        throw new RuleViolation("file", "Formato não permitido. Use PDF, JPEG ou PNG.");
    }

    private void validateContent(byte[] bytes, String type) throws IOException {
        if (type.equals("application/pdf")) {
            try (var pdf = Loader.loadPDF(bytes)) {
                var catalog = pdf.getDocumentCatalog();
                var names = catalog.getNames();
                require(
                        "file",
                        !pdf.isEncrypted()
                                && pdf.getNumberOfPages() > 0
                                && !catalog.getCOSObject()
                                        .containsKey(org.apache.pdfbox.cos.COSName.getPDFName("OpenAction"))
                                && (names == null || names.getJavaScript() == null && names.getEmbeddedFiles() == null),
                        "Use PDF sem senha, scripts, ações automáticas ou arquivos incorporados.");
            }
        } else {
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                require("file", readers.hasNext(), "Imagem inválida.");
                var reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    require(
                            "file",
                            (long) reader.getWidth(0) * reader.getHeight(0) <= 25_000_000,
                            "A imagem excede o limite de resolução de 25 milhões de pixels.");
                    require("file", reader.read(0) != null, "Imagem inválida.");
                } finally {
                    reader.dispose();
                }
            }
        }
    }

    private Path path(String key) {
        UUID id;
        try {
            id = UUID.fromString(key);
        } catch (IllegalArgumentException error) {
            throw new RuleViolation("file", "Referência de arquivo inválida.");
        }
        return root.resolve(id + ".bin");
    }

    @Override
    public byte[] load(String key) {
        Path target = path(key);
        require("file", Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS), "Arquivo indisponível.");
        try (var stream = Files.newInputStream(target, LinkOption.NOFOLLOW_LINKS)) {
            return stream.readNBytes(10 * 1024 * 1024 + 1);
        } catch (IOException error) {
            throw new RuleViolation("file", "Arquivo indisponível. Tente novamente.");
        }
    }
}
