/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.common;

/**
 * Represents elements like lifecycle-callbackType from the javaee XSD or
 * around-timeout or around-invoke as described by the Interceptors spec.
 */
public interface InterceptorCallback
{
    /**
     * @return the callback class, or null if unspecified
     */
    String getClassName();

    /**
     * @return the callback method
     */
    String getMethodName();
}
