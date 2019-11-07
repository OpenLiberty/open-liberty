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
package com.ibm.ejb2x.ejbinwar.web;

import javax.ejb.LocalHome;
import javax.ejb.RemoteHome;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;

import com.ibm.ejb2x.ejbinwar.intf.Comp2xViewStatefulLocalHome;
import com.ibm.ejb2x.ejbinwar.intf.Comp2xViewStatefulRemoteHome;

@SuppressWarnings("serial")
@Stateful
@LocalHome(Comp2xViewStatefulLocalHome.class)
@RemoteHome(Comp2xViewStatefulRemoteHome.class)
public class Comp2xViewStatefulBean implements SessionBean {
    public void ejbCreate() {
    }

    @Override
    public void ejbActivate() {
    }

    @Override
    public void ejbPassivate() {
    }

    @Override
    public void ejbRemove() {
    }

    @Override
    public void setSessionContext(SessionContext arg0) {
    }

    public boolean verifyComp2xStatefulLookup() {
        return true;
    }
}
