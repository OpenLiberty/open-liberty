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

public class SubResourcePrivateConstructor {

    final private String whichConstructor;

    private SubResourcePrivateConstructor() {
        whichConstructor = "private";
    }

    private SubResourcePrivateConstructor(String s) {
        whichConstructor = s;
    }

    public static Object getPrivateInstance(String query) {
        if (query == null) {
            return new SubResourcePrivateConstructor();
        }
        return new SubResourcePrivateConstructor(query);
    }

    @GET
    public String info() {
        return whichConstructor;
    }

    @Path("other")
    public SubResourcePackageConstructor other() {
        return new SubResourcePackageConstructor("subpackage");
    }
}
