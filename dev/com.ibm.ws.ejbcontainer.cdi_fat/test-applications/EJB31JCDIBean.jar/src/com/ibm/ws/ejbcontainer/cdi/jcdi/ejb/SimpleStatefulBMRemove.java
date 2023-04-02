/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.cdi.jcdi.ejb;

/**
 * Common local interface for beans that verify EJBContainer.removeStatefulBean.
 **/
public interface SimpleStatefulBMRemove extends SimpleStatefulRemove {
    /**
     * Begins a UserTransaction, leaving it active so the bean runs in
     * a sticky global transaction.
     **/
    public void beginUserTransaction();

    /**
     * Commits the UserTransaction that was started with a call to
     * beginUserTransaction.
     */
    public void commitUserTransaction();
}
