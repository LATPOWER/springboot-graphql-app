package com.example.graphql.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.example.graphql.model.Author;
import com.example.graphql.model.Book;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class BookController {

    private final List<Book> books = new CopyOnWriteArrayList<>();

    public BookController() {
        Author tolkien = new Author("1", "J.R.R. Tolkien");
        Author orwell = new Author("2", "George Orwell");
        Author martin = new Author("3", "George R.R. Martin");

        books.add(new Book("1", "The Lord of the Rings", tolkien, 1178));
        books.add(new Book("2", "The Hobbit", tolkien, 310));
        books.add(new Book("3", "1984", orwell, 328));
        books.add(new Book("4", "Animal Farm", orwell, 112));
        books.add(new Book("5", "A Game of Thrones", martin, 694));
    }

    @QueryMapping
    public List<Book> allBooks() {
        return books;
    }

    @QueryMapping
    public Book bookById(@Argument String id) {
        return books.stream()
                .filter(book -> book.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @QueryMapping
    public List<Book> booksByAuthor(@Argument String authorName) {
        return books.stream()
                .filter(book -> book.getAuthor().getName().equalsIgnoreCase(authorName))
                .toList();
    }

    @MutationMapping
    public Book addBook(@Argument String title, @Argument String author, @Argument int pages) {
        String bookId = UUID.randomUUID().toString();
        Author newAuthor = new Author(UUID.randomUUID().toString(), author);
        Book newBook = new Book(bookId, title, newAuthor, pages);
        books.add(newBook);
        return newBook;
    }
}
