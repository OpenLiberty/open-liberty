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
 * Represents the lifecycle-callbackType type from the javaee XSD.
 */
public interface LifecycleCallback
                extends InterceptorCallback
{
    /**
     * @return &lt;lifecycle-callback-class>, or null if unspecified
     */
    @Override
    String getClassName();

    /**
     * @return &lt;lifecycle-callback-method>
     */
    @Override
    String getMethodName();
}
