package core.cosmos.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import core.ext.cosmos.Id;
import core.ext.cosmos.module.ZonedDateTimeModule;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

/**
 * @author Neal
 */
public class FormatTest {
    @Test
    public void formatDate() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new ZonedDateTimeModule());
        Item item = new Item();
        item.id = "1";
        item.updatedTime = ZonedDateTime.now();
        item.createdTime = ZonedDateTime.now();
        mapper.writeValueAsString(item);
    }

    static class Item {
        @Id
        public String id;

        @JsonProperty("updated_time")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSX")
        public ZonedDateTime updatedTime;

        @JsonProperty("created_time")
        public ZonedDateTime createdTime;

    }
}
