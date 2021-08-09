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
package com.ibm.ws.jaxrs.fat.exceptionmappers.nullconditions;

import java.util.HashSet;
import java.util.Set;

public class Application extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> clazzes = new HashSet<Class<?>>();
        clazzes.add(GuestbookNullExceptionMapper.class);
        clazzes.add(GuestbookResource.class);
        clazzes.add(GuestbookThrowExceptionMapper.class);
        clazzes.add(GuestbookRuntimeExceptionMapper.class);
        clazzes.add(GuestbookThrowableMapper.class);
        return clazzes;
    }

}
