/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat.mix.ejb;

import java.util.logging.Logger;

import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.Stateless;

/**
 * Bean implementation class for Enterprise Bean
 **/
@Stateless
@Local(InheritanceAnnLocal.class)
@Asynchronous
public class InheritanceStatelessAnnBean extends SuperClass {
    public final static String CLASSNAME = InheritanceStatelessAnnBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    public static volatile long asyncMethThreadId;
    public static volatile long asyncMeth2ThreadId;

    public void test_beanMethodAsync(String param) {
        svLogger.info("--> Entering method, test_beanMethodAsync, that has a String parameter: " + param);

        asyncMethThreadId = Thread.currentThread().getId();
        svLogger.info("--> Since the method should be asynchronous it should run under a different thread, asyncMethThreadId = " + asyncMethThreadId);
        svInheritanceLatch.countDown();

        svLogger.info("--> Exiting method, test_metaDataComplete, that has a String parameter.");
        return;
    }

    public void test_AnnAndXMLMethodAsync(String param) {
        svLogger.info("--> Entering method, test_AnnAndXMLMethodAsync, that has a String parameter: " + param);

        asyncMeth2ThreadId = Thread.currentThread().getId();
        svLogger.info("--> Since the method should be asynchronous it should run under a different thread, asyncMeth2ThreadId = " + asyncMeth2ThreadId);
        svInheritanceLatch.countDown();

        svLogger.info("--> Exiting method, test_AnnAndXMLMethodAsync, that has a String parameter.");
        return;
    }
}