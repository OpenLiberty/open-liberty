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
package com.ibm.ws.javaee.dd.ejb;

/**
 * Represents &lt;remove-method>.
 */
public interface RemoveMethod
{
    /**
     * @return &lt;bean-method>
     */
    NamedMethod getBeanMethod();

    /**
     * @return true if &lt;retain-if-exception> is specified
     * @see #isRetainIfException
     */
    boolean isSetRetainIfException();

    /**
     * @return &lt;retain-if-exception> if specified
     * @see #isSetRetainIfException
     */
    boolean isRetainIfException();
}
