package network.client;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class WriteThread implements Runnable {
    private final BufferedWriter writer;
    private final BlockingQueue<String> outgoing;
    private final Consumer<String> errorListener;
    private volatile boolean running = true;

    public WriteThread(BufferedWriter writer, BlockingQueue<String> outgoing, Consumer<String> errorListener) {
        this.writer = writer;
        this.outgoing = outgoing;
        this.errorListener = errorListener;
    }

    @Override
    public void run() {
        try {
            while (running) {
                String line = outgoing.poll(500, TimeUnit.MILLISECONDS);
                if (line == null) {
                    continue;
                }
                writer.write(line);
                writer.newLine();
                writer.flush();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            if (running) {
                errorListener.accept("Failed to send message");
            }
        }
    }

    public void stop() {
        running = false;
    }
}