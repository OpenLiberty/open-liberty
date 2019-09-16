/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;

/**
 * EJBHome interface for Stateful Session bean for testing @Remove methods.
 **/
public interface RemoveCMTEJBRemoteHome extends EJBHome {
    /**
     * Default create method with no parameters.
     * <p>
     *
     * @return RemoveCMTEJBRemote The StatefulBean EJB object.
     * @exception javax.ejb.CreateException
     *                StatefulBean EJB object was not created.
     */
    public RemoveCMTEJBRemote create() throws CreateException, java.rmi.RemoteException;

    /**
     * Default create method with one parameter.
     * <p>
     *
     * @return RemoveCMTEJBRemote The StatefulBean EJB object.
     * @exception javax.ejb.CreateException
     *                StatefulBean EJB object was not created.
     */
    public RemoveCMTEJBRemote create(String string) throws CreateException, java.rmi.RemoteException;
}
