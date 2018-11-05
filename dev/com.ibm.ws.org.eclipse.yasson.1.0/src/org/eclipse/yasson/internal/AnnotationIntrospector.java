/*******************************************************************************
 * Copyright (c) 2016, 2018 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * Roman Grigoriadi
 ******************************************************************************/

package org.eclipse.yasson.internal;

import org.eclipse.yasson.ImplementationClass;
import org.eclipse.yasson.internal.components.AdapterBinding;
import org.eclipse.yasson.internal.components.DeserializerBinding;
import org.eclipse.yasson.internal.components.SerializerBinding;
import org.eclipse.yasson.internal.model.AnnotationTarget;
import org.eclipse.yasson.internal.model.CreatorModel;
import org.eclipse.yasson.internal.model.JsonbAnnotatedElement;
import org.eclipse.yasson.internal.model.JsonbCreator;
import org.eclipse.yasson.internal.model.Property;
import org.eclipse.yasson.internal.model.customization.ClassCustomization;
import org.eclipse.yasson.internal.model.customization.ClassCustomizationBuilder;
import org.eclipse.yasson.internal.properties.MessageKeys;
import org.eclipse.yasson.internal.properties.Messages;
import org.eclipse.yasson.internal.serializer.JsonbDateFormatter;
import org.eclipse.yasson.internal.serializer.JsonbNumberFormatter;

import javax.json.bind.JsonbException;
import javax.json.bind.adapter.JsonbAdapter;
import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.bind.annotation.JsonbNillable;
import javax.json.bind.annotation.JsonbNumberFormat;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbTypeAdapter;
import javax.json.bind.annotation.JsonbTypeDeserializer;
import javax.json.bind.annotation.JsonbTypeSerializer;
import javax.json.bind.annotation.JsonbVisibility;
import javax.json.bind.config.PropertyVisibilityStrategy;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.bind.serializer.JsonbSerializer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * Introspects configuration on classes and their properties by reading annotations.
 *
 * @author Roman Grigoriadi
 */
public class AnnotationIntrospector {

    private final JsonbContext jsonbContext;

    /**
     * Annotations to report exception when used in combination with {@link JsonbTransient}
     */
    public static final List<Class<? extends Annotation>> TRANSIENT_INCOMPATIBLE =
            Arrays.asList(JsonbDateFormat.class, JsonbNumberFormat.class, JsonbProperty.class,
                          JsonbTypeAdapter.class, JsonbTypeSerializer.class, JsonbTypeDeserializer.class);

    /**
     * Creates annotation introspecting component passing {@link JsonbContext} inside.
     *
     * @param jsonbContext mandatory
     */
    public AnnotationIntrospector(JsonbContext jsonbContext) {
        Objects.requireNonNull(jsonbContext);
        this.jsonbContext = jsonbContext;
    }

    /**
     * Gets a name of property for JSON marshalling.
     * Can be different writeName for same property.
     * @param property property representation - field, getter, setter (not null)
     * @return read name
     */
    public String getJsonbPropertyJsonWriteName(Property property) {
        Objects.requireNonNull(property);
        return getJsonbPropertyCustomizedName(property, property.getGetterElement());
    }

    /**
     * Gets a name of property for JSON unmarshalling.
     * Can be different from writeName for same property.
     * @param property property representation - field, getter, setter (not null)
     * @return write name
     */
    public String getJsonbPropertyJsonReadName(Property property) {
        Objects.requireNonNull(property);
        return getJsonbPropertyCustomizedName(property, property.getSetterElement());
    }

    private String getJsonbPropertyCustomizedName(Property property, JsonbAnnotatedElement<Method> methodElement) {
        JsonbProperty methodAnnotation = getMethodAnnotation(JsonbProperty.class, methodElement);
        if (methodAnnotation != null && !methodAnnotation.value().isEmpty()) {
            return methodAnnotation.value();
        }
        //in case of property name getter/setter override field value
        JsonbProperty fieldAnnotation = getFieldAnnotation(JsonbProperty.class, property.getFieldElement());
        if (fieldAnnotation != null && !fieldAnnotation.value().isEmpty()) {
            return fieldAnnotation.value();
        }

        return null;
    }


