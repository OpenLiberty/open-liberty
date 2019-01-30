/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.params;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.ws.jaxrs.fat.params.form.FormParamResource;
import com.ibm.ws.jaxrs.fat.params.header.HeaderParamDefaultResource;
import com.ibm.ws.jaxrs.fat.params.header.HeaderParamExceptionResource;
import com.ibm.ws.jaxrs.fat.params.query.QueryParamsExceptionResource;

/**
 * Parameters annotation test application.
 */
public class Application extends javax.ws.rs.core.Application {

    Set<Class<?>> classes = new HashSet<Class<?>>();

    public Application() {
        classes = new HashSet<Class<?>>();
        classes.add(HeaderParamResource.class);
        classes.add(MatrixParamResource.class);
        classes.add(QueryParamResource.class);
        classes.add(EncodingParamResource.class);
        classes.add(AutoDecodeParamResource.class);
        classes.add(DefaultValueResource.class);
        classes.add(CookieParamResource.class);

        classes.add(HeaderParamDefaultResource.class);
        classes.add(HeaderParamExceptionResource.class);

        classes.add(QueryParamsExceptionResource.class);

        classes.add(FormParamResource.class);

        classes.add(QueryParamNotSetResource.class);
        classes.add(MatrixParamNotSetResource.class);

        classes.add(PathSegmentResource.class);

        classes.add(DiffCaptureVariablesParamsResource.class);

        classes = Collections.unmodifiableSet(classes);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }
}
