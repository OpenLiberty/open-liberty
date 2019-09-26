/*******************************************************************************
 * Copyright (c) 2010, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejb2x.ejbinwar.intf;

import javax.ejb.CreateException;
import javax.naming.NamingException;

public interface BasicLooseStatelessInterface {
    public boolean callVerifyComp2xStatefulLocalLookup() throws NamingException, CreateException;

    public boolean callVerifyComp2xStatefulRemoteLookup() throws NamingException, CreateException;

    public boolean callVerifyXMLComp2xStatefulLocalLookup() throws NamingException, CreateException;

    public boolean callVerifyXMLComp2xStatefulRemoteLookup() throws NamingException, CreateException;
}
