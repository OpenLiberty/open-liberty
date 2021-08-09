/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.impl.core.converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.models.media.Schema;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotationMap;
import com.ibm.ws.microprofile.openapi.impl.model.media.SchemaImpl;

public class ModelConverterContextImpl implements ModelConverterContext {
    //private static final Logger LOGGER = LoggerFactory.getLogger(ModelConverterContextImpl.class);

    private final List<ModelConverter> converters;
    private final Map<String, Schema> modelByName;
    private final HashMap<Type, Schema> modelByType;
    private final Set<Type> processedTypes;

    public ModelConverterContextImpl(List<ModelConverter> converters) {
        this.converters = converters;
        modelByName = new TreeMap<String, Schema>();
        modelByType = new HashMap<Type, Schema>();
        processedTypes = new HashSet<Type>();
    }

    public ModelConverterContextImpl(ModelConverter converter) {
        this(new ArrayList<ModelConverter>());
        converters.add(converter);
    }

    @Override
    public Iterator<ModelConverter> getConverters() {
        return converters.iterator();
    }

    @Override
    public void defineModel(String name, Schema model) {
        defineModel(name, model, null, null);
    }

    @Override
    public void defineModel(String name, Schema model, Type type, String prevName) {
        //if (LOGGER.isTraceEnabled()) {
        //    LOGGER.trace(String.format("defineModel %s %s", name, model));
        //}
        modelByName.put(name, model);

        if (StringUtils.isNotBlank(prevName) && !prevName.equals(name)) {
            modelByName.remove(prevName);
        }

        if (type != null) {
            modelByType.put(type, model);
        }
    }

    public Map<String, Schema> getDefinedModels() {
        return Collections.unmodifiableMap(modelByName);
    }

    @Override
    public Schema resolve(Type type) {
        if (processedTypes.contains(type)) {
            return modelByType.get(type);
        } else {
            processedTypes.add(type);
        }
        //if (LOGGER.isDebugEnabled()) {
        //  LOGGER.debug(String.format("resolve %s", type));
        //}
        Iterator<ModelConverter> converters = this.getConverters();
        Schema resolved = null;
        if (converters.hasNext()) {
            ModelConverter converter = converters.next();
            //LOGGER.debug("trying extension " + converter);
            resolved = converter.resolve(type, this, converters);
        }
        if (resolved != null) {
            modelByType.put(type, resolved);

            SchemaImpl resolvedImpl = (SchemaImpl) resolved;
            if (resolvedImpl.getName() != null) {
                modelByName.put(resolvedImpl.getName(), resolved);
            }
        }

        return resolved;
    }

    @Override
    public Schema resolve(Type type, Annotation[] annotations) {
        //if (LOGGER.isDebugEnabled()) {
        //    LOGGER.debug(String.format("resolveProperty %s", type));
        //}
        Iterator<ModelConverter> converters = this.getConverters();
        if (converters.hasNext()) {
            ModelConverter converter = converters.next();
            return converter.resolve(type, this, annotations, converters);
        }
        return null;
    }

    @Override
    public Schema resolveAnnotatedType(Type type, List<Annotation> annotations, String elementName) {
        //if (LOGGER.isDebugEnabled()) {
        //    LOGGER.debug(String.format("resolveProperty %s", type));
        //}
        AnnotationMap map = new AnnotationMap();
        annotations.forEach(a -> map.add(a));
        Annotated annotated = new AnnotatedMember(null, map) {
            @Override
            public Annotated withAnnotations(AnnotationMap annotationMap) {
                return this;
            }

            @Override
            public Class<?> getDeclaringClass() {
                return null;
            }

            @Override
            public Member getMember() {
                return null;
            }

            @Override
            public void setValue(Object o, Object o1) throws UnsupportedOperationException, IllegalArgumentException {

            }

            @Override
            public Object getValue(Object o) throws UnsupportedOperationException, IllegalArgumentException {
                return null;
            }

            @Override
            public java.lang.reflect.AnnotatedElement getAnnotated() {
                return null;
            }

            @Override
            protected int getModifiers() {
                return 0;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public JavaType getType() {
                return null;
            }

            @Override
            public Class<?> getRawType() {
                return null;
            }

            @Override
            public boolean equals(Object o) {
                return false;
            }

            @Override
            public int hashCode() {
                return 0;
            }

            @Override
            public String toString() {
                return null;
            }
        };

        Iterator<ModelConverter> converters = this.getConverters();
        if (converters.hasNext()) {
            ModelConverter converter = converters.next();
            return converter.resolveAnnotatedType(type, annotated, elementName, this, converters);
        }
        return null;
    }
}
