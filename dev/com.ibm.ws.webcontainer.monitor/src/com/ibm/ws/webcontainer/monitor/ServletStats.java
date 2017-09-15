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
package com.ibm.ws.webcontainer.monitor;

import com.ibm.websphere.monitor.meters.Counter;
import com.ibm.websphere.monitor.meters.Meter;
import com.ibm.websphere.monitor.meters.StatisticsMeter;

/**
 * This is used to report Servlet Related Statistics.
 * Each Servlet will have one instance of ServletStatsMXBean.
 * Statistic reported :
 * 1) Application Name
 * 2) Servlet Name
 * 3) Request Count for sevlet
 * 4) Average Response Time in nano seconds.
 * 
 */
public class ServletStats extends Meter implements ServletStatsMXBean {

    private String appName;
    private String servletName;

    //Following is the stats we are reporting for Servlets.
    private Counter requestCount;
    private final StatisticsMeter responseTime;

    /**
     * Constructor.
     * We will store AppName and Servlet Name
     */
    public ServletStats(String aName, String sName) {
        setAppName(aName);
        setServletName(sName);
        requestCount = new Counter();
        requestCount.setDescription("This shows number of requests to a servlet");
        requestCount.setUnit("ns");
        responseTime = new StatisticsMeter();
        responseTime.setDescription("Average Response Time for servlet");
        responseTime.setUnit("ns");
    }

    /**
     * Getter for Application Name.
     * */
    @Override
    public String getAppName() {
        return this.appName;
    }

    /**
     * Getter for Description
     **/
    @Override
    public String getDescription() {
        return "Report Servlet Stats for specified Servlet and application.";
    }

    /**
     * Demonstrate Number of Requests, recieved for this servlet.
     * 
     **/
    @Override
    public long getRequestCount() {
        return this.requestCount.getCurrentValue();
    }

    /**
     * Reports Average Response Time.
     * We store Cummulative Time (total service time for specified servlet)
     * responseTime = CummulativeTime / number of requests.
     * 
     * */
    @Override
    public double getResponseTime() {
        return responseTime.getMean();
    }

    /**
     * Getter for Servlet Name
     * */
    @Override
    public String getServletName() {
        return this.servletName;
    }

    /**
     * @param appName the appName to set
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }

    /**
     * @param servletName the servletName to set
     */
    public void setServletName(String servletName) {
        this.servletName = servletName;
    }

    /**
     * @param requestCount the requestCount to set
     */
    public void setRequestCount(Counter requestCount) {
        this.requestCount = requestCount;
    }

    /**
     * This will increment request count by i.
     * Typically i would be 1.
     * 
     * @param i
     */
    public void incrementCountBy(int i) {
        this.requestCount.incrementBy(i);
    }

    /**
     * updateRT()
     * 
     * This is responsible for updating Response Time.
     * We already have valid serviceStartTime.
     * Now we should take (current time - serviceStartTime).
     * Add it to cummulative time.
     * 
     * @param elapsed
     * 
     * @param l
     */
    public void updateRT(long elapsed) {
        this.responseTime.addDataPoint(elapsed);

    }

    /**
     * Method getRequestCountDetails()
     * This is returning the details for requestCount.
     * Type = Counter.
     * Data: count, description, unit, Readings
     **/
    @Override
    public Counter getRequestCountDetails() {
        return this.requestCount;
    }

    /**
     * Method getResponseTimeDetails()
     * This is returning the details for responseTime.
     * Type = StatisticMeter.
     * Data: mean, min, max, description, unit
     **/

    @Override
    public StatisticsMeter getResponseTimeDetails() {
        // TODO Auto-generated method stub
        return this.responseTime;
    }

}
