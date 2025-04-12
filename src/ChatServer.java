import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.Color;

public class ChatServer {
    private List<ClientHandler> clients = new ArrayList<>();
    private ServerSocket serverSocket;

    public ChatServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);
    }

    public void start() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                ClientHandler clientHandler = new ClientHandler(socket, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            } catch (IOException e) {
                System.out.println("Server exception: " + e.getMessage());
                break;
            }
        }
    }

    public void broadcast(String sender, Color color, String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(sender, color, message);
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Client disconnected. Total clients: " + clients.size());
    }

    public static void main(String[] args) {
        try {
            ChatServer server = new ChatServer(1234);
            server.start();
        } catch (IOException e) {
            System.out.println("Failed to start server: " + e.getMessage());
        }
    }

    class ClientHandler implements Runnable {
        private Socket socket;
        private ChatServer server;
        private PrintWriter out;
        private String nickname;
        private Color nicknameColor = Color.BLACK;

        public ClientHandler(Socket socket, ChatServer server) {
            this.socket = socket;
            this.server = server;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Получаем никнейм и цвет
                String firstMessage = in.readLine();
                if (firstMessage.startsWith("NICK:")) {
                    nickname = firstMessage.substring(5);
                    String colorMessage = in.readLine();
                    if (colorMessage.startsWith("COLOR:")) {
                        nicknameColor = new Color(Integer.parseInt(colorMessage.substring(6)));
                    }
                }

                server.broadcast("Server", Color.RED, nickname + " joined the chat!");

                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    server.broadcast(nickname, nicknameColor, clientMessage);
                }
            } catch (IOException e) {
                System.out.println("Client handler exception: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Socket close error: " + e.getMessage());
                }
                server.removeClient(this);
                server.broadcast("Server", Color.RED, nickname + " left the chat.");
            }
        }

        public void sendMessage(String sender, Color color, String message) {
            out.println(sender + "|" + color.getRGB() + "|" + message);
        }
    }
}