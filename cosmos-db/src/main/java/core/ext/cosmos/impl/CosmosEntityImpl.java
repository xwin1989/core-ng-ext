package core.ext.cosmos.impl;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.implementation.HttpConstants;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.FeedResponse;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import core.ext.cosmos.CosmosRepository;
import core.ext.cosmos.Entity;
import core.framework.internal.validate.Validator;
import core.framework.log.ActionLogContext;
import core.framework.log.Markers;
import core.framework.util.StopWatch;
import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Neal
 */
public class CosmosEntityImpl<T> implements CosmosRepository<T> {
    private final Logger logger = LoggerFactory.getLogger(CosmosEntityImpl.class);
    private final CosmosImpl cosmos;
    private final Class<T> entityClass;
    private final String entityName;
    private final Validator<T> validator;
    private CosmosContainer cosmosContainer;

    public CosmosEntityImpl(CosmosImpl cosmos, Class<T> entityClass) {
        this.cosmos = cosmos;
        this.entityClass = entityClass;
        this.validator = Validator.of(entityClass);
        this.entityName = entityClass.getAnnotation(Entity.class).name();
    }

    @Override
    public Optional<T> get(String id) {
        StopWatch watch = new StopWatch();
        if (Strings.isBlank(id)) throw new Error("id must not be null");
        double requestCharge = 0d; //todo maybe trace each charge, two precision
        int returnedDocs = 0;
        try {
            CosmosItemResponse<T> result = cosmosContainer().readItem(id, new PartitionKey(id), entityClass);
            if (result.getItem() != null) returnedDocs = 1;
            requestCharge = result.getRequestCharge();
            return Optional.ofNullable(result.getItem());
        } catch (CosmosException ex) {
            if (ex.getStatusCode() == HttpConstants.StatusCodes.NOTFOUND)//404 + 429
                return Optional.empty();
            throw ex;
        } finally {
            long elapsed = watch.elapsed();
            ActionLogContext.track("cosmos", elapsed, returnedDocs, 0);
            logger.debug("get, entity={}, id={}, returnedDocs={}, requestCharge={}, elapsed={}",
                entityName,
                id,
                returnedDocs,
                requestCharge,
                elapsed);
            checkSlowOperation(elapsed);
        }
    }

    @Override
    public void upsert(T entity) {
        var watch = new StopWatch();
        validator.validate(entity, false);
        double requestCharge = 0d;
        try {
            CosmosItemResponse<T> response = cosmosContainer().upsertItem(entity);
            requestCharge = response.getRequestCharge();
        } finally {
            long elapsed = watch.elapsed();
            ActionLogContext.track("cosmos", elapsed, 0, 1);
            logger.debug("upsert, entity={}, requestCharge={}, elapsed={}", entityName, requestCharge, elapsed);
            checkSlowOperation(elapsed);
        }
    }

    @Override
    public void insert(T entity) {
        var watch = new StopWatch();
        validator.validate(entity, false);
        double requestCharge = 0d;
        try {
            CosmosItemResponse<T> response = cosmosContainer().createItem(entity);
            requestCharge = response.getRequestCharge();
        } finally {
            long elapsed = watch.elapsed();
            ActionLogContext.track("cosmos", elapsed, 0, 1);
            logger.debug("insert, entity={}, requestCharge={}, elapsed={}", entityName, requestCharge, elapsed);
            checkSlowOperation(elapsed);
        }
    }

    @Override
    public Optional<T> findOne(SqlQuerySpec query) {
        return findOne(query, entityClass);
    }

    @Override
    public <V> Optional<V> findOne(SqlQuerySpec query, Class<V> clazz) {
        var watch = new StopWatch();
        int returnedDocs = 0;
        double requestCharge = 0d;
        try {
            List<V> results = new ArrayList<>();
            CosmosPagedIterable<V> items = cosmosContainer().queryItems(query, new CosmosQueryRequestOptions(), clazz);
            requestCharge = fetch(items, results);
            if (results.isEmpty()) return Optional.empty();
            if (results.size() > 1) throw new Error("more than one row returned");
            returnedDocs = 1;
            return Optional.of(results.get(0));
        } finally {
            long elapsed = watch.elapsed();
            ActionLogContext.track("cosmos", elapsed, returnedDocs, 0);
            logger.debug("findOne, class={}, sql={}, params={}, returnedDocs={}, requestCharge={}, elapsed={}",
                clazz.getSimpleName(),
                query.getQueryText(),
                query.getParameters(),
                returnedDocs,
                requestCharge,
                elapsed);
            checkSlowOperation(elapsed);
        }
    }

    @Override
    public List<T> find(SqlQuerySpec query) {
        return find(query, entityClass);
    }

    @Override
    public <V> List<V> find(SqlQuerySpec query, Class<V> clazz) {
        var watch = new StopWatch();
        List<V> results = new ArrayList<>();
        double requestCharge = 0d;
        try {
            CosmosPagedIterable<V> items = cosmosContainer().queryItems(query, new CosmosQueryRequestOptions(), clazz);
            items.iterableByPage().forEach(page -> page.getRequestCharge());
            requestCharge = fetch(items, results);
            checkTooManyRowsReturned(results.size());
            return results;
        } finally {
            long elapsed = watch.elapsed();
            int size = results.size();
            ActionLogContext.track("cosmos", elapsed, size, 0);
            logger.debug("find, clazz={}, sql={}, params={}, returnedDocs={}, requestCharge={}, elapsed={}",
                entityName,
                query.getQueryText(),
                query.getParameters(),
                size,
                requestCharge,
                elapsed);
            checkSlowOperation(elapsed);
        }
    }

    @Override
    public void delete(String id) {
        var watch = new StopWatch();
        double requestCharge = 0d;
        try {
            CosmosItemResponse<Object> response = cosmosContainer().deleteItem(id, new PartitionKey(id), new CosmosItemRequestOptions());
            requestCharge = response.getRequestCharge();
        } finally {
            long elapsed = watch.elapsed();
            ActionLogContext.track("cosmos", elapsed, 0, 1);
            logger.debug("delete, entity={}, id={}, requestCharge={}, elapsed={}", entityName, id, requestCharge, elapsed);
            checkSlowOperation(elapsed);
        }
    }

    private <V> double fetch(CosmosPagedIterable<V> iterable, List<V> results) {
        double requestCharge = 0d;
        for (FeedResponse<V> response : iterable.iterableByPage()) {
            requestCharge += response.getRequestCharge();
            response.getElements().forEach(element -> results.add(element));
        }
        return requestCharge;
    }

    private void checkSlowOperation(long elapsed) {
        if (elapsed > cosmos.slowOperationThresholdInNanos)
            logger.warn(Markers.errorCode("SLOW_COSMOSDB"), "slow cosmosDB query, elapsed={}", elapsed);
    }

    private void checkTooManyRowsReturned(int size) {
        if (size > cosmos.tooManyRowsReturnedThreshold)
            logger.warn(Markers.errorCode("TOO_MANY_ROWS_RETURNED"), "too many rows returned, returnedRows={}", size);
    }

    private CosmosContainer cosmosContainer() {
        if (this.cosmosContainer == null) {
            this.cosmosContainer = cosmos.getDatabase().getContainer(entityName);
        }
        return cosmosContainer;
    }
}
