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
package com.ibm.ws.jsp.jsx.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

public class ArgTag extends TagSupport implements Tag {
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3257006540509754937L;
	String value;

    /**
     * @see TagSupport#doStartTag()
     */
    public int doStartTag() throws JspException {
        CallTag parent = (CallTag) getParent();
        parent.getArguments().add(value);
        return SKIP_BODY;
    }

    /**
     * @see TagSupport#release()
     */
    public void release() {
        value = null;
    }

    /**
     * Gets the value
     * @return Returns a String
     */
    public String getValue() {
        return value;
    }
    /**
     * Sets the value
     * @param value The value to set
     */
    public void setValue(String value) {
        this.value = value;
    }
}