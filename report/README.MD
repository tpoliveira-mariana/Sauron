# Relatório do projeto Sauron

Sistemas Distribuídos 2019-2020, segundo semestre


## Autores

**Grupo A20**

| Número | Nome              | Utilizador                              | Correio eletrónico                                                 |
| -------|-------------------|-----------------------------------------| -------------------------------------------------------------------|
| 89403  | Alexandre Duarte  | <https://github.com/alexduarte23>       | <mailto:alexandre.a.duarte@tecnico.ulisboa.pt>                     |
| 89426  | Daniel Lopes      | <https://github.com/DFL13>              | <mailto:daniel.f.lopes@tecnico.ulisboa.pt>                         |
| 89504  | Mariana Oliveira  | <https://github.com/tpoliveira-mariana> | <mailto:mariana.de.oliveira@tecnico.ulisboa.pt@tecnico.ulisboa.pt> |

![Alexandre Duarte](images/89403.jpg) ![Daniel Lopes](images/89426.png) ![Mariana Oliveira](images/89504.jpg)


## Melhorias da primeira parte

- [Adição de testes ao comando trace](https://github.com/tecnico-distsys/A20-Sauron/commit/bca534e2c7fd1d997ff14e00fad5211b07dcbcf0)
- [Adição de testes ao comando cam_join](https://github.com/tecnico-distsys/A20-Sauron/commit/ff15d99e0141b82c9fd22776e5f24157cc2fc2c7)
- [Formatação de output removida do SiloFrontend](https://github.com/tecnico-distsys/A20-Sauron/commit/5eefeb7a9514c07af7c8fd3438820cded4ec55c9)


## Modelo de faltas
Considerem-se os pressupostos do enunciado.
Seja _f_ o número de faltas a tolerar, então são necessárias, no mínimo, _f + 1_ réplicas. 

##### Faltas toleradas:
* Falta do cliente;
* Omissão de mensagens por parte dos canais de comunicação;
* _Crash_ que não leve a perda de informação (ctrl + z, por exemplo); 
* Partições da rede.

##### Faltas não toleradas:
* _Crash_ de réplicas que leve à perda da sua informação;
* Falta do Zookeeper.

## Solução

![Fault Tolerance Diagram](FaultTolerance.png)

_(Breve explicação da solução, suportada pela figura anterior)_
Aquando da sua criação, o SiloFrontend conecta-se a uma réplica (escolhida aleatoriamente,
ou pelo cliente). Depois de estabelecida a conecção, a réplica 1 _crasha_. Assim, quando 
tenta enviar um CamJoinReques à réplica 1, é lançada a exceção _UNAVAILABLE_. O cliente 


## Protocolo de replicação

_(Explicação do protocolo)_

_(descrição das trocas de mensagens)_


## Opções de implementação

_(Descrição de opções de implementação, incluindo otimizações e melhorias introduzidas)_

Considerando que o gossip não pretende garantir a coerência do ponto de vista do cliente,
decidimos que implementar uma _cache_ no mesmo seria uma solução eficaz. Desta forma, o 
cliente, que passou também a guardar o _timestamp_ da última versão do servidor vista
(_prevTS_), consegue escolher entre descartar ou não cada resposta do servidor aos seus
pedidos de leitura. Basta verificar se _prevTS_ é superior ao _timestamp_ da resposta
recebida.

#### QUEREMOS POR AQUI A CENA DOS UUID?


## Notas finais

Relativamente à recuperação de faltas por _crash_ de réplicas, pensámos em possíveis 
soluções, que decidimos não implementar. Considere-se que há mais do que uma réplica 
em funcionamento, até ao momento em que a réplica A _crasha_.

**Solução 1:**

Quando A volta a ficar disponível, eventualmente, recebe gossip de outras réplicas. 
Quando tal acontece, comparando o seu _replicaTS_ com o _updateTS_ do gossip recebido,
apercebe-se que perdeu informação (a sua entrada do _replicaTS_ vai ter um valor inferior 
ao presente na entrada respetiva do _replicaTS_).
Apercebendo-se de que perdeu informação, A pede a uma das outras réplicas que lhe envie os
seus mapas (com o registo das cameras e das observações). Supondo que B recebe este 
pedido, B envia a A uma mensagem gRPC com **todos os mapas e a sua versão atual**. Desta
forma A fica na mesma versão que B.

**Problema:**

As mensagens do gRPC têm limite de tamanho, logo, se a informação de uma réplica ocupar 
memória superior a este valor (o que é bastante provável) o envio falha. Assim, pode dar-se
o caso de pedidos de _cam_join_ que as outras réplicas já tenham retirado dos seus _logs_
não serjam partilhados com A. Desta forma, todos os _reports_ feitos por essa camera que
cheguem a A vão acumular-se no _log_. Este acumular de pedidos acabará provocar um novo 
_crash_ de A por falta de memória. É um ciclo vicioso.

**Solução 1.1:**

Em vez de enviar todos os mapas numa só mensagem de gRPC, **envia apenas fragmentos** dos 
mesmos em cada mensagem, até que tudo tenha sido enviado.

**Problema:**

Entre mensagens, a réplica que está a enviar os seus mapas pode receber um pedido de
update de um cliente, que pode levar ao update da sua versão. Neste caso, qual a versão
a atribuir à réplica A? Podíamos sempre impedir updates durante o processo de recuperação
de uma réplica (tanto na que envia os mapas como na que os recebe). No entanto, considerando
que os mapas podem ser gigantes e que as mensagens se podem perder, tal processo poderia
demorar muito tempo. Por esta razão, optámos por não implementar este mecanismo.