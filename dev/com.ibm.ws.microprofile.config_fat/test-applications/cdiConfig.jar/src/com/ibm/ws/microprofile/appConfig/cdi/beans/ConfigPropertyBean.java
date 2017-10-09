/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.cdi.beans;

import java.net.URL;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@RequestScoped
public class ConfigPropertyBean {

    @Inject
    @ConfigProperty()
    String nullKey;

    @Inject
    @ConfigProperty(name = "")
    String emptyKey;

    @Inject
    @ConfigProperty
    String defaultKey;

    @Inject
    @ConfigProperty(name = "URL_KEY", defaultValue = "http://www.default.com")
    URL URL_KEY;

    @Inject
    @ConfigProperty(name = "FAKE_URL_KEY", defaultValue = "http://www.default.com")
    URL DEFAULT_URL_KEY;

    /**
     * @return the nullKey
     */
    public String getnullKey() {
        return nullKey;
    }

    public String getemptyKey() {
        return emptyKey;
    }

    public String getdefaultKey() {
        return defaultKey;
    }

    public URL getURL_KEY() {
        return URL_KEY;
    }

    public URL getDEFAULT_URL_KEY() {
        return DEFAULT_URL_KEY;
    }
}
