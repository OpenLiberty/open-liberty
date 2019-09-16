/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.home;

import java.util.List;

import javax.annotation.Resource;
import javax.ejb.RemoteHome;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

@Stateless
@RemoteHome(TestEJBHome.class)
public class TestEJBHomeBean {
    @Resource
    private SessionContext context;

    public String echo(String s) {
        return s;
    }

    public TestEJBHome lookupTestEJBHome(String s) {
        return (TestEJBHome) context.lookup(s);
    }

    public TestEJBObject getSessionContextEJBObject() {
        return (TestEJBObject) context.getEJBObject();
    }

    public TestEJBHome getSessionContextEJBHome() {
        // The spec does not require this cast to work without a narrow, but
        // this is likely an oversight given it does require getEJBObject to be
        // directly castable.
        return (TestEJBHome) context.getEJBHome();
    }

    public List<?> testWriteValue(List<?> list) {
        return list;
    }
}
