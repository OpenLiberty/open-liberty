/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.v32.fat.statefullifecycletx;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Remove;
import javax.naming.InitialContext;

import com.ibm.wsspi.uow.UOWManager;

@SuppressWarnings("serial")
public class AbstractStatefulLifecycleTxBean implements Serializable {
    private static final Logger logger = Logger.getLogger(AbstractStatefulLifecycleTxBean.class.getName());

    private static UOWManager uowManager;
    private static Map<Long, String> uowIdMethods;
    private static Set<String> methods;
    private static List<IllegalStateException> failures;

    private static void addFailure(String message) {
        logger.info("adding failure " + message);
        failures.add(new IllegalStateException(message));
    }

    public static void test(String beanName, UOWManager uowManager, boolean callerGlobalTx) throws Exception {
        AbstractStatefulLifecycleTxBean.failures = new ArrayList<IllegalStateException>();
        AbstractStatefulLifecycleTxBean.uowManager = uowManager;
        AbstractStatefulLifecycleTxBean.uowIdMethods = new HashMap<Long, String>();
        AbstractStatefulLifecycleTxBean.methods = new HashSet<String>();
        addUOWId("caller", false);

        String[] expectedMethods;

        Object o = new InitialContext().lookup("java:module/" + beanName);
        if (o instanceof StatefulLifecycleTx2xHome) {
            StatefulLifecycleTx2xHome home = (StatefulLifecycleTx2xHome) o;
            home.create().remove();

            expectedMethods = new String[] { "PostConstruct", "Init", "PrePassivate", "PostActivate", "PreDestroy" };
        } else {
            AbstractStatefulLifecycleTxBean bean = (AbstractStatefulLifecycleTxBean) o;
            bean.remove(callerGlobalTx);

            expectedMethods = new String[] { "PostConstruct", "PrePassivate", "PostActivate", "Remove", "PreDestroy" };
        }

        for (String method : expectedMethods) {
            if (!methods.contains(method)) {
                addFailure("method " + method + " not called");
            }
        }

        if (!failures.isEmpty()) {
            throw failures.get(0);
        }
    }

    private static String uowTypeToString(int uowType) {
        switch (uowType) {
            case UOWManager.UOW_TYPE_GLOBAL_TRANSACTION:
                return "global";
            case UOWManager.UOW_TYPE_LOCAL_TRANSACTION:
                return "local";
            case UOWManager.UOW_TYPE_ACTIVITYSESSION:
                return "activity";
        }
        return "unknown";
    }

    private static void addUOWId(String method, boolean callerGlobalTx) {
        long uowId = uowManager.getLocalUOWId();
        logger.info("adding uowId=0x" + Long.toHexString(uowId) + " for " + method);

        if (callerGlobalTx) {
            if (!uowIdMethods.containsKey(uowId)) {
                addFailure("expected uowId=0x" + Long.toHexString(uowId) +
                           " to have already been used by the caller");
            }
        } else {
            String oldMethod = uowIdMethods.put(uowId, method);
            if (oldMethod != null) {
                addFailure("unexpected uowId=0x" + Long.toHexString(uowId) +
                           " for " + method + " already used by " + oldMethod);
            }
        }

        if (!methods.add(method)) {
            addFailure("unexpected method " + method + " already called");
        }
    }

    private static void assertUOWType(int uowTypeExpected) {
        int uowTypeActual = uowManager.getUOWType();
        if (uowTypeActual != uowTypeExpected) {
            addFailure("expected uowType=" + uowTypeExpected + " (" + uowTypeToString(uowTypeExpected) + "), " +
                       "got " + uowTypeActual + " (" + uowTypeToString(uowTypeActual) + ")");
        }
    }

    @Resource
    private EJBContext context;

    protected void checkLocalUOW(String reason) {
        assertUOWType(UOWManager.UOW_TYPE_LOCAL_TRANSACTION);
        addUOWId(reason, false);

        try {
            context.setRollbackOnly();
            addFailure("unexpected successful setRollbackOnly");
        } catch (IllegalStateException e) {
            logger.info("ignoring setRollbackOnly exception: " + e);
        }

        try {
            context.getRollbackOnly();
            addFailure("unexpected successful getRollbackOnly");
        } catch (IllegalStateException e) {
            logger.info("ignoring getRollbackOnly exception: " + e);
        }
    }

    protected void checkGlobalUOW(String reason, boolean callerGlobalTx) {
        assertUOWType(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION);
        addUOWId(reason, callerGlobalTx);

        try {
            context.setRollbackOnly();
        } catch (IllegalStateException e) {
            failures.add(e);
        }

        try {
            if (!context.getRollbackOnly()) {
                addFailure("expected getRollbackOnly()=true");
            }
        } catch (IllegalStateException e) {
            failures.add(e);
        }
    }

    @Remove
    public void remove(boolean callerGlobalTx) {
        logger.info("remove");
        assertUOWType(UOWManager.UOW_TYPE_GLOBAL_TRANSACTION);
        addUOWId("Remove", callerGlobalTx);
    }
}
