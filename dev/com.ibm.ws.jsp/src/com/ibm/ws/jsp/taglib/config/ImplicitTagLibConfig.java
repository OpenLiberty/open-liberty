/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.taglib.config;

public class ImplicitTagLibConfig {
    private String uri = null;
    private String prefix = null;
    private String location = null;
    
    public ImplicitTagLibConfig(String uri, String prefix, String location) {
        this.uri = uri;
        this.prefix = prefix;
        this.location = location;    
    }
    
    public String getLocation() {
        return location;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getUri() {
        return uri;
    }
}
