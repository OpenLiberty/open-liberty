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
package com.ibm.ws.jaxrs.fat.annotation.multipleapp;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/resource3")
public class MyResource3 {
    @GET
    public String get() {
        return "Hello world 3!";
    }
}
