/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.util;

import java.util.Enumeration;

/**
 * Singleton empty enumeration.
 */
@SuppressWarnings("unchecked")
public class EmptyEnumeration implements java.util.Enumeration, java.io.Serializable{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3546642126931963953L;
	private static Enumeration _instance;
/**
 * EmptyEnumeration constructor comment.
 */
private EmptyEnumeration() {
}
/**
 * This method was created in VisualAge.
 * @return com.ibm.servlet.prototype.util.EmptyEnumeration
 */
private synchronized static void createInstance() {
	if(_instance == null){
		_instance = new EmptyEnumeration();
	};
}
/**
 * hasMoreElements method comment.
 */
public boolean hasMoreElements() {
	return false;
}
/**
 * This method was created in VisualAge.
 * @return Enumeration
 */
public static Enumeration instance() {
	if(_instance == null){
		createInstance();
	}
	return _instance;
}
/**
 * nextElement method comment.
 */
public Object nextElement() {
	return null;
}
}
