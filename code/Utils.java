import Data_Structures.Transaction;
import java.util.Random;

public class Utils{
    

    // Calcular o Líder da época
    public static int getLeader(int epoch, int[] nodes){
 
        int index = epoch % nodes.length; 
        return nodes[index];
    }


    // Cria 5 ghost transactions
    public static Transaction[] generateTransactions(){
        Random rd = new Random();
        Transaction[] retval = new Transaction[5];
        
        for (int i = 0; i < retval.length; i++) {
            int sender = rd.nextInt(9999);         
            int receiver =  rd.nextInt(9999);  
            int id = rd.nextInt(9999);           
            double ammount = rd.nextDouble(9999);
            
            retval[i] = new Transaction(sender,receiver,id, ammount);

        }
        return retval;
    }

}


