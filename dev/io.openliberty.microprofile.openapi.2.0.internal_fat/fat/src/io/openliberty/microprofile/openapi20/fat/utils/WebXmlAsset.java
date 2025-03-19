/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.microprofile.openapi20.fat.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jboss.shrinkwrap.api.asset.Asset;

public class WebXmlAsset implements Asset {

    private final String name;

    public WebXmlAsset(String name) {
        super();
        this.name = name;
    }

    @Override
    public InputStream openStream() {
        StringBuilder sb = new StringBuilder();
        // Build file
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<web-app xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"");
        sb.append(" version=\"4.0\">\n");

        sb.append("<module-name>");
        sb.append(name);
        sb.append("</module-name>\n");

        sb.append("</web-app>\n");

        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

}
