/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.util;

import java.security.AccessController;
import java.security.PrivilegedAction;
import com.ibm.ws.util.ThreadContextAccessor;
import com.ibm.wsspi.webcontainer.WebContainer;

// Unprivileged code must not be given visibility to this class.
@SuppressWarnings("unchecked")
public class ThreadContextHelper {
	
	static final ThreadContextAccessor contextAccessor = (ThreadContextAccessor) AccessController.doPrivileged(new PrivilegedAction() {
		public Object run() {
			return ThreadContextAccessor.getThreadContextAccessor();
		}
	});

	public static Object setClassLoader(final ClassLoader cl) {		
		if (contextAccessor.isPrivileged()) {
			contextAccessor.setContextClassLoader(Thread.currentThread(), cl);
		} else {
			AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					contextAccessor.setContextClassLoader(Thread.currentThread(), cl);
					return null;
				}
			});
		}

		return null;
	}
	
	public static ClassLoader getContextClassLoader() {		
		if (contextAccessor.isPrivileged()) {
			return contextAccessor.getContextClassLoader(Thread.currentThread());
		}

		return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return contextAccessor.getContextClassLoader(Thread.currentThread());
			}
		});
	}
	
	public static ClassLoader getExtClassLoader(){
		WebContainer webContainer = WebContainer.getWebContainer();
		if (webContainer==null)
			return null;
		else
			return webContainer.getExtClassLoader();
	}
}
