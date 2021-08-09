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
package com.ibm.ws.jsp.translator.visitor;


public class JspVisitorResult {
    protected String jspVisitorId = "";

    public JspVisitorResult(String jspVisitorId) {
        this.jspVisitorId = jspVisitorId;
    }

    public String getJspVisitorId() {
        return jspVisitorId;
    }
}
