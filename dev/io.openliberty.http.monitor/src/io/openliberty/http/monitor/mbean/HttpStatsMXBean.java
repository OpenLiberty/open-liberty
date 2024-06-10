/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor.mbean;

/**
 *
 */
public interface HttpStatsMXBean {

	/*
	 * HTTP Attributes
	 */
	
	@Deprecated //As per beta-fencing guidelines //As per beta-fencing guidelines
	public String getRequestMethod();
	
	@Deprecated //As per beta-fencing guidelines
	public int getResponseStatus();
	
	@Deprecated //As per beta-fencing guidelines
	public String getHttpRoute();
	
	@Deprecated //As per beta-fencing guidelines
	public String getScheme();
	
	@Deprecated //As per beta-fencing guidelines
	public String getNetworkProtocolName();
	
	@Deprecated //As per beta-fencing guidelines
	public String getNetworkProtocolVersion();
		
	@Deprecated //As per beta-fencing guidelines
	public String getServerName();
	
	@Deprecated //As per beta-fencing guidelines
	public int getServerPort();
	
	@Deprecated //As per beta-fencing guidelines
	public String getErrorType();
	
	/*
	 * Metric values
	 */
	@Deprecated //As per beta-fencing guidelines
	public long getCount();
	
	@Deprecated //As per beta-fencing guidelines
	public double getDuration();
    
}
