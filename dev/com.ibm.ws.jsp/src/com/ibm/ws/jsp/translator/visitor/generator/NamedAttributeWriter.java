/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.translator.visitor.generator;

public class NamedAttributeWriter extends MethodWriter {
    private String attributeName = null;
    private String varName = null;
    
    public NamedAttributeWriter(String attributeName, String varName) {
        super();
        this.attributeName = attributeName;
        this.varName = varName;
    }
    
    public String getVarName() {
        return varName;
    }
    
    public String getAttributeName() {
        return attributeName;
    }
}
