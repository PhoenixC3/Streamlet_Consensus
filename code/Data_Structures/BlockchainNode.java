package Data_Structures;

import java.io.Serializable;

// This class represents a node in the blockchain

// A node has a block and a reference to the previous node
// The previous node is null if this is the first node in the blockchain
// With this implementation we can easily traverse the blockchain
public class BlockchainNode implements Serializable{

    private static final long serialVersionUID = 1L;
    
    // The block in this node
    private Block block;
    // Reference to the previous node
    private BlockchainNode previous;
    // attribute to check if the node is finalized
    private boolean finalized = false;

    // Constructor
    public BlockchainNode(Block block, BlockchainNode previous){
        this.block = block;
        this.previous = previous;
    }

    // Set the previous node
    public void setPrevious(BlockchainNode previous){
        this.previous = previous;
    }

    // Get the block in this node
    public Block getBlock(){
        return block;
    }

    // Get the previous node
    public BlockchainNode getPrevious(){
        return previous;
    }

    // finalize
    public void finalizeBlock(){
        finalized = true;
    }

    // check if the node is finalized
    public boolean isFinalized(){
        return finalized;
    }

}
