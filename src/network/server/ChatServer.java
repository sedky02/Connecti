package network.server;

import service.ChatService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {
    private final int port;
    private final ClientManager clientManager;
    private final ChatService chatService;

    public ChatServer(int port) {
        this.port = port;
        this.clientManager = new ClientManager();
        this.chatService = new ChatService();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, clientManager, chatService);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            throw new RuntimeException("Server failed to start on port " + port, e);
        }
    }
}