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
package com.ibm.ws.javaee.ddmodel.wsbnd.impl;

import java.util.Map;

import com.ibm.ws.javaee.dd.common.DisplayName;

/**
 *
 */
public class DisplayNameImpl implements DisplayName {

    private final String value;
    private final String lang;

    /**
     * @param displayNameConfig
     */
    public DisplayNameImpl(Map<String, Object> config) {
        lang = (String) config.get("lang");
        value = (String) config.get("value");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.javaee.dd.common.DisplayName#getLang()
     */
    @Override
    public String getLang() {
        return lang;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.javaee.dd.common.DisplayName#getValue()
     */
    @Override
    public String getValue() {
        return value;
    }

}