    /**
     * Searches for JsonbCreator annotation on constructors and static methods.
     *
     * @param clazz class to search
     * @return JsonbCreator metadata object
     */
    public JsonbCreator getCreator(Class<?> clazz) {
        JsonbCreator jsonbCreator = null;
        Constructor<?>[] declaredConstructors =
                AccessController.doPrivileged((PrivilegedAction<Constructor<?>[]>) clazz::getDeclaredConstructors);

        for (Constructor<?> constructor : declaredConstructors) {
            final javax.json.bind.annotation.JsonbCreator annot = findAnnotation(constructor.getDeclaredAnnotations(), javax.json.bind.annotation.JsonbCreator.class);
            if (annot != null) {
                jsonbCreator = createJsonbCreator(constructor, jsonbCreator, clazz);
            }
        }

        Method[] declaredMethods =
                AccessController.doPrivileged((PrivilegedAction<Method[]>) clazz::getDeclaredMethods);
        for (Method method : declaredMethods) {
            final javax.json.bind.annotation.JsonbCreator annot = findAnnotation(method.getDeclaredAnnotations(), javax.json.bind.annotation.JsonbCreator.class);
            if (annot != null && Modifier.isStatic(method.getModifiers())) {
                if (!clazz.equals(method.getReturnType())) {
                    throw new JsonbException(Messages.getMessage(MessageKeys.INCOMPATIBLE_FACTORY_CREATOR_RETURN_TYPE, method, clazz));
                }
                jsonbCreator = createJsonbCreator(method, jsonbCreator, clazz);
            }
        }
        return jsonbCreator;
    }

    private JsonbCreator createJsonbCreator(Executable executable, JsonbCreator existing, Class<?> clazz) {
        if (existing != null) {
            throw new JsonbException(Messages.getMessage(MessageKeys.MULTIPLE_JSONB_CREATORS, clazz));
        }

        final Parameter[] parameters = executable.getParameters();

        CreatorModel[] creatorModels = new CreatorModel[parameters.length];
        for (int i=0; i<parameters.length; i++) {
            final Parameter parameter = parameters[i];
            final JsonbProperty jsonbPropertyAnnotation = parameter.getAnnotation(JsonbProperty.class);
            if (jsonbPropertyAnnotation != null && !jsonbPropertyAnnotation.value().isEmpty()) {
                creatorModels[i] = new CreatorModel(jsonbPropertyAnnotation.value(), parameter, jsonbContext);
            } else {
                creatorModels[i] = new CreatorModel(parameter.getName(), parameter, jsonbContext);
            }
        }

        return new JsonbCreator(executable, creatorModels);
    }

    /**
     * Checks for {@link JsonbAdapter} on a property.
     * @param property property not null
     * @return components info
     */
    public AdapterBinding getAdapterBinding(Property property) {
        Objects.requireNonNull(property);
        JsonbTypeAdapter adapterAnnotation = getAnnotationFromProperty(JsonbTypeAdapter.class, property)
                .orElseGet(()-> getAnnotationFromPropertyType(property, JsonbTypeAdapter.class));
        if (adapterAnnotation == null) {
            return null;
        }

        return getAdapterBindingFromAnnotation(adapterAnnotation, ReflectionUtils.getOptionalRawType(property.getPropertyType()));
    }

    /**
     * Checks for {@link JsonbAdapter} on a type.
     * @param clsElement type not null
     * @return components info
     */
    public AdapterBinding getAdapterBinding(JsonbAnnotatedElement<Class<?>> clsElement) {
        Objects.requireNonNull(clsElement);

        JsonbTypeAdapter adapterAnnotation = clsElement.getElement().getAnnotation(JsonbTypeAdapter.class);
        if (adapterAnnotation == null) {
            return null;
        }

        return getAdapterBindingFromAnnotation(adapterAnnotation, Optional.ofNullable(clsElement.getElement()));
    }

