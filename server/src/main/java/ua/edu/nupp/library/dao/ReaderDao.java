package ua.edu.nupp.library.dao;

import ua.edu.nupp.library.model.Reader;
import java.sql.*;
import java.util.List;

public class ReaderDao {
    private Reader map(ResultSet r) throws SQLException { return new Reader(r.getLong("id"),r.getString("card_number"),r.getString("full_name"),r.getString("email"),r.getString("phone")); }
    public List<Reader> findAll() throws SQLException { return Jdbc.query("SELECT id,card_number,full_name,email,phone FROM reader ORDER BY full_name",p->{},this::map); }
    public Reader findById(long id) throws SQLException { return Jdbc.one("SELECT id,card_number,full_name,email,phone FROM reader WHERE id=?",p->p.setLong(1,id),this::map); }
    public Reader findByCard(String card) throws SQLException { return Jdbc.one("SELECT id,card_number,full_name,email,phone FROM reader WHERE card_number=?",p->p.setString(1,card),this::map); }
    public long create(Reader x) throws SQLException { return Jdbc.update("INSERT INTO reader(card_number,full_name,email,phone) VALUES(?,?,?,?)",p->{p.setString(1,x.cardNumber());p.setString(2,x.fullName());p.setString(3,x.email());p.setString(4,x.phone());}); }
    public boolean update(long id, Reader x) throws SQLException { return Jdbc.update("UPDATE reader SET card_number=?,full_name=?,email=?,phone=? WHERE id=?",p->{p.setString(1,x.cardNumber());p.setString(2,x.fullName());p.setString(3,x.email());p.setString(4,x.phone());p.setLong(5,id);})>0; }
    public boolean delete(long id) throws SQLException { return Jdbc.update("DELETE FROM reader WHERE id=?",p->p.setLong(1,id))>0; }
}
