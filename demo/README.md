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

Com as instruções acima é possível correr qualquer tipo de instrução suportada pela aplicação (seccção *2.*).
Posteriormente demonstram-se-ão algumas situações de replicação e de tolerância a faltas (secção *3.*).  


##2. Demonstração Funcionalidades

Nesta secção vamos correr os comandos necessários para testar todas as operações. 
Cada subsecção é respetiva a cada operação presente no *silo*.

Para iniciar a execução dos comandos desta secção é necessário lançar uma réplica com o comando seguinte na diretoria `silo-server`:

```
$ mvn exec:java
```

### 2.1. *cam_join*

Para testar este comando e para popular a réplica criamos três câmeras diferentes e enviamos observações à réplica com os seguinte comandos:

```
$ eye localhost 2181 Tagus 38.737613 -9.303164 < eye_1.txt
$ eye localhost 2181 Alameda 30.303164 -10.737613 < eye_2.txt
$ eye localhost 2181 Lisboa 32.737613 -15.303164 < eye_3.txt
```

Existem também algumas restrições que podem ser testadas como as seguintes:

- 2.1.1. Teste do tamanho do nome.  
O servidor deve rejeitar esta operação. 
Para isso basta executar um *eye* com o seguinte comando:

```
$ eye localhost 2181 ab 10.0 10.0
$ eye localhost 2181 abcdefghijklmnop 10.0 10.0
```

Deve ser obtida a seguinte resposta
```
Invalid camera name provided.
```

### 2.2. *report*

Esta operação já foi testada no comando anterior ao popular a réplica.

No entanto falta testar o sucesso do comando *zzz*. 
Para testar basta abrir um cliente *spotter* e correr o comando seguinte:

```
-> trail person 1
```

O resultado deverá ser o seguinte, em que os timestamps têm cerca de dois segundos de diferença

```
person,1,[timestamp],Tagus,38.737613,-9.303164
person,1,[timestamp],Tagus,38.737613,-9.303164
```

### 2.3. *cam_info*

Para executar todos os seguintes comandos é necessário manter o cliente *spotter* lançado anteriormente:

2.3.1. Teste para uma câmera existente. 
De seguida, corremos o comando:

```
-> info Alameda
```

Deve ser obtida a resposta:

```
Alameda,30.303164,-10.737613
```

2.3.2. Teste para câmera inexistente.  

Correndo o seguinte comando:

```
-> info aaaaa
```

Deve ser obtida a resposta:

```
Invalid usage of info - Non existing camera
```

### 2.4. *track*

Esta operação vai ser testada utilizando o comando *spot* com um identificador.

2.5.1. Teste com uma pessoa inexistente:

```
-> spot person 9
```

Deverá devolver:

```
Invalid usage of spot - No ID matches the one given!
```

2.4.2. Teste com uma pessoa:

```
-> spot person 15
```

Deverá devolver:

```
person,15,[timestamp],Alameda,30.303164,-10.737613
```

2.4.3. Teste com um carro:

```
-> spot car 11AA22
```

Deverá devolver:

```
car,[timestamp],Alameda,30.303164,-10.737613
```

### 2.5. *trackMatch*

Esta operação vai ser testada utilizando o comando *spot* com um fragmento de identificador.

2.5.1. Teste com uma pessoa inexistente:

```
-> spot person 9*
```

Deverá devolver:

```
Invalid usage of spot - No ID matches the one given!
```

2.5.2. Teste com uma pessoa:

```
-> spot person 2*
```

Deverá devolver:

```
person,[timestamp],Alameda,30.303164,-10.737613
```

2.5.3. Teste com quatro pessoas:

```
-> spot person 1*
```

Deverá devolver:

```
person,1,[timestamp],Tagus,38.737613,-9.303164
person,15,[timestamp],Alameda,30.303164,-10.737613
person,17,[timestamp],Lisboa,32.737613,-15.303164
person,19,[timestamp],Lisboa,32.737613,-15.303164
```


2.5.4. Teste com um carro:

```
-> spot car 11AA*
car,00AA00,<timestamp>,Tagus,38.737613,-9.303164
```

Deverá devolver:

```
car,11AA22,[timestamp],Alameda,30.303164,-10.737613
```

2.5.5. Teste com dois carros:

```
-> spot car 1122*
```

Deverá responder:

```
car,1122AA,[timestamp],Lisboa,32.737613,-15.303164
car,1122BB,[timestamp],Lisboa,32.737613,-15.303164
```

### 2.6. *trace*

Esta operação vai ser testada utilizando o comando *trail* com um identificador.

2.6.1. Teste com uma pessoa inexistente:

```
> trail person 9
```

Deverá devolver:

```
Invalid usage of trail - No ID matches the one given!
```

2.6.2. Teste com uma pessoa:

```
-> trail person 19
```

Deverá devolver:

```
person,19,[timestamp],Lisboa,32.737613,-15.303164
person,19,[timestamp],Alameda,30.303164,-10.737613
```

2.6.3. Teste com um carro inexistente:

```
-> trail car AABB77
```

Deverá devolver:

```
Invalid usage of trail - No ID matches the one given!
```

2.6.4. Teste com um carro:

```
-> trail car 1122AA
```

Deverá devolver:

```
car,1122AA,[timestamp],Lisboa,32.737613,-15.303164
car,1122AA,[timestamp],Alameda,30.303164,-10.737613
car,1122AA,[timestamp],Tagus,38.737613,-9.303164
```

## 3. Replicação e Tolerância a Faltas

Nesta secção vamos efetuar os procedimentos de forma a exemplificar os mecanismos de replicação, coerência no cliente e de tolerância a faltas da aplicação. 
Cada subsecção é respetiva a uma situação possível de acontecer durante a execução da aplicação *silo*.

### 3.1. Replicação e Coerência no Cliente

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

### 3.2. Tolerância a Faltas


## 4. Correr Testes Automáticos

Para correr os testes automáticos é necessário primeiro lançar uma réplica com o comando seguinte na diretoria `silo-server`:

```
mvn exec:java
```

De seguida para executar os testes basta correr o seguinte comando na diretoria `silo-client`:

```
mvn verify
```

## 5. Notas Finais