    private AdapterBinding getAdapterBindingFromAnnotation(JsonbTypeAdapter adapterAnnotation, Optional<Class<?>> expectedClass) {
        final Class<? extends JsonbAdapter> adapterClass = adapterAnnotation.value();
        final AdapterBinding adapterBinding = jsonbContext.getComponentMatcher().introspectAdapterBinding(adapterClass, null);

        if (expectedClass.isPresent() && !(ReflectionUtils.getRawType(adapterBinding.getBindingType()).isAssignableFrom(expectedClass.get()))) {
            throw new JsonbException(Messages.getMessage(MessageKeys.ADAPTER_INCOMPATIBLE, adapterBinding.getBindingType(), expectedClass.get()));
        }
        return adapterBinding;
    }

    /**
     * Checks for {@link JsonbDeserializer} on a property.
     * @param property property not null
     * @return components info
     */
    public DeserializerBinding getDeserializerBinding(Property property) {
        Objects.requireNonNull(property);
        JsonbTypeDeserializer deserializerAnnotation = getAnnotationFromProperty(JsonbTypeDeserializer.class, property)
                .orElseGet(()-> getAnnotationFromPropertyType(property, JsonbTypeDeserializer.class));
        if (deserializerAnnotation == null) {
            return null;
        }

        final Class<? extends JsonbDeserializer> deserializerClass = deserializerAnnotation.value();
        return jsonbContext.getComponentMatcher().introspectDeserializerBinding(deserializerClass, null);
    }

    /**
     * Checks for {@link JsonbDeserializer} on a type.
     * @param clsElement type not null
     * @return components info
     */
    public DeserializerBinding getDeserializerBinding(JsonbAnnotatedElement<Class<?>> clsElement) {
        Objects.requireNonNull(clsElement);
        JsonbTypeDeserializer deserializerAnnotation = clsElement.getElement().getAnnotation(JsonbTypeDeserializer.class);
        if (deserializerAnnotation == null) {
            return null;
        }

        final Class<? extends JsonbDeserializer> deserializerClass = deserializerAnnotation.value();
        return jsonbContext.getComponentMatcher().introspectDeserializerBinding(deserializerClass, null);
    }

    /**
     * Checks for {@link JsonbSerializer} on a property.
     * @param property property not null
     * @return components info
     */
    public SerializerBinding getSerializerBinding(Property property) {
        Objects.requireNonNull(property);
        JsonbTypeSerializer serializerAnnotation = getAnnotationFromProperty(JsonbTypeSerializer.class, property)
                .orElseGet(()-> getAnnotationFromPropertyType(property, JsonbTypeSerializer.class));
        if (serializerAnnotation == null) {
            return null;
        }

        final Class<? extends JsonbSerializer> serializerClass = serializerAnnotation.value();
        return jsonbContext.getComponentMatcher().introspectSerializerBinding(serializerClass, null);

    }

    /**
     * Checks for {@link JsonbSerializer} on a type.
     * @param clsElement type not null
     * @return components info
     */
    public SerializerBinding getSerializerBinding(JsonbAnnotatedElement<Class<?>> clsElement) {
        Objects.requireNonNull(clsElement);
        JsonbTypeSerializer serializerAnnotation = clsElement.getElement().getAnnotation(JsonbTypeSerializer.class);
        if (serializerAnnotation == null) {
            return null;
        }

        final Class<? extends JsonbSerializer> serializerClass = serializerAnnotation.value();
        return jsonbContext.getComponentMatcher().introspectSerializerBinding(serializerClass, null);
    }

