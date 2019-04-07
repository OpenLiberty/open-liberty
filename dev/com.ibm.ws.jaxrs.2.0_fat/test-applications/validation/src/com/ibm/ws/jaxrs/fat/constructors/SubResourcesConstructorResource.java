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

import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/subresource")
public class SubResourcesConstructorResource {

    @Path("emptypackage")
    public SubResourcePackageConstructor packageEmptyConstructor() {
        return new SubResourcePackageConstructor();
    }

    @Path("stringpackage")
    public SubResourcePackageConstructor packageStringConstructor() {
        return new SubResourcePackageConstructor("packageString");
    }

    @Path("emptypublic")
    public SubResourcePublicConstructor publicEmptyConstructor() {
        return new SubResourcePublicConstructor();
    }

    @Path("stringpublic")
    public SubResourcePublicConstructor publicStringConstructor(@QueryParam("q") String s) {
        return new SubResourcePublicConstructor(s);
    }

    @Path("emptyprivate")
    public Object privateEmptyConstructor() {
        return SubResourcePrivateConstructor.getPrivateInstance(null);
    }

    @Path("stringprivate")
    public Object privateStringConstructor(@QueryParam("q") String s) {
        return SubResourcePrivateConstructor.getPrivateInstance(s);
    }

    @Path("sub")
    public Object subconstructor(@QueryParam("which") String which) {
        if ("package".equals(which)) {
            return new SubResourcePackageConstructor();
        } else if ("public".equals(which)) {
            return new SubResourcePublicConstructor();
        }
        return null;
    }

}
