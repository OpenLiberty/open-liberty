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
package com.ibm.ws.logging.internal.osgi.stackjoiner;

import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class MethodProxy {
	
	private static final TraceComponent tc = Tr.register(MethodProxy.class);
	private Method method;
	private String className;
	private String methodName;
	
	public MethodProxy(Instrumentation inst, String className, String methodName, Class<?>... parameterTypes) {
		this.className = className;
		this.methodName = methodName;
		Class<?> classRetrieved = retrieveClass(inst, className);
		if (classRetrieved != null) {
			Method method = ReflectionHelper.getDeclaredMethod(classRetrieved, methodName, parameterTypes);
			setMethodProxy(method);
		}
		
	}
	
	public boolean isInitialized() {
		if (getMethodProxy() == null) {
			if (tc.isDebugEnabled())
				Tr.debug(tc, "Stack joiner could not be initialized. Failed to reflect method " + methodName + " in " + className + ".");
			return false;
		}
		return true;
	}
	
	@SuppressWarnings("rawtypes")
	private Class<?> retrieveClass(Instrumentation inst, String className) {
	    if (inst != null) {
	        Class[] loadedClasses = inst.getAllLoadedClasses();
	        for (int i = 0; i < loadedClasses.length; i++) {
	            String name = loadedClasses[i].getName();
	            if (name.equals(className)) {
	                return loadedClasses[i];
	            }
	        }
	    }
	    return null;
	}
	
	private void setMethodProxy(Method method) {
		this.method = method;
	}
	
	public Method getMethodProxy() {
	    return method;
	}
	
}