/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.token;

import java.util.Iterator;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Audience checker for signed Json Assertion.
 */
public class CheckAudience {
    private static final TraceComponent tc = Tr.register(CheckAudience.class, TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);
    // URI that the client is accessing, as seen by the server
    private final String clientId;
    //payload
    private JWTPayload payload;

    /**
     * Public constructor.
     *
     * @param uri
     *            the URI against which the signed OAuth token was exercised.
     */
    public CheckAudience(String uri) {
        this.clientId = uri;
    }

    /**
     * Public constructor.
     *
     * @param uri
     *            the URI against which the signed OAuth token was exercised.
     */
    public CheckAudience(String uri, JWTPayload payload2) {
        this.clientId = uri;
        this.payload = payload2;
    }

    /**
     * @throws IDTokenValidationFailedException
     *
     */

    public void check() throws IDTokenValidationFailedException {
        checkStrings(this.clientId, this.payload);
    }

    boolean singleAudienceElementCheck(String clientId, String aud) {
        return (aud.equals(clientId));
    }

    boolean multipleAudienceElementCheck(String clientId, List<String> audList) {
        Iterator<String> it = audList.iterator();
        while (it.hasNext()) {
            if (it.next().equals(clientId)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void checkStrings(String clientId, JWTPayload payload) throws IDTokenValidationFailedException {
        boolean audienceCheck = false;
        String aud = null;
        String azp = null;
        Object audienceElement = payload.get(PayloadConstants.AUDIENCE);
        if (audienceElement instanceof String) {
            //if (((String) audienceElement).equalsIgnoreCase(clientId)) {
            //    audienceCheck = true;
            //}
            aud = (String) audienceElement;
            audienceCheck = singleAudienceElementCheck(clientId, aud);
        } else if (audienceElement instanceof List) {
            if (((List<String>) audienceElement).size() == 1) {
                //if (((List<String>) audienceElement).get(0).equalsIgnoreCase(clientId)) {
                //   audienceCheck = true;
                //}
                aud = ((List<String>) audienceElement).get(0);
                audienceCheck = singleAudienceElementCheck(clientId, aud);
            } else if (((List<String>) audienceElement).size() > 1) {
                if (multipleAudienceElementCheck(clientId, (List<String>) audienceElement)) {
                    azp = (String) payload.get(PayloadConstants.AUTHORIZED_PARTY);
                    if (azp != null) {
                        if (!(azp.equals(clientId))) {
                            Tr.error(tc, "OIDC_IDTOKEN_VERIFY_AUD_AZP_ERR", new Object[] { azp, clientId });
                            throw new IDTokenValidationFailedException(Tr.formatMessage(tc,
                                    "OIDC_IDTOKEN_VERIFY_AUD_AZP_ERR", new Object[] { azp, clientId }));
                        } else {
                            audienceCheck = true;
                        }
                    }
                }
            }
        }
        if (!audienceCheck) {
            //throw new SignatureException("Wrong Audience in the token.");
            Tr.error(tc, "OIDC_IDTOKEN_VERIFY_AUD_ERR", new Object[] { aud, clientId });
            throw IDTokenValidationFailedException.format("OIDC_IDTOKEN_VERIFY_AUD_ERR", aud, clientId);
        }
    }

    // Marked this method off because it's not in use for now
    //@SuppressWarnings("unchecked")
    //private static void checkUri(String ourUriString, JWTPayload payload) throws SignatureException {
    //
    //    boolean audienceCheck = false;
    //    URI ourUri = URI.create(ourUriString);
    //    URI tokenUri = null;
    //    Object audienceElement = payload.get(PayloadConstants.AUDIENCE);
    //
    //    if (audienceElement instanceof String) {
    //        tokenUri = URI.create((String) audienceElement);
    //        if (!ourUri.getScheme().equalsIgnoreCase(tokenUri.getScheme())) {
    //            throw new SignatureException("scheme in token URI (" + tokenUri.getScheme() + ") is wrong");
    //        }
    //
    //        if (!ourUri.getAuthority().equalsIgnoreCase(tokenUri.getAuthority())) {
    //            throw new SignatureException("authority in token URI (" + tokenUri.getAuthority() + ") is wrong");
    //        }
    //        audienceCheck = true;
    //    }
    //    else if (audienceElement instanceof List) {
    //        if (((List<?>) audienceElement).size() == 1) {
    //            tokenUri = URI.create(((List<String>) audienceElement).get(0));
    //            if (!ourUri.getScheme().equalsIgnoreCase(tokenUri.getScheme())) {
    //                throw new SignatureException("scheme in token URI (" + tokenUri.getScheme() + ") is wrong");
    //            }
    //
    //            if (!ourUri.getAuthority().equalsIgnoreCase(tokenUri.getAuthority())) {
    //                throw new SignatureException("authority in token URI (" + tokenUri.getAuthority() + ") is wrong");
    //            }
    //            audienceCheck = true;
    //        }
    //        else if (((List<?>) audienceElement).size() > 1) {
    //            String azp = (String) payload.get(PayloadConstants.AUTHORIZED_PARTY);
    //            if (azp != null) {
    //                tokenUri = URI.create(azp);
    //                if (ourUri.getScheme().equalsIgnoreCase(tokenUri.getScheme()) &&
    //                    ourUri.getAuthority().equalsIgnoreCase(tokenUri.getAuthority())) {
    //                    //throw new SignatureException("scheme in token URI (" + tokenUri.getScheme() + ") is wrong");
    //                    audienceCheck = true;
    //                }
    //            }
    //        }
    //
    //        if (!audienceCheck) {
    //            throw new SignatureException("Wrong Scheme and/or Authority in token URI.");
    //        }
    //    }
    //}
}
