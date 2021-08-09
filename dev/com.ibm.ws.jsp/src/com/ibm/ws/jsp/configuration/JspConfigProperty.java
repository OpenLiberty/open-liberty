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
package com.ibm.ws.jsp.configuration;

public class JspConfigProperty {
    public static final int IS_XML_TYPE = 1;
    public static final int EL_IGNORED_TYPE = 2;
    public static final int SCRIPTING_INVALID_TYPE = 3;
    public static final int PAGE_ENCODING_TYPE = 4;
    public static final int PRELUDE_TYPE = 5;
    public static final int CODA_TYPE = 6;
    public static final int DEFERRED_SYNTAX_ALLOWED_AS_LITERAL_TYPE = 7; // jsp2.1ELwork
    public static final int TRIM_DIRECTIVE_WHITESPACES_TYPE = 8; // jsp2.1work
    public static final int EL_IGNORED_SET_TRUE_TYPE = 9; // jsp2.1work
    public static final int DEFAULT_CONTENT_TYPE = 10;//jsp2.1MR2work
    public static final int BUFFER = 11;//jsp2.1MR2work
    public static final int ERROR_ON_UNDECLARED_NAMESPACE = 12;//jsp2.1MR2work

    private Object propertyValue = null;
    private int propertyType = 0;

    public JspConfigProperty(int type, Object value) {
        this.propertyType = type;
        this.propertyValue = value;
    }

    public int getType() {
        return propertyType;
    }

    public Object getValue() {
        return propertyValue;
    }
}