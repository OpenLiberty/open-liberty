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
package com.ibm.ws.sip.container.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
 
/**
 * The class is input stream for external library de-serialization
 * when the class being resolved it turns to both branches of class loaders: 
 * the bundle class loader and if the class is not there to a thread context class loader.
 * 
 * @author Roman Mandeleil
 */
public class ThreadContextInputStream extends ObjectInputStream {

	public ThreadContextInputStream(InputStream in) throws IOException {
      super(in);
    }

   /**
    * Resolve the class using both class loading branches: the bundle class loader and 
    * if the class is not there to a thread context class loader. 
    */
   protected Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException {
	   
	   ClassLoader threadContextClassLoader = Thread.currentThread().getContextClassLoader();
	   ClassLoader bundleClassLoader = this.getClass().getClassLoader();
	   Class loadedClass = null;
		
	   try {
		   loadedClass = Class.forName(desc.getName(), false, threadContextClassLoader);
		} catch (ClassNotFoundException e) {
		   loadedClass = Class.forName(desc.getName(), false, bundleClassLoader);
		}
	   
      return loadedClass;
   }
}


