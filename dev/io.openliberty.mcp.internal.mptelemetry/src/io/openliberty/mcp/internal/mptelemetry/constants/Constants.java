/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.mptelemetry.constants;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class Constants {
    private static final TraceComponent tc = Tr.register(Constants.class);
    
    //MCP Operation Metric name + desc
    public static final String MCP_SERVER_OPERATION_DURATION_NAME = "mcp.server.operation.duration";
    public static final String MCP_SERVER_OPERATION_DURATION_DESC = Tr.formatMessage(tc, "mcp.server.operation.duration.description");
    
    //MCP Session Metric name + desc
    public static final String MCP_SERVER_SESSION_DURATION_NAME = "mcp.server.session.duration";
    public static final String MCP_SERVER_SESSION_DURATION_DESC = Tr.formatMessage(tc, "mcp.server.session.duration.description");

}
