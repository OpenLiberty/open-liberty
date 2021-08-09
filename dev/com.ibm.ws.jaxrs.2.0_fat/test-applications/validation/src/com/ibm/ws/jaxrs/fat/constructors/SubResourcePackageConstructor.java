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
package com.ibm.ws.jaxrs.fat.constructors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

public class SubResourcePackageConstructor {

    final private String whichConstructor;

    SubResourcePackageConstructor() {
        whichConstructor = "package";
    }

    SubResourcePackageConstructor(String s) {
        whichConstructor = s;
    }

    @GET
    public String getInfo() {
        return whichConstructor;
    }

    @Path("/other")
    public SubResourcePublicConstructor other() {
        return new SubResourcePublicConstructor();
    }
}
