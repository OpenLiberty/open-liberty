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
package com.ibm.websphere.ejbcontainer.mbean;

/**
 * The snapshot of the data for a persistent timer.
 *
 * @ibm-api
 */
public class EJBPersistentTimerInfo {
    private String id;
    private String application;
    private String module;
    private String ejb;
    private long nextTimeout;
    private String info;
    private String scheduleExpression;
    private String automaticTimerMethod;

    @Override
    public String toString() {
        return super.toString() + "[id=" + id + ']';
    }

    /**
     * The unique ID of the timer as stored in the datastore. This ID uniquely
     * identifies the timer and will not change for the duration of the timer's
     * existence.
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * The administrative application name that contains the EJB that created
     * the timer.
     */
    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    /**
     * The module URI that contains the EJB that created the timer.
     */
    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    /**
     * The EJB name that created the timer.
     */
    public String getEJB() {
        return ejb;
    }

    public void setEJB(String ejb) {
        this.ejb = ejb;
    }

    /**
     * The next timeout in {@code System.currentTimeMillis()} format. This time
     * might be in the past if the timer was delayed or requires catch-ups.
     */
    public long getNextTimeout() {
        return nextTimeout;
    }

    public void setNextTimeout(long nextTimeout) {
        this.nextTimeout = nextTimeout;
    }

    /**
     * Returns {@code toString()} of the info object used to create the timer,
     * or null if no info object was used to create the timer, the application
     * is not currently running and the class cannot be loaded, or an error
     * occurs while invoking the {@code toString()} method.
     */
    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    /**
     * Returns a string representation of the {@code javax.ejb.ScheduleExpression} used
     * to create the calendar timer, or null if the timer is not a calendar
     * timer. The returned string is intended for human display, so the format
     * is unspecified and might change in the future.
     */
    public String getScheduleExpression() {
        return scheduleExpression;
    }

    public void setScheduleExpression(String scheduleExpression) {
        this.scheduleExpression = scheduleExpression;
    }

    /**
     * Returns the name of the method that is declared to have the automatic
     * timer, or null if the timer is not an automatic timer. This is intended
     * to help disambiguate the specific automatic timer if the EJB declares
     * multiple automatic timers on different methods, but it will not be unique
     * if the EJB declares multiple automatic timers on the same method.
     */
    public String getAutomaticTimerMethod() {
        return automaticTimerMethod;
    }

    public void setAutomaticTimerMethod(String automaticTimerMethod) {
        this.automaticTimerMethod = automaticTimerMethod;
    }
}
