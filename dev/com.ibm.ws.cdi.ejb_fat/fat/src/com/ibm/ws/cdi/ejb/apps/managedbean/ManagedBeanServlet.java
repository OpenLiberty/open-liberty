/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.managedbean;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 *
 */
@WebServlet("")
public class ManagedBeanServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 1599035198651566335L;
    @Resource
    MyManagedBean myManagedBean;

    @Test
    public void testManagedBeanInterceptor() {
        List<String> allMsg = new ArrayList<String>();

        allMsg.addAll(CounterUtil.getMsgList());
        allMsg.addAll(myManagedBean.getMsgList());

        assertEquals(7, allMsg.size());
        assertEquals("MyNonCDIInterceptor:AroundConstruct called injectedInt:16", allMsg.get(0));
        assertEquals("MyCDIInterceptor:AroundConstruct called injectedStr:HelloYou", allMsg.get(1));
        assertEquals("MyNonCDIInterceptor:PostConstruct called injectedInt:16", allMsg.get(2));
        assertEquals("MyCDIInterceptor:PostConstruct called injectedStr:HelloYou", allMsg.get(3));
        assertEquals("MyManagedBean called postConstruct()", allMsg.get(4));
        assertEquals("MyNonCDIInterceptor:AroundInvoke called injectedInt:16", allMsg.get(5));
        assertEquals("MyCDIInterceptor:AroundInvoke called injectedStr:HelloYou", allMsg.get(6));
    }
}
