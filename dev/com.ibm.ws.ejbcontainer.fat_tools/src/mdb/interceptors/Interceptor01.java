/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or other-
 * wise divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
package mdb.interceptors;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBException;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class Interceptor01 implements Serializable {

    private static final long serialVersionUID = 9027385894354125019L;
    private static final String CLASS_NAME = Interceptor01.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    @AroundInvoke
    private Object aroundInvoke(InvocationContext invCtx) throws Exception {
        CheckInvocation.getInstance().recordCallInfo("AroundInvoke", "Interceptor01.aroundInvoke", this);
        svLogger.info("Interceptor01.aroundInvoke: this=" + this);
        return invCtx.proceed();
    }

    @PostConstruct
    protected void postConstruct(InvocationContext invCtx) {
        CheckInvocation.getInstance().recordCallInfo("PostConstruct", "Interceptor01.postConstruct", this);
        svLogger.info("Interceptor01.postConstruct: this=" + this);
        try {
            invCtx.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected exception", e);
        }
    }

    @PreDestroy
    void preDestroy(InvocationContext invCtx) {
        CheckInvocation.getInstance().recordCallInfo("PreDestroy", "Interceptor01.preDestroy", this);
        svLogger.info("Interceptor01.preDestroy: this=" + this);
        try {
            invCtx.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected exception", e);
        }
    }
}
