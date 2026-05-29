# Spring Boot 4 GraphQL Application

A Spring Boot 4.0.6 application with a GraphQL endpoint, designed to be invoked via Postman.

## Tech Stack

- **Spring Boot 4.0.6** (Spring Framework 7)
- **Spring for GraphQL** (with `@Controller` + `@QueryMapping` / `@MutationMapping`)
- **Java 17**
- **Gradle**

## Project Structure

```
src/main/
├── java/com/example/graphql/
│   ├── SpringbootGraphqlAppApplication.java   # Main entry point
│   ├── controller/
│   │   └── BookController.java                # GraphQL controller
│   └── model/
│       ├── Book.java                          # Book model
│       └── Author.java                        # Author model
└── resources/
    ├── application.properties                 # App configuration
    └── graphql/
        └── schema.graphqls                    # GraphQL schema definition
```

## Running the Application

```bash
./gradlew bootRun
```

The application starts on **http://localhost:8080**.

## GraphQL Endpoint

| Endpoint              | Method | Description                    |
|-----------------------|--------|--------------------------------|
| `POST /graphql`       | POST   | GraphQL query/mutation endpoint|
| `GET  /graphiql`      | GET    | GraphiQL browser UI            |

## Testing with Postman

Send **POST** requests to `http://localhost:8080/graphql` with:
- **Header:** `Content-Type: application/json`

### Query: Get All Books

```json
{
  "query": "{ allBooks { id title author { id name } pages } }"
}
```

### Query: Get Book by ID

```json
{
  "query": "{ bookById(id: \"1\") { id title author { name } pages } }"
}
```

### Query: Get Books by Author

```json
{
  "query": "{ booksByAuthor(authorName: \"George Orwell\") { id title pages } }"
}
```

### Mutation: Add a New Book

```json
{
  "query": "mutation { addBook(title: \"Brave New World\", author: \"Aldous Huxley\", pages: 311) { id title author { name } pages } }"
}
```

## Sample Data

The application comes pre-loaded with these books:

| ID | Title                | Author              | Pages |
|----|----------------------|----------------------|-------|
| 1  | The Lord of the Rings| J.R.R. Tolkien       | 1178  |
| 2  | The Hobbit           | J.R.R. Tolkien       | 310   |
| 3  | 1984                 | George Orwell        | 328   |
| 4  | Animal Farm          | George Orwell        | 112   |
| 5  | A Game of Thrones    | George R.R. Martin   | 694   |
