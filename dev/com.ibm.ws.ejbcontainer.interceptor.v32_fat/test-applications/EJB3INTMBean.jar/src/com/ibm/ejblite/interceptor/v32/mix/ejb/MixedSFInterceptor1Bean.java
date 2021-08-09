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
package com.ibm.ejblite.interceptor.v32.mix.ejb;

import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.Local;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptors;
import javax.interceptor.InvocationContext;

//import javax.annotation.PreDestroy;

@Local(MixedSFLocal.class)
@Stateful
@Interceptors({ MixedInterceptor.class })
public class MixedSFInterceptor1Bean {
    private static final String LOGGER_CLASS_NAME = MixedSFInterceptor1Bean.class.getName();
    private final static Logger svLogger = Logger.getLogger(LOGGER_CLASS_NAME);

    private String ivString;

    @Interceptors({ MLInterceptor1.class })
    public void setString(String str) {
        ivString = str;
    }

    public String getString() {
        return ivString;
    }

    /** Name of this class */
    private static final String CLASS_NAME = "MixedSFInterceptor1Bean";

    @AroundInvoke
    public Object aroundInvoke(InvocationContext inv) throws Exception {
        ResultsLocal results = ResultsLocalBean.getSFBean();
        results.addAroundInvoke(CLASS_NAME, "aroundInvoke");

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

    @PostConstruct
    private void postConstruct() {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            results.addPostConstruct(CLASS_NAME, "postConstruct");
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    @SuppressWarnings("unused")
    // @PreDestroy - defined in XML
    private void preDestroy() {
        try {
            ResultsLocal results = ResultsLocalBean.getSFBean();
            results.addPreDestroy(CLASS_NAME, "preDestroy");
        } catch (Exception e) {
            throw new EJBException("unexpected Throwable", e);
        }
    }

    @Remove
    // @Interceptors({ MLInterceptor2.class })
    // Interceptor defined in XML
    public void destroy() {
        svLogger.info(CLASS_NAME + ".destroy");
    }
}
