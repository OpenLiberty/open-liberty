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

import javax.ejb.EJBException;
import javax.interceptor.InvocationContext;

/**
 * Super class of a class level interceptor class for FVT. All of the
 * interceptor methods in this class are protected in order to test whether EJB
 * container can invoke protected methods on interceptor class as required by
 * EJB 3 specification. Note, the intent is that none of the methods are
 * overridden by a subclass so that we can show that interceptor methods are
 * invoked in both the interceptor class and its super class.
 */
public abstract class SuperCLInterceptor2 {
    /** Name of this class */
    private static final String CLASS_NAME = "SuperCLInterceptor2";

    /**
     * Unique instance ID for this interceptor instance.
     */
    protected String ivInstanceId;

    protected Object superAroundInvoke(InvocationContext inv) throws Exception {
        ResultsLocal results = ResultsLocalBean.getSFBean();
        results.addAroundInvoke(CLASS_NAME, "superAroundInvoke");

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

    protected void superPostConstruct(InvocationContext inv) {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            results.addPostConstruct(CLASS_NAME, "superPostConstruct");

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
            results.setPostContructContextData(data);

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

    protected void superPostActivate(InvocationContext inv) {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            results.addPostActivate(CLASS_NAME, "superPostActivate");
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
            results.setPostActivateContextData(data);

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

    protected void superPrePassivate(InvocationContext inv) {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            results.addPrePassivate(CLASS_NAME, "superPrePassivate");

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
            results.setPrePassivateContextData(data);

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

    protected void superPreDestroy(InvocationContext inv) {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            results.addPreDestroy(CLASS_NAME, "superPreDestroy");

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
            results.setPreDestroyContextData(data);

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
