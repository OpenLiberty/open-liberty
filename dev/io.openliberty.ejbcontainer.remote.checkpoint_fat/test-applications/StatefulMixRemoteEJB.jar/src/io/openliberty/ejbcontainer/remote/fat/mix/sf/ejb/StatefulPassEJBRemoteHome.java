/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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
package io.openliberty.ejbcontainer.remote.fat.mix.sf.ejb;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;

/**
 * EJBLocalHome interface for Stateful Session bean for testing @Remove
 * methods.
 **/
public interface StatefulPassEJBRemoteHome extends EJBHome {
    /**
     * Default create method with no parameters. <p>
     *
     * @return StatefulPassEJBRemote The StatefulBean EJB object.
     * @exception javax.ejb.CreateException StatefulBean EJB object was not created.
     */
    public StatefulPassEJBRemote create() throws CreateException, RemoteException;

    /**
     * Default create method with one parameter. <p>
     *
     * @return StatefulPassEJBRemote The StatefulBean EJB object.
     * @exception javax.ejb.CreateException StatefulBean EJB object was not created.
     */
    public StatefulPassEJBRemote create(String str, Integer i, SerObj serObj, SerObj2 serObj2) throws CreateException, RemoteException;
}