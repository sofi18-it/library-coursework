package ua.edu.nupp.library.dao;

import ua.edu.nupp.library.model.Booking;
import ua.edu.nupp.library.model.BookingState;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class BookingDao {
    private static final String COLUMNS = "id,publication_id,reader_id,created_at,state,f1_wait_score,f2_demand,f3_shortage,integral_index";
    private Booking map(ResultSet r) throws SQLException { return new Booking(r.getLong("id"),r.getLong("publication_id"),r.getLong("reader_id"),r.getTimestamp("created_at").toLocalDateTime(),BookingState.valueOf(r.getString("state")),r.getDouble("f1_wait_score"),r.getDouble("f2_demand"),r.getDouble("f3_shortage"),r.getDouble("integral_index")); }
    public List<Booking> findAll() throws SQLException { return Jdbc.query("SELECT "+COLUMNS+" FROM booking ORDER BY created_at DESC",p->{},this::map); }
    public Booking findById(long id) throws SQLException { return Jdbc.one("SELECT "+COLUMNS+" FROM booking WHERE id=?",p->p.setLong(1,id),this::map); }
    public long countActiveForPublication(long publicationId) throws SQLException { Long n=Jdbc.one("SELECT COUNT(*) AS n FROM booking WHERE publication_id=? AND state<>'COMPLETED'",p->p.setLong(1,publicationId),r->r.getLong("n")); return n==null?0:n; }
    public long create(Booking b) throws SQLException { return Jdbc.update("INSERT INTO booking(publication_id,reader_id,created_at,state,f1_wait_score,f2_demand,f3_shortage,integral_index) VALUES(?,?,?,'CREATED',?,?,?,?)",p->{p.setLong(1,b.publicationId());p.setLong(2,b.readerId());p.setTimestamp(3,Timestamp.valueOf(b.createdAt()));p.setDouble(4,b.f1WaitScore());p.setDouble(5,b.f2Demand());p.setDouble(6,b.f3Shortage());p.setDouble(7,b.integralIndex());}); }
    public boolean updateDetails(long id,long publicationId,long readerId) throws SQLException { return Jdbc.update("UPDATE booking SET publication_id=?,reader_id=? WHERE id=? AND state='CREATED'",p->{p.setLong(1,publicationId);p.setLong(2,readerId);p.setLong(3,id);})>0; }
    public boolean delete(long id) throws SQLException { return Jdbc.update("DELETE FROM booking WHERE id=? AND state='CREATED'",p->p.setLong(1,id))>0; }
    public boolean updateStateAndScores(long id,BookingState state,double f1,double f2,double f3,double integral) throws SQLException { return Jdbc.update("UPDATE booking SET state=?,f1_wait_score=?,f2_demand=?,f3_shortage=?,integral_index=? WHERE id=?",p->{p.setString(1,state.name());p.setDouble(2,f1);p.setDouble(3,f2);p.setDouble(4,f3);p.setDouble(5,integral);p.setLong(6,id);})>0; }

    /** Completes the final state, creates the loan and decrements stock in one transaction. */
    public boolean issue(long id,double f1,double f2,double f3,double integral,LocalDate dueDate) throws SQLException {
        try(Connection c=ua.edu.nupp.library.config.Database.open()) {
            c.setAutoCommit(false);
            try(PreparedStatement check=c.prepareStatement("SELECT state,publication_id FROM booking WHERE id=? FOR UPDATE")) {
                check.setLong(1,id); long publicationId;
                try(ResultSet r=check.executeQuery()) { if(!r.next()||!"READY_FOR_ISSUE".equals(r.getString("state"))) { c.rollback(); return false; } publicationId=r.getLong("publication_id"); }
                try(PreparedStatement stock=c.prepareStatement("UPDATE publication SET available_copies=available_copies-1 WHERE id=? AND available_copies>0")) { stock.setLong(1,publicationId); if(stock.executeUpdate()==0) { c.rollback(); throw new IllegalStateException("Немає доступного примірника для видачі."); } }
                try(PreparedStatement loan=c.prepareStatement("INSERT INTO loan(booking_id,issued_at,due_date) VALUES(?,?,?)")) { loan.setLong(1,id);loan.setDate(2,Date.valueOf(LocalDate.now()));loan.setDate(3,Date.valueOf(dueDate));loan.executeUpdate(); }
                try(PreparedStatement update=c.prepareStatement("UPDATE booking SET state='COMPLETED',f1_wait_score=?,f2_demand=?,f3_shortage=?,integral_index=? WHERE id=?")) { update.setDouble(1,f1);update.setDouble(2,f2);update.setDouble(3,f3);update.setDouble(4,integral);update.setLong(5,id);update.executeUpdate(); }
                c.commit(); return true;
            } catch(Exception e) { c.rollback(); if(e instanceof SQLException sql) throw sql; if(e instanceof RuntimeException runtime) throw runtime; throw new SQLException(e); }
        }
    }
}
