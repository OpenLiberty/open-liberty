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
import java.lang.reflect.Type;
import java.util.Iterator;

import org.eclipse.microprofile.openapi.models.media.Schema;

import com.fasterxml.jackson.databind.introspect.Annotated;

public interface ModelConverter {

    /**
     * @param type
     * @param context
     * @param annotations to consider when resolving the property
     * @param chain the chain of model converters to try if this implementation cannot process
     * @return null if this ModelConverter cannot convert the given Type
     */
    public Schema resolve(Type type,
                          ModelConverterContext context,
                          Annotation[] annotations,
                          Iterator<ModelConverter> chain);

    /**
     * @param type
     * @param context
     * @param chain the chain of model converters to try if this implementation cannot process
     * @return null if this ModelConverter cannot convert the given Type
     */
    public Schema resolve(Type type, ModelConverterContext context, Iterator<ModelConverter> chain);

    /**
     * @param type
     * @param member
     * @param elementName
     * @param context
     * @param chain the chain of model converters to try if this implementation cannot process
     * @return null if this ModelConverter cannot convert the given Type
     */
    public Schema resolveAnnotatedType(Type type,
                                       Annotated member,
                                       String elementName,
                                       ModelConverterContext context,
                                       Iterator<ModelConverter> chain);

}