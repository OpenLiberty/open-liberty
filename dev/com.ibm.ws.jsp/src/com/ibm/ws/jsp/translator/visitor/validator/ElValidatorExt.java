/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.translator.visitor.validator;

import org.apache.jasper.compiler.ELNode;
import org.w3c.dom.Element;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.translator.JspTranslationException;

/**
 *
 */
public interface ElValidatorExt {
    
    public void validateElFunction(ELNode.Nodes el,Element jspElement,String expression, ValidateResult result, ClassLoader loader, JspConfiguration jspConfiguration) throws JspCoreException;

    public void prepareExpression(ELNode.Nodes el,String expression, ValidateResult result, ClassLoader loader, JspConfiguration jspConfiguration) throws  JspTranslationException;
}
