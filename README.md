# TFD24-25

# Breve Descricao

5 replicas a correr em processos independentes
Tem de se connectar entre si e trocar mensagens
O Protocolo corre em epocas, onde sao propostos blocos e sao votados posteriormente

# Tarefas ( Por Fazer )

- Handle de mensagens
- Fazer com que a cada mensagem exista "echo"
- Fazer com que o código execute todo ao mesmo tempo


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
