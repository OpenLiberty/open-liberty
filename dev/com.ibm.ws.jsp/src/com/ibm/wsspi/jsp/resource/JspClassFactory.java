/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.jsp.resource;


import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class will return an instance of any class as contained in the classesMap.
 * @author dmeisenb, kennas
 */
public class JspClassFactory {

	//HashMap which stores String combinations of the type of class and the full path to
	//the version we want.
	//	i.e. - "FileLocker", "com.ibm.ws.jsp.translator.utils.FileLocker"
	private static HashMap classesMap = new HashMap();
		//add default values to the classesMap.
		static {
			classesMap.put("FileLocker", "com.ibm.ws.jsp.translator.utils.FileLocker");
		}

	Logger logger = Logger.getLogger("com.ibm.ws.jsp","com.ibm.ws.jsp.resources.messages");

	/**
	 * adds/replaces the path to implementations of classes.
	 * @param key - the type of class
	 * @param value - the path to the implementation class
	 */
	public static void updateMap(String key, String value){
		classesMap.put(key, value);
	}

	/**
	 * Creates an instance of the type of class specified by the key arg, dependent on the
	 * value stored in the classesMap.
	 *
	 * @param key
	 * @return an object of the type corresponding to the key.
	 */
	public Object getInstanceOf(String key){

			try {
				ClassLoader loader = Thread.currentThread().getContextClassLoader();

				return Class.forName((String)classesMap.get(key),true,loader).newInstance();

			} catch (IllegalAccessException e) {
				logger.logp(Level.SEVERE,"JspClassFactory","getInstanceOf","jsp.error.function.classnotfound",(String) classesMap.get(key));
			} catch (InstantiationException e) {
				logger.logp(Level.SEVERE,"JspClassFactory","getInstanceOf","jsp.error.function.classnotfound",(String) classesMap.get(key));
			} catch (ClassNotFoundException e) {
				logger.logp(Level.SEVERE,"JspClassFactory","getInstanceOf","jsp.error.function.classnotfound",(String) classesMap.get(key));
			}
			return null;
	}






}
