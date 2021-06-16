package core.ext.cosmos.module;

import com.azure.cosmos.implementation.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import core.ext.cosmos.Cosmos;
import core.ext.cosmos.CosmosRepository;
import core.ext.cosmos.Entity;
import core.ext.cosmos.impl.CosmosImpl;
import core.framework.internal.module.Config;
import core.framework.internal.module.ModuleContext;
import core.framework.internal.module.ShutdownHook;
import core.framework.util.Types;

import java.time.Duration;
import java.util.List;

import static core.framework.util.Strings.format;

/**
 * @author Neal
 */
public class CosmosConfig extends Config {
    private ModuleContext context;
    private String name;
    private boolean entityAdded;

    private String endpoint;
    private String key;
    private String databaseId;
    private List<String> preferredRegions;

    private CosmosImpl cosmos;

    @Override
    protected void initialize(ModuleContext context, String name) {
        this.context = context;
        this.name = name;

        cosmos = new CosmosImpl();
        //enable cosmos jsr310
        //todo custom objectMapper
        ObjectMapper objectMapper = Utils.getSimpleObjectMapper();
        objectMapper.registerModule(new ZonedDateTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.context.startupHook.add(cosmos::initialize);
        this.context.shutdownHook.add(ShutdownHook.STAGE_7, timeout -> cosmos.close());
        context.beanFactory.bind(Cosmos.class, name, cosmos);
    }

    @Override
    protected void validate() {
        if (endpoint == null) throw new Error("Endpoint not configured");
        if (key == null) throw new Error("Key not configured");
        if (databaseId == null) throw new Error("DatabaseId not configured");
        if (preferredRegions == null) throw new Error("PreferredRegions not configured");
        if (!entityAdded)
            throw new Error("cosmos is configured but no entity added, please remove unnecessary config, name=" + name);
    }

    public void endpoint(String endpoint) {
        if (this.endpoint != null)
            throw new Error(format("cosmos endpoint is already configured, name={}, endpoint={}, previous={}", name, endpoint, this.endpoint));
        this.endpoint = endpoint;
        cosmos.endpoint(endpoint);
    }

    public void key(String key) {
        if (this.key != null)
            throw new Error(format("cosmos key is already configured, name={}, key={}, previous={}", name, key, this.key));
        this.key = key;
        cosmos.key(key);
    }

    public void databaseId(String databaseId) {
        if (this.databaseId != null)
            throw new Error(format("cosmos databaseId is already configured, name={}, databaseId={}, previous={}", name, databaseId, this.databaseId));
        this.databaseId = databaseId;
        cosmos.databaseId(databaseId);
    }

    public void preferredRegions(List<String> preferredRegions) {
        if (this.preferredRegions != null)
            throw new Error(format("cosmos databaseId is already configured, name={}, databaseId={}, previous={}", name, databaseId, this.databaseId));
        if (preferredRegions == null || preferredRegions.isEmpty())
            throw new Error("cosmos preferredRegion can't empty");
        this.preferredRegions = preferredRegions;
        cosmos.preferredRegions(preferredRegions);
    }

    public void slowOperationThreshold(Duration threshold) {
        cosmos.slowOperationThreshold(threshold);
    }

    public void tooManyRowsReturnedThreshold(int threshold) {
        cosmos.tooManyRowsReturnedThreshold(threshold);
    }

    public <T> void entity(Class<T> entityClass) {
        if (entityClass == null || entityClass.getAnnotation(Entity.class) == null)
            throw new Error("entity must have Entity annotation");
        context.beanFactory.bind(Types.generic(CosmosRepository.class, entityClass), name, cosmos.entity(entityClass));
        entityAdded = true;
    }
}
