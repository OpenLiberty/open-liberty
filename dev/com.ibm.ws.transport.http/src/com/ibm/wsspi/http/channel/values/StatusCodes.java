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
package com.ibm.wsspi.http.channel.values;

import com.ibm.wsspi.genericbnf.BNFHeaders;
import com.ibm.wsspi.genericbnf.GenericKeys;
import com.ibm.wsspi.http.channel.HttpChannelUtils;
import com.ibm.wsspi.http.channel.error.HttpError;

/**
 * Class representing possible values for the HTTP Response status code.
 */
public class StatusCodes extends GenericKeys {

    /** Largest possible status code value */
    private static final int MAX_CODE = 750;
    /** Array of status codes */
    private static StatusCodes[] statusCodes = new StatusCodes[MAX_CODE + 1];

    // Following are all of the predefined HTTP status codes

    /** Undefined status code for unexpected values */
    public static final StatusCodes UNDEF = new StatusCodes(0, "Undefined", false);
    /** Status code for 202 Accepted */
    public static final StatusCodes ACCEPTED = new StatusCodes(202, "Accepted", false);
    /** Status code for 502 Bad Gateway */
    public static final StatusCodes BAD_GATEWAY = new StatusCodes(502, "Bad Gateway", true);
    /** Status code for 405 Method Not Allowed */
    public static final StatusCodes BAD_METHOD = new StatusCodes(405, "Method Not Allowed", true);
    /** Status code for 400 Bad Request */
    public static final StatusCodes BAD_REQUEST = new StatusCodes(400, "Bad Request", true);
    /** Status code for 409 Conflict */
    public static final StatusCodes CONFLICT = new StatusCodes(409, "Conflict", true);
    /** Status code for 100 Continue */
    public static final StatusCodes CONTINUE = new StatusCodes(100, "Continue", false);
    /** Status code for 201 Created */
    public static final StatusCodes CREATED = new StatusCodes(201, "Created", false);
    /** Status code for 413 Request Entity Too Large */
    public static final StatusCodes ENTITY_TOO_LARGE = new StatusCodes(413, "Request Entity Too Large", true);
    /** Status code for 417 Expectation Failed */
    public static final StatusCodes EXPECTATION_FAILED = new StatusCodes(417, "Expectation Failed", true);
    /** Status code for 403 Forbidden */
    public static final StatusCodes FORBIDDEN = new StatusCodes(403, "Forbidden", true);
    /** Status code for 302 Found */
    public static final StatusCodes FOUND = new StatusCodes(302, "Found", false);
    /** Status code for 504 Gateway Timeout */
    public static final StatusCodes GATEWAY_TIMEOUT = new StatusCodes(504, "Gateway Timeout", true);
    /** Status code for 410 Gone */
    public static final StatusCodes GONE = new StatusCodes(410, "Gone", true);
    /** Status code for 500 Internal Server Error */
    public static final StatusCodes INTERNAL_ERROR = new StatusCodes(500, "Internal Server Error", true);
    /** Status code for 411 Length Required */
    public static final StatusCodes LENGTH_REQUIRED = new StatusCodes(411, "Length Required", true);
    /** Status code for 301 Moved Permanently */
    public static final StatusCodes MOVED_PERM = new StatusCodes(301, "Moved Permanently", false);
    /** Status code for 300 Multiple Choices */
    public static final StatusCodes MULT_CHOICE = new StatusCodes(300, "Multiple Choices", false);
    /** Status code for 204 No Content */
    public static final StatusCodes NO_CONTENT = new StatusCodes(204, "No Content", false);
    /** Status code for 406 Not Acceptable */
    public static final StatusCodes NOT_ACCEPTABLE = new StatusCodes(406, "Not Acceptable", true);
    /** Status code for 203 Non-Authoritative Information */
    public static final StatusCodes NOT_AUTHORITATIVE = new StatusCodes(203, "Non-Authoritative Information", false);
    /** Status code for 404 Not Found */
    public static final StatusCodes NOT_FOUND = new StatusCodes(404, "Not Found", true);
    /** Status code for 501 Not Implemented */
    public static final StatusCodes NOT_IMPLEMENTED = new StatusCodes(501, "Not Implemented", true);
    /** Status code for 304 Not Modified */
    public static final StatusCodes NOT_MODIFIED = new StatusCodes(304, "Not Modified", false);
    /** Status code for 200 OK */
    public static final StatusCodes OK = new StatusCodes(200, "OK", false);
    /** Status code for 206 Partial Content */
    public static final StatusCodes PARTIAL = new StatusCodes(206, "Partial Content", false);
    /** Status code for 402 Payment Required */
    public static final StatusCodes PAYMENT_REQUIRED = new StatusCodes(402, "Payment Required", true);
    /** Status code for 412 Precondition Failed */
    public static final StatusCodes PRECON_FAILED = new StatusCodes(412, "Precondition Failed", true);
    /** Status code for 416 Requested Range Not Satisfiable */
    public static final StatusCodes RANGE_UNAVAIL = new StatusCodes(416, "Requested Range Not Satisfiable", true);
    /** Status code for 407 Proxy Authentication Required */
    public static final StatusCodes PROXY_AUTH = new StatusCodes(407, "Proxy Authentication Required", true);
    /** Status code for 408 Request Timeout */
    public static final StatusCodes REQ_TIMEOUT = new StatusCodes(408, "Request Timeout", true);
    /** Status code for 414 Request-URI Too Large */
    public static final StatusCodes REQ_TOO_LONG = new StatusCodes(414, "Request-URI Too Large", true);
    /** Status code for 205 Reset Content */
    public static final StatusCodes RESET = new StatusCodes(205, "Reset Content", false);
    /** Status code for 303 See Other */
    public static final StatusCodes SEE_OTHER = new StatusCodes(303, "See Other", false);
    /** Status code for 101 Switching Protocols */
    public static final StatusCodes SWITCHING_PROTOCOLS = new StatusCodes(101, "Switching Protocols", false);
    /** Status code for 401 Unauthorized */
    public static final StatusCodes UNAUTHORIZED = new StatusCodes(401, "Unauthorized", false);
    /** Status code for 503 Service Unavailable */
    public static final StatusCodes UNAVAILABLE = new StatusCodes(503, "Service Unavailable", true);
    /** Status code for 415 Unsupported Media Type */
    public static final StatusCodes UNSUPPORTED_TYPE = new StatusCodes(415, "Unsupported Media Type", true);
    /** Status code for 305 Use Proxy */
    public static final StatusCodes USE_PROXY = new StatusCodes(305, "Use Proxy", false);
    /** Status code for 307 Temporary Redirect */
    public static final StatusCodes TEMP_REDIRECT = new StatusCodes(307, "Temporary Redirect", false);
    /** Status code for 505 HTTP Version Not Supported */
    public static final StatusCodes UNSUPPORTED_VERSION = new StatusCodes(505, "HTTP Version Not Supported", true);

