# Sistema de Chamados

API REST para cadastro e acompanhamento de chamados, clientes, categorias e técnicos, desenvolvida com Java e Spring Boot.

## Requisitos

- JDK 21
- Git (opcional, para clonar o repositório)

O projeto inclui o Maven Wrapper, então não é necessário instalar o Maven.

## Como executar

Clone o repositório e entre na pasta do projeto. Em seguida, inicie a aplicação:

**Windows (PowerShell):**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux ou macOS:**

```bash
./mvnw spring-boot:run
```

Quando o Spring Boot indicar que iniciou, a API estará disponível em `http://localhost:8080`.

Para encerrar, use `Ctrl+C` no terminal em que a aplicação está rodando.

## Banco de dados H2

O projeto usa H2 em memória. Os dados são temporários e são apagados quando a aplicação é encerrada.

- Console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:chamadosdb`
- Usuário: `sa`
- Senha: deixe em branco

## Como executar os testes

Os testes também podem ser executados pelo Maven Wrapper:

**Windows (PowerShell):**

```powershell
.\mvnw.cmd test
```

**Linux ou macOS:**

```bash
./mvnw test
```

O Maven compila o projeto e apresenta o resultado dos testes no terminal. Os testes de integração usam o banco H2.

## Endpoints

Os endpoints recebem e retornam JSON. Para testar manualmente, use uma ferramenta como Postman ou Insomnia, ou envie requisições HTTP para `http://localhost:8080`.

| Recurso | Rotas |
| --- | --- |
| Clientes | `GET/POST /clientes`, `GET/PUT/DELETE /clientes/{id}` |
| Categorias | `GET/POST /categorias`, `GET/PUT/DELETE /categorias/{id}` |
| Técnicos | `GET/POST /tecnicos`, `GET/PUT/DELETE /tecnicos/{id}` |
| Chamados | `GET/POST /chamados`, `GET/PUT/DELETE /chamados/{id}` |
| Ações de chamados | `PATCH /chamados/{id}/atribuir`, `PATCH /chamados/{id}/status` |
| Comentários | `POST /chamados/{id}/comentarios` |
| Chamados de um cliente | `GET /clientes/{id}/chamados` |

Para criar um registro, envie um `POST` com `Content-Type: application/json`. Exemplo de corpo para criar um cliente:

```json
{
  "nome": "Ana Silva",
  "email": "ana@example.com",
  "telefone": "47999990000"
}
```

### Criar um chamado

Crie antes o cliente e a categoria, e use os IDs retornados nas respostas. O técnico é opcional ao abrir o chamado.

```json
{
  "titulo": "Computador não inicia",
  "descricao": "O equipamento não liga após a atualização.",
  "prioridade": "ALTA",
  "clienteId": 1,
  "categoriaId": 1,
  "tecnicoId": null
}
```

O chamado é criado com status `ABERTO` e a data de abertura é preenchida automaticamente.

Para atribuir um técnico, envie `PATCH /chamados/{id}/atribuir`:

```json
{
  "tecnicoId": 1
}
```

Para alterar o status, envie `PATCH /chamados/{id}/status`:

```json
{
  "status": "EM_ATENDIMENTO"
}
```

Os status aceitos são `ABERTO`, `EM_ATENDIMENTO`, `AGUARDANDO_CLIENTE` e `FECHADO`. Ao fechar, a data de fechamento é preenchida. Chamados fechados não podem ser alterados nem receber atribuição de técnico.

### Comentários e histórico do chamado

Envie um `POST /chamados/{id}/comentarios` para registrar uma interação. O texto e o autor são obrigatórios, e a data é preenchida automaticamente:

```json
{
  "texto": "Estou analisando o problema.",
  "autor": "Técnico"
}
```

Os comentários são listados em ordem de data no detalhe retornado por `GET /chamados/{id}`, no campo `comentarios`.

## Filtros, paginação e ordenação

`GET /chamados` permite combinar os filtros opcionais `status`, `prioridade` e `clienteId`. Também aceita os parâmetros de paginação `page` (começa em zero), `size` e `sort`.

```text
GET /chamados?status=ABERTO&prioridade=ALTA&clienteId=1
GET /chamados?page=0&size=20&sort=titulo,asc
GET /chamados?status=EM_ATENDIMENTO&page=1&size=5&sort=dataAbertura,desc
GET /clientes/1/chamados?page=0&size=10&sort=dataAbertura,desc
```

A paginação padrão retorna 10 itens por página, ordenados por data de abertura decrescente. Prioridades aceitas: `BAIXA`, `MEDIA`, `ALTA` e `CRITICA`.

## Testes automatizados

`.\mvnw.cmd test` executa todos os testes, incluindo testes de persistência com `@DataJpaTest`, testes de controller com `@WebMvcTest` e testes de integração com MockMvc e H2. Eles cobrem criação, validação, duplicidade, respostas 404, filtros e comentários.
