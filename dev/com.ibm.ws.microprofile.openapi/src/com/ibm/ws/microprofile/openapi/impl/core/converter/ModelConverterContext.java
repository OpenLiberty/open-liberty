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
import java.util.List;

import org.eclipse.microprofile.openapi.models.media.Schema;

public interface ModelConverterContext {

    /**
     * needs to be called whenever a Model is defined which can be referenced from another
     * Model or Property
     *
     * @param name the name of the model
     * @param model the Model
     */
    void defineModel(String name, Schema model);

    /**
     * needs to be called whenever a Schema is defined which can be referenced from another
     * Model or Property
     *
     * @param name the name of the model
     * @param model the Model
     * @param type the Type
     * @param prevName the (optional) previous name
     */
    void defineModel(String name, Schema model, Type type, String prevName);

    /**
     * @param type The Schema
     * @return a Model representation of the Class. Any referenced models will be defined already.
     */
    Schema resolve(Type type);

    Schema resolve(Type type, Annotation[] annotations);

    Schema resolveAnnotatedType(Type type, List<Annotation> annotations, String elementName);

    /**
     * @return an Iterator of ModelConverters. This iterator is not reused
     */
    public Iterator<ModelConverter> getConverters();
}