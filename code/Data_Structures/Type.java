package Data_Structures;

public enum Type {
    Propose, Vote, Echo
}

// Propose: to be used for proposing blocks. The Content is a Block.
// Vote: to be used for voting on blocks. The Content is a Block., with the Transactions field empty.
// Echo: to be used when echoing a message. The Content is a Message