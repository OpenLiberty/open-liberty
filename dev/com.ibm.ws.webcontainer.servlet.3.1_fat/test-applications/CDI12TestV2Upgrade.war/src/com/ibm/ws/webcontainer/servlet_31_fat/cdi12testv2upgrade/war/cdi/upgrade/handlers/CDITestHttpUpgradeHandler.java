/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2upgrade.war.cdi.upgrade.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.WebConnection;

import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.ApplicationFieldBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.CDIDataBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.DependentFieldBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.MethodBean;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.UpgradeProducesType;
import com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2.log.ApplicationLog;

/**
 * CDI test HTTP upgrade handler implementation.
 */
public class CDITestHttpUpgradeHandler implements HttpUpgradeHandler {
    private static final Logger LOG = Logger.getLogger(CDITestHttpUpgradeHandler.class.getName());

    //

    protected static void logEntry(String className, String methodName) {
        LOG.info(className + ": " + methodName + ": ENTRY");
    }

    protected static void logExit(String className, String methodName) {
        LOG.info(className + ": " + methodName + ": EXIT");
    }

    protected static void logInfo(String className, String methodName, String text) {
        LOG.info(className + ": " + methodName + ": " + text);
    }

    protected static void logException(String className, String methodName, Exception e) {
        LOG.info(className + ": " + methodName + ": " + " Unexpected exception [ " + e + " ]");
        LOG.throwing(className, methodName, e);
    }

    //

    private static final String LOG_CLASS_NAME = "CDITestHttpUpgradeHandler";

    private static void logEntry(String methodName) {
        logEntry(LOG_CLASS_NAME, methodName);
    }

    private static void logExit(String methodName) {
        logExit(LOG_CLASS_NAME, methodName);
    }

    private static void logInfo(String methodName, String text) {
        logInfo(LOG_CLASS_NAME, methodName, text);
    }

    private static void logException(String methodName, Exception e) {
        logException(LOG_CLASS_NAME, methodName, e);
    }

    //

    // The shared application log; makes use of injection, but not
    // meant to directly test injection.

    @Inject
    ApplicationLog applicationLog;

    protected void logBeanActivity(String className, String methodName, String text) {
        String activityText = ":" + className + ":" + methodName + ":" + text + ":";
        applicationLog.addLine(activityText);
    }

    private void logBeanState(String className, String methodName, String injectionCase, CDIDataBean bean) {
        String beanData = ((bean == null) ? "null" : bean.getData());
        String activityText = injectionCase + ":" + beanData;
        logBeanActivity(className, methodName, activityText);
    }

    protected void logFieldBeans(String className, String methodName) {
        // logBeanState(className, methodName, "FieldQualified", fieldBeanUpgrade);
        logBeanState(className, methodName, "Field", fieldBeanDependent);
        // logBeanState(className, methodName, "Field", "Conversation", fieldBeanConversation);
        // logBeanState(className, methodName, "Field", fieldBeanRequest);
        // logBeanState(className, methodName, "Field", fieldBeanSession);
        logBeanState(className, methodName, "Field", fieldBeanApplication);
    }

    protected void logMethodBean(String className, String methodName) {
        logBeanState(className, methodName, "Method", methodBean);
    }

    protected void logProduces(String className, String methodName) {
        String activityText = (producesText == null) ? "null" : producesText;
        logBeanActivity("logProducesText", activityText);
    }

    protected void logBeanState(String className, String methodName) {
        logFieldBeans(className, methodName);
        logMethodBean(className, methodName);
        logProduces(className, methodName);
    }

    // Local bean logging ...

    private void logBeanActivity(String methodName, String text) {
        logBeanActivity(LOG_CLASS_NAME, methodName, text);
    }

    private void logState(String methodName) {
        logBeanState(LOG_CLASS_NAME, methodName);
    }

    // Field injection cases.

    // @Inject
    // @UpgradeType
    // CDIDataBean fieldBeanUpgrade;

    @Inject
    DependentFieldBean fieldBeanDependent;

