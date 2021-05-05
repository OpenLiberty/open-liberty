/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import com.ibm.ws.kernel.productinfo.ProductInfo;

/**
 * Retrieves the printStackTraceOverride method from BaseTraceService via reflection.
 */
public class ThrowableInfo {
	
	private static final TraceComponent tc = Tr.register(ThrowableInfo.class);
	final String BASE_TRACE_SERVICE_CLASS_NAME = "com.ibm.ws.logging.internal.impl.BaseTraceService";
	final String BASE_TRACE_SERVICE_METHOD_NAME = "printStackTraceOverride";
	private Method btsMethod;
	
	
	public ThrowableInfo(Instrumentation inst) {
		if (isEnabled()) {
	    	Class<?> btsClass = retrieveClass(inst, BASE_TRACE_SERVICE_CLASS_NAME);
	        if (btsClass != null) {
	        	Method method = ReflectionHelper.getDeclaredMethod(btsClass, BASE_TRACE_SERVICE_METHOD_NAME, Throwable.class, PrintStream.class);
	        	setBtsMethod(method);
	        }
		}
	}
	public boolean isInitialized() {
		if (isEnabled()) {
			if (getBtsMethod() == null) {
				if (tc.isDebugEnabled())
					Tr.debug(tc, "Stack joiner could not be initialized. Failed to reflect method " + BASE_TRACE_SERVICE_METHOD_NAME + " in " + BASE_TRACE_SERVICE_CLASS_NAME + ".");
				return false;
			}
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("rawtypes")
	private Class<?> retrieveClass(Instrumentation inst, String classGroup) {
	    if (inst != null) {
	        Class[] loadedClasses = inst.getAllLoadedClasses();
	        for (int i = 0; i < loadedClasses.length; i++) {
	            String name = loadedClasses[i].getName();
	            if (name.equals(classGroup)) {
	                return loadedClasses[i];
	            }
	        }
	    }
	    return null;
	}
	
	private void setBtsMethod(Method method) {
	    btsMethod = method;
	}
	
	public Method getBtsMethod() {
	    return btsMethod;
	}
	
	/**
	 * Returns true if the stack joiner feature has been enabled, otherwise false
	 * @return true if the stack joiner feature has been enabled, otherwise false
	 */
	public boolean isEnabled() {
		if (ProductInfo.getBetaEdition() || (System.getenv("WLP_LOGGING_STACK_JOIN") != null && System.getenv("WLP_LOGGING_STACK_JOIN").equals("true")) ) return Boolean.TRUE;
		return Boolean.FALSE;
	}
}