    private <T extends Annotation> T getAnnotationFromPropertyType(Property property, Class<T> annotationClass) {
        final Optional<Class<?>> optionalRawType = ReflectionUtils.getOptionalRawType(property.getPropertyType());
        if (!optionalRawType.isPresent()) {
            //will not work for type variable properties, which are bound to class that is annotated.
            return null;
        }
        return findAnnotation(collectAnnotations(optionalRawType.get()).getAnnotations(), annotationClass);
    }

    /**
     * Checks if property is nillable.
     * Looks for {@link JsonbProperty} nillable attribute only.
     * JsonbNillable is checked only for ClassModels.
     *
     * @param property property to search in, not null
     * @return True if property should be serialized when null.
     */
    public Optional<Boolean> isPropertyNillable(Property property) {
        Objects.requireNonNull(property);

        final Optional<JsonbProperty> jsonbProperty = getAnnotationFromProperty(JsonbProperty.class, property);
        return jsonbProperty.map(JsonbProperty::nillable);

    }

    /**
     * Checks for JsonbNillable annotation on a class, its superclasses and interfaces.
     *
     * @param clazzElement class to search JsonbNillable in.
     * @return true if found
     */
    public boolean isClassNillable(JsonbAnnotatedElement<Class<?>> clazzElement) {
        final JsonbNillable jsonbNillable = findAnnotation(clazzElement.getAnnotations(), JsonbNillable.class);
        if (jsonbNillable != null) {
            return jsonbNillable.value();
        }
        return jsonbContext.getConfigProperties().getConfigNullable();
    }

    /**
     * Checks for {@link JsonbPropertyOrder} annotation.
     *
     * @param clazzElement class to search on
     * @return ordered properties names or null if not found
     */
    public String[] getPropertyOrder(JsonbAnnotatedElement<Class<?>> clazzElement) {
        final JsonbPropertyOrder jsonbPropertyOrder = clazzElement.getElement().getAnnotation(JsonbPropertyOrder.class);
        return jsonbPropertyOrder != null ? jsonbPropertyOrder.value() : null;
    }

    /**
     * Checks if property is annotated transient. If JsonbTransient annotation is present on field getter or setter, and other annotation is present
     * on either of it, JsonbException is thrown with message describing collision.
     *
     * @param property  The property to inspect if there is any {@link JsonbTransient} annotation defined for it
     * @return  Set of {@link AnnotationTarget}s specifying in which scope the {@link JsonbTransient} is applied
     */
    public EnumSet<AnnotationTarget> getJsonbTransientCategorized(Property property) {
        Objects.requireNonNull(property);
        EnumSet<AnnotationTarget> transientTarget = EnumSet.noneOf(AnnotationTarget.class);
        Map<AnnotationTarget, JsonbTransient> annotationFromPropertyCategorized = getAnnotationFromPropertyCategorized(JsonbTransient.class, property);
        if (annotationFromPropertyCategorized.size() > 0) {
            transientTarget.addAll(annotationFromPropertyCategorized.keySet());
            return transientTarget;
        }

        return transientTarget;
    }

    /**
     * Search {@link JsonbDateFormat} on property, if not found looks at annotations declared on property type class.
     *
     * @param property Property to search on.
     * @return  Map of {@link JsonbDateFormatter} instances categorized by their scopes (class, property, getter or setter). If there is no date
     * formatter specified for given property, an empty map would be returned
     */
    public  Map<AnnotationTarget, JsonbDateFormatter> getJsonbDateFormatCategorized(Property property) {
        Objects.requireNonNull(property);

        Map<AnnotationTarget, JsonbDateFormatter> result = new HashMap<>();
        Map<AnnotationTarget, JsonbDateFormat> annotationFromPropertyCategorized = getAnnotationFromPropertyCategorized(JsonbDateFormat.class, property);
        if (annotationFromPropertyCategorized.size() != 0) {
            annotationFromPropertyCategorized.forEach((key, annotation) -> result.put(key, createJsonbDateFormatter(annotation.value(), annotation.locale(), property)));
        }

        // No date format on property, try class level
        // if property is not TypeVariable and its class is not date skip it
        final Optional<Class<?>> propertyRawTypeOptional = ReflectionUtils.getOptionalRawType(property.getPropertyType());
        if (propertyRawTypeOptional.isPresent()) {
            Class<?> rawType = propertyRawTypeOptional.get();
            if (!(Date.class.isAssignableFrom(rawType) || Calendar.class.isAssignableFrom(rawType)
                    || TemporalAccessor.class.isAssignableFrom(rawType))) {
                return new HashMap<>();
            }
        }

        JsonbDateFormat classLevelDateFormatter = findAnnotation(property.getDeclaringClassElement().getAnnotations(), JsonbDateFormat.class);
        if(classLevelDateFormatter != null) {
            result.put(AnnotationTarget.CLASS, createJsonbDateFormatter(classLevelDateFormatter.value(), classLevelDateFormatter.locale(), property));
        }

        return result;
    }

