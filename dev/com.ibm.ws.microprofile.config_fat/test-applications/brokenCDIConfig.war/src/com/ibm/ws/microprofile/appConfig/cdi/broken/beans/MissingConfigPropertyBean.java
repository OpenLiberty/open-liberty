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
package com.ibm.ws.microprofile.appConfig.cdi.broken.beans;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@RequestScoped
public class MissingConfigPropertyBean {

    @Inject
    @ConfigProperty
    String nonExistantKey;

    @Inject
    @ConfigProperty()
    String nullKey;

    @Inject
    @ConfigProperty(name = "")
    String emptyKey;

    /**
     * @return the nullKey
     */
    public String getnullKey() {
        return nullKey;
    }

    public String getemptyKey() {
        return emptyKey;
    }

    public String getnonExistantKey() {
        return nonExistantKey;
    }

}
