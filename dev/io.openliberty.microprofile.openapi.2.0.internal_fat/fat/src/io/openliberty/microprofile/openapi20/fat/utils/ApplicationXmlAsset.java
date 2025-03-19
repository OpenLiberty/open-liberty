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
import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.Asset;

public class ApplicationXmlAsset implements Asset {

    private final String name;
    private List<ModuleRecord> webModules = new ArrayList<>();

    public ApplicationXmlAsset(String name) {
        this.name = name;
    }

    @Override
    public InputStream openStream() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<application xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"");
        sb.append(" version=\"8\">\n");

        sb.append("<application-name>");
        sb.append(name);
        sb.append("</application-name>\n");

        for (ModuleRecord webModule : webModules) {
            sb.append("<module><web>\n");
            sb.append("<web-uri>");
            sb.append(webModule.archiveName);
            sb.append("</web-uri>\n");
            sb.append("<context-root>");
            sb.append(webModule.contextRoot);
            sb.append("</context-root>\n");
            sb.append("</web></module>\n");
        }

        sb.append("</application>\n");

        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    public ApplicationXmlAsset withWebModule(Archive<?> archive, String contextRoot) {
        webModules.add(new ModuleRecord(archive.getName(), contextRoot));
        return this;
    }

    private static class ModuleRecord {
        private String archiveName;
        private String contextRoot;

        public ModuleRecord(String archiveName, String contextRoot) {
            this.archiveName = archiveName;
            this.contextRoot = contextRoot;
        }
    }

}
