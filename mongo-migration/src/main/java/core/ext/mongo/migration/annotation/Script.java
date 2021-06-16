package core.ext.mongo.migration.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Neal
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Script {
    /**
     * Issue number
     */
    String ticket();

    /**
     * Issue description
     */
    String description();

    /**
     * Target test method ( must in current class )
     */
    String testMethod();

    /**
     * Execute every times, default is false.
     */
    boolean runAlways() default false;

    /**
     * Execute oder. default is -1. if order is will be random
     */
    int order() default -1;

    /**
     * specify runnable environment,default is all environment.("dev", "uat", "staging", "prod")
     */
    String[] runAt() default {};

    /**
     * Backup collect before execution
     */
    boolean autoBackup() default false;
}
