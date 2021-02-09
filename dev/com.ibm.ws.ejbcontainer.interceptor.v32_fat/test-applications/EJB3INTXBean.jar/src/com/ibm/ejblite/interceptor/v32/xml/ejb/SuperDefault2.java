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

import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.interceptor.InvocationContext;

/**
 * Super class of a default interceptor class for FVT. All of the interceptor
 * methods in this class are private in order to test whether EJB container can
 * invoke private methods on interceptor class as required by EJB 3
 * specification.
 */
public abstract class SuperDefault2 {

    /**
     * Definitions for the logger
     */
    private final static String CLASSNAME = SuperDefault2.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** Name of this class */
    private static final String CLASS_NAME = "SuperDefault2";

    /**
     * Unique instance ID for this interceptor instance.
     */
    protected String ivInstanceId;

    Object getResultsObject(InvocationContext inv) throws Exception {
        svLogger.info("SD2 Invocation target: "
                      + inv.getTarget().getClass().getName());
        ResultsLocal results = ResultsLocalBean.getSFBean();
        return results;
    }

    void addAroundInvoke(Object results, String className, String msg) throws Exception {
        ((ResultsLocal) results).addAroundInvoke(className, msg);
    }

    void setAroundInvokeContextData(Object results, String data) throws Exception {
        ((ResultsLocal) results).setAroundInvokeContextData(data);
    }

    void addPostConstruct(Object results, String className, String msg) throws Exception {
        ((ResultsLocal) results).addPostConstruct(className, msg);
    }

    void setPostContructContextData(Object results, String data) throws Exception {
        ((ResultsLocal) results).setPostContructContextData(data);
    }

    void addPostActivate(Object results, String className, String msg) throws Exception {
        ((ResultsLocal) results).addPostActivate(className, msg);
    }

    void setPostActivateContextData(Object results, String data) throws Exception {
        ((ResultsLocal) results).setPostActivateContextData(data);
    }

    void addPrePassivate(Object results, String className, String msg) throws Exception {
        ((ResultsLocal) results).addPrePassivate(className, msg);
    }

    void setPrePassivateContextData(Object results, String data) throws Exception {
        ((ResultsLocal) results).setPrePassivateContextData(data);
    }

    void addPreDestroy(Object results, String className, String msg) throws Exception {
        ((ResultsLocal) results).addPreDestroy(className, msg);
    }

    void setPreDestroyContextData(Object results, String data) throws Exception {
        ((ResultsLocal) results).setPreDestroyContextData(data);
    }

