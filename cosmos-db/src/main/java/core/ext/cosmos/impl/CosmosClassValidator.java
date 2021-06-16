package core.ext.cosmos.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import core.ext.cosmos.Entity;
import core.ext.cosmos.Id;
import core.framework.internal.reflect.Classes;
import core.framework.internal.reflect.Fields;
import core.framework.internal.validate.ClassValidator;
import core.framework.internal.validate.ClassVisitor;
import core.framework.util.Maps;
import core.framework.util.Sets;

import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static core.framework.util.Strings.format;

/**
 * @author Neal
 */
public class CosmosClassValidator implements ClassVisitor {
    private final ClassValidator validator;
    private final Map<String, Set<String>> fields = Maps.newHashMap();
    private Field id;

    public CosmosClassValidator(Class<?> entityClass) {
        validator = new ClassValidator(entityClass);
        validator.allowedValueClasses = Set.of(String.class, Boolean.class, Integer.class, Long.class, Double.class, ZonedDateTime.class);
        validator.allowChild = true;
        validator.visitor = this;
    }

    void validateEntityClass() {
        validator.validate();

        if (id == null) {
            throw new Error("cosmos entity class must have @Id field, class=" + validator.instanceClass.getCanonicalName());
        }
    }

    @Override
    public void visitClass(Class<?> objectClass, String path) {
        if (path == null && !objectClass.isAnnotationPresent(Entity.class))
            throw new Error(format("cosmos entity class must have @Entity, class={}", objectClass.getCanonicalName()));
    }

    @Override
    public void visitField(Field field, String parentPath) {
        if (field.isAnnotationPresent(Id.class)) {
            validateId(field, parentPath == null);
        } else {
            JsonProperty property = field.getDeclaredAnnotation(JsonProperty.class);
            if (property == null)
                throw new Error(format("cosmos entity field must have @JsonProperty, field={}", Fields.path(field)));
            String propertyName = property.value();

            Set<String> fields = this.fields.computeIfAbsent(parentPath, key -> Sets.newHashSet());
            if (fields.contains(propertyName)) {
                throw new Error(format("found duplicate field, field={}, property={}", Fields.path(field), propertyName));
            }
            fields.add(propertyName);
        }
    }

    @Override
    public void visitEnum(Class<?> enumClass) {
        Set<String> enumValues = Sets.newHashSet();
        List<Field> fields = Classes.enumConstantFields(enumClass);
        for (Field field : fields) {
            boolean added = enumValues.add(field.getName());
            if (!added) {
                throw new Error(format("cosmos enum value must be unique, field={}, value={}", Fields.path(field), field.getName()));
            }
            JsonProperty property = field.getDeclaredAnnotation(JsonProperty.class);
            if (property == null) {
                throw new Error(format("cosmos enum must have json annotation, please separate view and entity, field={}", Fields.path(field)));
            }
        }
    }

    private void validateId(Field field, boolean topLevel) {
        if (topLevel) {
            if (id != null)
                throw new Error(format("cosmos entity class must have only one @Id field, previous={}, current={}", Fields.path(id), Fields.path(field)));
            Class<?> fieldClass = field.getType();
            if (!String.class.equals(fieldClass) || !"id".equals(field.getName())) {
                throw new Error(format("@Id field type must be String and name must be `id`, field={}, class={}", Fields.path(field), fieldClass.getCanonicalName()));
            }
            id = field;
        } else {
            throw new Error(format("cosmos nested entity class must not have @Id field, field={}", Fields.path(field)));
        }
    }
}

