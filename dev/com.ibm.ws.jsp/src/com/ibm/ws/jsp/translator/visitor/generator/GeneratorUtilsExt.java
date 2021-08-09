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
package com.ibm.ws.jsp.translator.visitor.generator;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.translator.visitor.validator.ValidateResult;

/**
 *
 */
public interface GeneratorUtilsExt {
    
    public void generateELFunctionCode(JavaCodeWriter writer, ValidateResult validatorResult) throws JspCoreException;
    
    /**
     * Produces a String representing a call to the EL interpreter.
     * @param expression a String containing zero or more "${}" expressions
     * @param expectedType the expected type of the interpreted result
     * @param defaultPrefix Default prefix, or literal "null"
     * @param fnmapvar Variable pointing to a function map.
     * @param XmlEscape True if the result should do XML escaping
     * @param pageContextVar Variable for PageContext variable name in generated Java code.
     * @return a String representing a call to the EL interpreter.
     */
    public String interpreterCall(boolean isTagFile, String expression, Class expectedType, String fnmapvar, boolean XmlEscape, String pageContextVar); //PI59436
    
    public String getClassFileVersion();
}
