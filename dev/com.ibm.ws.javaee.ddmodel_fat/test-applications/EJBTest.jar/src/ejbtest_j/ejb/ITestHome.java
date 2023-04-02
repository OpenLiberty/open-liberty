/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
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
package ejbtest_j.ejb;

import jakarta.ejb.CreateException;
import jakarta.ejb.EJBHome;
import java.rmi.RemoteException;

/**
 * Home interface for Enterprise Bean: TestBean
 */
public interface ITestHome extends EJBHome {
    /**
     * Creates a default instance of Session Bean: TestBean
     */
    ITestRemote create(String ID) throws CreateException, RemoteException;
}
