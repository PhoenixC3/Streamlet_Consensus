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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Node {
    private final ScheduledExecutorService scheduler;
    private ServerSocket serverSocket;
    private int port;
    private HashMap<Integer, Socket> connectedPeers;
    private HashMap<Integer, ObjectOutputStream> outputStreams;
    private final int[] knownPorts = {8001, 8002, 8003, 8004, 8005};

    // * Volatile -> variavel que pode ser alterada por varios threads
    private volatile int epoch = 0;
    private int epochDuration = 4; // segundos
    private volatile int currentLeader;

    private volatile List<Block> blockChain = new LinkedList<Block>();

    // CONTÉM APENAS BLOCOS NOTARIZADOS SEGUIDOS
    private volatile Queue<Block> notarizedBlocks = new LinkedList<Block>();

    // * Lock para garantir que apenas um thread acede a uma variavel de cada vez
    private Lock lock = new ReentrantLock();

    // Lista de Blocos e Quem já o enviou
    private volatile Map<Block, List<Integer>> msgReceivedBy = new HashMap<>();

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

        lock.lock();
        try {
            blockChain.add(genBlock);
        } finally {
            lock.unlock();
        }

        try {
            startServer();
            Thread.sleep(15 * 1000);

            for (int port : knownPorts) {
                if (port != this.port) {
                    connectToPeer(port);
                }
            }

            // Começar protocolo
            startClock();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Inicia o relogio para começar o protocolo de x em x segundos
    private void startClock() {
        scheduler.scheduleAtFixedRate(() -> {
            startStreamlet();
        }, 0, epochDuration, TimeUnit.SECONDS);
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

    // Inicia uma nova epoca
    private void startStreamlet() {
        lock.lock();
        try {
            epoch++;
        } finally {
            lock.unlock();
        }

        currentLeader = Utils.getLeader(epoch, knownPorts);
        System.out.println("Epoch " + epoch + " started. Current leader: " + currentLeader);
        
        // Se for o lider, propõe um bloco
        if (port == currentLeader) {
            proposeBlock();
        }

        // ? Ele deve verificar se tem blocos notarizados quando recebe mensagens e não a cada epoca ???
        checkForFinalization();
    }

    // Propor um Bloco
    private void proposeBlock() {
        Block newBlock = new Block(getLastBlockHash(), epoch, blockChain.size() + 1, Utils.generateTransactions());
        broadcast(new Message(Type.Propose, newBlock, port));
        System.out.println("Proposed block at epoch " + epoch);
    }

    // Controlo de limpar a queue é feito ao notorizar um bloco
    private void checkForFinalization() {
        if (notarizedBlocks.size() >= 3 && checkLastBlocks()) {
            int temp = notarizedBlocks.size();
            // * Lock para garantir que apenas um thread acede a uma variavel de cada vez
            lock.lock();
            try {
                // Remove todos os blocos notarizados da queue e adiciona ao blockchain menos o último
                for (int i = 0; i < temp - 1; i++) {
                    blockChain.add(notarizedBlocks.poll());
                }
            }
            finally {
                lock.unlock();
            }
            System.out.println("Finalized blocks up to epoch " + blockChain.get(blockChain.size() - 1).getEpoch());
        }
    }

    // Verifica se os 3 últimos blocos notarizados são seguidos
    private boolean checkLastBlocks() {
        if (notarizedBlocks.size() < 3) {
            return false;
        }
        // Verifica se os 3 últimos blocos notarizados são seguidos
        Block[] blocks = notarizedBlocks.toArray(new Block[0]);
        int lastIndex = blocks.length - 1;

        return blocks[lastIndex].getEpoch() == blocks[lastIndex - 1].getEpoch() + 1 &&
           blocks[lastIndex - 1].getEpoch() == blocks[lastIndex - 2].getEpoch() + 1;
    }

    //Da broadcast para todos os peers a quem ja esta conectado
    // ! Temos de retirar dos connectedPeers quando algum dá crash
    private void broadcast(Message msg) {
        List<Integer> disconnectedPeers = new LinkedList<>();
        for (int peerPort : connectedPeers.keySet()) {
            try {
                sendMessage(connectedPeers.get(peerPort), peerPort, msg);
            } catch (IOException e) {
                System.out.println("------------------------------");
                System.out.println("Client 127.0.0.1:" + peerPort + " removed.");
                System.out.println("------------------------------");
                disconnectedPeers.add(peerPort);
            }
        }

        // Só podemos remover depois de enviar a mensagem a todos para não dar erro
        for (int peerPort : disconnectedPeers) {
            connectedPeers.remove(peerPort);
            outputStreams.remove(peerPort); 
        }
    }

    //Envia a mensagem para o peer da rede local com peerPort
    private void sendMessage(Socket socket, int peerPort, Message message) throws IOException {
        ObjectOutputStream oos = outputStreams.get(peerPort);

        oos.writeObject(message);
        oos.flush();
    }


    private byte[] getLastBlockHash() {
        if (blockChain.isEmpty()) return new byte[0];
        return blockChain.get(blockChain.size() - 1).getHash();
    }

    // Handle das mensagens com cada peer
    // ! Ainda não está a fazer nada, precisamos de dar handle às mensagens TODAS
    private class ClientHandler implements Runnable {
        private Socket sock;
        private ObjectInputStream ois;

        public ClientHandler(Socket socket) {
            this.sock = socket;
        }

        // ! FAZER HANDLE DE TODOS OS TIPOS DE MENSAGENS
        @Override
        public void run() {
            // ficar a espera de mensagens
            try {
                ois = new ObjectInputStream(sock.getInputStream());
                while (true) {
                    Message msg = (Message) ois.readObject();
                    System.out.println("Received message: " + msg.getType() + " from " + msg.getSender());

                    switch (msg.getType()) {
                        case Propose:
                            handlePropose(msg);
                            break;
                        case Vote:
                            // handleVote(msg);
                            break;
                        case Echo:
                            // handleEcho(msg);
                            break;
                        default:
                            break;
                    }
                }
            } catch (Exception e) {
                System.out.println("Client disconnected");
            }
        }



        // ! FALTA DAR ECHO (tem de se dar echo de todas as mensagens recebidas)
        // Verifica se o bloco recebido é válido e se é maior que o maior bloco notarizado
        private void handlePropose(Message msg) {
            Block rcvdBlock;
            if( msg.getContent() instanceof Block ) {
                rcvdBlock = (Block) msg.getContent();

                Block[] blocks = notarizedBlocks.toArray(new Block[0]);
                if ( checkReceived(rcvdBlock, msg.getSender()) && (rcvdBlock.getLength() > blocks[blocks.length - 1].getLength() || epoch == 1) ) {
                    Message vote = new Message(Type.Vote, rcvdBlock, port);
                    broadcast(vote);
                }
            }
        }

        // Verifica se a mensagem já foi recebida e adiciona caso não tenha sido
        private boolean checkReceived(Block b, int sender) {
            if (msgReceivedBy.containsKey(b)) {
                List<Integer> senders = msgReceivedBy.get(b);
                if (senders.contains(sender)) {
                    return true;
                } else {
                    lock.lock();
                    try{
                        senders.add(sender);
                    }
                    finally {
                        lock.unlock();
                    }
                    return false;
                }
            } else {
                List<Integer> senders = new LinkedList<>();
                senders.add(sender);
                try{
                    msgReceivedBy.put(b, senders);
                }
                finally {
                    lock.unlock();
                }
                return false;
            }
        }

        private void handleVote(Message msg) {

        }

        private void handleEcho(Message msg) {

        }
    }
}
