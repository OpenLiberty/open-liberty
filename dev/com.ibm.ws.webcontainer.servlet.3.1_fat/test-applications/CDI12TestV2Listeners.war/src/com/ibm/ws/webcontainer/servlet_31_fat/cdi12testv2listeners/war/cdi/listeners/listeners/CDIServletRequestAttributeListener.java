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
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2listeners.war.cdi.listeners.listeners;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.annotation.WebListener;

import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.CDICaseInjection;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.CDIDataBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.log.ApplicationLog;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2listeners.war.cdi.listeners.beans.CDIApplicationFieldBean_AL;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2listeners.war.cdi.listeners.beans.CDIConstructorBean_RAL;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2listeners.war.cdi.listeners.beans.CDIDependentFieldBean_RAL;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2listeners.war.cdi.listeners.beans.CDIListenerType_RAL;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2listeners.war.cdi.listeners.beans.CDIMethodBean_RAL;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2listeners.war.cdi.listeners.beans.CDIRequestFieldBean_RAL;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2listeners.war.cdi.listeners.beans.CDISessionFieldBean_RAL;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2listeners.war.cdi.listeners.interceptors.RequestAttributeListenerType;

@WebListener("CDI Test Servlet Request Attribute Listener")
public class CDIServletRequestAttributeListener implements ServletRequestAttributeListener {
    private static final String LOG_CLASS_NAME = CDIServletRequestAttributeListener.class.getSimpleName();
    private static final Logger LOG = Logger.getLogger(CDIServletRequestAttributeListener.class.getName());

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

    private void logEvent(String methodName, ServletRequestAttributeEvent attributeEvent) {
        String[] eventLines = CDIEventPrinter.getEventText(attributeEvent);

        for (String eventLine : eventLines) {
            logInfo(methodName, eventLine);
        }

        for (String eventLine : eventLines) {
            applicationLog(methodName, eventLine);
        }
    }

    //

    @Override
    @RequestAttributeListenerType
    public void attributeAdded(ServletRequestAttributeEvent attributeEvent) {
        String methodName = "attributeAdded";
        logEvent(methodName, attributeEvent);

        String eventName = attributeEvent.getName();

        if ((eventName != null) && eventName.startsWith("CDI")) {
            String eventValue = attributeEvent.getValue().toString();

            Object interceptorAttribute = attributeEvent.getServletRequest().getAttribute("RALInterceptor");

            if (interceptorAttribute != null) {
                eventValue = eventValue.concat(interceptorAttribute.toString());
            } else {
                eventValue = eventValue.concat("null");
            }

            addBeanData(eventName, eventValue);
            logBeanData();
        }
    }

    @Override
    @RequestAttributeListenerType
    public void attributeRemoved(ServletRequestAttributeEvent attributeEvent) {
        String methodName = "attributeRemoved";
        logEvent(methodName, attributeEvent);
    }

    @Override
    @RequestAttributeListenerType
    public void attributeReplaced(ServletRequestAttributeEvent attributeEvent) {
        String methodName = "attributeReplaced";
        logEvent(methodName, attributeEvent);
    }

    //

    // Test case: Constructor injection
    private final CDIDataBean constructorBean;

    @Inject
    public CDIServletRequestAttributeListener(CDIConstructorBean_RAL constructorBean) {
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
    @CDIListenerType_RAL
    private CDIDataBean fieldBean;

    // Test case: Initializer method injection.
    private CDIDataBean methodBean;

    @Inject
    public void setMethodBean(CDIMethodBean_RAL methodBean) {
        this.methodBean = methodBean;
    }

    // Test case: Dependent bean injection
    @Inject
    private CDIDependentFieldBean_RAL dependentFieldBean;

    // Test case: Request bean injection
    @Inject
    private CDIRequestFieldBean_RAL requestFieldBean;

    // Test case: Session bean injection
    @Inject
    private CDISessionFieldBean_RAL sessionFieldBean;

    // Test case: Application bean injection
    @Inject
    private CDIApplicationFieldBean_AL applicationFieldBean;

    //

    private void addBeanData(String attributeName, Object attributeValue) {
        String addData = attributeName + "=" + String.valueOf(attributeValue);

        constructorBean.addData(addData);
        postConstruct = ((postConstruct == null) ? addData : (postConstruct + ":" + addData));
        // setPreDestroy(beanDataValue);
        fieldBean.addData(addData);
        methodBean.addData(addData);
        dependentFieldBean.addData(addData);
        requestFieldBean.addData(addData);
        sessionFieldBean.addData(addData);
        applicationFieldBean.addData(addData);
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
