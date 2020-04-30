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
Seja _f_ o número de faltas a tolerar, então são necessárias, no mínimo, _f + 1_ réplicas 
e a réplica funcional não pode guardar dados cuja memória ocupada seja superior ao tamanho máximo de uma mensagem de gRPC.

##### Faltas toleradas:
* Falta do cliente;
* Omissão de mensagens por parte dos canais de comunicação;
* Partições da rede;
* Falta silenciosa de réplicas, apenas se o espaço ocupado pelas estruturas de dados da réplica
que as tenta recuperar for **inferior** ao limite de tamanho de uma mensagem gRPC.

##### Faltas não toleradas:
* Falta silenciosa de réplicas, se o espaço ocupado pelas estruturas de dados da réplica
que as tenta recuperar for **superior** ao limite de tamanho de uma mensagem gRPC.
* Falta do Zookeeper

## Solução

_(Figura da solução de tolerância a faltas)_

_(Breve explicação da solução, suportada pela figura anterior)_


## Protocolo de replicação

_(Explicação do protocolo)_

_(descrição das trocas de mensagens)_


## Opções de implementação

_(Descrição de opções de implementação, incluindo otimizações e melhorias introduzidas)_



## Notas finais

_(Algo mais a dizer?)_