package com.example.graphqlclient.controller;

import java.util.List;

import com.example.graphqlclient.client.BookGraphQlClient;
import com.example.graphqlclient.model.Book;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL gateway controller. This client application exposes its own GraphQL
 * endpoint whose resolvers delegate to the backend springboot-graphql-app.
 */
@Controller
public class BookGatewayController {

    private final BookGraphQlClient bookClient;

    public BookGatewayController(BookGraphQlClient bookClient) {
        this.bookClient = bookClient;
    }

    @QueryMapping
    public List<Book> allBooks() {
        return bookClient.allBooks();
    }

    @QueryMapping
    public Book bookById(@Argument String id) {
        return bookClient.bookById(id);
    }

    @QueryMapping
    public List<Book> booksByAuthor(@Argument String authorName) {
        return bookClient.booksByAuthor(authorName);
    }

    @MutationMapping
    public Book addBook(@Argument String title, @Argument String author, @Argument int pages) {
        return bookClient.addBook(title, author, pages);
    }
}
