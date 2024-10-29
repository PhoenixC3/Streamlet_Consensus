package Data_Structures;

import java.io.Serializable;

public class Transaction implements Serializable{

    private int sender;          // Sender (integer): the sender of the transaction.
    private int receiver;        // Receiver (integer): the receiver of the transaction.
    private int id;              // Transaction id (integer): a nonce that together with the sender should form a unique id for the tx.
    private double ammount;      // Amount (double): the amount to be transferred.


    public Transaction(int sender, int receiver, int id, double ammount){
        this.sender = sender;
        this.receiver = receiver;
        this.id = id;
        this.ammount = ammount;
    }

    public Transaction(){
    }

    public int getSender(){
        return sender;
    }

    public int getReceiver(){
        return receiver;
    }

    public int getId(){
        return id;
    }

    public double getAmmount(){
        return ammount;
    }

    public String toString(){
        return "Transaction: sender: " + sender + ", receiver: " + receiver + ", id: " + id + ", ammount: " + ammount;
    }

    @Override
    public boolean equals(Object obj){
        if(obj == null){
            return false;
        }
        if(obj == this){
            return true;
        }
        if(obj.getClass() != this.getClass()){
            return false;
        }
        Transaction transaction = (Transaction) obj;
        return (this.sender == transaction.sender && this.receiver == transaction.receiver && this.id == transaction.id && this.ammount == transaction.ammount);
    }
}
