import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
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
    private int[] knownPorts = null;

    // * Volatile -> variavel que pode ser alterada por varios threads
    private volatile int epoch = 0;
    private int epochDuration; // segundos
    private int currentLeader;
    
    private volatile Blockchain blockchain;

    // * Lock para garantir que apenas um thread acede a uma variavel de cada vez
    private Lock lock = new ReentrantLock();

    // Lista de Blocos e Quem já o enviou
    private volatile Map<Block, List<Integer>> msgReceivedBy = new HashMap<>();

    // Lista de Blocos e Quem já o enviou
    private volatile Map<Message, List<Integer>> msgSentTo = new HashMap<>();

    private volatile Queue<Message> msgQueue = new LinkedList<>();

    // * Threshold para garbage collection de mensagens, value = numero de epocas
    private static final int GARBAGE_COLLECTION_THRESHOLD = 10;


    public Node(int port) {
        this.port = port;
        blockchain = new Blockchain(port);
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    // Inicia o Peer
    public void startNode(String time, String epochTime) {

        //Read server ports from file
        try {
            List<Integer> getPorts = readPortsFromFile("serverPorts.txt");
            knownPorts = new int[getPorts.size()];

            for (int i = 0; i < getPorts.size(); i++) {
                knownPorts[i] = getPorts.get(i);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        connectedPeers = new HashMap<>();
        outputStreams = new HashMap<>();

        epochDuration = Integer.parseInt(epochTime);

        Transaction[] gen = {};
        Block genBlock = new Block (new byte[0],0,0,gen);

        lock.lock();
        try {
            blockchain.addBlock(genBlock);
        } finally {
            lock.unlock();
        }

        try {
            startServer();

            Thread.sleep(5 * 1000);

            for (int port : knownPorts) {
                if (port != this.port) {
                    connectToPeer(port);
                }
            }

            for (int port : knownPorts) {
                if (port != this.port) {
                    if (connectedPeers.containsKey(port)) {
                        outputStreams.get(port).writeObject("RECOVERY");
                        outputStreams.get(port).flush();
                        outputStreams.get(port).reset();

                        outputStreams.get(port).writeObject(this.port);
                        outputStreams.get(port).flush();
                        outputStreams.get(port).reset();
                    }
                }
            }

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime parsedTime = LocalTime.parse(time, timeFormatter);

            LocalDateTime startTime = LocalDateTime.of(LocalDate.now(), parsedTime);

            while (LocalDateTime.now().isBefore(startTime)) {
            }

            // Sincronizar o relogio
            synchronizeClock(startTime);

            // Começar protocolo
            startClock();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Sincroniza o relogio para começar o protocolo no inicio de uma nova epoca
    private void synchronizeClock(LocalDateTime startTime) {
        long epochDurationMillis = epochDuration * 1000;
        long currentTime = System.currentTimeMillis();
        long startTimeMillis = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long timeSinceEpochStart = (currentTime - startTimeMillis) % epochDurationMillis;
        long timeUntilNextEpoch = epochDurationMillis - timeSinceEpochStart;

        long nextEpochStartTime = currentTime + timeUntilNextEpoch;

        // Loop until the current time reaches the next epoch start time
        while (System.currentTimeMillis() < nextEpochStartTime) {
        }
    }

    //Read server addresses from file
    private List<Integer> readPortsFromFile(String fileName) throws IOException {
        List<Integer> ipPortPairs = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;

            while ((line = br.readLine()) != null) {
                ipPortPairs.add(Integer.parseInt(line.trim()));
            }
        }

        return ipPortPairs;
    }

    // Inicia o relogio para começar o protocolo de x em x segundos
    private void startClock() {
        scheduler.scheduleAtFixedRate(() -> {
            lock.lock();

            try {
                List<Integer> disconnectedPeers = new LinkedList<>();
                for (int peerPort : connectedPeers.keySet()) {
                    try {
                        outputStreams.get(peerPort).writeObject("ALIVE");
                        outputStreams.get(peerPort).flush();
                        outputStreams.get(peerPort).reset();
                    } catch (IOException e) {
                        System.out.println("[Node] - Client 127.0.0.1:" + peerPort + " removed.");
                        disconnectedPeers.add(peerPort);
                    }
                }
        
                // Só podemos remover depois de enviar a mensagem a todos para não dar erro
                for (int peerPort : disconnectedPeers) {
                    connectedPeers.remove(peerPort);
                    outputStreams.remove(peerPort); 
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }

            startStreamlet();
        }, 0, epochDuration, TimeUnit.SECONDS);
    }

    //Cria server socket e fica a espera de conexoes
    private void startServer() throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.msgQueue = new LinkedList<>();
        System.out.println("Listening on port: " + port);

        new Thread(() -> readMessages()).start();

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
            outputStreams.get(peerPort).flush();
    
            System.out.println("[Node] - Connected to 127.0.0.1:" + peerPort);
        } catch (Exception e) {
            System.out.println("Failed to connect to client 127.0.0.1:" + peerPort);
        }
    }

    // Inicia uma nova epoca
    private void startStreamlet() {
        lock.lock();
        try {
            epoch++;
            msgReceivedBy.entrySet().removeIf(entry -> entry.getKey().getEpoch() + GARBAGE_COLLECTION_THRESHOLD < epoch);
            if ( this.epoch % GARBAGE_COLLECTION_THRESHOLD == 0 ) {
                msgSentTo.clear();
            }
        } finally {
            lock.unlock();
        }
        
        currentLeader = Utils.getLeader(epoch, knownPorts);

        System.out.println();
        System.out.println("########## Epoch " + epoch + " started. ##########");
        System.out.println("Current leader: " + currentLeader );
        System.out.println();
        
        // Se for o lider, propõe um bloco
        if (port == currentLeader) {
            proposeBlock();
        }

    }

    // Propor um Bloco
    private void proposeBlock() {
        byte[] previousHash;
        int length;
        Block newBlock;

        lock.lock();
        try{
            // COPY THE LEAVES TO AVOID REFERENCES
            LinkedList<BlockchainNode> leaves = new LinkedList<>(blockchain.getLeaves());
            if(leaves.size() == 1){
                previousHash = leaves.get(0).getBlock().calculateHash();
                length = leaves.get(0).getBlock().getLength() + 1;
            }else{
                int maxLength = 0;
                for (BlockchainNode node : leaves) {
                    if (node.getBlock().getLength() > maxLength) {
                        maxLength = node.getBlock().getLength();
                    }
                }
                // retirar da lista de folhas os que nao tem o maximo de comprimento
                for (int i = leaves.size() - 1; i >= 0; i--) {
                    if (leaves.get(i).getBlock().getLength() < maxLength) {
                        leaves.remove(i);
                    }
                }
                Random rd = new Random();
                int index = rd.nextInt(leaves.size());
                previousHash = leaves.get(index).getBlock().calculateHash();
                length = leaves.get(0).getBlock().getLength() + 1;
            }

            // Criar um novo bloco usando o previousHash, a época atual, e o comprimento baseado na blockchain
            newBlock = new Block(previousHash, epoch, length, Utils.generateTransactions());
        } finally {
            lock.unlock();
        }

        // Antes de dar broadcast adicionar aos received
        List<Integer> senders = new LinkedList<>();
        lock.lock();
        try{
            senders.add(port);
            msgReceivedBy.put(newBlock, senders);
        }
        finally {
            lock.unlock();
        }

        // Broadcast do novo bloco como uma mensagem de proposta
        broadcast(new Message(Type.Propose, newBlock, port));

        System.out.println("[Node] - Proposed block at epoch " + epoch);
    }

    //Da broadcast para todos os peers a quem ja esta conectado
    // ! Temos de retirar dos connectedPeers quando algum dá crash
    private void broadcast(Message msg) {
        lock.lock();

        try {
            List<Integer> disconnectedPeers = new LinkedList<>();
            for (int peerPort : connectedPeers.keySet()) {
                try {
                    sendMessage(connectedPeers.get(peerPort), peerPort, msg);
                } catch (IOException e) {
                    System.out.println("[Node] - Client 127.0.0.1:" + peerPort + " removed.");
                    disconnectedPeers.add(peerPort);
                }
            }
    
            // Só podemos remover depois de enviar a mensagem a todos para não dar erro
            for (int peerPort : disconnectedPeers) {
                connectedPeers.remove(peerPort);
                outputStreams.remove(peerPort); 
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    //Envia a mensagem para o peer da rede local com peerPort
    private void sendMessage(Socket socket, int peerPort, Message message) throws IOException {
        ObjectOutputStream oos;
        oos = outputStreams.get(peerPort);
    
        oos.writeObject(message);
        oos.flush();
        oos.reset();
    }

    // Thread que vai ler as mensagens incoming por ordem
    private void readMessages(){
        int confusion_start = 1;
        int confusion_duration = 2;
        int curr_epoch = 0;
        while(true){
            lock.lock();
            try{
                curr_epoch = this.epoch;
            }finally {
                lock.unlock();
            }

            if(!msgQueue.isEmpty() && ( curr_epoch < confusion_start || curr_epoch >= confusion_start + confusion_duration - 1) ){
                Message msg;
                lock.lock();
                try{
                    msg = msgQueue.poll();
                }finally {
                    lock.unlock();
                }
                
                switch (msg.getType()) {
                    case Propose:
                        // System.out.println("Received message: " + msg.getType() + " from " + msg.getSender());
                        handlePropose(msg);
                        break;
                    case Vote:
                        handleVote(msg);
                        break;
                    case Echo:
                        handleEcho(msg);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    // Verifica se a mensagem já foi recebida e adiciona caso não tenha sido
    // Returns true if the message was already received
    private boolean checkReceived(Block b, int sender) {
        if (msgReceivedBy.containsKey(b)) {
            List<Integer> senders = msgReceivedBy.get(b);

            if (senders.contains(sender)) {
                return true;
            } else {
                lock.lock();

                try{
                    senders.add(sender);
                    msgReceivedBy.put(b, senders);
                }
                finally {
                    lock.unlock();
                }

                return false;
            }
        } else {
            List<Integer> senders = new LinkedList<>();
            senders.add(sender);

            lock.lock();

            try{
                msgReceivedBy.put(b, senders);
            }
            finally {
                lock.unlock();
            }
            
            return false;
        }
    }

    // Verifica se o bloco recebido é válido e se é maior que o maior bloco notarizado
    private void handlePropose(Message msg) {
        Block rcvdBlock;

        if( msg.getContent() instanceof Block ) {
            boolean validLength = true;
            rcvdBlock = (Block) msg.getContent();

            LinkedList<BlockchainNode> leaves = blockchain.getLeaves();

            // verificar se a epoca do bloco recebido é blocos na blockchain
            for (BlockchainNode node : leaves) {
                if (node.getBlock().getEpoch() >= rcvdBlock.getEpoch()) {
                    validLength = false;
                }
            }

            if (!checkReceived(rcvdBlock, msg.getSender()) && validLength && msg.getSender() != port) {
                if (msgReceivedBy.get(rcvdBlock).size() > (knownPorts.length / 2)) {
                    notarizeBlock(rcvdBlock);
                }

                Message vote = new Message(Type.Vote, rcvdBlock, port);

                broadcast(vote);
            }

            sendEcho(msg);
        }
    }


    // * Dá handle dos votos e dá echo 
    private void handleVote(Message msg) {
        Block votedBlock;

        if (msg.getContent() instanceof Block) {
            votedBlock = (Block) msg.getContent();
            boolean validLength = true;

            LinkedList<BlockchainNode> leaves = blockchain.getLeaves();

            // verificar se a epoca do bloco recebido é >= blocos na blockchain
            for (BlockchainNode node : leaves) {
                // ? Será que temos de mudar para ser APENAS MAIOR
                if (node.getBlock().getEpoch() >= votedBlock.getEpoch()) {
                    validLength = false;
                }
            }

            if (!checkReceived(votedBlock, msg.getSender()) && validLength) {
                //Verificar quorum e notarizar
                if (msgReceivedBy.get(votedBlock).size() > (knownPorts.length / 2)) {
                    notarizeBlock(votedBlock);
                }
            }

            sendEcho(msg);
        }
    }

    //A collection of at more than n/2 votes from distinct nodes for the same block is called a notarization for the block
    private void notarizeBlock(Block block) {
        lock.lock();

        try {
            blockchain.addBlock(block);
            LinkedList<BlockchainNode> leaves = blockchain.getLeaves();
            System.out.println("[Blockchain] - Number of forks: " + leaves.size());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private void handleEcho(Message msg) {
        Message rcvdEcho;

        if( msg.getContent() instanceof Message ) {
            rcvdEcho = (Message) msg.getContent();
            
            //Da handle da mensagem que veio em echo
            if (rcvdEcho.getType() == Type.Propose) {
                handlePropose(rcvdEcho);
            }
            else if (rcvdEcho.getType() == Type.Vote) {
                handleVote(rcvdEcho);
            }
        }
    }

    private void sendEcho(Message originalMessage) {
        Block rcvdBlock;
        
        if (originalMessage.getContent() instanceof Block) {
            rcvdBlock = (Block) originalMessage.getContent();
            
            LinkedList<Integer> sendTo = new LinkedList<>();
    
            for (int port : connectedPeers.keySet()) {
                if (!checkSent(originalMessage, port)) {
                    sendTo.add(port);
                }
            }
    
            broadcastTo(new Message(Type.Echo, originalMessage, port), sendTo);
        }
    }

    // Verifica se a mensagem já foi enviada e adiciona caso não tenha sido
    private boolean checkSent(Message msg, int receiver) {
        if (msgSentTo.containsKey(msg)) {
            List<Integer> received = msgSentTo.get(msg);

            if (received.contains(receiver)) {
                return true;
            } else {
                lock.lock();

                try {
                    received.add(receiver);
                    msgSentTo.put(msg, received);
                } finally {
                    lock.unlock();
                }

                return false;
            }
        } else {
            List<Integer> received = new LinkedList<>();
            received.add(receiver);

            lock.lock();

            try {
                msgSentTo.put(msg, received);
            } finally {
                lock.unlock();
            }

            return false;
        }
    }

    private void broadcastTo(Message msg, LinkedList<Integer> sendTo) {
        lock.lock();

        try {
            List<Integer> disconnectedPeers = new LinkedList<Integer>();

            for (int peerPort : sendTo) {
                try {
                    sendMessage(connectedPeers.get(peerPort), peerPort, msg);
                } catch (IOException e) {
                    System.out.println("[Node] - Client 127.0.0.1:" + peerPort + " removed.");

                    disconnectedPeers.add(peerPort);
                }
            }
    
            // Só podemos remover depois de enviar a mensagem a todos para não dar erro
            for (int peerPort : disconnectedPeers) {
                connectedPeers.remove(peerPort);
                outputStreams.remove(peerPort); 
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    // Handle das mensagens com cada peer
    private class ClientHandler implements Runnable {
        private Socket sock;
        private ObjectInputStream ois;

        public ClientHandler(Socket socket) {
            this.sock = socket;
        }

        @Override
        public void run() {
            // ficar a espera de mensagens
            try {
                ois = new ObjectInputStream(sock.getInputStream());

                while (true) {
                    Object receivedObject = ois.readObject();

                    if (receivedObject instanceof Message) {
                        Message msg = (Message) receivedObject;

                        lock.lock();
                        try {
                            msgQueue.add(msg);
                        } finally {
                            lock.unlock();
                        }
                    } else if (receivedObject instanceof Blockchain) {
                        Blockchain receivedBlockchain = (Blockchain) receivedObject;
                        int receivedEpoch = (int) ois.readObject();
                        int receivedPort = (int) ois.readObject();

                        lock.lock();
                        try {
                            blockchain = new Blockchain(port, receivedBlockchain);
                            epoch = receivedEpoch;
                        } finally {
                            lock.unlock();
                        }

                        String fileStatus = (String) ois.readObject();

                        if (fileStatus.equals("HAVEFILE")) {
                            File file = new File("blockchain_" + port + ".json");

                            lock.lock();
                            try {
                                if (!file.exists()) {
                                    outputStreams.get(receivedPort).writeObject("SENDIT");
                                    outputStreams.get(receivedPort).flush();
                                    outputStreams.get(receivedPort).reset();
    
                                    
                                    //Receber
                                    byte[] fileData = (byte[]) ois.readObject();

                                    try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                                        fileOutputStream.write(fileData);
                                        fileOutputStream.flush();
                                    }
                                }   
                                else {
                                    outputStreams.get(receivedPort).writeObject("DONOTHING");
                                    outputStreams.get(receivedPort).flush();
                                    outputStreams.get(receivedPort).reset();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                lock.unlock();
                            }
                        }
                    }
                    else {
                        String receivedString = (String) receivedObject;

                        if (receivedString.equals("RECOVERY")) {
                            int receivedPort = (int) ois.readObject();

                            if (epoch != 0) {
                                if (!connectedPeers.containsKey(receivedPort)) {
                                    connectToPeer(receivedPort);
                                }

                                LinkedList<BlockchainNode> leaves = new LinkedList<>();

                                lock.lock();
                                try {
                                    leaves = blockchain.getLeaves();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    lock.unlock();
                                }

                                if (!Arrays.equals(leaves.getFirst().getBlock().getHash(), new byte[0])) {

                                    lock.lock();
                                    try {
                                        outputStreams.get(receivedPort).writeObject(blockchain);
                                        outputStreams.get(receivedPort).flush();
                                        outputStreams.get(receivedPort).reset();
            
                                        outputStreams.get(receivedPort).writeObject(epoch);
                                        outputStreams.get(receivedPort).flush();
                                        outputStreams.get(receivedPort).reset();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                     finally {
                                        lock.unlock();
                                    }

                                    outputStreams.get(receivedPort).writeObject(port);
                                    outputStreams.get(receivedPort).flush();
                                    outputStreams.get(receivedPort).reset();

                                    File file = new File("blockchain_" + port + ".json");

                                    if (file.exists()) {
                                        outputStreams.get(receivedPort).writeObject("HAVEFILE");
                                        outputStreams.get(receivedPort).flush();
                                        outputStreams.get(receivedPort).reset();

                                        String rec = (String) ois.readObject();

                                        if (rec.equals("SENDIT")) {
                                            byte[] fileData = new byte[(int) file.length()];

                                            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                                                fileInputStream.read(fileData);
                                            }

                                            outputStreams.get(receivedPort).writeObject(fileData);
                                            outputStreams.get(receivedPort).flush();
                                            outputStreams.get(receivedPort).reset();
                                        }
                                    }
                                    else {
                                        outputStreams.get(receivedPort).writeObject("NOFILE");
                                        outputStreams.get(receivedPort).flush();
                                        outputStreams.get(receivedPort).reset();
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("[Node] - Client disconnected");
            }
        }
    }
}
