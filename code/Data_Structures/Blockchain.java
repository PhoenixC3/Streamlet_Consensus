package Data_Structures;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;

// This class represents a blockchain
// It will have a list of the leaves of the blockchain
// The leaves are the last nodes of the blockchain that may have been born from forks
public class Blockchain implements Serializable{

    private int node;

    private static final long serialVersionUID = 1L;

    // List of leaves
    private LinkedList<BlockchainNode> leaves;

    // Constructor
    public Blockchain(int node){
        this.node = node;
        leaves = new LinkedList<BlockchainNode>();
    }

    // Add a block to the blockchain
    // If the block is already in the blockchain, do nothing
    public boolean addBlock(Block block){
        boolean fin = false;

        // check if the block is already in the 
        if(contains(block)){
            return false;
        }

        // If the blockchain is empty, create the first node
        if(leaves.isEmpty()){
            leaves.add(new BlockchainNode(block, null));
            return true;
        }else{
            System.out.println("Block notarized at epoch " + block.getEpoch());
            // If !empty we need to check if the block has a reference to the previous block
            // If it does, we update the leaves list by removing the previous node and adding the new one
            // and we finalize the previous blocks if necessary
            // If it doesn't, we add the block to the leaves list
            for(BlockchainNode node : leaves){
                if(Arrays.equals(node.getBlock().calculateHash(), block.getHash())){
                    BlockchainNode newNode = new BlockchainNode(block, node);

                    // If we finalize the blocks, we need to clean the leaves list
                    // so we can converge to a single chain
                    if (finalizeBlocks(newNode) ){
                        fin = true;
                        leaves.clear();
                    }else{
                        leaves.remove(node);
                    }
                    leaves.add(newNode);
                    // If the block is finalized and the blockchain in memory is higher than 2 blocks append to JSON
                    if(fin){
                        if(getLength(newNode) > 2){
                            BlockchainNode temp = newNode;
                            // Skip the last 2 finalized blocks
                            temp = temp.getPrevious();
                            temp = temp.getPrevious();

                            BlockchainNode setNull = newNode;
                            setNull = setNull.getPrevious();
                            setNull.clearPrevious(); 
                            appendToJSON(temp);
                        }
                    }
                    return true;
                }
            }
            // If the block is not in the blockchain, we add it to the leaves list 
            // but we need to check whats the previous block
            BlockchainNode previousNode = getPreviousNode(block);
            if( previousNode == null){
                System.out.println("####### Error: Block without reference to previous block #######");
                return true; 
            }
            BlockchainNode newNode = new BlockchainNode(block, previousNode);
            leaves.add(newNode);
            return true;

        }
    }

    // Check if the blocks last 3 blocks are from sequential epochs
    // If they are, finalize previous blocks except the last one
    // Return true if the blocks were finalized
    private boolean finalizeBlocks(BlockchainNode n){
        Boolean finalized = false;
        BlockchainNode current = n;

        // Check if the last 3 blocks are from sequential epochs
        if (current.getBlock().getLength() > 2 && current.getBlock().getEpoch() == current.getPrevious().getBlock().getEpoch() + 1 &&
        current.getPrevious().getBlock().getEpoch() == current.getPrevious().getPrevious().getBlock().getEpoch() + 1){

            // Finalize the previous blocks
            current = current.getPrevious();
            System.out.println("#############");
            System.out.println("Finalized blocks up to epoch: " + current.getBlock().getEpoch());
            System.out.println("#############");
            while(current != null && !current.isFinalized()){
                current.finalizeBlock();
                current = current.getPrevious();
            }
            finalized = true;
        }

        return finalized;
    }

    // Check if the block is in the blockchain
    private boolean contains(Block block){
        // For each leaf, check if leaf.getBlock() == block and
        // traverse the blockchain to check if the block is in the blockchain
        for(BlockchainNode node : leaves){
            BlockchainNode current = node;
            while(current != null){
                if(current.getBlock().equals(block)){
                    return true;
                }
                current = current.getPrevious();
            }
        }
        return false;
    }

