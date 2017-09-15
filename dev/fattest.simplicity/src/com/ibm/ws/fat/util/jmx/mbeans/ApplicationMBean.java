/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util.jmx.mbeans;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.remote.JMXServiceURL;

import com.ibm.ws.fat.util.Props;
import com.ibm.ws.fat.util.StopWatch;
import com.ibm.ws.fat.util.jmx.JmxException;
import com.ibm.ws.fat.util.jmx.SimpleJmxOperation;
import com.ibm.ws.fat.util.jmx.SimpleMBean;

/**
 * Convenience class to work with the ApplicationMBean for a particular application on a specific Liberty server
 * 
 * @author Tim Burns
 */
public class ApplicationMBean extends SimpleMBean {

    private final static Logger LOG = Logger.getLogger(ApplicationMBean.class.getName());

    /**
     * Describes the current state of an application
     */
    public static enum ApplicationState {
        /** The application is running */
        STARTED,
        /** The application is not running */
        STOPPED,
        /** The state of the application is unknown or unrecognized */
        OTHER
    }

    private final String applicationName;

    /**
     * Encapsulate the MBean for a particular application on a server
     * 
     * @param url the JMX connection URL where you want to find the MBean
     * @param applicationName the name of an application
     * @throws JmxException if the object name for the input application cannot be constructed
     */
    public ApplicationMBean(JMXServiceURL url, String applicationName) throws JmxException {
        super(url, getObjectName("WebSphere:service=com.ibm.websphere.application.ApplicationMBean,name=" + applicationName));
        this.applicationName = applicationName;
    }

    /**
     * @return the name of the application
     */
    public String getApplicationName() {
        return this.applicationName;
    }

    /**
     * Changes the state of the application
     * 
     * @param operationName the state change to invoke
     * @throws JmxException if the state change fails
     */
    protected void invokeOperation(String operationName) throws JmxException {
        StopWatch timer = null;
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info(Props.getInstance().getProperty(Props.LOGGING_BREAK_SMALL));
            LOG.info(operationName + "ing the application named " + this.getApplicationName());
            LOG.info(Props.getInstance().getProperty(Props.LOGGING_BREAK_SMALL));
            timer = new StopWatch();
            timer.start();
        }
        SimpleJmxOperation.invoke(this.getUrl(), this.getObjectName(), operationName, null, null);
        if (LOG.isLoggable(Level.INFO)) {
            timer.stop();
            LOG.info(Props.getInstance().getProperty(Props.LOGGING_BREAK_SMALL));
            LOG.info(operationName + " operation completed after " + timer.getTimeElapsedAsString());
            LOG.info(Props.getInstance().getProperty(Props.LOGGING_BREAK_SMALL));
        }
    }

    /**
     * Start the application. This method will not return until the start has finished?
     * 
     * @throws JmxException if the start fails
     */
    public void start() throws JmxException {
        this.invokeOperation("start");
    }

    /**
     * Stop the application. This method will not return until the stop has finished?
     * 
     * @throws JmxException if the stop fails
     */
    public void stop() throws JmxException {
        this.invokeOperation("stop");
    }

    /**
     * Restart the application. This method will not return until the restart has finished?
     * 
     * @throws JmxException if the restart fails
     */
    public void restart() throws JmxException {
        this.invokeOperation("restart");
        this.waitForState(ApplicationState.STARTED);
    }

    private void waitForState(ApplicationState expectedState) throws JmxException {
        int attempts = 60;
        while (attempts-- > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.getCause();
            }
            ApplicationState currentState = this.getState();
            if (currentState == expectedState) {
                break;
            }
        }
    }

    /**
     * Detects the current state of the application
     * 
     * @return the current state of the application, or null if the state cannot be determined
     * @throws JmxException if the state can't be detected
     */
    public ApplicationState getState() throws JmxException {
        Object result = SimpleJmxOperation.getAttribute(this.getUrl(), this.getObjectName(), "State");
        try {
            return ApplicationState.valueOf(result.toString().trim().toUpperCase());
        } catch (Throwable thrown) {
            return ApplicationState.OTHER;
        }
    }

}
