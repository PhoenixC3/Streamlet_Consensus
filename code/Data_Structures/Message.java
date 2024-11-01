package Data_Structures;
import java.io.Serializable;

public class Message implements Content, Serializable {

    private static final long serialVersionUID = 1L;

    // Propose: to be used for proposing blocks. The Content is a Block.
    // Vote: to be used for voting on blocks. The Content is a Block., with the Transactions field empty.
    // Echo: to be used when echoing a message. The Content is a Message
    public Type type;
    public Content content;             // Content (Message or Block)
    public int sender; // porto d

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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Message message = (Message) obj;
        return sender == message.getSender() && type == message.getType() && (content != null ? content.equals(message.getContent()) : message.getContent() == null);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + sender;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        return result;
    }
}




