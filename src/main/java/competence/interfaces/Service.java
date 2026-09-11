package competence.interfaces;

import java.util.List;

public interface Service<T> {

    void add(T entity);

    List<T> getAll();

    void update(T entity);

    void delete(T entity);
}
