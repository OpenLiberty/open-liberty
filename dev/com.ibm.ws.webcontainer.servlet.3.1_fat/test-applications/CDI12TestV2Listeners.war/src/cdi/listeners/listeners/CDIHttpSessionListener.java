/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi.listeners.listeners;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import cdi.beans.v2.CDICaseInjection;
import cdi.beans.v2.CDIDataBean;
import cdi.beans.v2.log.ApplicationLog;
import cdi.listeners.beans.CDIApplicationFieldBean_L;
import cdi.listeners.beans.CDIConstructorBean_SL;
import cdi.listeners.beans.CDIDependentFieldBean_SL;
import cdi.listeners.beans.CDIListenerType_SL;
import cdi.listeners.beans.CDIMethodBean_SL;
import cdi.listeners.beans.CDISessionFieldBean_SL;
import cdi.listeners.interceptors.SessionListenerType;

@WebListener("CDI Test Http Session Listener")
public class CDIHttpSessionListener implements HttpSessionListener {
    private static final String LOG_CLASS_NAME = CDIHttpSessionListener.class.getSimpleName();
    private static final Logger LOG = Logger.getLogger(CDIHttpSessionListener.class.getName());

    private static String logInfo(String methodName, String text) {
        String logText = LOG_CLASS_NAME + ": " + methodName + ": " + text;
        LOG.info(logText);
        return logText;
    }

    //

    @Inject
    ApplicationLog applicationLog;

    private void applicationLog(String methodName, String text) {
        applicationLog.addLine(LOG_CLASS_NAME + ": " + methodName + ": " + text);
    }

    private void applicationLog(String text) {
        applicationLog.addLine(text);
    }

    //

    private void logEvent(String methodName, HttpSessionEvent sessionEvent) {
        String[] eventLines = CDIEventPrinter.getEventText(sessionEvent);

        for (String eventLine : eventLines) {
            logInfo(methodName, eventLine);
        }

        for (String eventLine : eventLines) {
            applicationLog(methodName, eventLine);
        }
    }

    //

    @Override
    @SessionListenerType
    public void sessionCreated(HttpSessionEvent sessionEvent) {
        String methodName = "sessionCreated";
        logEvent(methodName, sessionEvent);

        addBeanData("SL:C" + sessionEvent.getSession().getAttribute("SLInterceptor"));
        logBeanData();
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent sessionEvent) {
        String methodName = "sessionDestroyed";
        logEvent(methodName, sessionEvent);

        // Don't track session destruction.
        // We would have to reliably sequence the second session destruction
        // with the other events, and that's not worth doing for the
        // injection tests.

        // addBeanData("SL:D");
        // logBeanData();
    }

    //

    // Test case: Constructor injection
    private final CDIDataBean constructorBean;

    @Inject
    public CDIHttpSessionListener(CDIConstructorBean_SL constructorBean) {
        this.constructorBean = constructorBean;
    }

    // Test case: Post-construct injection.
    private String postConstruct;

    @PostConstruct
    void start() {
        postConstruct = "Start";
    }

    // Test case: Pre-destroy injection.
    // TODO: Not entirely sure how to make this work.  Have it dummied up for now.
    @PreDestroy
    void stop() {
        setPreDestroy("Stop");
    }

    public void setPreDestroy(String preDestroy) {
        // TODO
    }

    public String getPreDestroy() {
        // TODO
        return "Stop";
    }

    // Test case: Qualified bean injection
    @Inject
    @CDIListenerType_SL
    private CDIDataBean fieldBean;

    // Test case: Initializer method injection.
    private CDIDataBean methodBean;

    @Inject
    public void setMethodBean(CDIMethodBean_SL methodBean) {
        this.methodBean = methodBean;
    }

    // Test case: Dependent bean injection
    @Inject
    private CDIDependentFieldBean_SL dependentFieldBean;

    // No request scope is available for a session listener.
    // Test case: Request bean injection
    // @Inject
    // private CDIRequestFieldBean_SL requestFieldBean;

    // Test case: Session bean injection
    @Inject
    private CDISessionFieldBean_SL sessionFieldBean;

    // Test case: Application bean injection
    @Inject
    private CDIApplicationFieldBean_L applicationFieldBean;

    //

    private void addBeanData(String data) {
        constructorBean.addData(data);
        postConstruct = ((postConstruct == null) ? data : (postConstruct + ":" + data));
        // setPreDestroy(data);
        fieldBean.addData(data);
        methodBean.addData(data);
        dependentFieldBean.addData(data);
        // requestFieldBean.addData(data);
        sessionFieldBean.addData(data);
        applicationFieldBean.addData(data);
    }

    //

    private void logBeanData(String methodName, String beanText) {
        logInfo(methodName, beanText);
        applicationLog(beanText);
    }

    private void logBeanData() {
        String methodName = "logBeanData";

        logBeanData(methodName, getBeanText(constructorBean, CDICaseInjection.Constructor));
        logBeanData(methodName, getBeanText(postConstruct, CDICaseInjection.PostConstruct));
        // getBeanText(getPreDestroy(), CDICaseInjection.PreDestroy);
        logBeanData(methodName, getBeanText(fieldBean, CDICaseInjection.Field));
        logBeanData(methodName, getBeanText(methodBean, CDICaseInjection.Method));
        logBeanData(methodName, getBeanText(dependentFieldBean, CDICaseInjection.Field));
        // logBeanData(methodName, getBeanText(requestFieldBean, CDICaseInjection.Field));
        logBeanData(methodName, getBeanText(sessionFieldBean, CDICaseInjection.Field));
        logBeanData(methodName, getBeanText(applicationFieldBean, CDICaseInjection.Field));
    }

    //

    public String getTypeName() {
        return LOG_CLASS_NAME;
    }

    private String prependType(String responseText) {
        return (":" + getTypeName() + ":" + responseText + ":");
    }

    private String getBeanText(CDIDataBean dataBean, CDICaseInjection injectionCase) {
        String beanText = injectionCase.getTag() + ":";
        beanText += ((dataBean == null) ? "Failed" : dataBean.getData());
        return prependType(beanText);
    }

    private String getBeanText(String value, CDICaseInjection injectionCase) {
        value = (injectionCase.getTag() + ":" + ((value == null) ? "Failed" : value));
        return prependType(value);
    }
}
