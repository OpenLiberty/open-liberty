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
package com.ibm.ws.ejbcontainer.remote.fat.xml.sf.ejb;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;

/**
 * EJBHome interface for Stateful Session bean for testing Init/ejbCreate
 * methods.
 **/
public interface InitEJBRemoteHome extends EJBHome {
    /**
     * Default create method with no parameters.
     * <p>
     *
     * @return InitEJBRemote The StatefulBean EJB object.
     * @exception javax.ejb.CreateException
     *                StatefulBean EJB object was not created.
     */
    public InitEJBRemote create() throws CreateException, java.rmi.RemoteException;

    /**
     * Default create method with one parameter.
     * <p>
     *
     * @return InitEJBRemote The StatefulBean EJB object.
     * @exception javax.ejb.CreateException
     *                StatefulBean EJB object was not created.
     */
    public InitEJBRemote create(String string) throws CreateException, java.rmi.RemoteException;

    /**
     * Custom create method with no parameters.
     * <p>
     *
     * @return InitEJBRemote The StatefulBean EJB object.
     * @exception javax.ejb.CreateException
     *                StatefulBean EJB object was not created.
     */
    public InitEJBRemote createDefault() throws CreateException, java.rmi.RemoteException;

    /**
     * Custom create method with one parameter.
     * <p>
     *
     * @return InitEJBRemote The StatefulBean EJB object.
     * @exception javax.ejb.CreateException
     *                StatefulBean EJB object was not created.
     */
    public InitEJBRemote createBasic(String string) throws CreateException, java.rmi.RemoteException;

    /**
     * Custom create method with one parameter, different type.
     * <p>
     *
     * @return InitEJBRemote The StatefulBean EJB object.
     * @exception javax.ejb.CreateException
     *                StatefulBean EJB object was not created.
     */
    public InitEJBRemote createBasic(int integer) throws CreateException, java.rmi.RemoteException;

    /**
     * Custom create method with two parameters.
     * <p>
     *
     * @return InitEJBRemote The StatefulBean EJB object.
     * @exception javax.ejb.CreateException
     *                StatefulBean EJB object was not created.
     */
    public InitEJBRemote createAdv(String string, int integer) throws CreateException, java.rmi.RemoteException;

    /**
     * Custom create method with one parameter.
     * <p>
     *
     * Intended to not have an 'Init' method of its own.
     *
     * @return InitEJBRemote The StatefulBean EJB object.
     * @exception javax.ejb.CreateException
     *                StatefulBean EJB object was not created.
     */
    public InitEJBRemote createDup(String string) throws CreateException, java.rmi.RemoteException;

}
