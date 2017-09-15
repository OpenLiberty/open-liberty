/*******************************************************************************
 * Copyright (c) 1998, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

/**
 * <code>IllegalPassivationAttempt</code> is thrown by
 * <code>ManagedContainer</code> during a <code>passivate()</code>
 * or <code>passivateAll</code> operation when a bean or beans in the
 * container cache cannot be passivated.
 * <p>
 * 
 * @see ManagedContainer
 * 
 */

public class IllegalPassivationAttempt
                extends CSIException
{
    private static final long serialVersionUID = 2504861767350634798L;
}
