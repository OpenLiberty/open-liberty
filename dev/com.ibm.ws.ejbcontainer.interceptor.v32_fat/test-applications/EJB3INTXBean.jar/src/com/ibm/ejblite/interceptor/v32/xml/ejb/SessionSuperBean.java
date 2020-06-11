/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejblite.interceptor.v32.xml.ejb;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.interceptor.InvocationContext;

/**
 * SessionSuperBean is used as a super class for either a Stateless or Stateful
 * Session Bean during testing of EJB 3 interceptor method invocation.
 */
public class SessionSuperBean {

    /**
     * Definitions for the logger
     */
    private final static String CLASSNAME = SessionSuperBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** Name of this class */
    private static final String CLASS_NAME = "SessionSuperBean";

    // @AroundInvoke, defined in xml
    public Object superAroundInvoke(InvocationContext inv) throws Exception {
        svLogger.info(CLASS_NAME + ".superAroundInvoke");
        ResultsLocal results = ResultsLocalBean.getSFBean();
        results.addAroundInvoke(CLASS_NAME, "superAroundInvoke");
        Method bm = inv.getMethod();

        if (bm.getName().equals("sum")) {
            Object[] args = inv.getParameters();
            Integer i1 = (Integer) args[0];
            Integer i2 = (Integer) args[1];
            Integer[] newArgs = new Integer[2];
            newArgs[0] = new Integer(i1.intValue() + 10);
            newArgs[1] = new Integer(i2.intValue() + 10);
            inv.setParameters(newArgs);
        } else if (bm.getName().equals("badSum1")) {
            try {
                Object[] args = inv.getParameters();
                Integer i2 = (Integer) args[1];
                Object[] newArgs = new Object[2];
                newArgs[0] = new String("badValue");
                newArgs[1] = new Integer(i2.intValue());
                inv.setParameters(newArgs);
            } catch (IllegalArgumentException iae) {
                svLogger.info("Caught expected exception in superAroundInvoke: "
                              + iae.getMessage());
                results.addException(CLASS_NAME, "superAroundInvoke",
                                     "IllegalArgumentException");
            }
        } else if (bm.getName().equals("badSum2")) {
            Object[] args = inv.getParameters();
            Integer i1 = (Integer) args[0];
            Object[] newArgs = new Object[2];
            newArgs[0] = new Integer(i1.intValue());
            newArgs[1] = new String("badValue");
            inv.setParameters(newArgs);
        } else if (bm.getName().equals("badSum3")) {
            try {
                Object[] args = inv.getParameters();
                Integer i1 = (Integer) args[0];
                Integer i2 = (Integer) args[1];
                Integer[] newArgs = new Integer[3];
                newArgs[0] = new Integer(i1.intValue() + 10);
                newArgs[1] = new Integer(i2.intValue() + 10);
                newArgs[2] = new Integer(310);
                inv.setParameters(newArgs);
            } catch (IllegalArgumentException iae) {
                svLogger.info("Caught expected exception in superAroundInvoke: "
                              + iae.getMessage());
                throw new FVTWrapperException(iae);
            }
        } else if (bm.getName().equals("target")) {
            Object[] newArgs = new Object[1];
            newArgs[0] = inv.getTarget();
            inv.setParameters(newArgs);
        }

        // Validate and update context data.
        Map<String, Object> map = inv.getContextData();
        String data;
        if (map.containsKey("AroundInvoke")) {
            data = (String) map.get("AroundInvoke");
            data = data + ":" + CLASS_NAME;
        } else {
            data = CLASS_NAME;

        }

        map.put("AroundInvoke", data);
        results.setAroundInvokeContextData(data);

        // Verify around invoke does not see any context data from lifecycle
        // callback events.
        if (map.containsKey("PostConstruct")) {
            throw new IllegalStateException("PostConstruct context data shared with AroundInvoke interceptor");
        } else if (map.containsKey("PostActivate")) {
            throw new IllegalStateException("PostActivate context data shared with AroundInvoke interceptor");
        } else if (map.containsKey("PrePassivate")) {
            throw new IllegalStateException("PrePassivate context data shared with AroundInvoke interceptor");
        } else if (map.containsKey("PreDestroy")) {
            throw new IllegalStateException("PreDestroy context data shared with AroundInvoke interceptor");
        }

        // Verify same InvocationContext passed to each AroundInvoke
        // interceptor method.
        InvocationContext ctxData;
        if (map.containsKey("InvocationContext")) {
            ctxData = (InvocationContext) map.get("InvocationContext");
            if (inv != ctxData) {
                throw new IllegalStateException("Same InvocationContext not passed to each AroundInvoke interceptor");
            }
        } else {
            map.put("InvocationContext", inv);
        }

        Object rv = inv.proceed();
        return rv;
    }

    // @PostConstruct, defined in xml
    public void superPostConstruct() {
        svLogger.info(CLASS_NAME + ".superPostConstruct");
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            results.addPostConstruct(CLASS_NAME, "superPostConstruct");

        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    // @PostActivate, defined in xml
    public void superPostActivate() {
        svLogger.info(CLASS_NAME + ".superPostActivate");
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            results.addPostActivate(CLASS_NAME, "superPostActivate");
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    // @PrePassivate, defined in xml
    public void superPrePassivate() {
        svLogger.info(CLASS_NAME + ".superPrePassivate");
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            results.addPrePassivate(CLASS_NAME, "superPrePassivate");

        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    // @PreDestroy, defined in xml
    public void superPreDestroy() {
        svLogger.info(CLASS_NAME + ".superPreDestroy");
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            results.addPreDestroy(CLASS_NAME, "superPreDestroy");

        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }
}