    /**
     * Search for {@link JsonbDateFormat} annotation on java class and construct {@link JsonbDateFormatter}.
     * If not found looks at annotations declared on property type class.
     * @param clazzElement class to search not null
     * @return formatter to use
     */
    public JsonbDateFormatter getJsonbDateFormat(JsonbAnnotatedElement<Class<?>> clazzElement) {
        Objects.requireNonNull(clazzElement);
        final JsonbDateFormat format = findAnnotation(clazzElement.getAnnotations(), JsonbDateFormat.class);
        if (format == null) {
            return jsonbContext.getConfigProperties().getConfigDateFormatter();
        }
        return new JsonbDateFormatter(format.value(), format.locale());
    }

    /**
     * Search for {@link JsonbNumberFormat} annotation on java class.
     *
     * @param clazzElement class to search not null
     * @return formatter to use
     */
    public JsonbNumberFormatter getJsonbNumberFormat(JsonbAnnotatedElement<Class<?>> clazzElement) {
        final JsonbNumberFormat formatAnnotation = findAnnotation(clazzElement.getAnnotations(), JsonbNumberFormat.class);
        if (formatAnnotation == null) {
            return null;
        }
        return new JsonbNumberFormatter(formatAnnotation.value(), formatAnnotation.locale());
    }

    /**
     * Search {@link JsonbNumberFormat} on property, if not found looks at annotations declared on property type class.
     *
     * @param property Property to search on.
     * @return  Map of {@link JsonbNumberFormatter} instances categorized by their scopes (class, property, getter or setter). If there is no number
     * formatter specified for given property, an empty map would be returned
     */
    public Map<AnnotationTarget, JsonbNumberFormatter> getJsonNumberFormatter(Property property) {
        Map<AnnotationTarget, JsonbNumberFormatter> result = new HashMap<>();
        Map<AnnotationTarget, JsonbNumberFormat> annotationFromPropertyCategorized = getAnnotationFromPropertyCategorized(JsonbNumberFormat.class, property);
        if(annotationFromPropertyCategorized.size() == 0) {
            final Optional<Class<?>> propertyRawTypeOptional = ReflectionUtils.getOptionalRawType(property.getPropertyType());
            if (propertyRawTypeOptional.isPresent()) {
                Class<?> rawType = propertyRawTypeOptional.get();
                if (!Number.class.isAssignableFrom(rawType)) {
                    return new HashMap<>();
                }
            }
        } else {
            annotationFromPropertyCategorized.forEach((key, annotation) -> result.put(key, new JsonbNumberFormatter(annotation.value(), annotation.locale())));
        }

        JsonbNumberFormat classLevelNumberFormatter = findAnnotation(property.getDeclaringClassElement().getAnnotations(), JsonbNumberFormat.class);
        if(classLevelNumberFormatter != null) {
            result.put(AnnotationTarget.CLASS, new JsonbNumberFormatter(classLevelNumberFormatter.value(), classLevelNumberFormatter.locale()));
        }

        return result;
    }

