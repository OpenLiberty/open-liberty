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
package io.openliberty.security.oidcclientcore.authentication;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException;
import io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException.ValidationResult;
import io.openliberty.security.oidcclientcore.exceptions.StateTimestampException;
import io.openliberty.security.oidcclientcore.storage.OidcStorageUtils;
import io.openliberty.security.oidcclientcore.storage.Storage;
import io.openliberty.security.oidcclientcore.utils.Utils;

public abstract class AuthenticationResponseValidator {

    public static final TraceComponent tc = Tr.register(AuthenticationResponseValidator.class);

    public static final int STORED_STATE_VALUE_LENGTH = AuthorizationRequestUtils.STATE_LENGTH + Utils.TIMESTAMP_LENGTH;

    protected final HttpServletRequest request;
    protected final HttpServletResponse response;
    protected Storage storage;

    public AuthenticationResponseValidator(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public abstract void validateResponse() throws AuthenticationResponseException;

    protected abstract String getStoredStateValue(String responseState) throws AuthenticationResponseException;

    @FFDCIgnore(StateTimestampException.class)
    public void verifyState(String stateParameter, String clientId, @Sensitive String clientSecret, long clockSkewInSeconds,
                            long authenticationTimeLimitInSeconds) throws AuthenticationResponseException {
        if (stateParameter.length() < STORED_STATE_VALUE_LENGTH) {
            String nlsMessage = Tr.formatMessage(tc, "STATE_VALUE_IN_CALLBACK_INCORRECT_LENGTH", stateParameter, STORED_STATE_VALUE_LENGTH);
            throw new AuthenticationResponseException(ValidationResult.INVALID_RESULT, clientId, nlsMessage);
        }

        String storedStateValue = getStoredStateValue(stateParameter);
        String expectedStateValue = OidcStorageUtils.createStateStorageValue(stateParameter, clientSecret);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "storedStateValue:'" + storedStateValue + "' expectedStateValue:'" + expectedStateValue + "'");
        }
        if (expectedStateValue.equals(storedStateValue)) {
            try {
                verifyStateTimestampWithinClockSkew(stateParameter, clockSkewInSeconds, authenticationTimeLimitInSeconds);
            } catch (StateTimestampException e) {
                throw new AuthenticationResponseException(ValidationResult.INVALID_RESULT, clientId, e.getMessage());
            }
        } else {
            String nlsMessage = Tr.formatMessage(tc, "STATE_VALUE_IN_CALLBACK_DOES_NOT_MATCH_STORED_VALUE", stateParameter);
            throw new AuthenticationResponseException(ValidationResult.INVALID_RESULT, clientId, nlsMessage);
        }
    }

    void verifyStateTimestampWithinClockSkew(String responseState, long clockSkewInSeconds, long authenticationTimeLimitInSeconds) throws StateTimestampException {
        long clockSkewMillSeconds = clockSkewInSeconds * 1000;

        long timestampFromStateValue = Utils.convertNormalizedTimeStampToLong(responseState);
        long currentTime = (new Date()).getTime();
        long minDate = timestampFromStateValue - clockSkewMillSeconds;
        long maxDate = timestampFromStateValue + clockSkewMillSeconds + (authenticationTimeLimitInSeconds * 1000);

        if (!((minDate < currentTime) && (currentTime < maxDate))) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "error current: " + currentTime + "  ran at:" + timestampFromStateValue);
                Tr.debug(tc, "current time must be between " + minDate + " and " + maxDate);
            }
            throw new StateTimestampException(responseState, currentTime, minDate, maxDate);
        }
    }

}