    @SuppressWarnings("unused")
    private Object superAroundInvoke(InvocationContext inv) throws Exception {
        Object results = getResultsObject(inv);
        addAroundInvoke(results, CLASS_NAME, "superAroundInvoke");

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
        setAroundInvokeContextData(results, data);

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

    @SuppressWarnings("unused")
    private void superPostConstruct(InvocationContext inv) {
        try {
            Object results = getResultsObject(inv);
            addPostConstruct(results, CLASS_NAME, "superPostConstruct");

            // Validate and update context data.
            Map<String, Object> map = inv.getContextData();
            String data;
            if (map.containsKey("PostConstruct")) {
                data = (String) map.get("PostConstruct");
                data = data + ":" + CLASS_NAME;
            } else {
                data = CLASS_NAME;

            }

            map.put("PostConstruct", data);
            setPostContructContextData(results, data);

            // Verify around invoke does not see any context data from lifecycle
            // callback events.
            if (map.containsKey("PostActivate")) {
                throw new IllegalStateException("PostActivate context data shared with PostConstruct interceptor");
            } else if (map.containsKey("AroundInvoke")) {
                throw new IllegalStateException("AroundInvoke context data shared with PostConstruct interceptor");
            } else if (map.containsKey("PrePassivate")) {
                throw new IllegalStateException("PrePassivate context data shared with PostConstruct interceptor");
            } else if (map.containsKey("PreDestroy")) {
                throw new IllegalStateException("PreDestroy context data shared with PostConstruct interceptor");
            }

            // Verify same InvocationContext passed to each PostConstruct
            // interceptor method.
            InvocationContext ctxData;
            if (map.containsKey("InvocationContext")) {
                ctxData = (InvocationContext) map.get("InvocationContext");
                if (inv != ctxData) {
                    throw new IllegalStateException("Same InvocationContext not passed to each PostConstruct interceptor");
                }
            } else {
                map.put("InvocationContext", inv);
            }

            inv.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    @SuppressWarnings("unused")
    private void superPostActivate(InvocationContext inv) {
        try {
            Object results = getResultsObject(inv);
            addPostActivate(results, CLASS_NAME, "superPostActivate");
            // Validate and update context data.
            Map<String, Object> map = inv.getContextData();
            String data;
            if (map.containsKey("PostActivate")) {
                data = (String) map.get("PostActivate");
                data = data + ":" + CLASS_NAME;
            } else {
                data = CLASS_NAME;

            }

            map.put("PostActivate", data);
            setPostActivateContextData(results, data);

            // Verify around invoke does not see any context data from lifecycle
            // callback events.
            if (map.containsKey("PostConstruct")) {
                throw new IllegalStateException("PostConstruct context data shared with PostActivate interceptor");
            } else if (map.containsKey("AroundInvoke")) {
                throw new IllegalStateException("AroundInvoke context data shared with PostActivate interceptor");
            } else if (map.containsKey("PrePassivate")) {
                throw new IllegalStateException("PrePassivate context data shared with PostActivate interceptor");
            } else if (map.containsKey("PreDestroy")) {
                throw new IllegalStateException("PreDestroy context data shared with PostActivate interceptor");
            }

            // Verify same InvocationContext passed to each PostActivate
            // interceptor method.
            InvocationContext ctxData;
            if (map.containsKey("InvocationContext")) {
                ctxData = (InvocationContext) map.get("InvocationContext");
                if (inv != ctxData) {
                    throw new IllegalStateException("Same InvocationContext not passed to each PostActivate interceptor");
                }
            } else {
                map.put("InvocationContext", inv);
            }
            inv.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    @SuppressWarnings("unused")
    private void superPrePassivate(InvocationContext inv) {
        try {
            Object results = getResultsObject(inv);
            addPrePassivate(results, CLASS_NAME, "superPrePassivate");

            // Validate and update context data.
            Map<String, Object> map = inv.getContextData();
            String data;
            if (map.containsKey("PrePassivate")) {
                data = (String) map.get("PrePassivate");
                data = data + ":" + CLASS_NAME;
            } else {
                data = CLASS_NAME;
            }

            map.put("PrePassivate", data);
            setPrePassivateContextData(results, data);

            // Verify around invoke does not see any context data from lifecycle
            // callback events.
            if (map.containsKey("PostActivate")) {
                throw new IllegalStateException("PostActivate context data shared with PrePassivate interceptor");
            } else if (map.containsKey("AroundInvoke")) {
                throw new IllegalStateException("AroundInvoke context data shared with PrePassivate interceptor");
            } else if (map.containsKey("PreDestroy")) {
                throw new IllegalStateException("PreDestroy context data shared with PrePassivate interceptor");
            } else if (map.containsKey("PostConstruct")) {
                throw new IllegalStateException("PostConstruct context data shared with PrePassivate interceptor");
            }

            // Verify same InvocationContext passed to each PrePassivate
            // interceptor method.
            InvocationContext ctxData;
            if (map.containsKey("InvocationContext")) {
                ctxData = (InvocationContext) map.get("InvocationContext");
                if (inv != ctxData) {
                    throw new IllegalStateException("Same InvocationContext not passed to each PrePassivate interceptor");
                }
            } else {
                map.put("InvocationContext", inv);
            }

            inv.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    @SuppressWarnings("unused")
    private void superPreDestroy(InvocationContext inv) {
        try {
            Object results = getResultsObject(inv);
            addPreDestroy(results, CLASS_NAME, "superPreDestroy");

            // Validate and update context data.
            Map<String, Object> map = inv.getContextData();
            String data;
            if (map.containsKey("PreDestroy")) {
                data = (String) map.get("PreDestroy");
                data = data + ":" + CLASS_NAME;
            } else {
                data = CLASS_NAME;
            }

            map.put("PreDestroy", data);
            setPreDestroyContextData(results, data);

            // Verify around invoke does not see any context data from lifecycle
            // callback events.
            if (map.containsKey("PostActivate")) {
                throw new IllegalStateException("PostActivate context data shared with PreDestroy interceptor");
            } else if (map.containsKey("AroundInvoke")) {
                throw new IllegalStateException("AroundInvoke context data shared with PreDestroy interceptor");
            } else if (map.containsKey("PrePassivate")) {
                throw new IllegalStateException("PrePassivate context data shared with PreDestroy interceptor");
            } else if (map.containsKey("PostConstruct")) {
                throw new IllegalStateException("PostConstruct context data shared with PreDestroy interceptor");
            }

            // Verify same InvocationContext passed to each PreDestroy
            // interceptor method.
            InvocationContext ctxData;
            if (map.containsKey("InvocationContext")) {
                ctxData = (InvocationContext) map.get("InvocationContext");
                if (inv != ctxData) {
                    throw new IllegalStateException("Same InvocationContext not passed to each PreDestroy interceptor");
                }
            } else {
                map.put("InvocationContext", inv);
            }

            inv.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }
}