    public JsonbNumberFormatter getConstructorNumberFormatter(JsonbAnnotatedElement<Parameter> param) {
        JsonbNumberFormat annotation = param.getAnnotation(JsonbNumberFormat.class);
        if (annotation != null) {
            return new JsonbNumberFormatter(annotation.value(), annotation.locale());
        }
        return null;
    }

    public JsonbDateFormatter getConstructorDateFormatter(JsonbAnnotatedElement<Parameter> param) {
        JsonbDateFormat annotation = param.getAnnotation(JsonbDateFormat.class);
        if (annotation != null) {
            return new JsonbDateFormatter(DateTimeFormatter
                    .ofPattern(annotation.value(), Locale.forLanguageTag(annotation.locale())),
                    annotation.value(), annotation.locale());
        }
        return null;
    }

    /**
     * Creates {@link JsonbDateFormatter} caches formatter instance if possible.
     * For DEFAULT_FORMAT appropriate singleton instances from java.time.format.DateTimeFormatter
     * are used in date converters.
     */
    private JsonbDateFormatter createJsonbDateFormatter(String format, String locale, Property property) {
        if (JsonbDateFormat.TIME_IN_MILLIS.equals(format) || JsonbDateFormat.DEFAULT_FORMAT.equals(format)) {
            //for epochMillis formatter is not used, for default format singleton instances of DateTimeFormatter
            //are used in the converters
            return new JsonbDateFormatter(format, locale);
        }

        final Optional<Class<?>> optionalRawType = ReflectionUtils.getOptionalRawType(property.getPropertyType());
        final Class<?> propertyRawType = optionalRawType.orElse(null);

        if (propertyRawType != null
                && !TemporalAccessor.class.isAssignableFrom(propertyRawType)
                && !Date.class.isAssignableFrom(propertyRawType)
                && !Calendar.class.isAssignableFrom(propertyRawType)) {
            throw new IllegalStateException(Messages.getMessage(MessageKeys.UNSUPPORTED_DATE_TYPE, propertyRawType));
        }

        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        builder.appendPattern(format);
        if (jsonbContext.getConfigProperties().isZeroTimeDefaulting()) {
            builder.parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0);
            builder.parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0);
            builder.parseDefaulting(ChronoField.HOUR_OF_DAY, 0);
        }
        DateTimeFormatter dateTimeFormatter = builder.toFormatter(Locale.forLanguageTag(locale));
        return new JsonbDateFormatter(dateTimeFormatter, format, locale);
    }

    /**
     * Get a @JsonbVisibility annotation from a class or its package.
     * @param clazz Class to lookup annotation
     * @return Instantiated PropertyVisibilityStrategy if annotation is present
     */
    public Optional<PropertyVisibilityStrategy> getPropertyVisibilityStrategy(Class<?> clazz) {
        JsonbVisibility visibilityAnnotation = findAnnotation(clazz.getDeclaredAnnotations(), JsonbVisibility.class);
        if ((visibilityAnnotation == null) && (clazz.getPackage() != null)) {
            visibilityAnnotation = findAnnotation(clazz.getPackage().getDeclaredAnnotations(), JsonbVisibility.class);
        }
        final Optional<JsonbVisibility> visibilityOptional = Optional.ofNullable(visibilityAnnotation);
        return visibilityOptional.map(jsonbVisibility -> ReflectionUtils.createNoArgConstructorInstance(jsonbVisibility.value()));
    }

    /**
     * Gets an annotation from first resolved annotation in a property in this order:
     * <p>1. Field, 2. Getter, 3 Setter.</p>
     * First found overrides other.
     *
     * @param annotationClass Annotation class to search for
     * @param property property to search in
     * @param <T> Annotation type
     * @return Annotation if found, null otherwise
     */
    private <T extends Annotation> Optional<T> getAnnotationFromProperty(Class<T> annotationClass, Property property) {
        T fieldAnnotation = getFieldAnnotation(annotationClass, property.getFieldElement());
        if (fieldAnnotation != null) {
            return Optional.of(fieldAnnotation);
        }

        T getterAnnotation = getMethodAnnotation(annotationClass, property.getGetterElement());
        if (getterAnnotation != null) {
            return Optional.of(getterAnnotation);
        }

        T setterAnnotation = getMethodAnnotation(annotationClass, property.getSetterElement());
        if (setterAnnotation != null) {
            return Optional.of(setterAnnotation);
        }

        return Optional.empty();
    }

    /**
     * An override of {@link #getAnnotationFromProperty(Class, Property)} in which it returns the results as a map so that the caller can decide which
     * one to be used for read/write operation. Some annotations should have different behaviours based on the scope that they're applied on.
     *
     * @param annotationClass   The annotation class to search
     * @param property  The property to search in
     * @param <T>   Annotation type
     * @return  A map of all occurrences of requested annotation for given property. Caller can determine based on {@link AnnotationTarget} that given
     * annotation is specified on what level (Class, Property, Getter or Setter). If no annotation found for given property, an empty map would be
     * returned
     */
    private <T extends Annotation> Map<AnnotationTarget, T> getAnnotationFromPropertyCategorized(Class<T> annotationClass, Property property) {
        Map<AnnotationTarget, T> result = new HashMap<>();
        T fieldAnnotation = getFieldAnnotation(annotationClass, property.getFieldElement());
        if (fieldAnnotation != null) {
            result.put(AnnotationTarget.PROPERTY, fieldAnnotation);
        }

        T getterAnnotation = getMethodAnnotation(annotationClass, property.getGetterElement());
        if (getterAnnotation != null) {
            result.put(AnnotationTarget.GETTER, getterAnnotation);
        }

        T setterAnnotation = getMethodAnnotation(annotationClass, property.getSetterElement());
        if (setterAnnotation != null) {
            result.put(AnnotationTarget.SETTER, setterAnnotation);
        }

        return result;
    }


    private <T extends Annotation> T getFieldAnnotation(Class<T> annotationClass, JsonbAnnotatedElement<Field> fieldElement) {
        if (fieldElement == null) {
            return null;
        }
        return findAnnotation(fieldElement.getAnnotations(), annotationClass);
    }

    private <T extends Annotation> T findAnnotation(Annotation[] declaredAnnotations, Class<T> annotationClass) {
        return findAnnotation(declaredAnnotations, annotationClass, new HashSet<>());
    }

    /**
     * Finds annotations incompatible with {@link JsonbTransient} annotation.
     * @param target target to check
     * @throws JsonbException If incompatible annotation is found.
     */
    @SuppressWarnings("unchecked")
    public void checkTransientIncompatible(JsonbAnnotatedElement<?> target) {
        if (target == null) {
            return;
        }

        for (Class<? extends Annotation> ann : TRANSIENT_INCOMPATIBLE) {
            Annotation annotation = findAnnotation(target.getAnnotations(), ann);
            if (annotation != null) {
                throw new JsonbException(Messages.getMessage(MessageKeys.JSONB_TRANSIENT_WITH_OTHER_ANNOTATIONS));
            }
        }
    }

    /**
     * Searches for annotation, collects processed, to avoid StackOverflow.
     */
    @SuppressWarnings("unchecked")
    private <T extends Annotation> T findAnnotation(Annotation[] declaredAnnotations, Class<T> annotationClass, Set<Annotation> processed) {
        for (Annotation candidate : declaredAnnotations) {
            final Class<? extends Annotation> annType = candidate.annotationType();
            if (annType.equals(annotationClass)) {
                return (T) candidate;
            }
            processed.add(candidate);
            final List<Annotation> inheritedAnnotations = new ArrayList<>(Arrays.asList(annType.getDeclaredAnnotations()));
            inheritedAnnotations.removeAll(processed);
            if (inheritedAnnotations.size() > 0) {
                final T inherited = findAnnotation(inheritedAnnotations.toArray(new Annotation[inheritedAnnotations.size()]), annotationClass, processed);
                if (inherited != null) {
                    return inherited;
                }
            }
        }
        return null;
    }

    private <T extends Annotation> T getMethodAnnotation(Class<T> annotationClass, JsonbAnnotatedElement<Method> methodElement) {
        if (methodElement == null) {
            return null;
        }
        return findAnnotation(methodElement.getAnnotations(), annotationClass);
    }

    private <T extends Annotation> void collectFromInterfaces(Class<T> annotationClass, Class clazz, Map<Class<?>, T> collectedAnnotations) {

        for (Class<?> interfaceClass : clazz.getInterfaces()) {
            T annotation = findAnnotation(interfaceClass.getDeclaredAnnotations(), annotationClass);
            if (annotation != null) {
                collectedAnnotations.put(interfaceClass, annotation);
            }
            collectFromInterfaces(annotationClass, interfaceClass, collectedAnnotations);
        }
    }

    /**
     * Get class interfaces recursively.
     *
     * @param cls Class to process.
     * @return A list of all class interfaces.
     */
    public Set<Class<?>> collectInterfaces(Class<?> cls) {
        Set<Class<?>> collected = new LinkedHashSet<>();
        Queue<Class<?>> toScan = new LinkedList<>();
        toScan.addAll(Arrays.asList(cls.getInterfaces()));
        Class<?> nextIfc;
        while((nextIfc = toScan.poll()) != null) {
            collected.add(nextIfc);
            toScan.addAll(Arrays.asList(nextIfc.getInterfaces()));
        }
        return collected;
    }

    /**
     * Processes customizations.
     *
     * @param clsElement Element to process.
     * @return Populated {@link ClassCustomization} instance.
     */
    public ClassCustomization introspectCustomization(JsonbAnnotatedElement<Class<?>> clsElement) {
        final ClassCustomizationBuilder builder = new ClassCustomizationBuilder();
        builder.setNillable(isClassNillable(clsElement));
        builder.setDateFormatter(getJsonbDateFormat(clsElement));
        builder.setNumberFormatter(getJsonbNumberFormat(clsElement));
        builder.setCreator(getCreator(clsElement.getElement()));
        builder.setPropertyOrder(getPropertyOrder(clsElement));
        builder.setAdapterInfo(getAdapterBinding(clsElement));
        builder.setSerializerBinding(getSerializerBinding(clsElement));
        builder.setDeserializerBinding(getDeserializerBinding(clsElement));
        return builder.buildClassCustomization();
    }

    public Class<?> getImplementationClass(Property property) {
        Optional<ImplementationClass> annotationFromProperty = getAnnotationFromProperty(ImplementationClass.class, property);
        return annotationFromProperty.<Class<?>>map(ImplementationClass::value).orElse(null);
    }

    /**
     * Collect annotations of given class, its interfaces and the package.
     *
     * @param clazz Class to process.
     * @return Element with class and annotations.
     */
    public JsonbAnnotatedElement<Class<?>> collectAnnotations(Class<?> clazz) {
        JsonbAnnotatedElement<Class<?>> classElement = new JsonbAnnotatedElement<>(clazz);

        for (Class<?> ifc : collectInterfaces(clazz)) {
            addIfNotPresent(classElement, ifc.getDeclaredAnnotations());
        }

        if (!clazz.isPrimitive() && !clazz.isArray() && (clazz.getPackage() != null)) {
            addIfNotPresent(classElement, clazz.getPackage().getAnnotations());
        }
        return classElement;
    }

    private void addIfNotPresent(JsonbAnnotatedElement<?> element, Annotation... annotations) {
        for (Annotation annotation : annotations) {
            if (element.getAnnotation(annotation.annotationType()) == null) {
                element.putAnnotation(annotation);
            }
        }
    }
}
