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
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBException;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class Interceptor03 implements Serializable {

    private static final String CLASS_NAME = Interceptor03.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static final long serialVersionUID = 5375172574787612684L;

    @AroundInvoke
    Object aroundInvoke(InvocationContext invCtx) throws Exception {
        CheckInvocation.getInstance().recordCallInfo("AroundInvoke", "Interceptor03.aroundInvoke", this);
        svLogger.info("Interceptor03.aroundInvoke: this=" + this);
        for (Iterator<Entry<String, Object>> it = invCtx.getContextData().entrySet().iterator(); it.hasNext();) {
            Entry<String, Object> entry = it.next();
            svLogger.info("Interceptor03.aroundInvoke: ctxData.key=" + entry.getKey() + "; ctxData.value=" + (String) entry.getValue());
            CheckInvocation.getInstance().recordCallInfo(entry.getKey(), (String) entry.getValue(), this);
        }
        String targetStr = invCtx.getTarget().toString();
        String methodStr = invCtx.getMethod().toString();
        String parameterStr = Arrays.toString(invCtx.getParameters());
        svLogger.info("Interceptor03.aroundInvoke: getTarget=" + targetStr);
        svLogger.info("Interceptor03.aroundInvoke: getMethod=" + methodStr);
        svLogger.info("Interceptor03.aroundInvoke: getParameters=" + parameterStr);
        CheckInvocation.getInstance().recordCallInfo("Target", invCtx.getTarget().toString(), this);
        CheckInvocation.getInstance().recordCallInfo("Method", invCtx.getMethod().toString(), this);
        CheckInvocation.getInstance().recordCallInfo("Parameters", Arrays.toString(invCtx.getParameters()), this);
        return invCtx.proceed();
    }

    @PostConstruct
    public void postConstruct(InvocationContext invCtx) {
        CheckInvocation.getInstance().recordCallInfo("PostConstruct", "Interceptor03.postConstruct", this);
        svLogger.info("Interceptor03.postConstruct: this=" + this);
        try {
            invCtx.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected exception", e);
        }
    }

    @PreDestroy
    private void preDestroy(InvocationContext invCtx) {
        CheckInvocation.getInstance().recordCallInfo("PreDestroy", "Interceptor03.preDestroy", this);
        svLogger.info("Interceptor03.preDestroy: this=" + this);
        try {
            invCtx.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected exception", e);
        }
    }
}
