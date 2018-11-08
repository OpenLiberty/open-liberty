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

import org.eclipse.yasson.internal.components.AbstractComponentBinding;
import org.eclipse.yasson.internal.components.AdapterBinding;
import org.eclipse.yasson.internal.components.ComponentBindings;
import org.eclipse.yasson.internal.components.DeserializerBinding;
import org.eclipse.yasson.internal.components.SerializerBinding;
import org.eclipse.yasson.internal.model.customization.ComponentBoundCustomization;

import javax.json.bind.JsonbConfig;
import javax.json.bind.adapter.JsonbAdapter;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.bind.serializer.JsonbSerializer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Searches for a registered components or Serializer for a given type.
 *
 * @author Roman Grigoriadi
 */
public class ComponentMatcher {

    private final JsonbContext jsonbContext;

    /**
     * Supplier for component binging.
     * @param <T> component binding class
     */
    private interface ComponentSupplier<T extends AbstractComponentBinding> {

        T getComponent(ComponentBindings componentBindings);
    }

    private final ConcurrentMap<Type, ComponentBindings> userComponents;

    /**
     * Create component matcher.
     * @param context mandatory
     */
    ComponentMatcher(JsonbContext context) {
        Objects.requireNonNull(context);
        this.jsonbContext = context;
        userComponents = new ConcurrentHashMap<>();
        init();
    }

    /**
     * Called during context creation, introspecting user components provided with JsonbConfig.
     */
    void init() {
        final JsonbSerializer<?>[] serializers = (JsonbSerializer<?>[])jsonbContext.getConfig().getProperty(JsonbConfig.SERIALIZERS).orElseGet(()->new JsonbSerializer<?>[]{});
        for (JsonbSerializer serializer : serializers) {
            SerializerBinding serializerBinding = introspectSerializerBinding(serializer.getClass(), serializer);
            addSerializer(serializerBinding.getBindingType(), serializerBinding);
        }
        final JsonbDeserializer<?>[] deserializers = (JsonbDeserializer<?>[])jsonbContext.getConfig().getProperty(JsonbConfig.DESERIALIZERS).orElseGet(()->new JsonbDeserializer<?>[]{});
        for (JsonbDeserializer deserializer : deserializers) {
            DeserializerBinding deserializerBinding = introspectDeserializerBinding(deserializer.getClass(), deserializer);
            addDeserializer(deserializerBinding.getBindingType(), deserializerBinding);
        }

        final JsonbAdapter<?, ?>[] adapters = (JsonbAdapter<?, ?>[]) jsonbContext.getConfig().getProperty(JsonbConfig.ADAPTERS).orElseGet(()->new JsonbAdapter<?, ?>[]{});
        for (JsonbAdapter<?, ?> adapter : adapters) {
            AdapterBinding adapterBinding = introspectAdapterBinding(adapter.getClass(), adapter);
            addAdapter(adapterBinding.getBindingType(), adapterBinding);
        }
    }

    private ComponentBindings getBindingInfo(Type type) {
        return userComponents.compute(type, (type1, bindingInfo) -> bindingInfo != null ? bindingInfo : new ComponentBindings(type1));
    }

    private void addSerializer(Type bindingType, SerializerBinding serializer) {
        userComponents.computeIfPresent(bindingType, (type, bindings) -> {
            if (bindings.getSerializer() != null) {
                return bindings;
            }
            registerGeneric(bindingType);
            return new ComponentBindings(bindingType, serializer, bindings.getDeserializer(), bindings.getAdapterInfo());
        });
    }

    private void addDeserializer(Type bindingType, DeserializerBinding deserializer) {
        userComponents.computeIfPresent(bindingType, (type, bindings) -> {
            if (bindings.getDeserializer() != null) {
                return bindings;
            }
            registerGeneric(bindingType);
            return new ComponentBindings(bindingType, bindings.getSerializer(), deserializer, bindings.getAdapterInfo());
        });
    }

    private void addAdapter(Type bindingType, AdapterBinding adapter) {
        userComponents.computeIfPresent(bindingType, (type, bindings) -> {
            if (bindings.getAdapterInfo() != null) {
                return bindings;
            }
            registerGeneric(bindingType);
            return new ComponentBindings(bindingType, bindings.getSerializer(), bindings.getDeserializer(), adapter);
        });
    }

