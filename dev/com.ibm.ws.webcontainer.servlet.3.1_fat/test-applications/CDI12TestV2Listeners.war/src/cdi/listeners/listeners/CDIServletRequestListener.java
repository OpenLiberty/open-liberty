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
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;

import cdi.beans.v2.CDICaseInjection;
import cdi.beans.v2.CDIDataBean;
import cdi.beans.v2.log.ApplicationLog;
import cdi.listeners.beans.CDIApplicationFieldBean_L;
import cdi.listeners.beans.CDIConstructorBean_RL;
import cdi.listeners.beans.CDIDependentFieldBean_RL;
import cdi.listeners.beans.CDIListenerType_RL;
import cdi.listeners.beans.CDIMethodBean_RL;
import cdi.listeners.beans.CDIRequestFieldBean_RL;
import cdi.listeners.beans.CDISessionFieldBean_RL;
import cdi.listeners.interceptors.RequestListenerType;

@WebListener("CDI Test Servlet Request Listener")
public class CDIServletRequestListener implements ServletRequestListener {
    private static final String LOG_CLASS_NAME = CDIServletRequestListener.class.getSimpleName();
    private static final Logger LOG = Logger.getLogger(CDIServletRequestListener.class.getName());

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

    private void logEvent(String methodName, ServletRequestEvent requestEvent) {
        String[] eventLines = CDIEventPrinter.getEventText(requestEvent);

        for (String eventLine : eventLines) {
            logInfo(methodName, eventLine);
        }

        for (String eventLine : eventLines) {
            applicationLog(methodName, eventLine);
        }
    }

    //

    @Override
    @RequestListenerType
    public void requestInitialized(ServletRequestEvent requestEvent) {
        String methodName = "requestInitialized";
        logEvent(methodName, requestEvent);

        addBeanData("RL:I" + requestEvent.getServletRequest().getAttribute("RLInterceptor"));
        logBeanData();
    }

    @Override
    @RequestListenerType
    public void requestDestroyed(ServletRequestEvent requestEvent) {
        String methodName = "requestDestroyed";
        logEvent(methodName, requestEvent);

        addBeanData("RL:D" + requestEvent.getServletRequest().getAttribute("RLInterceptor"));
        logBeanData();
    }

    //

    // Test case: Constructor injection
    private final CDIDataBean constructorBean;

    @Inject
    public CDIServletRequestListener(CDIConstructorBean_RL constructorBean) {
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
    @CDIListenerType_RL
    private CDIDataBean fieldBean;

    // Test case: Initializer method injection.
    private CDIDataBean methodBean;

    @Inject
    public void setMethodBean(CDIMethodBean_RL methodBean) {
        this.methodBean = methodBean;
    }

    // Test case: Dependent bean injection
    @Inject
    private CDIDependentFieldBean_RL dependentFieldBean;

    // Test case: Request bean injection
    @Inject
    private CDIRequestFieldBean_RL requestFieldBean;

    // Test case: Session bean injection
    @Inject
    private CDISessionFieldBean_RL sessionFieldBean;

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
        requestFieldBean.addData(data);
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
        logBeanData(methodName, getBeanText(requestFieldBean, CDICaseInjection.Field));
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
