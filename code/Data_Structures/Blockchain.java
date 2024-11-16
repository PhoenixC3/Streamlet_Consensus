package Data_Structures;

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
    public void addBlock(Block block){
        // If the blockchain is empty, create the first node
        if(leaves.isEmpty()){
            leaves.add(new BlockchainNode(block, null));
        }else{
            // If !empty we need to check if the block has a reference to the previous block
            // If it does, we update the leaves list by removing the previous node and adding the new one
            // and we finalize the previous blocks if necessary
            // If it doesn't, we add the block to the leaves list
            for(BlockchainNode node : leaves){
                if(node.getBlock().getHash().equals(block.getHash())){
                    leaves.remove(node);
                    BlockchainNode newNode = new BlockchainNode(block, node);
                    finalizeBlocks(newNode);
                    leaves.add(newNode);
                    return;
                }
            }
        }
    }

    // Check if the blocks last 3 blocks are from sequential epochs
    // If they are, finalize previous blocks except the last one
    public void finalizeBlocks(BlockchainNode n){
        BlockchainNode current = n;

        // Check if the last 3 blocks are from sequential epochs
        if (current.getBlock().getEpoch() == current.getPrevious().getBlock().getEpoch() + 1 &&
        current.getPrevious().getBlock().getEpoch() == current.getPrevious().getPrevious().getBlock().getEpoch() + 2){
            // Finalize the previous blocks
            current = current.getPrevious();
            while(current != null && !current.isFinalized()){
                current.finalize();
                current = current.getPrevious();
            }
        }
    }

    // Get the leaves of the blockchain
    public LinkedList<BlockchainNode> getLeaves(){
        return leaves;
    }

    // get the longest chain
    public LinkedList<Block> getLongestChain(){
        LinkedList<Block> longestChain = new LinkedList<Block>();
        int maxLength = 0;
        for(BlockchainNode node : leaves){
            LinkedList<Block> chain = new LinkedList<Block>();
            BlockchainNode current = node;
            while(current != null){
                chain.addFirst(current.getBlock());
                current = current.getPrevious();
            }
            if(chain.size() > maxLength){
                maxLength = chain.size();
                longestChain = chain;
            }
        }
        return longestChain;
    }
}