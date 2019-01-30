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
package com.ibm.ws.jaxrs.fat.standard;

import java.util.HashSet;
import java.util.Set;

import com.ibm.ws.jaxrs.fat.standard.multipart.MultiPartResource;

public class Application extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> clazzes = new HashSet<Class<?>>();
        clazzes.add(FileResource.class);
        clazzes.add(MultiPartResource.class);
        return clazzes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> objs = new HashSet<Object>();
        objs.add(new BytesArrayResource());
        objs.add(new InputStreamResource());
        objs.add(new ReaderResource());
        objs.add(new StreamingOutputResource());
        objs.add(new MultiValuedMapResource());
        objs.add(new SourceResource());
        objs.add(new DataSourceResource());
        objs.add(new DSResource());
        objs.add(new StringResource());
        objs.add(new JAXBResource());
        return objs;
    }

}
