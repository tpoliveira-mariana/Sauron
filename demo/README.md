# Sauron - Guião de demonstração

## 1. Preparação do Sistema

Para poder testar a aplicação *silo* e os clientes *eye* e *spotter*, 
é necessário inicialmente preparar um ambiente com dados.

### 1.1 Compilar o Projeto

Para compilar o projeto é necessário instalar as dependências para o *silo* e os clientes(*eye* e *spotter*) e compilar estes componentes.
Para isso, basta ir à diretoria root do projeto e correr o comando:

```
$ mvn install -DskipTests
```

### 1.2 Servidor de Nomes

Antes de poder executar quer o servidor, quer qualquer um dos clientes é necessário  instalar o módulo [ZooKeeper](https://zookeeper.apache.org/releases.html#download).

Após ter instalado este módulo é necessário iniciar um servidor de nomes.
Para isso, basta ir à diretoria `/bin` dentro da diretoria de instalação do módulo e correr o seguinte comando:

```
$ ./zkServer.sh start
```

### 1.2. *Silo*

Para proceder aos testes, é preciso pelo menos uma instância(réplica) *silo* estar a correr.
Sempre que for necessário executar uma réplica será indicado o comando respetivo.
Para executar um réplica com os parâmetros default basta ir à diretoria *silo-server* e executar:

```
$ mvn exec:java
```

Por definição, este comando irá criar a instância *silo* número 1 que irá correr no endereço *localhost* e na porta *8081* e que se irá registar
no servidor de nomes a correr em `localhost` na porta `2181`.

### 1.3. *Eye*

Sempre que for necessário criar um cliente *eye* tal será indicado. 
Para criar um ciente *eye* basta ir à diretoria *eye* e correr o seguinte comando:

```
$ eye localhost 2181 Alameda 30.303164 -10.737613
```

Este comando irá criar uma câmera que irá utilizar o servidor de nomes que se encontra no endereço *locahost* e na porta *2181* para localizar as réplicas a contactar.

A câmera tem o nome dado pelo terceiro argumento e as suas coordenadas dadas pelo quarto e quinto argumentos.

**Nota:** Para correr o script *eye* é necessário fazer `mvn install` e adicionar ao *PATH* ou utilizar diretamente os executáveis gerados na diretoria `target/appassembler/bin/`.

### 1.3. *Spotter*

Alguns dos comandos são exclusivos do client *spotter*, por isso, é necessário executar um cliente *spotter*.
Para isso basta ir à diretoria *spotter* e correr o seguinte commando:

```
$ spotter localhost 2181
```

**Nota:** Para correr o script *spotter* é necessário fazer `mvn install` e adicionar ao *PATH* ou utilizar diretamente os executáveis gerados na diretoria `target/appassembler/bin/`.

Com as instruções acima é possível correr qualquer tipo de instrução suportada pela aplicação.
De seguida demonstram-se algumas situações de replicação e de tolerância a faltas.  


## 2. Replicação e Tolerância a Faltas

Nesta secção vamos efetuar os procedimentos de forma a exemplificar os mecanismos de replicação, coerência no cliente e de tolerância a faltas da aplicação. 
Cada subsecção é respetiva a uma situação possível de acontecer durante a execução da aplicação *silo*.

### 2.1. Replicação e Coerência no Cliente

Nesta subsecção iremos apresentar exemplos do mecanismo de replicação e de coerência no cliente em atuação.

Para tal, inicialmente, na diretoria `silo-server`, lançamos a primeira réplica(1) que irá correr  no endereço `localhost` e na porta `8081`.

O parâmetro `replicaNum` indica o número máximo de réplicas diferentes que estarão ativas para comunicação durante a execução.

O parâmetro `gossipTimer` indica o número de segundos entre cada ronda de partilha de mensagens entre réplicas

```
mvn exec:java -DreplicaNum=2 -DgosspiTimer=15
```

De seguida lançamos um cliente *eye* e enviamos as observações presentes no ficheiro fornecido à réplica 1.
Para isso corremos o seguinte comando na diretoria `eye`:

```
$ eye localhost 2181 Alameda 30.303164 -10.737613 < replication_test.txt
```

De seguida, verificamos se as observações forem bem submetidas na réplica 1.
Para isso, lançamos um cliente spotter, estando na diretoria `spotter` através do comando:

A opção `instance` indica preferência de conexão do cliente à réplica dada, neste caso 1

```
$ mvn exec:java -Dinstance=1
```

No cliente spotter executamos o comando:

```
-> spot person 1*
```

Ao que, se tudo tiver sido submetido corretamente, o servidor deve responder com:

```
person,1,[timestamp],Alameda,30.303164,-10.737613
person,15,[timestamp],Alameda,30.303164,-10.737613
person,17,[timestamp],Alameda,30.303164,-10.737613
```

De seguida, colocamos a execução da réplica 1 em suspenso utilizando as teclas `CTRL` + `Z`.
Lançamos uma segunda réplica(2) com o comando seguinte na diretoria `silo-server`:

```
mvn exec:java -Dinstance=2 -DreplicaNum=2
```

Esta réplica irá correr no endereço `localhost` e na porta `8082`

Posteriormente, verificamos que esta réplica se encontra limpa ao lançarmos um novo cliente *spotter* que se ligue a esta réplica, com o comando:

```
$ mvn exec:java -Dinstance=2
```

Neste cliente executamos novamente o comando:

```
-> spot person 1*
```

A resposta esperada deve ser:

```
Invalid usage of spot - No ID matches the one given!
```

No cliente *spotter* que ligámos à réplica 1 executamos o mesmo comando.

Como a réplica 1 está indisponível este imprimirá eventualmente a mensagem abaixo que indica que se conectou à réplica 2:

```
Choosing another replica to connect to...
Choosing random replica...
Connected to: /grpc/sauron/silo/2
```

Posteriormente será recebida a resposta da réplica 2 e a resposta esperada é a mesma que a obtida da réplica 1.

Esta resposta ilustra a coerência do ponto de vista das leituras do cliente, uma vez que este não imprime a resposta obtida pelo *spotter* anterior.

De seguida, ativamos novamente a réplica 1 com o comando:

```
fg
```

Esperamos até obtermos nesta réplica a seguinte mensagem, que indica que a réplica 1 trocou os seus pedidos com a réplica 2:

```
Replica 1 initiating gossip...
Connecting to replica 2 at localhost:8082...
Gossip to replica 2 successful, exiting gossip
```

Novamente no cliente *spotter* que ligámos à réplica 2 efetuamos o comando:

```
-> spot person 1*
```

A resposta esperada desta vez deverá ser:

```
person,1,[timestamp],Alameda,30.303164,-10.737613
person,15,[timestamp],Alameda,30.303164,-10.737613
person,17,[timestamp],Alameda,30.303164,-10.737613
```

### 2.2. Tolerância a Faltas


## 3. Correr Testes Automáticos

Para correr os testes automáticos é necessário primeiro lançar uma réplica com o comando seguinte na diretoria `silo-server`:

```
mvn exec:java
```

De seguida para executar os testes basta correr o seguinte comando na diretoria `silo-client`:

```
mvn verify
```


