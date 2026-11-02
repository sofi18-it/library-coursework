package ua.edu.nupp.library.model;

public record Publication(Long id, String isbn, String title, String author, String publisher,
                          int publicationYear, String category, int totalCopies, int availableCopies) { }
