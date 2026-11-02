package ua.edu.nupp.library.dao;

import ua.edu.nupp.library.config.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

final class Jdbc {
    @FunctionalInterface interface Binder { void bind(PreparedStatement statement) throws SQLException; }
    @FunctionalInterface interface Mapper<T> { T map(ResultSet result) throws SQLException; }
    private Jdbc() { }

    static int update(String sql, Binder binder) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(p); int changed = p.executeUpdate();
            if (changed > 0 && sql.stripLeading().toUpperCase().startsWith("INSERT")) {
                try (ResultSet keys = p.getGeneratedKeys()) { if (keys.next()) return keys.getInt(1); }
            }
            return changed;
        }
    }

    static <T> List<T> query(String sql, Binder binder, Mapper<T> mapper) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement p = c.prepareStatement(sql)) {
            binder.bind(p); try (ResultSet r = p.executeQuery()) { List<T> rows = new ArrayList<>(); while (r.next()) rows.add(mapper.map(r)); return rows; }
        }
    }

    static <T> T one(String sql, Binder binder, Mapper<T> mapper) throws SQLException {
        List<T> rows = query(sql, binder, mapper); return rows.isEmpty() ? null : rows.get(0);
    }
}
