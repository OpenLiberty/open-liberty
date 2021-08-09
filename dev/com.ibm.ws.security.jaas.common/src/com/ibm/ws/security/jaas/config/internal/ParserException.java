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
package com.ibm.ws.security.jaas.config.internal;

public class ParserException extends java.security.GeneralSecurityException
{
    private static final long serialVersionUID = 43255370417322370L;

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
