/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.test.image;

public interface Timeouts {
    long MS_IN_SEC = 1000;
    long NS_IN_SEC = 1000000000;

    long PROCESS_INTERVAL_MS = 1 * MS_IN_SEC;
    
    long INSTALL_FROM_JAR_TIMEOUT_NS = 10 * NS_IN_SEC;
    long INSTALL_BUNDLES_TIMEOUT_NS = 30 * Timeouts.NS_IN_SEC;    
    long INSTALL_FEATURES_TIMEOUT_NS = 30 * Timeouts.NS_IN_SEC;    

    long CREATE_SERVER_TIMEOUT_NS = 30 * Timeouts.NS_IN_SEC;    
    long START_SERVER_TIMEOUT_NS = 30 * Timeouts.NS_IN_SEC;
    long STOP_SERVER_TIMEOUT_NS = 10 * Timeouts.NS_IN_SEC;    
    long SCHEMAGEN_TIMEOUT_NS = 30 * Timeouts.NS_IN_SEC;    
}
