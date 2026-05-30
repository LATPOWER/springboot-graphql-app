package com.example.graphqlclient.controller;

import java.util.List;

import com.example.graphqlclient.client.BookGraphQlClient;
import com.example.graphqlclient.model.Book;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Convenience REST endpoints that proxy to the backend GraphQL API via the
 * {@link BookGraphQlClient}. Handy for quick testing from Postman or a browser.
 */
@RestController
@RequestMapping("/api/books")
public class BookRestController {

    private final BookGraphQlClient bookClient;

    public BookRestController(BookGraphQlClient bookClient) {
        this.bookClient = bookClient;
    }

    @GetMapping
    public List<Book> allBooks() {
        return bookClient.allBooks();
    }

    @GetMapping("/{id}")
    public Book bookById(@PathVariable String id) {
        return bookClient.bookById(id);
    }

    @GetMapping("/by-author")
    public List<Book> booksByAuthor(@RequestParam String authorName) {
        return bookClient.booksByAuthor(authorName);
    }

    @PostMapping
    public Book addBook(@RequestParam String title,
                        @RequestParam String author,
                        @RequestParam int pages) {
        return bookClient.addBook(title, author, pages);
    }
}
