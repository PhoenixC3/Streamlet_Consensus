import java.nio.file.Files;
import java.nio.file.Paths;

public class StreamletProtocol{

    // ! IMPLEMENTAR E TESTAR COMUNICAÇÃO COM ENTRE VARIAS REPLICAS
    public static void main (String[] args){

        int port = Integer.parseInt(args[0]);

        Node replica = new Node(port);

        String time = null;
        
        try {
            time = Files.readString(Paths.get("start_time.txt")).trim();
        } catch (Exception e) {
            e.printStackTrace();
        }

        replica.startNode(time);
    }
}   