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
package com.ibm.ws.microprofile.openapi.impl.core.jackson;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

//import io.swagger.annotations.ApiModel;

import com.fasterxml.jackson.databind.JavaType;
import com.ibm.ws.microprofile.openapi.impl.core.util.AnnotationsUtils;
import com.ibm.ws.microprofile.openapi.impl.core.util.PrimitiveType;

/**
 * Helper class used for converting well-known (property) types into
 * Swagger type names.
 */
public class TypeNameResolver {
    public final static TypeNameResolver std = new TypeNameResolver();

    protected TypeNameResolver() {}

    public String nameForType(JavaType type, Options... options) {
        return nameForType(type, options.length == 0 ? Collections.<Options> emptySet() : EnumSet.copyOf(Arrays.asList(options)));
    }

    public String nameForType(JavaType type, Set<Options> options) {
        if (type.hasGenericTypes()) {
            return nameForGenericType(type, options);
        }
        final String name = findStdName(type);
        return (name == null) ? nameForClass(type, options) : name;
    }

    protected String nameForClass(JavaType type, Set<Options> options) {
        return nameForClass(type.getRawClass(), options);
    }

    protected String nameForClass(Class<?> cls, Set<Options> options) {
        if (options.contains(Options.SKIP_API_MODEL)) {
            return cls.getSimpleName();
        }

        org.eclipse.microprofile.openapi.annotations.media.Schema mp = AnnotationsUtils.getSchemaAnnotation(cls);

        final String modelName = mp == null ? null : StringUtils.trimToNull(mp.name());
        return modelName == null ? cls.getSimpleName() : modelName;
    }

    protected String nameForGenericType(JavaType type, Set<Options> options) {
        final StringBuilder generic = new StringBuilder(nameForClass(type, options));
        final int count = type.containedTypeCount();
        for (int i = 0; i < count; ++i) {
            final JavaType arg = type.containedType(i);
            final String argName = PrimitiveType.fromType(arg) != null ? nameForClass(arg, options) : nameForType(arg, options);
            generic.append(WordUtils.capitalize(argName));
        }
        return generic.toString();
    }

    protected String findStdName(JavaType type) {
        return PrimitiveType.getCommonName(type);
    }

    public enum Options {
        SKIP_API_MODEL;
    }
}
