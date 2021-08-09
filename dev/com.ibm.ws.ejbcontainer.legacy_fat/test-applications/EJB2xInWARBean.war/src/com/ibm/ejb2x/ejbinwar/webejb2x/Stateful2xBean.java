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
package com.ibm.ejb2x.ejbinwar.webejb2x;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

@SuppressWarnings("serial")
public class Stateful2xBean implements SessionBean {
    private List<String> ivResult = new ArrayList<String>();

    @Override
    public void setSessionContext(SessionContext context) {
    }

    public void ejbCreate(String s) {
        ivResult.add(s + s);
    }

    @Override
    public void ejbRemove() {
    }

    @Override
    public void ejbActivate() {
        ivResult.add("activate");
    }

    @Override
    public void ejbPassivate() {
        ivResult.add("passivate");
        new Throwable(this + ".passivate").printStackTrace(System.out);
    }

    public List<String> test(String s) {
        ivResult.add(s + s);
        return ivResult;
    }
}
