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

package com.ibm.ws.microprofile.openapi.impl.jaxrs2.ext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.Operation;

import com.ibm.ws.microprofile.openapi.impl.jaxrs2.ResolvedParameter;

public interface OpenAPIExtension {

    String extractOperationMethod(Operation apiOperation, Method method, Iterator<OpenAPIExtension> chain);

    ResolvedParameter extractParameters(List<Annotation> annotations, Type type, Set<Type> typesToSkip, Components components,
                                        javax.ws.rs.Consumes classConsumes, javax.ws.rs.Consumes methodConsumes, boolean includeRequestBody, Iterator<OpenAPIExtension> chain);

    /**
     * Decorates operation with additional vendor based extensions.
     *
     * @param operation the operation, build from swagger definition
     * @param method the method for additional scan
     * @param chain the chain with swagger extensions to process
     */
    void decorateOperation(Operation operation, Method method, Iterator<OpenAPIExtension> chain);
}
