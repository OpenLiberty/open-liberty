/*******************************************************************************
 * Copyright (c) 2001, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.rmi.*;

/**
 * UserTransactionEnabledContext provides an interface that BeanO classes
 * can implement to signify they provide functionality for obtaining
 * a UserTransaction through its Context.
 * 
 * Currently, Session beans and MessageDrivenBeans, are designed to
 * allow UserTransactions to be made available through their respective
 * contexts.
 **/
public interface UserTransactionEnabledContext {
    public int getIsolationLevel();

    public boolean enlist(ContainerTx tx) // d114677
    throws RemoteException;

    public BeanId getId();

    public int getModuleVersion();//d140003.20
}
