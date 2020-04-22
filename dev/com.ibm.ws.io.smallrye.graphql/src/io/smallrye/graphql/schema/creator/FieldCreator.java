package io.smallrye.graphql.schema.creator;

import java.util.Optional;

import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.smallrye.graphql.schema.Annotations;
import io.smallrye.graphql.schema.SchemaBuilderException;
import io.smallrye.graphql.schema.helper.DefaultValueHelper;
import io.smallrye.graphql.schema.helper.DescriptionHelper;
import io.smallrye.graphql.schema.helper.Direction;
import io.smallrye.graphql.schema.helper.FormatHelper;
import io.smallrye.graphql.schema.helper.IgnoreHelper;
import io.smallrye.graphql.schema.helper.MethodHelper;
import io.smallrye.graphql.schema.helper.NonNullHelper;
import io.smallrye.graphql.schema.model.Field;
import io.smallrye.graphql.schema.model.Reference;

/**
 * Creates a Field object
 * 
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
public class FieldCreator {

    private final ReferenceCreator referenceCreator;

    public FieldCreator(ReferenceCreator referenceCreator) {
        this.referenceCreator = referenceCreator;
    }

    /**
     * Creates a field from a method only.
     * This is used in the case of an interface
     * 
     * @param methodInfo the java method
     * @return a Field model object
     */
    public Optional<Field> createFieldForInterface(MethodInfo methodInfo) {
        Annotations annotationsForMethod = Annotations.getAnnotationsForInterfaceField(methodInfo);

        if (!IgnoreHelper.shouldIgnore(annotationsForMethod)) {
            Type returnType = methodInfo.returnType();

            // Name
            String name = getFieldName(Direction.OUT, annotationsForMethod, methodInfo.name());

            // Description
            Optional<String> maybeDescription = DescriptionHelper.getDescriptionForField(annotationsForMethod, returnType);

            // Field Type
            validateFieldType(Direction.OUT, methodInfo);
            Reference reference = referenceCreator.createReferenceForInterfaceField(returnType, annotationsForMethod);

            Field field = new Field(methodInfo.name(),
                    MethodHelper.getPropertyName(Direction.OUT, methodInfo.name()),
                    name,
                    maybeDescription.orElse(null),
                    reference);

            // NotNull
            if (NonNullHelper.markAsNonNull(returnType, annotationsForMethod)) {
                field.setNotNull(true);
            }

            // Array
            field.setArray(ArrayCreator.createArray(returnType).orElse(null));

            // TransformInfo
            field.setTransformInfo(FormatHelper.getFormat(returnType, annotationsForMethod).orElse(null));

            // Default Value
            field.setDefaultValue(DefaultValueHelper.getDefaultValue(annotationsForMethod).orElse(null));

            return Optional.of(field);
        }
        return Optional.empty();
    }

    /**
     * Creates a field from a field and method.
     * Used by Type and Input
     * 
     * @param direction the direction (in/out)
     * @param fieldInfo the java property
     * @param methodInfo the java method
     * @return a Field model object
     */
    public Optional<Field> createFieldForPojo(Direction direction, FieldInfo fieldInfo, MethodInfo methodInfo) {
        Annotations annotationsForPojo = Annotations.getAnnotationsForPojo(direction, fieldInfo, methodInfo);

        if (!IgnoreHelper.shouldIgnore(annotationsForPojo)) {
            Type methodType = getMethodType(methodInfo, direction);

            // Name
            String name = getFieldName(direction, annotationsForPojo, methodInfo.name());

            // Description
            Optional<String> maybeDescription = DescriptionHelper.getDescriptionForField(annotationsForPojo, methodType);

            // Field Type
            validateFieldType(direction, methodInfo);
            Type fieldType = getFieldType(fieldInfo, methodType);

            Reference reference = referenceCreator.createReferenceForPojoField(direction, fieldType, methodType,
                    annotationsForPojo);

            Field field = new Field(methodInfo.name(),
                    MethodHelper.getPropertyName(direction, methodInfo.name()),
                    name,
                    maybeDescription.orElse(null),
                    reference);

            // NotNull
            if (NonNullHelper.markAsNonNull(methodType, annotationsForPojo)) {
                field.setNotNull(true);
            }

            // Array
            field.setArray(ArrayCreator.createArray(fieldType, methodType).orElse(null));

            // TransformInfo
            field.setTransformInfo(FormatHelper.getFormat(methodType, annotationsForPojo).orElse(null));

            // Default Value
            field.setDefaultValue(DefaultValueHelper.getDefaultValue(annotationsForPojo).orElse(null));

            return Optional.of(field);
        }
        return Optional.empty();
    }

    private static void validateFieldType(Direction direction, MethodInfo methodInfo) {
        Type returnType = methodInfo.returnType();
        if (direction.equals(Direction.OUT) && returnType.kind().equals(Type.Kind.VOID)) {
            throw new SchemaBuilderException(
                    "Can not have a void return method [" + methodInfo.name() + "] in class ["
                            + methodInfo.declaringClass().name() + "]");
        }
    }

    private static Type getMethodType(MethodInfo method, Direction direction) {
        if (direction.equals(Direction.IN)) {
            return method.parameters().get(0);
        }
        return method.returnType();
    }

    private static Type getFieldType(FieldInfo fieldInfo, Type defaultType) {
        if (fieldInfo == null) {
            return defaultType;
        }
        return fieldInfo.type();
    }

    /**
     * Get the field name. Depending on the direction, we either also look at getter/setters
     * 
     * @param direction the direction
     * @param annotationsForThisField annotations on this field
     * @param defaultFieldName the default field name
     * @return the field name
     */
    private static String getFieldName(Direction direction, Annotations annotationsForThisField,
            String defaultFieldName) {
        switch (direction) {
            case OUT:
                return getOutputNameForField(annotationsForThisField, defaultFieldName);
            case IN:
                return getInputNameForField(annotationsForThisField, defaultFieldName);
            default:
                return defaultFieldName;
        }
    }

    private static String getOutputNameForField(Annotations annotationsForThisField, String fieldName) {
        return annotationsForThisField.getOneOfTheseMethodAnnotationsValue(
                Annotations.NAME,
                Annotations.QUERY,
                Annotations.JSONB_PROPERTY)
                .orElse(annotationsForThisField.getOneOfTheseAnnotationsValue(
                        Annotations.NAME,
                        Annotations.QUERY,
                        Annotations.JSONB_PROPERTY)
                        .orElse(MethodHelper.getPropertyName(Direction.OUT, fieldName)));
    }

    private static String getInputNameForField(Annotations annotationsForThisField, String fieldName) {
        return annotationsForThisField.getOneOfTheseMethodAnnotationsValue(
                Annotations.NAME,
                Annotations.JSONB_PROPERTY)
                .orElse(annotationsForThisField.getOneOfTheseAnnotationsValue(
                        Annotations.NAME,
                        Annotations.JSONB_PROPERTY)
                        .orElse(MethodHelper.getPropertyName(Direction.IN, fieldName)));
    }
}
