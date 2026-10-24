package br.com.medclinic.infrastructure;

import br.com.medclinic.application.FileScanner;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClamAvScanner implements FileScanner {
    private final String host;
    private final int port;

    public ClamAvScanner(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void scan(byte[] content) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000);
            socket.setSoTimeout(30000);
            var output = new DataOutputStream(socket.getOutputStream());
            output.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
            for (int offset = 0; offset < content.length; offset += 8192) {
                int length = Math.min(8192, content.length - offset);
                output.writeInt(length);
                output.write(content, offset, length);
            }
            output.writeInt(0);
            output.flush();
            var response = new ByteArrayOutputStream();
            int next;
            while ((next = socket.getInputStream().read()) > 0 && response.size() < 4096) response.write(next);
            if (!response.toString(StandardCharsets.US_ASCII).strip().equals("stream: OK"))
                throw new IOException("Inspeção não aprovada.");
        }
    }
}