    // search the blockchain to get the previous node of a block
    private BlockchainNode getPreviousNode(Block block){
        // check for each block if the hash from previous block is the same as some block in the blockchain
        for(BlockchainNode node : leaves){
            BlockchainNode current = node;
            while(current != null){
                if(Arrays.equals(current.getBlock().calculateHash(), block.getHash())){
                    return current;
                }
                current = current.getPrevious();
            }
        }
        return null;
    }

    // Get the leaves of the blockchain
    public LinkedList<BlockchainNode> getLeaves(){
        return leaves;
    }

    // get the longest finalized chain
    public LinkedList<Block> getLongestChain(){
        LinkedList<Block> longestChain = new LinkedList<Block>();
        int maxLength = 0;
        for(BlockchainNode node : leaves){
            int length = 0;
            BlockchainNode current = node;
            BlockchainNode finalHead = null;
            while(current != null ){
                if (current.isFinalized()){
                    length++;
                    if (finalHead == null){
                        finalHead = current;
                    }
                }
                current = current.getPrevious();
            }
            if(length > maxLength){
                maxLength = length;
                longestChain.clear();
                current = finalHead;
                while(current != null){
                    longestChain.addFirst(current.getBlock());
                    current = current.getPrevious();
                }
            }
        }
        return longestChain;
    }

    // will always return the length of the chain including the genesis block
    public int getLength(BlockchainNode b){
        int length = 0;
        BlockchainNode current = b;
        while(current != null){
            length++;
            current = current.getPrevious();
        }
        return length;
    }

    private void appendToJSON(BlockchainNode startNode) {
        String filePath = "blockchain_" + this.node + ".json";
        try {
            File file = new File(filePath);

            // Step 1: Check if the file exists
            boolean isNewFile = !file.exists();

            try (FileWriter writer = new FileWriter(filePath, true)) {
                if (isNewFile) {
                    // Start a new JSON file with an opening array
                    writer.write("{\n");
                    writer.write("\"blocks\": [\n");
                } else {
                    // If the file already exists, clean up the last array bracket
                    RandomAccessFile raf = new RandomAccessFile(file, "rw");
                    long length = raf.length();
                    if (length > 0) {
                        // Remove the trailing "]}" to keep appending valid
                        raf.seek(length - 3);
                        raf.write(",\n".getBytes());
                    }
                    raf.close();
                }
                
                boolean isFirstBlock = isNewFile;
                LinkedList<Block> chain = getChain(startNode);
                
                for (Block current : chain){
                    if (!isFirstBlock) writer.write(",\n");

                    writer.write("  {\n");
                    writer.write("    \"epoch\": " + current.getEpoch() + ",\n");
                    writer.write("    \"hash\": \"" + Arrays.toString(current.getHash()) + "\",\n");
                    writer.write("    \"length\": " + current.getLength() + ",\n");

                    // Transactions
                    writer.write("    \"transactions\": [\n");
                    Transaction[] transactions = current.getTransactions();
                    for (int i = 0; i < transactions.length; i++) {
                        Transaction transaction = transactions[i];
                        writer.write("      {\n");
                        writer.write("        \"sender\": \"" + transaction.getSender() + "\",\n");
                        writer.write("        \"receiver\": \"" + transaction.getReceiver() + "\",\n");
                        writer.write("        \"id\": \"" + transaction.getId() + "\",\n");
                        writer.write("        \"amount\": " + transaction.getAmmount() + "\n");
                        writer.write("      }");
                        if (i < transactions.length - 1) writer.write(",");
                        writer.write("\n");
                    }
                    writer.write("    ]\n"); // End transactions array

                    writer.write("  }"); // End block object
                    isFirstBlock = false;
                }

                // Finalize JSON structure
                writer.write("\n]}");
            } catch (Exception e) {
                System.err.println("Error writing to JSON: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Error appending to JSON: " + e.getMessage());
        }
    }

    private LinkedList<Block> getChain(BlockchainNode startNode) {
        LinkedList<Block> chain = new LinkedList<Block>();
        BlockchainNode current = startNode;
        while (current != null) {
            chain.addFirst(current.getBlock());
            current = current.getPrevious();
        }
        return chain;
    }
}