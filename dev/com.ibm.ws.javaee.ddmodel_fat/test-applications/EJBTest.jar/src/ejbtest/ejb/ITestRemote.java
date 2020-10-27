/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ejbtest.ejb;

import javax.ejb.EJBObject;
import java.rmi.RemoteException;

/**
 * Remote interface for Enterprise Bean: TestBean
 */
public interface ITestRemote extends EJBObject {
    int getTotal() throws RemoteException;
    int getAlive() throws RemoteException;
    int getActive() throws RemoteException;
}
