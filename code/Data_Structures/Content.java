package Data_Structures;

import java.io.Serializable;

// Interface para o content da mensagem (Block or Message)
public interface Content extends Serializable {

    static final long serialVersionUID = 1L;
    
    // Retorna uma string com o conteúdo
    String stringContent();
}
