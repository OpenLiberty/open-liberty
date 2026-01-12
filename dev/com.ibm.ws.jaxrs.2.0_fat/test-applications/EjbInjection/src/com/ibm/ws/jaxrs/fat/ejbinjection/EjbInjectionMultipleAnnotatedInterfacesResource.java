/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.ejbinjection;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ws.rs.Path;

import com.ibm.ws.jaxrs.fat.ejbinjection.interfaces.annotated.FarewellEjbAnnotatedInterface;
import com.ibm.ws.jaxrs.fat.ejbinjection.interfaces.annotated.GreetEJBAnnotatedInterface;

@Stateless
@Local({ GreetEJBAnnotatedInterface.class, FarewellEjbAnnotatedInterface.class })
@Path("multipleannotatedinterfaces")
public class EjbInjectionMultipleAnnotatedInterfacesResource implements GreetEJBAnnotatedInterface, FarewellEjbAnnotatedInterface {

    @Resource
    SessionContext ctx;

    @Override
    public String hello() {
        if (ctx == null) {
            return "ctx is null";
        }
        return "Hello, World!";
    }

    @Override
    public String goodbye() {
        if (ctx == null) {
            return "ctx is null";
        }
        return "Goodbye, World!";
    }
}
