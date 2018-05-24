/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.cdi.jcdi.ejb;

import java.util.logging.Logger;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.Stateful;

/**
 * Simple Stateful bean to create and remove
 **/
@Stateful(name = "SimpleStatefulOnce")
@LocalBean
@Local(SimpleStatefulRemove.class)
@Remote(SimpleStatefulRemoveRemote.class)
public class SimpleStatefulBean implements SimpleStatefulRemove {
    private static final String CLASS_NAME = SimpleStatefulBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static int svRemoveCount = 0;

    @Resource(name = "EJBName")
    protected String ivEJBName = "SimpleStatefulOnce";

    /**
     * Resets the count of removed stateful beans to zero.
     **/
    @Override
    public void resetStatefulRemoveCount() {
        svLogger.info("- " + ivEJBName + ".resetStatefulRemoveCount()");
        svRemoveCount = 0;
    }

    /**
     * Returns the count of the number of removed stateful beans.
     */
    @Override
    public int getStatefulRemoveCount() {
        svLogger.info("- " + ivEJBName + ".getStatefulRemoveCount : " + svRemoveCount);
        return svRemoveCount;
    }

    @PreDestroy
    protected void remove() {
        ++svRemoveCount;
        svLogger.info("- " + ivEJBName + ".remove : " + svRemoveCount);
    }

}
