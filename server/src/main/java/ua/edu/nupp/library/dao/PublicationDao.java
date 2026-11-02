package ua.edu.nupp.library.dao;

import ua.edu.nupp.library.model.Publication;
import java.sql.*;
import java.util.List;

public class PublicationDao {
    private static final String COLUMNS = "id,isbn,title,author,publisher,publication_year,category,total_copies,available_copies";
    private Publication map(ResultSet r) throws SQLException { return new Publication(r.getLong("id"),r.getString("isbn"),r.getString("title"),r.getString("author"),r.getString("publisher"),r.getInt("publication_year"),r.getString("category"),r.getInt("total_copies"),r.getInt("available_copies")); }
    public List<Publication> findAll() throws SQLException { return Jdbc.query("SELECT "+COLUMNS+" FROM publication ORDER BY title",p->{},this::map); }
    public Publication findById(long id) throws SQLException { return Jdbc.one("SELECT "+COLUMNS+" FROM publication WHERE id=?",p->p.setLong(1,id),this::map); }
    public Publication findByIsbn(String isbn) throws SQLException { return Jdbc.one("SELECT "+COLUMNS+" FROM publication WHERE isbn=?",p->p.setString(1,isbn),this::map); }
    public long create(Publication x) throws SQLException { return Jdbc.update("INSERT INTO publication(isbn,title,author,publisher,publication_year,category,total_copies,available_copies) VALUES(?,?,?,?,?,?,?,?)",p->{p.setString(1,x.isbn());p.setString(2,x.title());p.setString(3,x.author());p.setString(4,x.publisher());p.setInt(5,x.publicationYear());p.setString(6,x.category());p.setInt(7,x.totalCopies());p.setInt(8,x.availableCopies());}); }
    public boolean update(long id, Publication x) throws SQLException { return Jdbc.update("UPDATE publication SET isbn=?,title=?,author=?,publisher=?,publication_year=?,category=?,total_copies=?,available_copies=? WHERE id=?",p->{p.setString(1,x.isbn());p.setString(2,x.title());p.setString(3,x.author());p.setString(4,x.publisher());p.setInt(5,x.publicationYear());p.setString(6,x.category());p.setInt(7,x.totalCopies());p.setInt(8,x.availableCopies());p.setLong(9,id);})>0; }
    public boolean delete(long id) throws SQLException { return Jdbc.update("DELETE FROM publication WHERE id=?",p->p.setLong(1,id))>0; }
}