    /** Standard phrase associated with this status code */
    protected String myPhrase = null;
    /** Byte[] for that standard phrase */
    protected byte[] myPhraseBytes = null;
    /** Error associated with error status codes */
    protected HttpError myError = null;
    /**
     * int representation of this status code -- ordinal works for all
     * instances except the UNDEF one
     */
    protected int myIntCode = 0;
    /** Representation of the status code+space+default phrase */
    protected byte[] bytesWithPhrase = null;
    /** Does this status code allow a body with the response? */
    protected boolean bBodyAllowed = true;
    /** Flag on whether or not this instance is an undefined one */
    protected boolean undefined = false;

    /**
     * Constructor with just a name and an ordinal. This requires that the init()
     * method be called.
     * 
     * @param name
     * @param ordinal
     */
    public StatusCodes(String name, int ordinal) {
        super(name, ordinal);
    }

    /**
     * Constructor for a status code/reason phrase object.
     * 
     * @param code
     * @param phrase
     * @param isError
     */
    public StatusCodes(int code, String phrase, boolean isError) {
        super("" + code, code);

        if (0 > code || MAX_CODE < code) {
            throw new IndexOutOfBoundsException(code + " is out of bounds");
        }
        init(code, phrase, isError);
        statusCodes[code] = this;
    }

    /**
     * Constructor copies the input value to create an exact match. Intended
     * for use with the "Undefined" enum value where the byte[] value will
     * differ, but the ordinal and name will still match. Note, this does
     * NOT go on the list of defined keys and thus not on the matcher tree.
     * 
     * @param val
     */
    protected StatusCodes(StatusCodes val) {
        super(val.getName(), val.getOrdinal());
        this.myPhrase = val.getDefaultPhrase();
        this.myPhraseBytes = val.getDefaultPhraseBytes();
        this.myIntCode = val.getIntCode();
        this.myError = val.getHttpError();
    }

