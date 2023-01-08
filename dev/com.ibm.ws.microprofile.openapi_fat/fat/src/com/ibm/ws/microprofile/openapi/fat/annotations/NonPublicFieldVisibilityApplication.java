/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.microprofile.openapi.fat.annotations;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

@ApplicationPath("/")
@Path("/")
public class NonPublicFieldVisibilityApplication extends Application {

    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public NonPublicFieldVisibilityDataObject testGet() {
        return new NonPublicFieldVisibilityDataObject();
    }

    public static class NonPublicFieldVisibilityDataObject {

        private String privateWithGetters = "value";

        @SuppressWarnings("unused")
        private final String privateWithoutGetters = "value";

        protected String protectedWithoutGetters = "value";

        String packageScopedWithoutGetters = "value";

        public String publicWithoutGetters = "value";

        public String getPrivateWithGetters() {
            return privateWithGetters;
        }

        public void setPrivateWithGetters(String privateWithGetters) {
            this.privateWithGetters = privateWithGetters;
        }

        @SuppressWarnings("unused")
        private String getPrivateGetter() {
            return "value";
        }

        public String getPublicGetter() {
            return "value";
        }

        protected String getProtectedGetter() {
            return "value";
        }

        String getPackageScopedGetter() {
            return "value";
        }
    }

}
