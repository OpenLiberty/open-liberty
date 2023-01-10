/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.injection.mix.ejbint;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;
import javax.sql.DataSource;

public interface StatelessInterceptorInjectionRemote extends EJBObject {
    public String getAnnotationDSInterceptorResults() throws RemoteException;

    public String getXMLDSInterceptorResults() throws RemoteException;

    public void setAuthAliasDS(DataSource ds) throws RemoteException;

    public void setCustomLoginDS(DataSource dsCustomLogin) throws RemoteException;
}
