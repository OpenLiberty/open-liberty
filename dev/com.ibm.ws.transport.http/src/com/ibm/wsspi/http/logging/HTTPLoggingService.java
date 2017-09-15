/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.logging;

/**
 * Interface for an HTTP logging service that handles NCSA access logs, debug or
 * error logs, as well as a FRCA access log.
 */
public interface HTTPLoggingService {

    /**
     * Query the NCSA access log file.
     * 
     * @return AccessLog
     */
    AccessLog getAccessLog();

    /**
     * Query the FRCA access log file.
     * 
     * @return AccessLog
     */
    AccessLog getFRCALog();

    /**
     * Query the debug/error log file.
     * 
     * @return DebugLog
     */
    DebugLog getDebugLog();

}
