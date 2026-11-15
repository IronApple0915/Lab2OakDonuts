package entity;

import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD contract shared by all data access objects.
 * @param <T> entity type
 */
public interface DAO<T> {
    // Optional is a container used to hold possibly-absent, non-null objects.
    Optional<T> get(int id);
    List<T> getAll();
    void insert(T t);
    void update(T t);
    void delete(T t);
    List<String> getColumnNames();
}
