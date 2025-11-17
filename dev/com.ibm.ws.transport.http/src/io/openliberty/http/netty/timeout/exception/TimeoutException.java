/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.netty.timeout.exception;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

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

        super(normalize(duration, unit));

        this.code = warningCode;
        this.duration = duration;
        this.unit = unit;

        //TODO (not needed for beta) -> Consider if we want to print out warnings for these exceptions
        //or just hand them up to the transport without logging. 
        // if(warningCode != null && unit != null){
        //     Tr.error(tc, warningCode, getMessage());
        // }else{
        //     Tr.error(tc, getMessage());
        // }
        
    }

    private static String normalize(long duration, TimeUnit unit){

        boolean singular = (duration==1);

        String shorthand;
        
        switch(unit){
            case DAYS:          shorthand = singular ?  "day":"days";           break;
            case HOURS:         shorthand = singular ?  "hour": "hours";        break;
            case MINUTES:       shorthand = singular ?  "minute": "minutes";    break;
            case SECONDS:       shorthand = singular ?  "second": "seconds";    break;
            case MICROSECONDS:  shorthand = "µs";                               break;
            case MILLISECONDS:  shorthand = "ms";                               break;
            case NANOSECONDS:   shorthand = "ns";                               break;
            default:            shorthand = unit.name().toLowerCase();          break;
        };
        return duration + " " + shorthand;
    }

}
