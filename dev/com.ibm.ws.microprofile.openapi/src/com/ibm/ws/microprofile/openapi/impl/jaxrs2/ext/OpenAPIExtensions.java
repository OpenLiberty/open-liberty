/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.openapi.impl.jaxrs2.ext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import com.ibm.ws.microprofile.openapi.impl.jaxrs2.DefaultParameterExtension;

public class OpenAPIExtensions {
    //private static Logger LOGGER = LoggerFactory.getLogger(OpenAPIExtensions.class);

    private static List<OpenAPIExtension> extensions = null;

    public static List<OpenAPIExtension> getExtensions() {
        return extensions;
    }

    public static void setExtensions(List<OpenAPIExtension> ext) {
        extensions = ext;
    }

    public static Iterator<OpenAPIExtension> chain() {
        return extensions.iterator();
    }

    static {
        extensions = new ArrayList<>();
        ServiceLoader<OpenAPIExtension> loader = ServiceLoader.load(OpenAPIExtension.class);
        for (OpenAPIExtension ext : loader) {
            //LOGGER.debug("adding extension " + ext);
            extensions.add(ext);
        }
        extensions.add(new DefaultParameterExtension());
    }
}