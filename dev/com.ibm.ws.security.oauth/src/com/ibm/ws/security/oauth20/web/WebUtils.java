/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSONObject;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.error.impl.BrowserAndServerLogMessage;
import com.ibm.ws.security.oauth20.util.OIDCConstants;

/**
 *  Class for common functions used when handling web requests
 */
public class WebUtils {

    private static TraceComponent tc = Tr.register(WebUtils.class);

    /**
     * Send the given error status code. If an errorCode is provided set it
     * in the response as a JSON object per the OAuth spec:
     * <a href="http://tools.ietf.org/html/rfc6749#section-5.2">
     * http://tools.ietf.org/html/rfc6749#section-5.2</a>
     *
     * @param response   HttpServletResponse object
     * @param statusCode HTTP status code
     * @param errorCode  error code String
     * @param errorDescription
     */
    public static void sendErrorJSON(HttpServletResponse response, int statusCode, String errorCode, String errorDescription) {
        sendErrorJSON(response, statusCode, errorCode, errorDescription, null, false);
    }

    public static void sendErrorJSON(HttpServletResponse response, int statusCode, String errorCode, String errorDescription, String authScheme) {
        sendErrorJSON(response, statusCode, errorCode, errorDescription, authScheme, false);
    }

    public static void sendErrorJSON(HttpServletResponse response, int statusCode, String errorCode,
            String errorDescription, boolean suppressBasicAuthChallenge) {
        sendErrorJSON(response, statusCode, errorCode, errorDescription, null, suppressBasicAuthChallenge);
    }

