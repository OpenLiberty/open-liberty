/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2016
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.http.logging.source;

/**
 *
 */
public class AccessLogData {

    private final String uriPath;
    private final String requestMethod;
    private final String queryString;
    private final String requestHost;
    private final String requestPort;
    private final String remoteHost;
    private final String userAgent;
    private final String requestProtocol;
    private final long responseSize;
    private final int responseCode;
    private final long elapsedTime;
    private final long requestStartTime;
    private final long timestamp;
    private final String sequence;

    public AccessLogData(String uriPath, String requestMethod, String queryString,
                         String requestHost, String requestPort,
                         long requestStartTime, String remoteHost, String userAgent,
                         String requestProtocol, long responseSize, int responseCode,
                         long elapsedTime, long timestamp, String sequence) {
        this.uriPath = uriPath;
        this.requestMethod = requestMethod;
        this.queryString = queryString;
        this.requestHost = requestHost;
        this.requestPort = requestPort;
        this.requestStartTime = requestStartTime;
        this.remoteHost = remoteHost;
        this.userAgent = userAgent;
        this.requestProtocol = requestProtocol;
        this.responseSize = responseSize;
        this.responseCode = responseCode;
        this.elapsedTime = elapsedTime;
        this.timestamp = timestamp;
        this.sequence = sequence;
    }

    /**
     * The URL path requested, not including any query string.
     */
    public String getURIPath() {
        return uriPath;
    }

    /**
     * The request method.
     */
    public String getRequestMethod() {
        return requestMethod;
    }

    /**
     * The request start time.
     *
     * @return
     */
    public long getRequestStartTime() {
        return requestStartTime;
    }

    /**
     * The Local IP-address.
     *
     * @return
     */
    public String getRequestHost() {
        return requestHost;
    }

    /**
     * The Local port.
     *
     * @return
     */
    public String getRequestPort() {
        return requestPort;
    }

    /**
     * The Remote host.
     */
    public String getRemoteHost() {
        return remoteHost;
    }

    /**
     * The client which sent the request, which would be nothing but the 'User-Agent' request header value.
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * The request protocol.
     */
    public String getRequestProtocol() {
        return requestProtocol;
    }

    /**
     * Size of response in bytes, excluding HTTP headers.
     */
    public long getResponseSize() {
        return responseSize;
    }

    /**
     * The reponse code.
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Elapsed time, in milliseconds, of the request/response exchange,
     * Millisecond accuracy, microsecond precision.
     */
    public long getElapsedTime() {
        return elapsedTime;
    }

    /**
     * The query string (prepended with a ?).
     */
    public String getQueryString() {
        return queryString;
    }

    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    public String getSequence() {
        return sequence;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "AccessLogData [uriPath=" + uriPath
               + ", requestMethod=" + requestMethod
               + ", queryString=" + queryString
               + ", requestHost=" + requestHost
               + ", requestPort=" + requestPort
               + ", remoteHost=" + remoteHost
               + ", userAgent=" + userAgent
               + ", requestProtocol=" + requestProtocol
               + ", responseSize=" + responseSize
               + ", responseCode=" + responseCode
               + ", elapsedTime=" + elapsedTime
               + ", requestStartTime=" + requestStartTime
               + ", timestamp=" + timestamp
               + ", sequence=" + sequence + "]";
    }

}
