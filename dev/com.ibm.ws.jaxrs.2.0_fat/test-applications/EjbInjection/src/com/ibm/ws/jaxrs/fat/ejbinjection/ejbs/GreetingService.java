/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.jaxrs.fat.ejbinjection.ejbs;

import javax.ejb.Stateless;

/**
 * Simple EJB service defined via annotation that will be injected
 * into standard JAX-RS resources.
 */
@Stateless
public class GreetingService {

    public String greet(String name) {
        return "Hello from EJB service, " + name + "!";
    }

    public String farewell(String name) {
        return "Goodbye from EJB service, " + name + "!";
    }
}
