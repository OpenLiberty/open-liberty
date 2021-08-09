/*
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.resourceresolver;

import java.net.URL;

import javax.faces.view.facelets.FaceletsResourceResolver;
import javax.faces.view.facelets.ResourceResolver;

/**
 * Ensure that a class annotated with FaceletsResourceResolver is scanned and found by the runtime and
 * used as the FaceletsResourceResolver.
 */
@FaceletsResourceResolver
public class TestFaceletsResourceResolver extends ResourceResolver {

    private final ResourceResolver parent;

    public TestFaceletsResourceResolver(ResourceResolver parent) {
        this.parent = parent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.faces.view.facelets.ResourceResolver#resolveUrl(java.lang.String)
     */
    @Override
    public URL resolveUrl(String path) {
        System.out.println("FaceletsResourceResolver annotation worked, using custom ResourceResolver");
        URL url = parent.resolveUrl(path);

        return url;
    }

}
