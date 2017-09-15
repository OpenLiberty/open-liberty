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
package com.ibm.ws.jsp.translator.visitor.configuration;


public class JspVisitorDefinition {
    private String id = null;
    private Class visitorResultClass = null;
    private Class visitorClass = null;
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Class getVisitorResultClass() {
        return visitorResultClass;
    }

    public void setVisitorResultClass(Class visitorResultClass) {
        this.visitorResultClass = visitorResultClass;
    }

    public Class getVisitorClass() {
        return (visitorClass);
    }

    public void setVisitorClass(Class visitorClass) {
        this.visitorClass = visitorClass;    
    }

}
