/*******************************************************************************
 * Copyright (c) 2011,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.common;

import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/**
 * Type which encapsulates a JNDI environment reference.
 * 
 * The current implementation stores the name of attribute
 * which stores the reference, and stores the reference value
 * itself.
 * 
 * TODO: The attribute name is static to particular subtypes and
 * should be refactored as a subclass API.
 */
public class JNDIEnvironmentRefType
    extends DDParser.ElementContentParsable
    implements JNDIEnvironmentRef {

    private final String element_local_name;

    private String getLocalName() {
        return element_local_name;
    }
    
    //
    
    protected JNDIEnvironmentRefType(String element_local_name) {
        this.element_local_name = element_local_name;
    }

    //

    JNDINameType jndi_env_name = new JNDINameType();

    @Override
    public String getName() {
        return jndi_env_name.getValue();
    }

    //
    
    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ( getLocalName().equals(localName)) {
            parser.parse(jndi_env_name);
            return true;
        }
        return false;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        describeHead(diag);
        describeBody(diag);
        describeTail(diag);
    }
    
    public void describeHead(DDParser.Diagnostics diag) {
        diag.describe( getLocalName(), jndi_env_name );
    }

    public void describeBody(DDParser.Diagnostics diag) {
        // Nothing
    }

    public void describeTail(DDParser.Diagnostics diag) {
        // Nothing
    }
}
