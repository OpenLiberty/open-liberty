/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.managedbean;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;
import javax.ejb.Stateful;
import javax.enterprise.context.ApplicationScoped;

/**
 *
 */
@Local
@Stateful
@ApplicationScoped
public class MyEJBBean implements MyEJBBeanLocal {

    List<String> msgList = new ArrayList<String>();

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cdi.test.managedbean.MyEJBBeanLocal#addToMsgList(java.lang.String)
     */
    @Override
    public void addToMsgList(String item) {
        msgList.add(item);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cdi.test.managedbean.MyEJBBeanLocal#getMsgList()
     */
    @Override
    public List<String> getMsgList() {
        return msgList;
    }
}
