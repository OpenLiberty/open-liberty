/*******************************************************************************
 * Copyright (c) 1997, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.tsx.tag;

import java.util.Hashtable;
import java.util.Stack;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;

import com.ibm.ws.jsp.tsx.db.ConnectionProperties;

public class PasswdTag extends BodyTagSupport {

    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3256446923367003186L;

	public PasswdTag() {}

    public int doEndTag()
        throws JspException {
            
        Hashtable connectionLookup = (Hashtable)pageContext.getAttribute("TSXConnectionLookup", PageContext.PAGE_SCOPE);
        if (connectionLookup == null) {
            throw new JspException("No dbconnect tag found in jsp");
        }
        Stack connectionStack = (Stack)pageContext.getAttribute("TSXConnectionStack", PageContext.PAGE_SCOPE);
        if (connectionStack == null) {
            throw new JspException("No dbconnect tag found in jsp");
        }
        
        String connectionId = (String)connectionStack.peek();
        ConnectionProperties connection = (ConnectionProperties)connectionLookup.get(connectionId);

        connection.setLoginPasswd(getBodyContent().getString());
        return (EVAL_PAGE);
    }
}