    /**
     * Initialize this status code with the input information.
     * 
     * @param code
     * @param phrase
     * @param isError
     */
    protected void init(int code, String phrase, boolean isError) {
        this.myPhrase = phrase;
        this.myPhraseBytes = HttpChannelUtils.getEnglishBytes(phrase);
        this.myIntCode = code;
        if (isError) {
            this.myError = new HttpError(code, this.myPhrase);
        }
        initSpecialArrays();
        checkForAllowedBody();
    }

    /**
     * Make a new "Undefined" enumerated value with the given input.
     * 
     * @param value
     * @return StatusCodes
     */
    public static StatusCodes makeUndefinedValue(int value) {
        StatusCodes code = new StatusCodes(StatusCodes.UNDEF);
        code.name = Integer.toString(value);
        code.byteArray = HttpChannelUtils.getEnglishBytes(code.getName());
        code.myIntCode = value;
        code.initSpecialArrays();
        code.checkForAllowedBody();
        code.hashcode = code.ordinal + code.name.hashCode();
        code.undefined = true;
        return code;
    }

    /**
     * Query whether this class instance is an undefined value or not.
     * 
     * @return boolean
     */
    public boolean isUndefined() {
        return this.undefined;
    }

    /**
     * Initialize the special arrays.
     */
    protected void initSpecialArrays() {
        int len = getByteArray().length;
        // set up the "status code + SPACE + default reason phrase"
        this.bytesWithPhrase = new byte[len + 1 + this.myPhraseBytes.length];
        System.arraycopy(getByteArray(), 0, this.bytesWithPhrase, 0, len);
        this.bytesWithPhrase[len] = BNFHeaders.SPACE;
        System.arraycopy(this.myPhraseBytes, 0, this.bytesWithPhrase, len + 1, this.myPhraseBytes.length);
    }

    /**
     * When marshalling, we can speed things up by returning the status code
     * plus a space plus the default reason phrase all in one array.
     * 
     * @return byte[]
     */
    public byte[] getStatusWithPhrase() {
        return this.bytesWithPhrase;
    }

    /**
     * Query the enumerated value that exists with the specified ordinal
     * value.
     * 
     * @param i
     * @return StatusCodes
     */
    public static StatusCodes getByOrdinal(int i) {
        if (0 > i || i >= MAX_CODE) {
            throw new IndexOutOfBoundsException("Index " + i + " is out of bounds");
        }
        return statusCodes[i];
    }

    /**
     * Query the default Reason-Phrase for this StatusCode.
     * 
     * @return String
     */
    public String getDefaultPhrase() {
        return this.myPhrase;
    }

    /**
     * Query the byte[] representation of the default Reason-Phrase for this
     * StatusCode.
     * 
     * @return byte[]
     */
    public byte[] getDefaultPhraseBytes() {
        return this.myPhraseBytes;
    }

    /**
     * Query the int value of this particular status code.
     * 
     * @return int
     */
    public int getIntCode() {
        return this.myIntCode;
    }

    /**
     * Checks to see if this was set as an error code.
     * 
     * @return boolean
     */
    public boolean isErrorCode() {
        return (null != getHttpError());
    }

    /**
     * If this is an error code, it should have an associated error code that
     * the connection may be closed with. If it does not exist, will return null.
     * 
     * @return HttpError
     */
    public HttpError getHttpError() {
        return this.myError;
    }

    /**
     * Debug print of this object simply returns the name, which is the
     * String representation of the integer code.
     * 
     * @return String
     */
    public String toString() {
        return "Status code: " + getName();
    }

    /**
     * Is a body allowed with this status code?
     * 
     * @return boolean
     */
    public boolean isBodyAllowed() {
        return this.bBodyAllowed;
    }

    /**
     * Set the flag on whether a body is allowed with this status code or not.
     * 
     * @param flag
     */
    public void setBodyAllowed(boolean flag) {
        this.bBodyAllowed = flag;
    }

    /**
     * Check the status code value to see whether a body is allowed for the
     * particular HTTP code.
     * <p>
     * This can be overridden through the setBodyAllowed() API.
     */
    protected void checkForAllowedBody() {
        int code = getIntCode();
        // check for explicit codes
        if (204 == code || 304 == code) {
            setBodyAllowed(false);
        }
        // all 1xx codes should not have a body
        else if (100 <= code && 200 > code) {
            setBodyAllowed(false);
        }
        // default everything else...
        else {
            setBodyAllowed(true);
        }
    }

}
