package core.ext.cosmos.module;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.PackageVersion;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;

import java.time.ZonedDateTime;

/**
 * @author Neal
 */
public class ZonedDateTimeModule extends SimpleModule {
    private static final long serialVersionUID = 1L;

    public ZonedDateTimeModule() {
        super(PackageVersion.VERSION);
        // serializer
        addSerializer(ZonedDateTime.class, ZonedDateTimeSerializer.INSTANCE);
        // deserializer
        addDeserializer(ZonedDateTime.class, InstantDeserializer.ZONED_DATE_TIME);
    }

}
