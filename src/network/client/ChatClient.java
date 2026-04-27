package network.client;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class ChatClient {
    private final String host;
    private final int port;
    private final Consumer<ReadThread.Packet> packetListener;
    private final Consumer<String> errorListener;

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    private final BlockingQueue<String> outgoing = new LinkedBlockingQueue<>();
    private ReadThread readThread;
    private WriteThread writeThread;
    private Thread readRunner;
    private Thread writeRunner;

    public ChatClient(String host, int port, Consumer<ReadThread.Packet> packetListener, Consumer<String> errorListener) {
        this.host = host;
        this.port = port;
        this.packetListener = packetListener;
        this.errorListener = errorListener;
    }

    public void start(String username) {
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            readThread = new ReadThread(reader, packetListener, errorListener);
            writeThread = new WriteThread(writer, outgoing, errorListener);

            readRunner = new Thread(readThread);
            writeRunner = new Thread(writeThread);

            readRunner.start();
            writeRunner.start();

        } catch (IOException e) {
            errorListener.accept("Connection failed: " + e.getMessage());
        }
    }

    public void sendRaw(String line) {
        outgoing.offer(line);
    }

    public void close() {
        try {
            if (readThread != null) {
                readThread.stop();
            }
            if (writeThread != null) {
                writeThread.stop();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
