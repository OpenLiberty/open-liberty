/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.netty.timeout.exception;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.http.constants.HttpGenerics;
import io.openliberty.netty.internal.impl.NettyConstants;

/**
 * Base timeout class to standarize the user messages seen for the transport's
 * IO inactivity events.
 */
public abstract class TimeoutException extends IOException {

    private static final TraceComponent tc = Tr.register(TimeoutException.class, NettyConstants.NETTY_TRACE_NAME, NettyConstants.BASE_BUNDLE);
    private static final long serialVersionUID = 1L;

    private final String code;
    private final long duration;
    private final TimeUnit unit;

    public TimeoutException(String warningCode, long duration, TimeUnit unit){
        this(warningCode, duration, unit, null, null);
    }

    public TimeoutException(String warningCode, long duration, TimeUnit unit, SocketAddress localAddress, SocketAddress remoteAddress){

        super(formatMessage(duration, unit, localAddress, remoteAddress));

        this.code = warningCode;
        this.duration = duration;
        this.unit = unit;
        
        if (warningCode != HttpGenerics.NO_WARNING_CODE_SET) {
            String message = formatMessage(duration, unit, localAddress, remoteAddress);
            Tr.warning(tc, warningCode, message);
        }
    }

    private static String formatMessage(long duration, TimeUnit unit, SocketAddress localAddress, SocketAddress remoteAddress){
        StringBuilder sb = new StringBuilder();
        sb.append("Timeout after ").append(normalize(duration, unit));
        
        if (localAddress != null || remoteAddress != null) {
            if (localAddress != null) {
                sb.append(" local=").append(localAddress);
            }
            if (remoteAddress != null) {
                sb.append(" remote=").append(remoteAddress);
            }
        }
        
        return sb.toString();
    }

    private static String normalize(long duration, TimeUnit unit){

        boolean singular = (duration == 1);

        String shorthand;
        
        switch(unit){
            case HOURS:         shorthand = singular ?  "hour": "hours";        break;
            case MINUTES:       shorthand = singular ?  "minute": "minutes";    break;
            case SECONDS:       shorthand = singular ?  "second": "seconds";    break;
            default:            shorthand = unit.name().toLowerCase();          break;
        };
        return duration + " " + shorthand;
    }

}
