/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

import java.rmi.Remote;

public interface HomeWrapperSet {

    /**
     * Returns the Remote reference of the remote home interface of this EJB.
     * This object will be used to bind to the naming service. This method
     * returns null if no remote interface is defined in the bean.
     */
    public Remote getRemote();

    /**
     * Returns the local home interface of this EJB.
     * This object will be used to bind to the naming service. This method
     * returns null if no local interface is defined in the bean.
     */
    public Object getLocal(); //LIDB859-4
}
