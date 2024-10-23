import Data_Structures.Transaction;

public class StreamletProtocol{


    // ! IMPLEMENTAR E TESTAR COMUNICAÇÃO COM ENTRE VARIAS REPLICAS
    public static void main (String[] args){

        Transaction temp = new Transaction(1, 2, 3, 4.0);
        System.out.println(temp.toString());
    }
}   