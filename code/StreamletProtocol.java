import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class StreamletProtocol{

    public static void main (String[] args){

        int port = Integer.parseInt(args[0]);

        Node replica = new Node(port);

        String time = null;

        String epoch_time = null;
        
        // config format
        // Epoch Duration in seconds - 10
        // Start Time - 17:38

        // Read the epoch duration and start time from the config file
        try{
            File myObj = new File("config.txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                
                if (data.contains("Epoch Duration")){
                    String[] parts = data.split("-");
                    epoch_time = parts[parts.length - 1];
                }
                else if (data.contains("Start Time")){
                    String[] parts = data.split("-");
                    time = parts[parts.length - 1];
                }
            }
            myReader.close();
        }catch(Exception e){
            System.out.println("Error reading config file");
        }

        System.out.println("Epoch Duration chosen: " + epoch_time + " seconds");
        System.out.println("Start Time chosen: " + time);
        System.out.println();
        replica.startNode(time, epoch_time);
    }
}   