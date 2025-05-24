# Streamlet consensus algorithm used to implement a distributed ledger

# Projeto Fase 2 Grupo 10

Daniel Gonzalez 58961; Manuel Campos 58166; Tiago Almeida 58161

Projeto implementado com forks, recovery & escrita no disco funcionais!

# Como funciona o código?

O Projeto é composto por 3 ficheiros base, o "config.txt", "StreamletProtocol.java" & "serverPorts.txt".
O ficheiro "config.txt" contém o tempo de duração de cada época assim como o horario exato em que o protocolo vai começar,
o .java tem o protocolo em si & o "serverPorts.txt" apresenta as diferentes ports que vão correr o protocolo, neste exemplo:
{8001, 8002, 8003, 8004, 8005}.


# Como correr o código?
Para correr o código basta:

1. Abrir o ficheiro config.txt e alterar o tempo para pelo menos + 1 minuto do que o horario atual
   Se são 21:10, escrever no ficheiro 21:11 ou 21:12 (pois dar run dos terminais pode fazer com que tempo passe e o relógio atualize para o próximo minuto)

2. Abrir 5 terminais e escrever em cada um:
   (Nota: Existe um sleep de 5 segundos no código, para se ter tempo de correr os 5 terminais. Se esses 5 segundos passarem e os 5 terminais não estiverem a correr, os processos vão se tentar conectar antes do outro estar online)

"javac StreamletProtocol.java" para compilar todas as classes (compilar apenas num dos terminais)
"java StreamletProtocol <port>" onde port é o número do porto que queremos atribuir dos mencionados acima {8001, 8002, 8003, 8004, 8005}

Ou seja uma execução com as portas atuais mencionadas seria em terminais diferentes:
java StreamletProtocol 8001
java StreamletProtocol 8002
java StreamletProtocol 8003
java StreamletProtocol 8004
java StreamletProtocol 8005
