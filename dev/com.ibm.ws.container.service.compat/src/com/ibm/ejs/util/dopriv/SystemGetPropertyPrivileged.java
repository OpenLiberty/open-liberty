/*******************************************************************************
 * Copyright (c) 2002, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.util.dopriv;

import java.security.PrivilegedAction;

/**
 * This class gets a system property while in privileged mode. Its purpose
 * is to eliminate the need to use an anonymous inner class in multiple modules
 * throughout the product, when the only privileged action required is to
 * get the value of a system property.
 */
public class SystemGetPropertyPrivileged implements PrivilegedAction<String> {
    private final String propertyName;
    private String propertyValue;
    private String propertyDefault = null;//d138969

    public SystemGetPropertyPrivileged(String propName) {
        propertyName = propName;
    }

    //d138969
    public SystemGetPropertyPrivileged(String propName, String propDefault) {
        propertyName = propName;
        propertyDefault = propDefault;
    }

    //d138969
    @Override
    public String run() {
        propertyValue = System.getProperty(propertyName, propertyDefault);//d138969
        return propertyValue;
    }

    public String getValue() {
        return propertyValue;
    }
} // SystemGetPropertyPrivileged

