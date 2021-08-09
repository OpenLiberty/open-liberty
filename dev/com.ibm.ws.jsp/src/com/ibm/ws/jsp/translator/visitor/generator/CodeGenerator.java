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

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfiguration;
import com.ibm.ws.jsp.translator.visitor.JspVisitorInputMap;
import com.ibm.ws.jsp.translator.visitor.validator.ValidateResult;
import com.ibm.wsspi.jsp.context.JspCoreContext;

public interface CodeGenerator {
    void init(JspCoreContext ctxt,
              Element element, 
              ValidateResult validatorResult,
              JspVisitorInputMap inputMap,
              ArrayList methodWriterList,
              FragmentHelperClassWriter fragmentHelperClassWriter,
              HashMap persistentData,
              JspConfiguration jspConfiguration, 
              JspOptions jspOptions) throws JspCoreException;
    void startGeneration(int section, JavaCodeWriter writer) throws JspCoreException;
    void endGeneration(int section, JavaCodeWriter writer) throws JspCoreException;
    JavaCodeWriter getWriterForChild(int section, Node jspElement) throws JspCoreException;
}

