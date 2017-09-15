/*******************************************************************************
 * Copyright (c) 2000 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 *  A <code>StatefulSessionHandleFactory</code> constructs Handles
 *  for stateful session beans. <p>
 */

package com.ibm.websphere.csi;

public interface StatefulSessionHandleFactory {

    /**
     * Return a <code>Handle</code> for a stateful session bean. <p>
     */

    public javax.ejb.Handle create(javax.ejb.EJBObject object)
                    throws CSIException;
}
