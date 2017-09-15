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
package com.ibm.ws.jsp.translator.utils;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import com.ibm.ws.jsp.JspCoreException;

public class FunctionSignature {
    private String returnType;
    private String methodName;
    private String className;
    private Class[] parameterTypes;

    public FunctionSignature(String className, String signature, String tagLibraryName, ClassLoader loader) throws JspCoreException {
        this.className = className;
        try {
            String ws = " \t\n\r";
            StringTokenizer sigTokenizer = new StringTokenizer(signature, ws + "(),", true);
            this.returnType = sigTokenizer.nextToken();

            do {
                this.methodName = sigTokenizer.nextToken();
            }
            while (ws.indexOf(this.methodName) != -1);

            String paren;
            do {
                paren = sigTokenizer.nextToken();
            }
            while (ws.indexOf(paren) != -1);

            if (!paren.equals("(")) {
                throw new JspCoreException(
                    "jsp.error.tld.fn.invalid.signature.parenexpected",
                    new Object[] { tagLibraryName, this.methodName });
            }

            String argType;
            do {
                argType = sigTokenizer.nextToken();
            }
            while (ws.indexOf(argType) != -1);

            if (!argType.equals(")")) {
                ArrayList parameterTypes = new ArrayList();
                do {
                    if (",(".indexOf(argType) != -1) {
                        throw new JspCoreException(
                            "jsp.error.tld.fn.invalid.signature",
                            new Object[] { tagLibraryName, this.methodName });
                    }

                    parameterTypes.add(JspTranslatorUtil.toClass(argType, loader));

                    String comma;
                    do {
                        comma = sigTokenizer.nextToken();
                    }
                    while (ws.indexOf(comma) != -1);

                    if (comma.equals(")")) {
                        break;
                    }
                    if (!comma.equals(",")) {
                        throw new JspCoreException(
                            "jsp.error.tld.fn.invalid.signature.commaexpected",
                            new Object[] { tagLibraryName, this.methodName });
                    }

                    // <arg-type>
                    do {
                        argType = sigTokenizer.nextToken();
                    }
                    while (ws.indexOf(argType) != -1);
                }
                while (true);
                this.parameterTypes = (Class[]) parameterTypes.toArray(new Class[parameterTypes.size()]);
            }
        }
        catch (NoSuchElementException e) {
            throw new JspCoreException(
                "jsp.error.tld.fn.invalid.signature",
                new Object[] { tagLibraryName, this.methodName });
        }
        catch (ClassNotFoundException e) {
            throw new JspCoreException(
                "jsp.error.tld.fn.invalid.signature.classnotfound",
                new Object[] { e.getMessage(), tagLibraryName, this.methodName });
        }
    }

    public String getReturnType() {
        return this.returnType;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public Class[] getParameterTypes() {
        return this.parameterTypes;
    }
    
    public String getFunctionClassName() {
        return this.className;
    }
}
