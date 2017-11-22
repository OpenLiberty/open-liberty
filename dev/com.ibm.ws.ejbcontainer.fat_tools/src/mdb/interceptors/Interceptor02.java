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

public class Interceptor02 implements Serializable {

    private static final long serialVersionUID = 7354759478072013626L;
    private static final String CLASS_NAME = Interceptor02.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    @AroundInvoke
    protected Object aroundInvoke(InvocationContext invCtx) throws Exception {
        CheckInvocation.getInstance().recordCallInfo("AroundInvoke", "Interceptor02.aroundInvoke", this);
        svLogger.info("Interceptor02.aroundInvoke: this=" + this);
        return invCtx.proceed();
    }

    @PostConstruct
    void postConstruct(InvocationContext invCtx) {
        CheckInvocation.getInstance().recordCallInfo("PostConstruct", "Interceptor02.postConstruct", this);
        svLogger.info("Interceptor02.postConstruct: this=" + this);
        try {
            invCtx.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected exception", e);
        }
    }

    @PreDestroy
    public void preDestroy(InvocationContext invCtx) {
        CheckInvocation.getInstance().recordCallInfo("PreDestroy", "Interceptor02.preDestroy", this);
        svLogger.info("Interceptor02.preDestroy: this=" + this);
        try {
            invCtx.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected exception", e);
        }
    }
}
