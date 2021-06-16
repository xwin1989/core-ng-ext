package core.ext.cosmos;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosDatabase;
import core.framework.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * @author Neal
 */

public class CosmosMigration {
    private final Logger logger = LoggerFactory.getLogger(CosmosMigration.class);
    private final CosmosDatabase database;
    private final CosmosClient client;

    public CosmosMigration(String propertyFileClasspath) {
        var properties = new Properties();
        properties.load(propertyFileClasspath);

        client = new CosmosClientBuilder()
            .endpoint(properties.get("cosmos.endpoint").orElseThrow())
            .key(properties.get("cosmos.key").orElseThrow())
            .preferredRegions(Arrays.asList(properties.get("cosmos.preferredRegions").get().split(",")))
            .consistencyLevel(ConsistencyLevel.SESSION)
            .contentResponseOnWriteEnabled(true)
            .buildClient();

        var response = client.createDatabaseIfNotExists(properties.get("cosmos.databaseId").orElseThrow());
        database = client.getDatabase(response.getProperties().getId());
    }

    public void migrate(Consumer<CosmosDatabase> consumer) {
        try {
            consumer.accept(database);
        } catch (Throwable e) {
            logger.error("failed to run migration", e);
            throw e;
        } finally {
            client.close();
        }
    }
}
