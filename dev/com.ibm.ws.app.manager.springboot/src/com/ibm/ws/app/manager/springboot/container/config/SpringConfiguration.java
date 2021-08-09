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
package com.ibm.ws.app.manager.springboot.container.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public class SpringConfiguration {

    // below '*_in-spring_app' booleans used to WARN when a currently non-supported configuration is detected.
    private boolean compression_configured_in_spring_app = false;
    private boolean session_configured_in_spring_app = false;

    /**
     * @return the compression_configured_in_spring
     */
    public boolean isCompression_configured_in_spring_app() {
        return compression_configured_in_spring_app;
    }

    /**
     * @return the session_configured_in_spring
     */
    public boolean isSession_configured_in_spring_app() {
        return session_configured_in_spring_app;
    }

    /**
     * @param compression_configured_in_spring the compression_configured_in_spring to set
     */
    public void setCompression_configured_in_spring_app(boolean compression_enabled_in_spring) {
        this.compression_configured_in_spring_app = compression_enabled_in_spring;
    }

    /**
     * @param session_configured_in_spring the session_configured_in_spring to set
     */
    public void setSession_configured_in_spring_app(boolean session_configured_in_spring) {
        this.session_configured_in_spring_app = session_configured_in_spring;
    }

    private LinkedHashSet<SpringErrorPageData> errorPages = new LinkedHashSet<>();
    private final HashMap<String, String> mimeMappings = new HashMap<String, String>();

    public HashSet<SpringErrorPageData> getErrorPages() {
        return errorPages;
    }

    public void addErrorPage(SpringErrorPageData errorPage) {
        this.errorPages.add(errorPage);
    }

    public void setErrorPages(LinkedHashSet<SpringErrorPageData> errorPages) {
        this.errorPages = errorPages;
    }

    public void addMimeMapping(String extension, String type) {
        mimeMappings.put(extension, type);
    }

    public HashMap<String, String> getMimeMappings() {
        return mimeMappings;
    }

    @Override
    public String toString() {
        return String.join("",
                           "Error pages = [",
                           errorPages.stream().map(Object::toString).collect(Collectors.joining(", ")),
                           "]; Mime mappings = [",
                           mimeMappings.toString(),
                           "]; session_configured_in_spring = ",
                           Boolean.toString(session_configured_in_spring_app),
                           "; compression_enabled_in_spring = ",
                           Boolean.toString(compression_configured_in_spring_app));
    }
}
