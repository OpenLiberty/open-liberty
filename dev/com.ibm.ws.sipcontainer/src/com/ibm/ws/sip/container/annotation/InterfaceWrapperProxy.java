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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class InterfaceWrapperProxy implements InvocationHandler {
	
	Object listener = null;
	
	public InterfaceWrapperProxy(Object listener){
		this.listener = listener;
		
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		return method.invoke(listener, args);
	}
	

}
