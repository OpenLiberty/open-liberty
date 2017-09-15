/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejs.ras;

/**
 * When an object is passed to Ras as a parameter to be traced or to substitute
 * into a message, the default behavior is to call toString() on the object. For
 * the cases where a non-default behavior is preferred, the Traceable interface
 * (and the toTraceString() method) can be implemented by the Object.
 * 
 * However, there are some objects for which we cannot call toString and the
 * objects are not allowed to add new non-J2EE defined methods to the Object. To
 * handle such cases, the Untraceable interface is defined.
 * 
 * A class should implement this interface to inform Ras not to call toString.
 * When an object that implements this interface is passed to Ras, Ras will
 * simply insert the objects classname into the stream.
 */
public interface Untraceable {

}
