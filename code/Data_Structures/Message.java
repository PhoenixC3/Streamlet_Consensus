package Data_Structures;

public class Message implements Content{

    // Propose: to be used for proposing blocks. The Content is a Block.
    // Vote: to be used for voting on blocks. The Content is a Block., with the Transactions field empty.
    // Echo: to be used when echoing a message. The Content is a Message
    public Type type;
    public Content content;             // Content (Message or Block)
    public int sender;

    public Message(Type type, Content content, int sender){
        this.type = type;
        this.content = content;
        this.sender = sender;
    }

    public Type getType(){
        return type;
    }

    // usar instanceof para verificar o tipo de content
    public Content getContent(){
        return content;
    }

    public int getSender(){
        return sender;
    }

    public String stringContent(){
        return "Message: Type: " + type;
    }
}




