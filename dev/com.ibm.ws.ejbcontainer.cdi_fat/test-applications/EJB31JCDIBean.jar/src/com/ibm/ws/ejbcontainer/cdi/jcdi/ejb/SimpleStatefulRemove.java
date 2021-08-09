/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.cdi.jcdi.ejb;

/**
 * Common local interface for beans that verify EJBContainer.removeStatefulBean.
 **/
public interface SimpleStatefulRemove {
    /**
     * Resets the count of removed stateful beans to zero.
     **/
    public void resetStatefulRemoveCount();

    /**
     * Returns the count of the number of removed stateful beans.
     */
    public int getStatefulRemoveCount();
}
