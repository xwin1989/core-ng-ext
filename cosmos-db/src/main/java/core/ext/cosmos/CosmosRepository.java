package core.ext.cosmos;

import com.azure.cosmos.models.SqlQuerySpec;

import java.util.List;
import java.util.Optional;

/**
 * @author Neal
 */
public interface CosmosRepository<T> {
    Optional<T> get(String id);

    void upsert(T entity);

    void insert(T entity);

    Optional<T> findOne(SqlQuerySpec query);

    <V> Optional<V> findOne(SqlQuerySpec query, Class<V> clazz);

    List<T> find(SqlQuerySpec query);

    <V> List<V> find(SqlQuerySpec query, Class<V> clazz);

    void delete(String id);
}
