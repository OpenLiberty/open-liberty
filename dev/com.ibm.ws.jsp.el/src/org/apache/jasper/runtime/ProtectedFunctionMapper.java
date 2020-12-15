/*
 * $Header: /cvshome/wascvs/jsp/src/org/apache/jasper/runtime/ProtectedFunctionMapper.java,v 1.1.1.1 2003/10/17 13:47:57 backhous Exp $
 * $Revision: 1.1.1.1 $
 * $Date: 2003/10/17 13:47:57 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * feature LIDB4147-9 "Integrate Unified Expression Language"  2006/08/14  Scott Johnson
 * defect 162846 - Lambda Expressions are not working 2015/02/20        Jay Sartoris
 */

package org.apache.jasper.runtime;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;

import javax.servlet.jsp.el.FunctionMapper;

/**
 * Maps EL functions to their Java method counterparts. Keeps the
 * actual Method objects protected so that JSP pages can't indirectly
 * do reflection.
 * 
 * @author Mark Roth
 * @author Kin-man Chung
 */
public final class ProtectedFunctionMapper extends javax.el.FunctionMapper implements FunctionMapper {

    /**
     * Maps "prefix:name" to java.lang.Method objects. Lazily created.
     */
    private HashMap fnmap = null;

    /**
     * Constructor has protected access.
     */
    private ProtectedFunctionMapper() {}

    /**
     * Generated Servlet and Tag Handler implementations call this
     * method to retrieve an instance of the ProtectedFunctionMapper.
     * This is necessary since generated code does not have access to
     * create instances of classes in this package.
     * 
     * @return A new protected function mapper.
     */
    public static ProtectedFunctionMapper getInstance() {
        ProtectedFunctionMapper funcMapper;
        if (System.getSecurityManager() != null) {
            funcMapper = (ProtectedFunctionMapper) AccessController.doPrivileged(
                            new PrivilegedAction() {
                                @Override
                                public Object run() {
                                    return new ProtectedFunctionMapper();
                                }
                            });
        } else {
            funcMapper = new ProtectedFunctionMapper();
        }
        funcMapper.fnmap = new java.util.HashMap();
        return funcMapper;
    }

    /**
     * Stores a mapping from the given EL function prefix and name to
     * the given Java method.
     * 
     * @param prefix The EL function prefix
     * @param fnName The EL function name
     * @param c The class containing the Java method
     * @param methodName The name of the Java method
     * @param args The arguments of the Java method
     * @throws RuntimeException if no method with the given signature
     *             could be found.
     */
    public void mapFunction(String prefix, String fnName,
                            final Class c, final String methodName, final Class[] args)
    {
        // 162846
        // Skip if null values were passed in. They indicate a function
        // added via a lambda or ImportHandler; nether of which need to be
        // placed in the Map.
        if (fnName == null) {
            return;
        }

        java.lang.reflect.Method method;
        if (System.getSecurityManager() != null) {
            try {
                method = (java.lang.reflect.Method) AccessController.doPrivileged(new PrivilegedExceptionAction() {

                    @Override
                    public Object run() throws Exception {
                        return c.getDeclaredMethod(methodName, args);
                    }
                });
            } catch (PrivilegedActionException ex) {
                throw new RuntimeException(
                                "Invalid function mapping - no such method: " + ex.getException().getMessage());
            }
        } else {
            try {
                method = c.getDeclaredMethod(methodName, args);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(
                                "Invalid function mapping - no such method: " + e.getMessage());
            }
        }

        this.fnmap.put(prefix + ":" + fnName, method);
    }

    /**
     * Resolves the specified local name and prefix into a Java.lang.Method.
     * Returns null if the prefix and local name are not found.
     * 
     * @param prefix the prefix of the function
     * @param localName the short name of the function
     * @return the result of the method mapping. Null means no entry found.
     **/
    @Override
    public Method resolveFunction(String prefix, String localName) {
        return (Method) this.fnmap.get(prefix + ":" + localName);
    }
}
