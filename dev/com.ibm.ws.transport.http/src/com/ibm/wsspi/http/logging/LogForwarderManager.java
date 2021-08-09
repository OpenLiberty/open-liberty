/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.logging;

import java.util.HashSet;
import java.util.Set;

/**
 * Registers the supported LogForwarder's to use with the Logging Service.
 */
public class LogForwarderManager {
    private static final Set<AccessLogForwarder> forwarders = new HashSet<AccessLogForwarder>();

    public static boolean registerAccessLogForwarder(AccessLogForwarder forwarder) {
        return forwarders.add(forwarder);
    }

    public static boolean deregisterAccessLogForwarder(AccessLogForwarder forwarder) {
        return forwarders.remove(forwarder);
    }

    /**
     * @return
     */
    public static Set<AccessLogForwarder> getAccessLogForwarders() {
        return forwarders;
    }
}
