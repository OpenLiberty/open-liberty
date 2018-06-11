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
package com.ibm.websphere.webcontainer;

import com.ibm.websphere.monitor.jmx.Counter;
import com.ibm.websphere.monitor.jmx.StatisticsMeter;

/**
 * Management12 interface for MBeans with names of the form "WebSphere:type=ServletStats,name=*"
 * where * is the name of a servlet within an application under the Liberty profile of the form <appName>.<servletName>. For example, myApp.DemoServlet. One such MBean for each servlet in the system is available
 * from the Liberty profile platform MBean server when the monitor-1.0 feature is enabled. This interface can be used to request a proxy object via the {@link javax.management.JMX#newMMBeanProxy} method.
 * 
 * @ibm-api
 */
public interface ServletStatsMXBean {

    /**
     * Retrieves the value of the read-only attribute Description, which is a description of the MBean itself.
     * 
     * @return description
     */
    public String getDescription();

    /**
     * Retrieves the value of the read-only attribute ServletName, the name of the servlet as specified in the deployment descriptor.
     * 
     * @return servlet name
     */
    public String getServletName();

    /**
     * Retrieves the value of the read-only attribute RequestCount, the number of requests the server has received for this servlet.
     * 
     * @return request count
     */
    public long getRequestCount();

    /**
     * Retrieves the value of the read-only attribute RequestCountDetails, which provides other details on the request count.
     * 
     * @return request count details
     */
    public Counter getRequestCountDetails();

    /**
     * Retrieves the value of the read-only attribute ResponseTime, which is the average (mean) time spent responding to each request for the servlet.
     * 
     * @return response time
     */
    public double getResponseTime();

    /**
     * Retrieves the value of the read-only attribute ResponseCountDetails, which provides statistical details on the response time.
     * 
     * @return response time details
     */
    public StatisticsMeter getResponseTimeDetails();

    /**
     * Retrieves the value of the read-only attribute AppName, the name of the application of which the servlet is a member.
     * 
     * @return app name
     */
    public String getAppName();

}
