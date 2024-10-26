import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.*;
import Data_Structures.*;

public class Node {
    private final ScheduledExecutorService scheduler;
    private ServerSocket serverSocket;
    private int port;
    private HashMap<Integer, Socket> connectedPeers;
    private HashMap<Integer, ObjectOutputStream> outputStreams;
    private final int[] knownPorts = {8001, 8002, 8003, 8004, 8005};

    private int epoch = 1;
    private int epochDuration = 4; // segundos
    private int currentLeader;

    private List<Block> blockChain = new LinkedList<Block>();
    private Queue<Block> notarizedBlocks = new LinkedList<Block>();

    // Lista de Blocos e Quem já o enviou
    private Map<Block, List<Integer>> msgReceivedBy = new HashMap<>();

    public Node(int port) {
        this.port = port;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    // Inicia o Peer
    public void startNode() {
        connectedPeers = new HashMap<>();
        outputStreams = new HashMap<>();

        Transaction[] gen = {};
        Block genBlock = new Block (new byte[0],0,0,gen);

        try {
            startServer();
            Thread.sleep(15 * 1000);

            for (int port : knownPorts) {
                if (port != this.port) {
                    connectToPeer(port);
                }
            }

            startClock();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startClock() {
        scheduler.scheduleAtFixedRate(() -> {
            startStreamlet();
        }, 0, epochDuration, TimeUnit.SECONDS);
    }

    private void checkForFinalization() {
        if (notarizedBlocks.size() >= 3) {
            for (int i = 0; i < 2; i++) {
                blockChain.add(notarizedBlocks.poll());
            }
            System.out.println("Finalized blocks up to epoch " + blockChain.get(blockChain.size() - 1).getEpoch());
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
    private void connectToPeer(int peerPort) {
        try {
            Socket socket = new Socket("127.0.0.1", peerPort);
            connectedPeers.put(peerPort, socket);
            outputStreams.put(peerPort, new ObjectOutputStream(socket.getOutputStream()));
    
            System.out.println("Connected to 127.0.0.1:" + peerPort);
        } catch (Exception e) {
            System.out.println("Failed to connect to client 127.0.0.1:" + peerPort);
            e.printStackTrace();
        }
    }

    private void startStreamlet() {
        currentLeader = knownPorts[epoch % knownPorts.length];
        System.out.println("Epoch " + epoch + " started. Current leader: " + currentLeader);
        
        if (port == currentLeader) {
            proposeBlock();
        }

        checkForFinalization();
        epoch++;
    }

    private void proposeBlock() {
        Block newBlock = new Block(getLastBlockHash(), epoch, blockChain.size() + 1, Utils.generateTransactions());
        broadcast(new Message(Type.Propose, newBlock, port));
        System.out.println("Proposed block at epoch " + epoch);
    }

    //Envia a mensagem para o peer da rede local com peerPort
    private void sendMessage(Socket socket, int peerPort, Message message) throws IOException {
        ObjectOutputStream oos = outputStreams.get(peerPort);

        try {
            oos.writeObject(message);
            oos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] getLastBlockHash() {
        if (blockChain.isEmpty()) return new byte[0];
        return blockChain.get(blockChain.size() - 1).getHash();
    }

    //Da broadcast para todos os peers a quem ja esta conectado
    // ! Temos de retirar dos connectedPeers quando algum dá crash
    private void broadcast(Message msg) {
        for (int peerPort : connectedPeers.keySet()) {
            try {
                sendMessage(connectedPeers.get(peerPort), peerPort, msg);
            } catch (IOException e) {
                System.out.println("Client 127.0.0.1:" + peerPort + " disconnected");
                connectedPeers.remove(peerPort);
            }
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
                Message message = (Message) ois.readObject();

                System.out.println("Received: " + message.content.stringContent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
