import Data_Structures.Transaction;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class Utils{
    

    // Calcular o Líder da época
    public static int getLeader(int epoch, int[] nodes) {
            try {
                // Converte o número da época para bytes e aplica o SHA-256
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] epochBytes = Integer.toString(epoch).getBytes();
                byte[] hash = digest.digest(epochBytes);

                // Usa o hash para inicializar um gerador de números aleatórios
                long seed = ((long) hash[0] & 0xFF) | ((long) hash[1] & 0xFF) << 8 |
                            ((long) hash[2] & 0xFF) << 16 | ((long) hash[3] & 0xFF) << 24;
                Random random = new Random(seed);

                // Gera um índice aleatório baseado no número de nós
                int index = random.nextInt(nodes.length);
                return nodes[index];
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return nodes[epoch % nodes.length];  // Fallback em caso de erro
            }
    }

    // gera um numero aleatorio para escolhermos um node do fork
    public static int randomFromFork(int epoch, int leavesSize) {
        try {
            // Converte o número da época para bytes e aplica o SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] epochBytes = Integer.toString(epoch).getBytes();
            byte[] hash = digest.digest(epochBytes);

            // Usa o hash para inicializar um gerador de números aleatórios
            long seed = ((long) hash[0] & 0xFF) | ((long) hash[1] & 0xFF) << 8 |
                        ((long) hash[2] & 0xFF) << 16 | ((long) hash[3] & 0xFF) << 24;
            Random random = new Random(seed);

            // Gera um índice aleatório baseado no número de folhas
            return random.nextInt(leavesSize);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return -1;  // Fallback em caso de erro
        }
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


