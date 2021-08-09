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

public class JspVisitorUsage {
    private int order = 0;
    private int visits = 0;
    private JspVisitorDefinition visitorDefinition = null;
    
    public JspVisitorUsage(int order, int visits, JspVisitorDefinition visitorDefinition) {
        this.order = order;
        this.visits = visits;
        this.visitorDefinition = visitorDefinition;
    }

    public int getOrder() {
        return order;
    }
    
    public int getVisits() {
        return visits;
    }

    public JspVisitorDefinition getJspVisitorDefinition() {
        return (visitorDefinition);
    }
}
