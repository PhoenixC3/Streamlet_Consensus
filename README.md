# TFD24-25

# TO-DO

- Fazer Garbage Collection

- Alterar o codigo para incluir nova estrutura de blockchains

# Projeto Fase 1 Grupo 10

Daniel Gonzalez 58961; Manuel Campos 58166; Tiago Almeida 58161

# Como funciona o código?

O Projeto é composto por 2 ficheiros base, o "start_time.txt" e o "StreamletProtocol.java".
O .txt contém o tempo em que é suposto os processos começarem a executar o protocolo e o .java é o protocolo em si.

Nesta 1ª fase temos o número de RÉPLICAS = 5 (estão presentes no código) assim como temos uma época = 8 segundos (também embutido no código).
Por isso, caso seja necessário alterar tanto o número de réplicas como o tamanho da época basta alterar ou a variável:

private int epochDuration = 8; // segundos ---> Node.java

OU

private final int[] knownPorts = {8001, 8002, 8003, 8004, 8005}; ---> Node.java

# Como correr o código?

Para correr o código basta:

1. Abrir o ficheiro start_time.txt e alterar o tempo para pelo menos + 1 minuto do que o horario atual
   Se são 21:10, escrever no ficheiro 21:11 ou 21:12 (pois dar run dos terminais pode fazer com que tempo passe e o relógio atualize para o próximo minuto)

2. Abrir 5 terminais e escrever em cada um:
   (Nota: Existe um sleep de 10 segundos no código, para se ter tempo de correr os 5 terminais. Se esses 10 segundos passarem e os 5 terminais não estiverem a correr, os processos vão se tentar conectar antes do outro estar online)

"javac StreamletProtocol.java" para compilar todas as classes (compilar apenas num dos terminais)
"java StreamletProtocol <port>" onde port é o número do porto que queremos atribuir dos mencionados acima {8001, 8002, 8003, 8004, 8005}

Ou seja uma execução com as portas atuais mencionadas seria em terminais diferentes:
java StreamletProtocol 8001
java StreamletProtocol 8002
java StreamletProtocol 8003
java StreamletProtocol 8004
java StreamletProtocol 8005
