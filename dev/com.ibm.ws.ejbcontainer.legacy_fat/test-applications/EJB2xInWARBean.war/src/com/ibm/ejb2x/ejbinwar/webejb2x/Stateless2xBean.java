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

import java.util.Arrays;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.InitialContext;

import org.junit.Assert;

@SuppressWarnings("serial")
public class Stateless2xBean implements SessionBean {
    @Override
    public void setSessionContext(SessionContext context) {
    }

    @Override
    public void ejbRemove() {
    }

    @Override
    public void ejbActivate() {
    }

    @Override
    public void ejbPassivate() {
    }

    public String test(String s) {
        return s + s;
    }

    public void testStatelessLocal() {
        try {
            Stateless2xLocalHome home = (Stateless2xLocalHome) new InitialContext().lookup("java:comp/env/ejb/Stateless2xLocal");
            Assert.assertEquals("paramparam", home.create().test("param"));
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    public void testStatefulLocal() {
        try {
            Stateful2xLocalHome home = (Stateful2xLocalHome) new InitialContext().lookup("java:comp/env/ejb/Stateful2xLocal");
            Stateful2xLocal bean = home.create("create");
            Assert.assertEquals(Arrays.asList("createcreate", "passivate", "activate", "paramparam", "passivate"), bean.test("param"));
            bean.remove();
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }
}
