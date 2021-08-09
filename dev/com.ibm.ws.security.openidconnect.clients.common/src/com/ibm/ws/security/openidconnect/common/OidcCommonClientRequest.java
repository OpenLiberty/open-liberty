/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.common;

import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.openidconnect.token.IDTokenValidationFailedException;
import com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException;

/*
 * Store the data for a httpServletRequest session
 *
 * Initialize when a session starts and
 * discard after it ends
 */
public abstract class OidcCommonClientRequest {

    private static final TraceComponent tcCommon = Tr.register(OidcCommonClientRequest.class,
            TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);

    public static final String TYPE_ID_TOKEN = "ID Token";
    public static final String TYPE_JWT_TOKEN = "Json Web Token";
    public static final String TYPE_ACCESS_TOKEN = "Access Token";
    public static final String NO_TOKEN = "No Token";
    public static final String BAD_TOKEN_OR_KEY = "Bad Token Or Key";
    public static final String BAD_JDK = "Bad JDK";
    public static final String EXPIRED_TOKEN = "Expired Token";
    public static final String NO_KEY = "No Key";

    public static final String ALL_AUDIENCES = "ALL_AUDIENCES";

    protected boolean bInboundRequired = false;
    protected boolean bInboundSupported = false;

    private String headerFailMsg = BAD_TOKEN_OR_KEY;
    private String rsFailMsg = null;

    public abstract String getTokenType();

    public abstract String getInboundPropagation();

    public boolean isInboundSupported() {
        return bInboundSupported;
    }

    /**
     * @param string
     * @param b
     * @param aud
     * @param clientId
     * @return
     * @throws JWTTokenValidationFailedException
     */
    // do not override
    public JWTTokenValidationFailedException errorCommon(boolean bTrError, TraceComponent tc, String[] msgCodes, Object[] objects) throws JWTTokenValidationFailedException {
        int msgIndex = 0;
        if (!TYPE_ID_TOKEN.equals(this.getTokenType())) {
            msgIndex = 1;
        }
        return errorCommon(bTrError, tc, msgCodes[msgIndex], objects);
    }

    // do not overridden
    public JWTTokenValidationFailedException errorCommon(boolean bTrError, TraceComponent tc, String msgCode, Object[] objects) throws JWTTokenValidationFailedException {
        if (bTrError && !bInboundSupported) {
            Tr.error(tcCommon, msgCode, objects);
        }
        if (TYPE_ID_TOKEN.equals(this.getTokenType())) {
            return IDTokenValidationFailedException.format(tc, msgCode, objects);
        } else {
            return JWTTokenValidationFailedException.format(tc, msgCode, objects);
        }
    }

    // do not overridden
    public void errorCommon(String[] msgCodes, Object[] objects) {
        int msgIndex = 0;
        if (!TYPE_ID_TOKEN.equals(this.getTokenType())) {
            msgIndex = 1;
        }
        if (!bInboundSupported) {
            Tr.error(tcCommon, msgCodes[msgIndex], objects);
        }
    }

    // do not overridden
    public void errorCommon(String msgCode, Object[] objects) {
        if (!bInboundSupported) {
            Tr.error(tcCommon, msgCode, objects);
        }
    }

    public String getHeaderFailMsg() {
        return headerFailMsg;
    }

    /**
     * @return the rsFailMsg
     */
    public String getRsFailMsg() {
        return rsFailMsg;
    }

    /**
     * @param rsFailMsg
     *            the rsFailMsg to set
     */
    public void setRsFailMsg(String headerFailMsg, String rsFailMsg) {
        this.headerFailMsg = headerFailMsg;
        this.rsFailMsg = rsFailMsg;
    }

    /**
     * @return
     */
    public abstract List<String> getAudiences();

    /**
     * @return
     */
    public abstract boolean isPreServiceUrl(String url);

    public abstract boolean allowedAllAudiences();

    public abstract boolean disableIssChecking();
}