    /**
     * Send the given error status code. If an errorCode is provided set it
     * in the response as a JSON object per the OAuth spec:
     * <a href="http://tools.ietf.org/html/rfc6749#section-5.2">
     * http://tools.ietf.org/html/rfc6749#section-5.2</a>
     *
     * @param response   HttpServletResponse object
     * @param statusCode HTTP status code
     * @param errorCode  error code String
     * @param errorDescription
     * @param authScheme Authentication scheme originally included in the Authorization header of the request. If null, defaults to "Basic."
     */
    public static void sendErrorJSON(HttpServletResponse response, int statusCode, String errorCode,
            String errorDescription, String authScheme, boolean suppressBasicAuthChallenge) {
        final String error = "error";
        final String error_description = "error_description";
        try {
            if (errorCode != null) {
                response.setStatus(statusCode);
                response.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                        OAuth20Constants.HTTP_CONTENT_TYPE_JSON);

                if (statusCode == HttpServletResponse.SC_UNAUTHORIZED) {
                    if (authScheme == null) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Authentication scheme was null, will default to basic if not suppressed.");
                        }
                        authScheme = "Basic";
                    }
                    String errMsg = authScheme + " " + error + "=\"" + errorCode + "\", " +
                            error_description + "=\"" + errorDescription + "\", " +
                            "realm=\"\"";
                    // Per section 1.2 of the HTTP Authentication: Basic and Digest Access Authentication spec (RFC 2617), "the
                    // realm directive (case-insensitive) is required for all authentication schemes that issue a challenge."

                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Error code:" + statusCode + " errMsg=" + errMsg + " response already committed: " + response.isCommitted());
                    }
                    if (!suppressBasicAuthChallenge) {
                        if (!response.isCommitted()) {
                            response.setHeader(Constants.WWW_AUTHENTICATE, errMsg);
                        }
                    }
                }

                JSONObject responseJSON = new JSONObject();
                responseJSON.put(error, errorCode);
                if (errorDescription != null) {
                    responseJSON.put(error_description, errorDescription);
                }
                PrintWriter pw;
                pw = response.getWriter();
                pw.write(responseJSON.toString());
                pw.flush();
            } else {
                response.sendError(statusCode);
            }
        } catch (IOException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Internal error processing token introspect request", e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (IOException ioe) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Internal error process token introspect request error", ioe);
            }
        }
    }

    /**
     * Set the status code and the given JSON object into the
     * response, and set the content type to application/json.
     *
     * @param response   HttpServletResponse object
     * @param statusCode HTTP status code
     * @param jsonObj    response JSONObject
     */
    public static void setJSONResponse(HttpServletResponse response, int statusCode, JSONObject jsonObj) {
        String jsonStr = null;
        if (jsonObj != null) {
            jsonStr = jsonObj.toString();
        }
        setJSONResponse(response, statusCode, jsonStr);
    }

    /**
     * Set the status code and the given JSON string into the
     * response, and set the content type to application/json.
     *
     * @param response   HttpServletResponse object
     * @param statusCode HTTP status code
     * @param jsonStr    response JSON object as String
     */
    public static void setJSONResponse(HttpServletResponse response, int statusCode, String jsonStr) {
        try {
            String cacheControlValue = response.getHeader(OAuth20Constants.HEADER_CACHE_CONTROL);
            if (cacheControlValue != null &&
                    !cacheControlValue.isEmpty()) {
                cacheControlValue = cacheControlValue + ", " + OAuth20Constants.HEADERVAL_CACHE_CONTROL;
            } else {
                cacheControlValue = OAuth20Constants.HEADERVAL_CACHE_CONTROL;
            }
            response.setHeader(OAuth20Constants.HEADER_CACHE_CONTROL, cacheControlValue);
            response.setHeader(OAuth20Constants.HEADER_PRAGMA,
                    OAuth20Constants.HEADERVAL_PRAGMA);
            response.setStatus(statusCode);
            if (jsonStr != null) {
                response.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                        OAuth20Constants.HTTP_CONTENT_TYPE_JSON);
                PrintWriter pw;
                pw = response.getWriter();
                pw.write(jsonStr);
                pw.flush();
            }
        } catch (IOException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Internal error processing token introspect request", e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (IOException ioe) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Internal error process token introspect request error", ioe);
            }
        }
    }

    /**
     * Encode the given String
     *
     * @param text
     * @param locale
     * @param encoding
     * @return encoded String
     */
    public static String encode(String text, Locale locale, String encoding) {
        String encodedText = text;
        try {
            encodedText = URLEncoder.encode(text, encoding);
        } catch (UnsupportedEncodingException e1) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Internal error encoding text", new Object[] { e1 });
        }
        return encodedText;
    }

    /**
     * Encode the given String array
     *
     * @param textArray
     * @param locale
     * @param encoding
     * @return array of encoded Strings
     */
    public static String[] encode(String textArray[], Locale locale, String encoding) {
        String encodedTextArray[] = null;
        if (textArray != null) {
            encodedTextArray = new String[textArray.length];
            int i = 0;
            for (String text : textArray) {
                try {
                    encodedTextArray[i] = URLEncoder.encode(text, encoding);
                } catch (UnsupportedEncodingException e1) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Internal error encoding text", new Object[] { e1 });
                }
                i++;
            }
        }
        return encodedTextArray;
    }

    /**
     * Encodes the given string so that it can be used within a html page.
     *
     * @param string the string to convert
     */
    public static String htmlEncode(String string) {
        return htmlEncode(string, true, true, true);
    }

    /**
     * Encodes the given string so that it can be used within a html page.
     *
     * @param string the string to convert
     * @param encodeNewline if true newline characters are converted to &lt;br&gt;'s
     * @param encodeSubsequentBlanksToNbsp if true subsequent blanks are converted to &amp;nbsp;'s
     * @param encodeNonLatin if true encode non-latin characters as numeric character references
     */
    public static String htmlEncode(String string, boolean encodeNewline, boolean encodeSubsequentBlanksToNbsp, boolean encodeNonLatin) {
        if (string == null) {
            return "";
        }

        StringBuilder sb = null; // create later on demand
        String app;
        char c;
        for (int i = 0; i < string.length(); ++i) {
            app = null;
            c = string.charAt(i);

            // All characters before letters
            if (c < 0x41) {
                switch (c) {
                case '"':
                    app = "&quot;";
                    break; // "
                case '&':
                    app = "&amp;";
                    break; // &
                case '<':
                    app = "&lt;";
                    break; // <
                case '>':
                    app = "&gt;";
                    break; // >
                case ' ':
                    if (encodeSubsequentBlanksToNbsp && (i == 0 || (i - 1 >= 0 && string.charAt(i - 1) == ' '))) {
                        // Space at beginning or after another space
                        app = "&#160;";
                    }
                    break;
                case '\n':
                    if (encodeNewline) {
                        app = "<br/>";
                    }
                    break;
                default:
                    // No special encoding needed
                    break;
                }
            } else if (encodeNonLatin && c > 0x80) {
                switch (c) {
                // german umlauts
                case '\u00E4':
                    app = "&auml;";
                    break;
                case '\u00C4':
                    app = "&Auml;";
                    break;
                case '\u00F6':
                    app = "&ouml;";
                    break;
                case '\u00D6':
                    app = "&Ouml;";
                    break;
                case '\u00FC':
                    app = "&uuml;";
                    break;
                case '\u00DC':
                    app = "&Uuml;";
                    break;
                case '\u00DF':
                    app = "&szlig;";
                    break;

                // misc
                // case 0x80: app = "&euro;"; break; sometimes euro symbol is ascii 128, should we suport it?
                case '\u20AC':
                    app = "&euro;";
                    break;
                case '\u00AB':
                    app = "&laquo;";
                    break;
                case '\u00BB':
                    app = "&raquo;";
                    break;
                case '\u00A0':
                    app = "&#160;";
                    break;

                default:
                    // encode all non basic latin characters
                    app = "&#" + ((int) c) + ";";
                    break;
                }
            }
            if (app != null) {
                if (sb == null) {
                    sb = new StringBuilder(string.substring(0, i));
                }
                sb.append(app);
            } else {
                if (sb != null) {
                    sb.append(c);
                }
            }
        }

        if (sb == null) {
            return string;
        } else {
            return sb.toString();
        }
    }

    public static void throwOidcServerException(HttpServletRequest request, OAuth20Exception e) throws OidcServerException {
        Tr.error(tc, e.getMsgKey(), e.getObjects());
        throw new OidcServerException(new BrowserAndServerLogMessage(tc, e.getMsgKey(), e.getObjects()), OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_BAD_REQUEST);

    }
}
