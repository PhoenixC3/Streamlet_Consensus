# TFD24-25

# Breve Descricao

5 replicas a correr em processos independentes
Tem de se connectar entre si e trocar mensagens
O Protocolo corre em epocas, onde sao propostos blocos e sao votados posteriormente

- Setup:
  Timer de 30 segundos para termos tempo de ligar todas as replicas e depois conecta-las entre si.
  Cada replica vai ter um PORTO associado. Informacoes sobre os PORTOS vao estar num .TXT.
  Cada replica vai ter de comunicar com todas as outras

# Tarefas ( Por Fazer )

- Criar codigo da classe StreamletProtocol que vai ser a "main" classe
- Criar ficheiros secundarios com o codigo de conexao/funcoes uteis
- Comecar a implementar o protocolo em si com as epocas
- While loop para trocar mensagens infinitamente

- Definir estrutura para a blockchain
- Definir Mapa de ReceivedMessages

- Definir Época com quem é o Leader etc

# Tarefas ( Concluido )

- Criar as classes mencionadas no enunciado
- Criar uma classe StreamletProtocol que vai ser a "main" classe

- Conseguir Ligar 2 e posteriormente 3 replicas entre si e trocarem pelo menos 3 mensagens
- Conseguir Ligar 5 replicas entre si e trocarem pelo menos 3 mensagens

# Passo a passo
Nodes geram ghost transactions -> Bloco(Transactions) -> Broadcast da Mensagem com o Bloco

Ronda -> 4 segundos

Notorização: Bloco recebe pelo menos (n/2) + 1 VOTES. 

Eleição de Lider: Época mod Nº de nodes

Propor: Líder propoe Bloco para uma das chains maiores

Vote: Node recebe bloco proposto, só vota se lenght > todas as outras chains

ECHO A TODAS AS MENSAGENS

Finalizar: Se 3 blocos com épocas seguidas forem vistos, finalizar os últimos dois blocos e o resto para trás

Guardar quem nos enviou a mensagem
