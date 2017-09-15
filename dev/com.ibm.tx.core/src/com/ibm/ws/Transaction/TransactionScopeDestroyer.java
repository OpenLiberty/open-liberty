/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.Transaction;

/**
 * An interface that allows the cdi TransactionContext type to register itself
 * with TransactionImpl, for notifying when the TransactionImpl reaches the
 * afterComplete stage. At this point, TransactionImpl will call the destroy()
 * method.
 *
 * This is a slightly roundabout way to avoid having a circular dependency on
 * the project containing TransactionContext.
 */
public interface TransactionScopeDestroyer {
    public void destroy();
}
