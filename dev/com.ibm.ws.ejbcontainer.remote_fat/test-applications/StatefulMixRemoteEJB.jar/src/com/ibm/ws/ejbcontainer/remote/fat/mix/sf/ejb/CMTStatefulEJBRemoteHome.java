/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;

/**
 * LocalHome interface for basic Container Managed Transaction Stateful
 * Session bean.
 **/
public interface CMTStatefulEJBRemoteHome extends EJBHome {
    /**
     * @return CMTStatefulEJBLocal The StatefulBean EJB object.
     * @exception javax.ejb.CreateException StatefulBean EJB object was not created.
     */
    public CMTStatefulEJBRemote create() throws CreateException, java.rmi.RemoteException;

}
