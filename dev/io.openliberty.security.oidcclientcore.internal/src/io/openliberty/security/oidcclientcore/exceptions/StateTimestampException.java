/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.exceptions;

import java.util.Date;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.security.oidcclientcore.utils.Utils;

public class StateTimestampException extends Exception {

    public static final TraceComponent tc = Tr.register(StateTimestampException.class);

    private static final long serialVersionUID = 1L;

    private final String state;
    private final long timestampFromStateValue;
    private final long now;
    private final long minDate;
    private final long maxDate;

    public StateTimestampException(String state, long now, long minDate, long maxDate) {
        this.state = state;
        this.timestampFromStateValue = Utils.convertNormalizedTimeStampToLong(state);
        this.now = now;
        this.minDate = minDate;
        this.maxDate = maxDate;
    }

    private String getDateString(long date) {
        return new Date(date).toInstant().toString();
    }

    @Override
    public String getMessage() {
        return Tr.formatMessage(tc, "STATE_VALUE_IN_CALLBACK_OUTSIDE_ALLOWED_TIME_FRAME", state, getDateString(timestampFromStateValue), getDateString(minDate),
                                getDateString(maxDate), getDateString(now));
    }

}