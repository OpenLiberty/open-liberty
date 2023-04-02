/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer;

/**
 * Data specific to an EJB request.
 */
public interface EJBRequestData
{
    /**
     * The EJB method being called.
     */
    EJBMethodMetaData getEJBMethodMetaData();

    /**
     * The arguments passed to the EJB method. The returned array should not be
     * modified.
     */
    Object[] getMethodArguments();

    /**
     * The unique identifier of the bean instance being invoked. The object
     * implements {@link Object#hashCode} and {@link Object#equals}.
     */
    Object getBeanId();

    /**
     * The bean instance being invoked. This method may only be called by after
     * activation collaborators.
     */
    Object getBeanInstance();
}
