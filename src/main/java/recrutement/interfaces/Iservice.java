package recrutement.interfaces;

import java.util.List;

public interface Iservice<T> {

    void add (T t);
    List<T> getAll() ;
    void update(T t);
    void delete(T t);

}
