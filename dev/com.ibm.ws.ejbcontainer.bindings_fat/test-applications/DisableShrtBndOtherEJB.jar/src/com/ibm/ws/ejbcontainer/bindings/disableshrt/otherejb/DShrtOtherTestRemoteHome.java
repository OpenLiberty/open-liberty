/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.disableshrt.otherejb;

import javax.ejb.CreateException;

public interface DShrtOtherTestRemoteHome extends javax.ejb.EJBHome {
    public DShrtOtherRemoteEJB create() throws CreateException, java.rmi.RemoteException;

}
