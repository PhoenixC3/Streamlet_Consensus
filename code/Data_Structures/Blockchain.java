package Data_Structures;

import java.util.Arrays;
import java.util.LinkedList;

// This class represents a blockchain
// It will have a list of the leaves of the blockchain
// The leaves are the last nodes of the blockchain that may have been born from forks
public class Blockchain{

    // List of leaves
    private LinkedList<BlockchainNode> leaves;

    // Constructor
    public Blockchain(){
        leaves = new LinkedList<BlockchainNode>();
    }

    // Add a block to the blockchain
    // If the block is already in the blockchain, do nothing
    public void addBlock(Block block){

        // check if the block is already in the 
        if(contains(block)){
            return;
        }

        // If the blockchain is empty, create the first node
        if(leaves.isEmpty()){
            leaves.add(new BlockchainNode(block, null));
        }else{
            // If !empty we need to check if the block has a reference to the previous block
            // If it does, we update the leaves list by removing the previous node and adding the new one
            // and we finalize the previous blocks if necessary
            // If it doesn't, we add the block to the leaves list
            for(BlockchainNode node : leaves){
                if(Arrays.equals(node.getBlock().getHash(), block.getHash())){
                    BlockchainNode newNode = new BlockchainNode(block, node);

                    // If we finalize the blocks, we need to clean the leaves list
                    // so we can converge to a single chain
                    if (finalizeBlocks(newNode) ){
                        leaves.clear();
                    }else{
                        leaves.remove(node);
                    }
                    leaves.add(newNode);
                    return;
                }
            }
        }
    }

    // Check if the blocks last 3 blocks are from sequential epochs
    // If they are, finalize previous blocks except the last one
    // Return true if the blocks were finalized
    public boolean finalizeBlocks(BlockchainNode n){
        Boolean finalized = false;
        BlockchainNode current = n;

        // Check if the last 3 blocks are from sequential epochs
        if (current.getBlock().getLength() > 2 && current.getBlock().getEpoch() == current.getPrevious().getBlock().getEpoch() + 1 &&
        current.getPrevious().getBlock().getEpoch() == current.getPrevious().getPrevious().getBlock().getEpoch() + 1){

            // Finalize the previous blocks
            current = current.getPrevious();
            System.out.println("Blocks finalized until epoch: " + current.getBlock().getEpoch());
            while(current != null && !current.isFinalized()){
                current.finalize();
                current = current.getPrevious();
            }
            finalized = true;
        }

        return finalized;
    }

    // Check if the block is in the blockchain
    public boolean contains(Block block){
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
}