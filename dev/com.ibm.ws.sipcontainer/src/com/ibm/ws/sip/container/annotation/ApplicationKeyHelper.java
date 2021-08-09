/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.annotation;

import java.lang.reflect.Method;

import javax.servlet.sip.SipServletRequest;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.container.parser.SipAppDesc;

/**
 * 
 * @author Roman Mandeleil
 */
public class ApplicationKeyHelper {
	
	private static final LogMgr c_logger = Log.get(SipAppDesc.class);

	Object objectToInvoke = null;
	Method applicationKeyMethod = null;
	String applicationKeyMethodstr = null;
	String applicationKeyClassName = null;

	@SuppressWarnings(value = { "unused" })
	private ApplicationKeyHelper (){}
	
	/**
	 * @param applicationKeyMethod 
	 * @param objectToInvoke
	 */
	public ApplicationKeyHelper (
			@SuppressWarnings("hiding") Method applicationKeyMethod){
		//save the name of the class and the method to be used, it will be loaded in a 
		//lazy fashion in the first time that it is called so it will be loaded in the 
		//correct CL
		applicationKeyMethodstr = applicationKeyMethod.getName();
		applicationKeyClassName = applicationKeyMethod.getDeclaringClass().getName();
	}

	/**
	 * @param req
	 * @return
	 */
	public String generateApplicationKey(SipServletRequest req){

		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "generateApplicationKey");
        }                    		

        String result = null;
		
		try{
			if (objectToInvoke == null || applicationKeyMethod == null){
				//Lazy load the app key method so it will be loaded in the application CL and not 
				//in the web loader CL
				//before calling this method the container is setting the CL to the app CL
				uploadAppKeyMethod();
			}
			
			// Invoke the application method 
			Object methodResult = this.applicationKeyMethod.invoke(objectToInvoke, req);
			
			if (methodResult != null) {
				result = methodResult.toString();
			}

		} catch (Throwable e) {
			if (c_logger.isErrorEnabled()) {
				Object[] args = {};
				c_logger.error(e.getMessage(),
						Situation.SITUATION_REQUEST, args, e);
			}
		}
		
        if (c_logger.isTraceEntryExitEnabled()) {
        	
        	Object[] params = {result};
            c_logger.traceExit(this, "generateApplicationKey", params);
        }                    		

        return result;
	}

	/**
	 * Load the method and object to use for the app key. those objects are cached for future use
	 * 
	 * @throws ClassNotFoundException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private void uploadAppKeyMethod() throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalAccessException, InstantiationException {
			Class clazz = Class.forName(applicationKeyClassName, true, Thread.currentThread().getContextClassLoader());
			applicationKeyMethod = clazz.getMethod(applicationKeyMethodstr, SipServletRequest.class);
			objectToInvoke = applicationKeyMethod.getDeclaringClass().newInstance();
	}
}
