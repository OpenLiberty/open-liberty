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
package com.ibm.ws.jsp.translator;

import org.w3c.dom.Element;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.translator.utils.JspId;

public class JspTranslationException extends JspCoreException {
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3257566191894410289L;
	protected JspId jspId = null;
    
    public JspTranslationException() {
        super();
    }

    public JspTranslationException(String message) {
        super(message);
    }

    public JspTranslationException(Element jspElement, String message) {
        super(message);
        String jspIdString = jspElement.getAttributeNS(Constants.JSP_NAMESPACE, "id");
        if (jspIdString.equals("") == false) 
            jspId = new JspId(jspIdString);
    }

    public JspTranslationException(String message, Object[] args) {
        super(message, args);
    }

    public JspTranslationException(Element jspElement, String message, Object[] args) {
        super(message, args);
        String jspIdString = jspElement.getAttributeNS(Constants.JSP_NAMESPACE, "id");
        if (jspIdString.equals("") == false) 
            jspId = new JspId(jspIdString);
    }

    public JspTranslationException(String message, Throwable exc) {
        super(message, exc);
    }

    public JspTranslationException(Element jspElement, String message, Throwable exc) {
        super(message, exc);
        String jspIdString = jspElement.getAttributeNS(Constants.JSP_NAMESPACE, "id");
        if (jspIdString.equals("") == false) 
            jspId = new JspId(jspIdString);
    }

    public JspTranslationException(Element jspElement, String message, Object[] args, Throwable exc) {
        super(message, args, exc);
        String jspIdString = jspElement.getAttributeNS(Constants.JSP_NAMESPACE, "id");
        if (jspIdString.equals("") == false) 
            jspId = new JspId(jspIdString);
    }
    
    public JspTranslationException(Throwable exc) {
        super(exc==null ? null : exc.toString(), exc);
    }
    
    public String getLocalizedMessage() {
        String msg = super.getLocalizedMessage();
        if (jspId != null) {
        	// defect 203252
            msg = jspId.getFilePath() + "(" + jspId.getStartSourceLineNum() + "," + jspId.getStartSourceColNum() + ") --> " + msg;
        }
        return (msg);
    }
	// Defect 202493
	public int getStartSourceLineNum() {
		if (jspId != null) {
			return jspId.getStartSourceLineNum();
		}
		return -1;
	}
	
	// defect 203252
	public String getFilePath(){
		if (jspId != null) {
			return jspId.getFilePath();
		}
		return null;
	}
}
