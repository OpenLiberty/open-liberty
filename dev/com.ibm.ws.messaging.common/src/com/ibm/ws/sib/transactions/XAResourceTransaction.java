/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.transactions;

import com.ibm.wsspi.sib.core.SIXAResource;

/**
 * A tagging interface which identifies a transaction managed by an XAResource.
 * It allows objects returned from the TransactionFactory createXAResource*()
 * methods to be used with both the implementation and the Core SPI.
 * The peculiar name comes from (i) the desire not to reuse the interface name from
 * an existing standard (XAResource) and (ii) the peculiar way that an XAResource is
 * implemented by objects which are actually transactions too.
 */
public interface XAResourceTransaction extends TransactionCommon, SIXAResource
{
}
