/*
The library management system should allow librarians to manage books, members, and borrowing activities.
The system should support adding, updating, and removing books from the library catalog.
Each book should have details such as title, author, ISBN, publication year, and availability status.
The system should allow members to borrow and return books.
Each member should have details such as name, member ID, contact information, and borrowing history.
The system should enforce borrowing rules, such as a maximum number of books that can be borrowed at a time and loan duration.
The system should handle concurrent access to the library catalog and member records.
The system should be extensible to accommodate future enhancements and new features.


Book
Member
Manager

 */






import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class Book {
    private final String isbn;
    private final String title;
    private final String author;
    private final int publicationYear;
    private boolean available;

    public Book(String isbn, String title, String author, int publicationYear) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.publicationYear = publicationYear;
        this.available = true;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}

class Member {
    private final String memberId;
    private final String name;
    private final String contactInfo;
    private final List<Book> borrowedBooks;

    public Member(String memberId, String name, String contactInfo) {
        this.memberId = memberId;
        this.name = name;
        this.contactInfo = contactInfo;
        this.borrowedBooks = new ArrayList<>();
    }

    public void borrowBook(Book book) {
        borrowedBooks.add(book);
    }

    public void returnBook(Book book) {
        borrowedBooks.remove(book);
    }

    public String getMemberId() {
        return memberId;
    }

    public String getName() {
        return name;
    }

    public List<Book> getBorrowedBooks() {
        return borrowedBooks;
    }
}

class LibraryManager {
    private static LibraryManager instance;
    private final Map<String, Book> catalog;
    private final Map<String, Member> members;
    private final int MAX_BOOKS_PER_MEMBER = 5;
    private final int LOAN_DURATION_DAYS = 14;

    private LibraryManager() {
        catalog = new ConcurrentHashMap<>();
        members = new ConcurrentHashMap<>();
    }

    public static synchronized LibraryManager getInstance() {
        if (instance == null) {
            instance = new LibraryManager();
        }
        return instance;
    }

    public void addBook(Book book) {
        catalog.put(book.getIsbn(), book);
    }

    public void removeBook(String isbn) {
        catalog.remove(isbn);
    }

    public Book getBook(String isbn) {
        return catalog.get(isbn);
    }

    public void registerMember(Member member) {
        members.put(member.getMemberId(), member);
    }

    public void unregisterMember(String memberId) {
        members.remove(memberId);
    }

    public Member getMember(String memberId) {
        return members.get(memberId);
    }

    public synchronized void borrowBook(String memberId, String isbn) {
        Member member = getMember(memberId);
        Book book = getBook(isbn);

        if (member != null && book != null && book.isAvailable()) {
            if (member.getBorrowedBooks().size() < MAX_BOOKS_PER_MEMBER) {
                member.borrowBook(book);
                book.setAvailable(false);
                System.out.println("Book borrowed: " + book.getTitle() + " by " + member.getName());
            } else {
                System.out.println("Member " + member.getName() + " has reached the maximum number of borrowed books.");
            }
        } else {
            System.out.println("Book or member not found, or book is not available.");
        }
    }

    public synchronized void returnBook(String memberId, String isbn) {
        Member member = getMember(memberId);
        Book book = getBook(isbn);

        if (member != null && book != null) {
            member.returnBook(book);
            book.setAvailable(true);
            System.out.println("Book returned: " + book.getTitle() + " by " + member.getName());
        } else {
            System.out.println("Book or member not found.");
        }
    }

    public List<Book> searchBooks(String keyword) {
        List<Book> matchingBooks = new ArrayList<>();
        for (Book book : catalog.values()) {
            if (book.getTitle().contains(keyword) || book.getAuthor().contains(keyword)) {
                matchingBooks.add(book);
            }
        }
        return matchingBooks;
    }
}

public class Main {
    public static void main(String[] args) {

        LibraryManager libraryManager = LibraryManager.getInstance();

        // Add books to the catalog
        libraryManager.addBook(new Book("ISBN1", "Book 1", "Author 1", 2020));
        libraryManager.addBook(new Book("ISBN2", "Book 2", "Author 2", 2019));
        libraryManager.addBook(new Book("ISBN3", "Book 3", "Author 3", 2021));

        // Register members
        libraryManager.registerMember(new Member("M1", "John Doe", "john@example.com"));
        libraryManager.registerMember(new Member("M2", "Jane Smith", "jane@example.com"));

        // Borrow books
        libraryManager.borrowBook("M1", "ISBN1");
        libraryManager.borrowBook("M2", "ISBN2");

        // Return books
        libraryManager.returnBook("M1", "ISBN1");

        // Search books
        List<Book> searchResults = libraryManager.searchBooks("Book");
        System.out.println("Search Results:");
        for (Book book : searchResults) {
            System.out.println(book.getTitle() + " by " + book.getAuthor());
        }
    }
}