    // Not available within the upgrade handler's scope.
    // See CDIServletUpgrade#performUpgrade and the comment
    // that follows.
    //
    // @Inject
    // ConversationFieldBean fieldBeanConversation;
    //
    // @Inject
    // RequestFieldBean fieldBeanRequest;
    //
    // @Inject
    // SessionFieldBean fieldBeanSession;

    @Inject
    ApplicationFieldBean fieldBeanApplication;

    protected void appendFieldData(String appendData) {
        // if (fieldBeanUpgrade != null) {
        //     fieldBeanUpgrade.addData(appendData);
        // }

        if (fieldBeanDependent != null) {
            fieldBeanDependent.addData(appendData);
        }

        // if (fieldBeanConversation != null) {
        //     fieldBeanConversation.addData(appendData);
        // }
        // if (fieldBeanRequest != null) {
        //     fieldBeanRequest.addData(appendData);
        // }
        // if (fieldBeanSession != null) {
        //     fieldBeanSession.addData(appendData);
        // }

        if (fieldBeanApplication != null) {
            fieldBeanApplication.addData(appendData);
        }
    }

    // Method injection cases.

    private CDIDataBean methodBean;

    @Inject
    public void setMethodBean(MethodBean methodBean) {
        this.methodBean = methodBean;
    }

    protected void appendMethodData(String appendData) {
        if (methodBean != null) {
            methodBean.addData(appendData);
        }
    }

    //

    protected void appendBeanData(String appendData) {
        appendFieldData(appendData);
        appendMethodData(appendData);
    }

    // Produces injection cases.

    @Inject
    @UpgradeProducesType
    private String producesText;

    //

    @PostConstruct
    void start() {
        String methodName = "start";
        logBeanActivity(methodName, "PostConstruct");
    }

    @PreDestroy
    void stop() {
        String methodName = "stop";
        logBeanActivity(methodName, "PreDestroy");
    }

    //

    @Override
    public void init(WebConnection webConnection) {
        String methodName = "init";
        logEntry(methodName);

        // Log the bean state *before* setting the listeners.
        // The listeners run in their own threads, meaning,
        // bean state updates made by the listeners may or
        // may not be made before this method returns from
        // the listener initialization calls.

        logBeanActivity(methodName, "Entry");
        logState(methodName);

        // The queue will be passed to the read listener then passed
        // from the read listener to the write listener.

        List<String> queue = new ArrayList<String>();

        // Set only the read listener.  The write listener is set
        // after all data has been read.

        try {
            setReadListener(webConnection, queue);
        } catch (Exception e) {
            logException(methodName, e);
        }

        logBeanActivity(methodName, "Exit");

        logExit(methodName);
    }

    @Override
    public void destroy() {
        String methodName = "destroy";
        logEntry(methodName);

        logBeanActivity(methodName, "Entry");
        logState(methodName);

        logBeanActivity(methodName, "Exit");

        logExit(methodName);
    }

    //

    public static final byte TERMINATION_CHAR = '\0';
    public static final String NUMBERS_TAG = "numbers";
    public static final String TERMINATION_TAG = "last";

    // Set during initialization of the upgrade handler.

    private CDITestReadListener readListener;

    private void setReadListener(WebConnection webConnection, List<String> queue) throws IOException {
        String methodName = "setReadListener";
        logEntry(methodName);

        logBeanActivity(methodName, "Entry");

        try {
            readListener = new CDITestReadListener(this, webConnection, queue); // throws IOException
            logInfo(methodName, "Read Listener [ " + readListener + " ]");
        } catch (IOException e) {
            logException(methodName, e);
            throw e;
        }

        logBeanActivity(methodName, "Exit");

        logExit(methodName);
    }

    //

    // Set by the read listener after all data has been read.

    private CDITestWriteListener writeListener;

    protected void setWriteListener(WebConnection webConnection, List<String> queue) throws IOException {
        String methodName = "setWriteListener";
        logEntry(methodName);

        logBeanActivity(methodName, "Entry");

        try {
            writeListener = new CDITestWriteListener(this, webConnection, queue); // throws IOException
            logInfo(methodName, "Write Listener [ " + writeListener + " ]");
        } catch (IOException e) {
            logException(methodName, e);
            throw e;
        }

        logBeanActivity(methodName, "Exit");

        logExit(methodName);
    }
}
