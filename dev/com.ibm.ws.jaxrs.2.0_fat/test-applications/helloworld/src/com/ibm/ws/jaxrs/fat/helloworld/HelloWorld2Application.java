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
package com.ibm.ws.jaxrs.fat.helloworld;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * javax.ws.rs.core.Application subclass for configuring JAX-RS app.
 * This is used specifically to test defect 171486, an issue found in CTS.
 * From javax.ws.rs.ApplicationPath Javadoc:
 * The supplied value is automatically percent encoded to conform to the path
 * production of RFC 3986 section 3.3. Note that percent encoded values are
 * allowed in the value, an implementation will recognize such values and will
 * not double encode the '%' character.
 */
@ApplicationPath("apppathrest%21")
public class HelloWorld2Application extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(HelloWorldResource.class);
        return classes;
    }
}
