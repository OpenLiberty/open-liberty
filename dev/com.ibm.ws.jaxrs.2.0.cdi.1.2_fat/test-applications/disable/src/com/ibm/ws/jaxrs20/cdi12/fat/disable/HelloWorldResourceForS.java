/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cdi12.fat.disable;

import javax.ws.rs.Path;

@Path("/helloworlds")
public class HelloWorldResourceForS extends HelloWorldResource {
    private final String type = "Singleton";

    public HelloWorldResourceForS() {
        this.setType(type);
    }
}