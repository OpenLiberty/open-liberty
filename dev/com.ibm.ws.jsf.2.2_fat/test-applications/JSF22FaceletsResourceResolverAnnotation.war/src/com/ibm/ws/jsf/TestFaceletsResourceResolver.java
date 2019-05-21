/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.jsf;

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
