public class StreamletProtocol{

    // ! IMPLEMENTAR E TESTAR COMUNICAÇÃO COM ENTRE VARIAS REPLICAS
    public static void main (String[] args){

        int port = Integer.parseInt(args[0]);

        Node replica = new Node(port);
        replica.startNode();
    }
}   