    /**
     * If type is not parametrized runtime component resolution doesn't has to happen.
     *
     * @param bindingType component binding type
     */
    private void registerGeneric(Type bindingType) {
        if (bindingType instanceof ParameterizedType && !jsonbContext.genericComponentsPresent()) {
            jsonbContext.registerGenericComponentFlag();
        }
    }

    /**
     * Lookup serializer binding for a given property runtime type.
     * @param propertyRuntimeType runtime type of a property
     * @param customization with component info
     * @return serializer optional
     */
    @SuppressWarnings("unchecked")
    public Optional<SerializerBinding<?>> getSerializerBinding(Type propertyRuntimeType, ComponentBoundCustomization customization) {

        if (customization == null || customization.getSerializerBinding() == null) {
            return searchComponentBinding(propertyRuntimeType, ComponentBindings::getSerializer);
        }
        return getComponentBinding(propertyRuntimeType, customization.getSerializerBinding());
    }

    /**
     * Lookup deserializer binding for a given property runtime type.
     * @param propertyRuntimeType runtime type of a property
     * @param customization customization with component info
     * @return serializer optional
     */
    @SuppressWarnings("unchecked")
    public Optional<DeserializerBinding<?>> getDeserializerBinding(Type propertyRuntimeType, ComponentBoundCustomization customization) {
        if (customization == null || customization.getDeserializerBinding() == null) {
            return searchComponentBinding(propertyRuntimeType, ComponentBindings::getDeserializer);
        }
        return Optional.of(customization.getDeserializerBinding());
    }

    /**
     * Get components from property model (if declared by annotation and runtime type matches),
     * or return components searched by runtime type
     *
     * @param propertyRuntimeType runtime type not null
     * @param customization customization with component info
     * @return components info if present
     */
    public Optional<AdapterBinding> getAdapterBinding(Type propertyRuntimeType, ComponentBoundCustomization customization) {
        if (customization == null || customization.getAdapterBinding() == null) {
            return searchComponentBinding(propertyRuntimeType, ComponentBindings::getAdapterInfo);
        }
        return getComponentBinding(propertyRuntimeType, customization.getAdapterBinding());
    }

    private <T extends AbstractComponentBinding> Optional<T> getComponentBinding(Type propertyRuntimeType, T componentBinding) {
        //need runtime check, ParameterizedType property may have generic components assigned which is not compatible
        //for given runtime type
        if (matches(propertyRuntimeType, componentBinding.getBindingType())) {
            return Optional.of(componentBinding);
        }
        return Optional.empty();
    }

    private <T extends AbstractComponentBinding> Optional<T> searchComponentBinding(Type runtimeType, ComponentSupplier<T> supplier) {
        for (ComponentBindings componentBindings : userComponents.values()) {
            final T component = supplier.getComponent(componentBindings);
            if (component != null && matches(runtimeType, componentBindings.getBindingType())) {
                return Optional.of(component);
            }
        }
        return Optional.empty();
    }

    private boolean matches(Type runtimeType, Type componentBindingType) {
        if (componentBindingType.equals(runtimeType)) {
            return true;
        }

        if (componentBindingType instanceof Class && runtimeType instanceof Class) {
            return ((Class<?>) componentBindingType).isAssignableFrom((Class) runtimeType);
        }

        //don't try to runtime generic scan if not needed
        if (!jsonbContext.genericComponentsPresent()) {
            return false;
        }
        return runtimeType instanceof ParameterizedType && componentBindingType instanceof ParameterizedType &&
                ReflectionUtils.getRawType(componentBindingType).isAssignableFrom(ReflectionUtils.getRawType(runtimeType)) &&
                matchTypeArguments((ParameterizedType) runtimeType, (ParameterizedType) componentBindingType);
    }

