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

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

@SuppressWarnings("serial")
public class XMLComp2xViewStatefulBean implements SessionBean {
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

    public boolean verifyXMLComp2xStatefulLookup() {
        return true;
    }
}
