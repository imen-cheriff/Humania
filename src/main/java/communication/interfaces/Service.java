package communication.interfaces;

import java.util.List;

public interface Service<T> {
    void add(T t);
    List<T> getAll();
    void update(T t);
    void delete(T t);
}