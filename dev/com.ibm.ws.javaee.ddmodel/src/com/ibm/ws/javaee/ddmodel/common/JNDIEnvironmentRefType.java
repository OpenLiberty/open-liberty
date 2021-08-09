/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.common;

import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/**
 *
 */
public class JNDIEnvironmentRefType extends DDParser.ElementContentParsable implements JNDIEnvironmentRef {

    @Override
    public String getName() {
        return jndi_env_name.getValue();
    }

    JNDINameType jndi_env_name = new JNDINameType();

    private final String element_local_name;

    protected JNDIEnvironmentRefType(String element_local_name) {
        this.element_local_name = element_local_name;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (element_local_name.equals(localName)) {
            parser.parse(jndi_env_name);
            return true;
        }
        return false;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describe(element_local_name, jndi_env_name);
    }
}
