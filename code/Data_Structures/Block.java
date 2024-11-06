package Data_Structures;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

// Note: All nodes of the system start with the genesis block, which has Length, Epoch and Hash equals 0.
public class Block implements Content, Serializable {

    private static final long serialVersionUID = 1L;

    private byte[] hash;                    // Hash (bytes): SHA1 hash of previous block.
    private int epoch;                      // Epoch (integer): the epoch number the block was generated.
    private int length;                     // Length (integer): the number of the block in the proposer blockchain.
    private Transaction[] transactions;     // Transactions (array of Transactions): the list of transactions on the block.

    public Block(byte[] previousHash, int epoch, int length, Transaction[] transactions){
        this.hash = previousHash;
        this.epoch = epoch;
        this.length = length;
        this.transactions = transactions;
    }

    public byte[] getHash(){
        return hash;
    }

    public int getEpoch(){
        return epoch;
    }

    public int getLength(){
        return length;
    }

    public Transaction[] getTransactions(){
        return transactions;
    }

    public String stringContent(){
        String result = "Block: Epoch: " + epoch + ", length: " + length ;
        return result;
    }

    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;

        if (obj == null || getClass() != obj.getClass()) return false;

        Block block = (Block) obj;

        return epoch == block.epoch && length == block.length && Arrays.equals(hash, block.hash) && Arrays.deepEquals(transactions, block.transactions);
    }

    @Override
    public int hashCode(){
        int result = 17;
        result = 31 * result + epoch;
        result = 31 * result + length;
        result = 31 * result + Arrays.hashCode(hash);
        result = 31 * result + Arrays.hashCode(transactions);
        return result;
   }
    // Método para calcular o hash do bloco
    public byte[] calculateHash(){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(hash);  // Inclui o hash do bloco anterior
            digest.update(Integer.toString(epoch).getBytes());
            digest.update(Integer.toString(length).getBytes());

            for (Transaction tx : transactions) {
                digest.update(tx.toString().getBytes());  // Converte as transações para bytes
            }

            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
