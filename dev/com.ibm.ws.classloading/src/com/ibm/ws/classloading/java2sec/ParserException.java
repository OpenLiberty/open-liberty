/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.java2sec;

/**
 *  Syntax error in the permission grant, throw an exception
 */
public class ParserException extends java.security.GeneralSecurityException {
    
    private static final long serialVersionUID = 5462018719430068594L;  
    public ParserException(String msg) {
        super(msg);
    }

    public ParserException(int lineno, String msg) {
        super("line " + lineno + ": " + msg);
    }

    public ParserException(int lineno, String expected, String result) {
        super("line " + lineno + ": expected '" + expected + "', found '" + result + "'");
    }
}
