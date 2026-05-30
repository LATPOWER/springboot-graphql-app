package com.example.graphqlclient.client;

import java.util.List;

import com.example.graphqlclient.model.Book;

import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.stereotype.Service;

/**
 * Thin service that talks to the backend springboot-graphql-app GraphQL API
 * using Spring for GraphQL's {@link HttpSyncGraphQlClient}.
 */
@Service
public class BookGraphQlClient {

    private static final String BOOK_FIELDS = "id title author { id name } pages";

    private final HttpSyncGraphQlClient client;

    public BookGraphQlClient(HttpSyncGraphQlClient backendGraphQlClient) {
        this.client = backendGraphQlClient;
    }

    public List<Book> allBooks() {
        String document = "query { allBooks { " + BOOK_FIELDS + " } }";
        return client.document(document)
                .retrieveSync("allBooks")
                .toEntityList(Book.class);
    }

    public Book bookById(String id) {
        String document = "query GetBook($id: ID!) { bookById(id: $id) { " + BOOK_FIELDS + " } }";
        return client.document(document)
                .variable("id", id)
                .retrieveSync("bookById")
                .toEntity(Book.class);
    }

    public List<Book> booksByAuthor(String authorName) {
        String document = "query BooksByAuthor($authorName: String!) { booksByAuthor(authorName: $authorName) { "
                + BOOK_FIELDS + " } }";
        return client.document(document)
                .variable("authorName", authorName)
                .retrieveSync("booksByAuthor")
                .toEntityList(Book.class);
    }

    public Book addBook(String title, String author, int pages) {
        String document = "mutation AddBook($title: String!, $author: String!, $pages: Int!) {"
                + " addBook(title: $title, author: $author, pages: $pages) { " + BOOK_FIELDS + " } }";
        return client.document(document)
                .variable("title", title)
                .variable("author", author)
                .variable("pages", pages)
                .retrieveSync("addBook")
                .toEntity(Book.class);
    }
}
