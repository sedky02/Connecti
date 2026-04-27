package network.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Consumer;

public class ReadThread implements Runnable {
    private final BufferedReader reader;
    private final Consumer<Packet> packetListener;
    private final Consumer<String> errorListener;
    private volatile boolean running = true;

    public ReadThread(BufferedReader reader, Consumer<Packet> packetListener, Consumer<String> errorListener) {
        this.reader = reader;
        this.packetListener = packetListener;
        this.errorListener = errorListener;
    }

    @Override
    public void run() {
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                Packet packet = parse(line);
                if (packet != null) {
                    packetListener.accept(packet);
                }
            }
        } catch (IOException e) {
            if (running) {
                errorListener.accept("Disconnected from server");
            }
        }
    }

    private Packet parse(String line) {
        String[] parts = line.split("\\|", 4);
        if (parts.length < 4) {
            return null;
        }
        return new Packet(parts[0], parts[1], parts[2], parts[3]);
    }

    public void stop() {
        running = false;
    }

    public static class Packet {
        private final String type;
        private final String sender;
        private final String target;
        private final String content;

        public Packet(String type, String sender, String target, String content) {
            this.type = type;
            this.sender = sender;
            this.target = target;
            this.content = content;
        }

        public String getType() {
            return type;
        }

        public String getSender() {
            return sender;
        }

        public String getTarget() {
            return target;
        }

        public String getContent() {
            return content;
        }
    }
}
