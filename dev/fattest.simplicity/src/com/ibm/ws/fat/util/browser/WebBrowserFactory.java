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
package com.ibm.ws.fat.util.browser;

import java.io.File;

/**
 * Factory for WebBrowser instances. The default browser type is Http Client.
 * 
 * @author Tim Burns
 * 
 */
public class WebBrowserFactory {

    protected static WebBrowserFactory INSTANCE;

    /**
     * Convenience method for retrieving a globally shared factory
     * 
     * @return a globally shared factory
     */
    public static WebBrowserFactory getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WebBrowserFactory();
        }
        return INSTANCE;
    }

    protected WebBrowserType defaultType;

    /**
     * Primary constructor.
     */
    public WebBrowserFactory() {
        this.defaultType = WebBrowserType.HttpClient; // default to HttpClient
    }

    /**
     * Creates a new WebBrowser instance.
     * 
     * @param resultDirectory
     *            the directory where HTTP response bodies should be stored.
     *            null indicates that HTTP response bodies should not be stored.
     * @param type
     *            the type of WebBrowser you want to create. null indicates that
     *            the default type should be used.
     * @return a new WebBrowser instance.
     */
    public WebBrowser createWebBrowser(File resultDirectory, WebBrowserType type) {
        if (WebBrowserType.HttpClient.equals(type)) {
            return new HttpClientBrowser(resultDirectory);
        } else if (WebBrowserType.HttpUnit.equals(type)) {
            return new HttpUnitBrowser(resultDirectory);
        } else if (WebBrowserType.HttpUnit.equals(this.getDefaultType())) {
            return new HttpUnitBrowser(resultDirectory);
        } else {
            return new HttpClientBrowser(resultDirectory);
        }
    }

    /**
     * Creates a new WebBrowser instance. Convenience method for:<br>
     * <code>factory.createWebBrowser(resultDirectory, factory.getDefaultType());</code>
     * 
     * @param resultDirectory
     *            the directory where HTTP response bodies should be stored.
     *            null indicates that HTTP response bodies should not be stored.
     * @return a new WebBrowser instance.
     */
    public WebBrowser createWebBrowser(File resultDirectory) {
        return this.createWebBrowser(resultDirectory, this.getDefaultType());
    }

    /**
     * Creates a new WebBrowser instance. Convenience method for:<br>
     * <code>factory.createWebBrowser(null, type);</code>
     * 
     * @param type
     *            the type of WebBrowser you want to create. null indicates that
     *            the default type should be used.
     * @return a new WebBrowser instance.
     */
    public WebBrowser createWebBrowser(WebBrowserType type) {
        return this.createWebBrowser(null, type);
    }

    /**
     * Creates a new WebBrowser instance. Convenience method for:<br>
     * <code>factory.createWebBrowser(null, factory.getDefaultType());</code>
     * 
     * @return a new WebBrowser instance.
     */
    public WebBrowser createWebBrowser() {
        return this.createWebBrowser(null, this.getDefaultType());
    }

    /**
     * Describes the default type of WebBrowser that this instance creates.
     * 
     * @return the default type of WebBrowser that this instance creates.
     */
    public WebBrowserType getDefaultType() {
        return this.defaultType;
    }

    /**
     * Configures the default type of WebBrowser that this instance should
     * create.
     * 
     * @param type
     *            the default type of WebBrowser that this instance should
     *            create. null indicates that the current default type should be
     *            used.
     */
    public void setDefaultType(WebBrowserType type) {
        if (type != null) {
            this.defaultType = type;
        }
    }

}
