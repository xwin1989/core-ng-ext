package core.ext.cosmos;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Neal
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface Id { //todo zoneLevel search
}
