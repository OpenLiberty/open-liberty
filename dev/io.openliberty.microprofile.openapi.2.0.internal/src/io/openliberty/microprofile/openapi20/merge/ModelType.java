/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.merge;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.info.Contact;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.info.License;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Discriminator;
import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.XML;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.eclipse.microprofile.openapi.models.security.OAuthFlows;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.tags.Tag;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Tools for reflectively navigating OpenAPI 2.0 model objects
 */
@Trivial
public enum ModelType {
    OPENAPI(OpenAPI.class),
    INFO(Info.class),
    EXTERNALDOC(ExternalDocumentation.class),
    SERVER(Server.class),
    SECURITY_REQUIREMENT(SecurityRequirement.class),
    TAG(Tag.class),
    PATHS(Paths.class),
    COMPONENTS(Components.class),
    CONTACT(Contact.class),
    LICENSE(License.class),
    OPERATION(Operation.class),
    PATH_ITEM(PathItem.class),
    CALLBACK(Callback.class),
    EXAMPLE(Example.class),
    HEADER(Header.class),
    LINK(Link.class),
    CONTENT(Content.class),
    DISCRIMINATOR(Discriminator.class),
    ENCODING(Encoding.class),
    MEDIA_TYPE(MediaType.class),
    SCHEMA(Schema.class),
    XML(XML.class),
    PARAMETER(Parameter.class),
    REQUEST_BODY(RequestBody.class),
    API_RESPONSE(APIResponse.class),
    API_RESPONSES(APIResponses.class),
    OAUTH_FLOW(OAuthFlow.class),
    OAUTH_FLOWS(OAuthFlows.class),
    SECURITY_SCHEME(SecurityScheme.class),
    SERVER_VARIABLE(ServerVariable.class);

    /**
     * Find the model object which {@code clazz} implements
     * 
     * @param clazz a class
     * @return the corresponding model object, or {@code null} if clazz does not implement any of the model interfaces
     */
    public static Optional<ModelType> getModelObject(Class<?> clazz) {
        for (ModelType obj : values()) {
            if (obj.clazz.isAssignableFrom(clazz)) {
                return Optional.of(obj);
            }
        }
        return Optional.empty();
    }

    /**
     * Create a new instance of the model object using {@code OASFactory}
     * 
     * @return the new instance
     */
    public Object createInstance() {
        return OASFactory.createObject(clazz);
    }

    /**
     * Get the parameter descriptors for the model type
     * <p>
     * Each parameter represents a matching getter/setter pair on the model interface
     * 
     * @return the parameter descriptors for the model type
     */
    public List<ModelType.ModelParameter> getParameters() {
        return parameters;
    }
    
    /**
     * Test whether an object is an instance of this model type
     * 
     * @param object the object to test
     * @return {@code true} if {@code object} is an instance of the interface associated with this model type, {@code false} otherwise
     */
    public boolean isInstance(Object object) {
        return clazz.isInstance(object);
    }

    /**
     * Represents a getter/setter pair on a model type
     */
    public static class ModelParameter {
        private Method getter;
        private Method setter;
        private Type type;
    
        private ModelParameter(Method getter, Method setter, Type type) {
            super();
            this.getter = getter;
            this.setter = setter;
            this.type = type;
        }
    
        /**
         * Set this parameter on an instance to a value
         * 
         * @param instance the instance to have the parameter set
         * @param value the value to set the parameter to
         */
        public void set(Object instance, Object value) {
            try {
                setter.invoke(instance, value);
            } catch (Exception e) {
                throw new RuntimeException("Error invoking " + setter.getName() + " on " + instance, e);
            }
        }
    
        /**
         * Get the value of this parameter from {@code instance}
         * 
         * @param instance the instance to get the parameter for
         * @return the value of this parameter
         */
        public Object get(Object instance) {
            try {
                return getter.invoke(instance);
            } catch (Exception e) {
                throw new RuntimeException("Error invoking " + getter.getName() + " on " + instance, e);
            }
        }

        /**
         * Get the generic type of this parameter
         * 
         * @return the generic type of this parameter
         */
        public Type getType() {
            return type;
        }

        @Override
        public String toString() {
            return getter.getName();
        }
    }

    private List<ModelType.ModelParameter> parameters;
    private Class<? extends Constructible> clazz;

    private ModelType(Class<? extends Constructible> clazz) {
        parameters = getParameters(clazz);
        this.clazz = clazz;
    }

    /**
     * Create the parameters for a model interface
     * 
     * @param clazz the model interface
     * @return the list of parameters
     */
    private static List<ModelType.ModelParameter> getParameters(Class<?> clazz) {
        List<Class<?>> classes = new ArrayList<>();
        classes.add(clazz);
        classes.addAll(Arrays.asList(clazz.getInterfaces()));
        
        return classes.stream()
                      .flatMap(c -> Arrays.stream(c.getDeclaredMethods())) // Find all the methods
                      .filter(m -> m.getName().startsWith("get")) // which are getters
                      .filter(m -> m.getParameterCount() == 0) // with no parameters
                      .filter(m -> Modifier.isPublic(m.getModifiers())) // and are public
                      .flatMap(m -> getDescriptor(clazz, m)) // and create a descriptor for each
                      .collect(toList());
    }

    /**
     * Create a parameter for a getter on a model interface
     * 
     * @param clazz the model interface
     * @param getter the getter method
     * @return a stream containing the parameter or an empty stream if {@code clazz} has no corresponding setter
     */
    private static Stream<ModelType.ModelParameter> getDescriptor(Class<?> clazz, Method getter) {
        String setterName = getter.getName().replaceFirst("get", "set");
        try {
            Method setter = clazz.getMethod(setterName, getter.getReturnType());
            ModelType.ModelParameter result = new ModelParameter(getter, setter, getter.getGenericReturnType());
            return Stream.of(result);
        } catch (NoSuchMethodException e) {
            return Stream.empty();
        }
    }
}