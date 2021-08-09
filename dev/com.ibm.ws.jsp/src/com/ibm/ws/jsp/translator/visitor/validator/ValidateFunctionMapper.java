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
package com.ibm.ws.jsp.translator.visitor.validator;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.el.FunctionMapper;

import com.ibm.ws.jsp.translator.utils.FunctionSignature;

public class ValidateFunctionMapper extends FunctionMapper  {
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3977017366255185969L;
	private HashMap fnMap = new HashMap();
	private HashMap sigMap = new HashMap();
    
    public void mapFunction(String fnQName, Method method) {
        fnMap.put(fnQName, method);
    }

    public Method resolveFunction(String prefix, String localName) {
        return (Method) fnMap.get(prefix + ":" + localName);
    }

    public void mapSignature(String fnQName, FunctionSignature signature) {
        sigMap.put(fnQName, signature);
    }

    public FunctionSignature getSignature(String fnQName) {
        return (FunctionSignature) sigMap.get(fnQName);   
    }
    
    //LIDB4147-9 Begin
    public Map getFnMap() {
    	return fnMap;
    }
    //LIDB4147-9 End
}
