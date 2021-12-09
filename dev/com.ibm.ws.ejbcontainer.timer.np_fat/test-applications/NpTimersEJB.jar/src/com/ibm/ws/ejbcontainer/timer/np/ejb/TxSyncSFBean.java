/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.ejb;

import static javax.ejb.TransactionAttributeType.SUPPORTS;

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.ejb.AfterBegin;
import javax.ejb.AfterCompletion;
import javax.ejb.BeforeCompletion;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;

@Stateful
public class TxSyncSFBean {
    private final static String CLASS_NAME = TxSyncSFBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    public static final String AFTER_BEGIN = "AfterBegin";
    public static final String BEFORE_COMPLETION = "BeforeCompletion";
    public static final String AFTER_COMPLETION_ROLLBACK = "AfterCompletionRollback";

    public static ArrayList<String> svInfo = new ArrayList<String>();

    private String ivInfo = null;

    @TransactionAttribute(SUPPORTS)
    public void setInfo(String info) {
        ivInfo = info;
    }

    @Remove
    public void remove() {
        svLogger.info("remove called");
    }

    @AfterBegin
    protected void afterBegin() {
        svLogger.info("afterBegin called");
        svInfo.add(AFTER_BEGIN);
    }

    @BeforeCompletion
    protected void beforeCompletion() {
        svLogger.info("beforeCompletion called");
        svInfo.add(BEFORE_COMPLETION);
    }

    @AfterCompletion
    protected void afterCompletion(boolean committed) {
        svLogger.info("afterCompletion called : " + committed);
        if (committed) {
            svInfo.add(ivInfo);
        } else {
            svInfo.add(AFTER_COMPLETION_ROLLBACK);
        }
    }
}
