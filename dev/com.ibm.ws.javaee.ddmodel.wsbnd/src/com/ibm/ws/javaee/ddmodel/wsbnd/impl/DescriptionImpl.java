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

import com.ibm.ws.javaee.dd.common.Description;

/**
 *
 */
public class DescriptionImpl implements Description {

    private final String value;
    private final String lang;

    /**
     * @param descriptionConfig
     */
    public DescriptionImpl(Map<String, Object> config) {
        lang = (String) config.get("lang");
        value = (String) config.get("value");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.javaee.dd.common.Description#getLang()
     */
    @Override
    public String getLang() {
        return lang;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.javaee.dd.common.Description#getValue()
     */
    @Override
    public String getValue() {
        return value;
    }

}