    /**
     * If runtimeType to adapt is a ParametrizedType, check all type args to match against components args.
     */
    private boolean matchTypeArguments(ParameterizedType requiredType, ParameterizedType componentBound) {
        final Type[] requiredTypeArguments = requiredType.getActualTypeArguments();
        final Type[] adapterBoundTypeArguments = componentBound.getActualTypeArguments();
        if (requiredTypeArguments.length != adapterBoundTypeArguments.length) {
            return false;
        }
        for(int i = 0; i< requiredTypeArguments.length; i++) {
            Type adapterTypeArgument = adapterBoundTypeArguments[i];
            if (!requiredTypeArguments[i].equals(adapterTypeArgument)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Introspect components generic information and put resolved types into metadata wrapper.
     *
     * @param adapterClass class of an components
     * @param instance components instance
     * @return introspected info with resolved typevar types.
     */
    AdapterBinding introspectAdapterBinding(Class<? extends JsonbAdapter> adapterClass, JsonbAdapter instance) {
        final ParameterizedType adapterRuntimeType = ReflectionUtils.findParameterizedType(adapterClass, JsonbAdapter.class);
        final Type[] adapterTypeArguments = adapterRuntimeType.getActualTypeArguments();
        Type adaptFromType = resolveTypeArg(adapterTypeArguments[0], adapterClass);
        Type adaptToType = resolveTypeArg(adapterTypeArguments[1], adapterClass);
        final ComponentBindings componentBindings = getBindingInfo(adaptFromType);
        if (componentBindings.getAdapterInfo() != null && componentBindings.getAdapterInfo().getAdapter().getClass().equals(adapterClass)) {
            return componentBindings.getAdapterInfo();
        }
        JsonbAdapter newAdapter = instance != null ? instance : jsonbContext.getComponentInstanceCreator().getOrCreateComponent(adapterClass);
        return new AdapterBinding(adaptFromType, adaptToType, newAdapter);
    }

    /**
     * If an instance of deserializerClass is present in context and is bound for same type, return that instance.
     * Otherwise create new instance and set it to context.
     *
     * @param deserializerClass class of deserializer
     * @param instance instance to use if not cached already
     * @return wrapper used in property models
     */
    @SuppressWarnings("unchecked")
    DeserializerBinding introspectDeserializerBinding(Class<? extends JsonbDeserializer> deserializerClass, JsonbDeserializer instance) {
        final ParameterizedType deserializerRuntimeType = ReflectionUtils.findParameterizedType(deserializerClass, JsonbDeserializer.class);
        Type deserializerBindingType = resolveTypeArg(deserializerRuntimeType.getActualTypeArguments()[0], deserializerClass);
        final ComponentBindings componentBindings = getBindingInfo(deserializerBindingType);
        if (componentBindings.getDeserializer() != null && componentBindings.getDeserializer().getClass().equals(deserializerClass)) {
            return componentBindings.getDeserializer();
        } else {
            JsonbDeserializer deserializer = instance != null ? instance : jsonbContext.getComponentInstanceCreator()
                    .getOrCreateComponent(deserializerClass);
            return new DeserializerBinding(deserializerBindingType, deserializer);
        }
    }

    /**
     * If an instance of serializerClass is present in context and is bound for same type, return that instance.
     * Otherwise create new instance and set it to context.
     *
     * @param serializerClass class of deserializer
     * @param instance instance to use if not cached
     * @return wrapper used in property models
     */
    @SuppressWarnings("unchecked")
    SerializerBinding introspectSerializerBinding(Class<? extends JsonbSerializer> serializerClass, JsonbSerializer instance) {
        final ParameterizedType serializerRuntimeType = ReflectionUtils.findParameterizedType(serializerClass, JsonbSerializer.class);
        Type serBindingType = resolveTypeArg(serializerRuntimeType.getActualTypeArguments()[0], serializerClass.getClass());
        final ComponentBindings componentBindings = getBindingInfo(serBindingType);
        if (componentBindings.getSerializer() != null && componentBindings.getSerializer().getClass().equals(serializerClass)) {
            return componentBindings.getSerializer();
        } else {
            JsonbSerializer serializer = instance != null ? instance : jsonbContext.getComponentInstanceCreator()
                    .getOrCreateComponent(serializerClass);
            return new SerializerBinding(serBindingType, serializer);
        }

    }


    private Type resolveTypeArg(Type adapterTypeArg, Type adapterType) {
        if(adapterTypeArg instanceof ParameterizedType) {
            return ReflectionUtils.resolveTypeArguments((ParameterizedType) adapterTypeArg, adapterType);
        } else if (adapterTypeArg instanceof TypeVariable) {
            return ReflectionUtils.resolveItemVariableType(new RuntimeTypeHolder(null, adapterType), (TypeVariable<?>) adapterTypeArg);
        } else {
            return adapterTypeArg;
        }
    }

}
