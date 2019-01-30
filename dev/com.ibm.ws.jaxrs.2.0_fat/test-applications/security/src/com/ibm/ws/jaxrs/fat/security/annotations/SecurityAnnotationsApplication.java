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
package com.ibm.ws.jaxrs.fat.security.annotations;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class SecurityAnnotationsApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(ClassLevelDenyAll.class);
        classes.add(MethodLevelDenyAll.class);
        classes.add(ClassLevelPermitAll.class);
        classes.add(MethodLevelPermitAll.class);
        classes.add(NoSecurityAnnotations.class);
        classes.add(ClassLevelRolesAllowed.class);
        classes.add(MethodLevelRolesAllowed.class);
        classes.add(ClassLevelAllAnnotations.class);
        classes.add(MethodLevelAllAnnotations.class);
        return classes;
    }

}
