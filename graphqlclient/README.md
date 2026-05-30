# graphqlclient

A Spring Boot 4 GraphQL **client** application that connects to the
[`springboot-graphql-app`](../README.md) GraphQL backend.

It is itself a Spring Boot 4 GraphQL application: it consumes the backend's
`/graphql` endpoint using Spring for GraphQL's `HttpSyncGraphQlClient`, and
re-exposes the same API both as its own GraphQL gateway endpoint and as a set of
convenience REST endpoints.

## Tech Stack

- **Spring Boot 4.0.6** (Spring Framework 7)
- **Spring for GraphQL** — both server (`@Controller`) and client (`HttpSyncGraphQlClient`)
- **Java 17**
- **Gradle**

## How it works

```
Postman / browser ──▶ graphqlclient (:8081) ──▶ springboot-graphql-app (:8080)
                       /graphql  (GraphQL gateway)        /graphql (backend)
                       /api/books (REST proxy)
```

- `BackendGraphQlClientConfig` builds an `HttpSyncGraphQlClient` from a
  `RestClient` pointed at `backend.graphql.url`.
- `BookGraphQlClient` issues the `allBooks`, `bookById`, `booksByAuthor` queries
  and the `addBook` mutation against the backend.
- `BookGatewayController` exposes the same GraphQL schema, delegating each
  resolver to the backend.
- `BookRestController` exposes simple REST endpoints that proxy to the backend.

## Configuration

| Property              | Default                          | Description                       |
|-----------------------|----------------------------------|-----------------------------------|
| `server.port`         | `8081`                           | Port this client listens on       |
| `backend.graphql.url` | `http://localhost:8080/graphql`  | Backend GraphQL endpoint to call  |

## Running

Start the backend first (from the repo root):

```bash
./gradlew bootRun
```

Then start this client (from the `graphqlclient` directory):

```bash
cd graphqlclient
./gradlew bootRun
```

The client starts on **http://localhost:8081**.

## Endpoints

| Endpoint                                   | Method | Description                          |
|--------------------------------------------|--------|--------------------------------------|
| `POST /graphql`                            | POST   | GraphQL gateway (delegates to backend)|
| `GET  /graphiql`                           | GET    | GraphiQL browser UI                  |
| `GET  /api/books`                          | GET    | All books (REST proxy)               |
| `GET  /api/books/{id}`                     | GET    | Book by id                           |
| `GET  /api/books/by-author?authorName=...` | GET    | Books by author                      |
| `POST /api/books?title=..&author=..&pages=`| POST   | Add a book                           |

### Example: GraphQL query against the client

```json
{
  "query": "{ allBooks { id title author { id name } pages } }"
}
```

### Example: REST against the client

```bash
curl http://localhost:8081/api/books
curl http://localhost:8081/api/books/1
curl "http://localhost:8081/api/books/by-author?authorName=George%20Orwell"
curl -X POST "http://localhost:8081/api/books?title=Brave%20New%20World&author=Aldous%20Huxley&pages=311"
```
