package ua.edu.nupp.library.dao;

import ua.edu.nupp.library.model.Loan;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class LoanDao {
    private Loan map(ResultSet r) throws SQLException { Date returned=r.getDate("returned_at"); return new Loan(r.getLong("id"),r.getLong("booking_id"),r.getDate("issued_at").toLocalDate(),r.getDate("due_date").toLocalDate(),returned==null?null:returned.toLocalDate()); }
    public List<Loan> findAll() throws SQLException { return Jdbc.query("SELECT id,booking_id,issued_at,due_date,returned_at FROM loan ORDER BY issued_at DESC",p->{},this::map); }
    public Loan findById(long id) throws SQLException { return Jdbc.one("SELECT id,booking_id,issued_at,due_date,returned_at FROM loan WHERE id=?",p->p.setLong(1,id),this::map); }
    public boolean updateDueDate(long id,LocalDate dueDate) throws SQLException { return Jdbc.update("UPDATE loan SET due_date=? WHERE id=? AND returned_at IS NULL",p->{p.setDate(1,Date.valueOf(dueDate));p.setLong(2,id);})>0; }
    public boolean delete(long id) throws SQLException { return Jdbc.update("DELETE FROM loan WHERE id=? AND returned_at IS NOT NULL",p->p.setLong(1,id))>0; }
    public boolean returnLoan(long id,LocalDate returnedAt) throws SQLException {
        try(Connection c=ua.edu.nupp.library.config.Database.open()) {
            c.setAutoCommit(false);
            try(PreparedStatement query=c.prepareStatement("SELECT b.publication_id,l.returned_at FROM loan l JOIN booking b ON b.id=l.booking_id WHERE l.id=? FOR UPDATE")) {
                query.setLong(1,id); long publicationId;
                try(ResultSet r=query.executeQuery()) { if(!r.next()) { c.rollback(); return false; } if(r.getDate("returned_at")!=null) { c.rollback(); throw new IllegalStateException("Цю видачу вже повернуто."); } publicationId=r.getLong("publication_id"); }
                try(PreparedStatement update=c.prepareStatement("UPDATE loan SET returned_at=? WHERE id=? AND returned_at IS NULL")) { update.setDate(1,Date.valueOf(returnedAt));update.setLong(2,id);if(update.executeUpdate()==0){c.rollback();return false;} }
                try(PreparedStatement stock=c.prepareStatement("UPDATE publication SET available_copies=LEAST(total_copies,available_copies+1) WHERE id=?")) { stock.setLong(1,publicationId);stock.executeUpdate(); }
                c.commit();return true;
            } catch(Exception e) { c.rollback();if(e instanceof SQLException sql)throw sql;if(e instanceof RuntimeException runtime)throw runtime;throw new SQLException(e); }
        }
    }
}
