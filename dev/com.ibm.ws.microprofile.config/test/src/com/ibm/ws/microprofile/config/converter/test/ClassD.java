/*******************************************************************************

    Copyright (c) 2017 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
    Contributors:

    IBM Corporation - initial API and implementation

*******************************************************************************/

package com.ibm.ws.microprofile.config.converter.test;

/**
 * A simple Class
 */
public class ClassD {

    private final String value;

    public ClassD(String blob) {
        value = blob;
    }

    public String getValue() {
        return value;
    }

}
