import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.io.*;

public class Node {
    private ServerSocket serverSocket;
    private int port;
    private HashMap<Integer, Socket> connectedPeers;
    private HashMap<Integer, ObjectOutputStream> outputStreams;
    private final int[] knownPorts = {8001, 8002, 8003, 8004, 8005};

    public Node(int port) {
        this.port = port;
    }

    // Inicia o Peer
    public void startNode() {
        connectedPeers = new HashMap<>();
        outputStreams = new HashMap<>();

        try {
            startServer();
            Thread.sleep(15 * 1000);

            for (int port : knownPorts) {
                if (port != this.port) {
                    connectToPeer(port);
                }
            }

            broadcast();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Cria server socket e fica a espera de conexoes
    private void startServer() throws IOException {
        this.serverSocket = new ServerSocket(port);
        System.out.println("Listening on port: " + port);

        new Thread(() -> {
            try {
                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        new Thread(new ClientHandler(socket)).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.out.println("Server Error: " + e.getMessage());
            }
        }).start();
    }

    //Liga-se ao peer na rede local com porta peerPort
    private void connectToPeer(int peerPort) throws IOException {
        Socket socket = new Socket("127.0.0.1", peerPort);
        connectedPeers.put(peerPort, socket);
        outputStreams.put(peerPort, new ObjectOutputStream(socket.getOutputStream()));

        System.out.println("Connected to 127.0.0.1:" + peerPort);
    }

    //Envia a mensagem para o peer da rede local com peerPort
    private void sendMessage(Socket socket, int peerPort, String message) throws IOException {
        ObjectOutputStream oos = outputStreams.get(peerPort);

        try {
            oos.writeObject(message);
            oos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Da broadcast para todos os peers a quem ja esta conectado
    private void broadcast() {
        try {
            for (Socket sock : connectedPeers.values()) {
                sendMessage(sock, sock.getPort(), "Teste: " + port);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Handle das mensagens com cada peer
    private class ClientHandler implements Runnable {
        private Socket sock;
        private ObjectInputStream ois;

        public ClientHandler(Socket socket) {
            this.sock = socket;
        }

        @Override
        public void run() {
            try {
                ois = new ObjectInputStream(sock.getInputStream());
                String message = (String) ois.readObject();

                System.out.println("Received: " + message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
