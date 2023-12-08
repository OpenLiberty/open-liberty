/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor;

import java.time.Duration;
import java.util.Optional;

import com.ibm.websphere.monitor.jmx.Meter;
import com.ibm.websphere.monitor.meters.StatisticsMeter;

/**
 *
 */
public class HttpStats extends Meter implements HttpStatsMXBean {

    private final StatisticsMeter responseTime;
    private String requestMethod, httpRoute;
    private int responseStatus;
    
    
    //others
    
    private String scheme, serverName, networkProtocolName, networkProtocolVersion;
    private int serverPort;

    public HttpStats() {

        responseTime = new StatisticsMeter();
        responseTime.setDescription("Cumulative Response Time (NanoSeconds) for a HTTP connection");
        responseTime.setUnit("ns");
    }
    
    public HttpStats(HttpStatAttributes httpStatAttributes) {
        responseTime = new StatisticsMeter();
        responseTime.setDescription("Cumulative Response Time (NanoSeconds) for a HTTP connection");
        responseTime.setUnit("ns");
        
        
        this.requestMethod = httpStatAttributes.getRequestMethod();
        this.httpRoute = httpStatAttributes.getHttpRoute().orElse("");
        this.responseStatus = httpStatAttributes.getResponseStatus().orElse(-1);
        
        this.scheme = httpStatAttributes.getScheme();
        
        this.networkProtocolName  = httpStatAttributes.getNetworkProtocolName();
        
        this.networkProtocolVersion = httpStatAttributes.getNetworkProtocolVersion();
        
        this.serverName = httpStatAttributes.getServerName();
        
        this.serverPort = httpStatAttributes.getServerPort();
    }

    /**
     * 
     * @param durationNanos in nanoseconds
     */
    public void updateDuration(long durationNanos) {
        responseTime.addDataPoint(durationNanos);
    }
    
    public void updateDuration(Duration duration) {
        responseTime.addDataPoint(duration.toNanos());
    }

    @Override
    public double getDuration() {
        return responseTime.getTotal();
    }

	@Override
	public String getRequestMethod() {
		return requestMethod;
	}
	
	public void setRequestMethod(String requestMethod) {
		this.requestMethod = requestMethod;
	}

	@Override
	public int getResponseStatus() {
		return responseStatus;
	}

	@Override
	public String getHttpRoute() {
		return httpRoute;
	}

	@Override
	public long getCount() {
		return responseTime.getCount();
	}

	@Override
	public String getScheme() {
		// TODO Auto-generated method stub
		return scheme;
	}

	@Override
	public String getNetworkProtocolName() {
		// TODO Auto-generated method stub
		return networkProtocolName;
	}

	@Override
	public String getNetworkProtocolVersion() {
		// TODO Auto-generated method stub
		return networkProtocolVersion;
	}

	@Override
	public String getServerName() {
		// TODO Auto-generated method stub
		return serverName;
	}

	@Override
	public int getServerPort() {
		// TODO Auto-generated method stub
		return serverPort;
	}

}
