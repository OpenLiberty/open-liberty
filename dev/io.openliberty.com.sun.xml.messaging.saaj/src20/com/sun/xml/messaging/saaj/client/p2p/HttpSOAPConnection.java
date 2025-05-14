/*
 * Copyright (c) 1997, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.messaging.saaj.client.p2p;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.MimeHeader;
import jakarta.xml.soap.MimeHeaders;
import jakarta.xml.soap.SOAPConnection;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;

import com.sun.xml.messaging.saaj.SOAPExceptionImpl;
import com.sun.xml.messaging.saaj.util.Base64;
import com.sun.xml.messaging.saaj.util.ByteInputStream;
import com.sun.xml.messaging.saaj.util.LogDomainConstants;
import com.sun.xml.messaging.saaj.util.ParseUtil;
import com.sun.xml.messaging.saaj.util.SAAJUtil;

/**
 * This represents a "connection" to the simple HTTP-based provider.
 *
 * @author Anil Vijendran (akv@eng.sun.com)
 * @author Rajiv Mordani (rajiv.mordani@sun.com)
 * @author Manveen Kaur (manveen.kaur@sun.com)
 *
 */
class HttpSOAPConnection extends SOAPConnection {

    protected static final Logger log =
        Logger.getLogger(LogDomainConstants.HTTP_CONN_DOMAIN,
                         "com.sun.xml.messaging.saaj.client.p2p.LocalStrings");

    /**
     * URLConnection connect timeout
     */
    private static int CONNECT_TIMEOUT;

    /**
     * URLConnection read timeout
     */
    private static int READ_TIMEOUT;

    static {
        Integer i = SAAJUtil.getSystemInteger("saaj.connect.timeout");
        if (i != null) {
            CONNECT_TIMEOUT = i;
        }
        i = SAAJUtil.getSystemInteger("saaj.read.timeout");
        if (i != null) {
            READ_TIMEOUT = i;
        }
    }

    MessageFactory messageFactory = null;

    boolean closed = false;

    public HttpSOAPConnection() throws SOAPException {

        try {
            messageFactory = MessageFactory.newInstance(SOAPConstants.DYNAMIC_SOAP_PROTOCOL);
        } catch (NoSuchMethodError ex) {
            //fallback to default SOAP 1.1 in this case for backward compatibility
            messageFactory = MessageFactory.newInstance();
        } catch (Exception ex) {
            log.log(Level.SEVERE, "SAAJ0001.p2p.cannot.create.msg.factory", ex);
            throw new SOAPExceptionImpl("Unable to create message factory", ex);
        }
    }

    @Override
    public void close() throws SOAPException {
        if (closed) {
            log.severe("SAAJ0002.p2p.close.already.closed.conn");
            throw new SOAPExceptionImpl("Connection already closed");
        }

        messageFactory = null;
        closed = true;
    }

    @Override
   public SOAPMessage call(SOAPMessage message, Object endPoint)
        throws SOAPException {
        if (closed) {
            log.severe("SAAJ0003.p2p.call.already.closed.conn");
            throw new SOAPExceptionImpl("Connection is closed");
        }

        if (endPoint instanceof java.lang.String) {
            try {
                endPoint = new URL((String) endPoint);
            } catch (MalformedURLException mex) {
                log.log(Level.SEVERE, "SAAJ0006.p2p.bad.URL", mex);
                throw new SOAPExceptionImpl("Bad URL: " + mex.getMessage());
            }
        }

        if (endPoint instanceof URL)
            try {
                SOAPMessage response = post(message, (URL)endPoint);
                return response;
            } catch (Exception ex) {
                // TBD -- chaining?
                throw new SOAPExceptionImpl(ex);
            } else {
            log.severe("SAAJ0007.p2p.bad.endPoint.type");
            throw new SOAPExceptionImpl("Bad endPoint type " + endPoint);
        }
    }

    SOAPMessage post(SOAPMessage message, URL endPoint) throws SOAPException, IOException {
        boolean isFailure = false;

        URL url = null;
        HttpURLConnection httpConnection = null;

        int responseCode = 0;
        try {
            // Process the URL
            URI uri = new URI(endPoint.toString());
            String userInfo = uri.getRawUserInfo();

            url = endPoint;

            if (dL > 0)
                d("uri: " + userInfo + " " + url + " " + uri);

            // TBD
            //    Will deal with https later.
            if (!url.getProtocol().equalsIgnoreCase("http")
                && !url.getProtocol().equalsIgnoreCase("https")) {
                log.severe("SAAJ0052.p2p.protocol.mustbe.http.or.https");
                throw new IllegalArgumentException(
                    "Protocol "
                        + url.getProtocol()
                        + " not supported in URL "
                        + url);
            }
            httpConnection = createConnection(url);

            httpConnection.setRequestMethod("POST");

            httpConnection.setDoOutput(true);
            httpConnection.setDoInput(true);
            httpConnection.setUseCaches(false);
            httpConnection.setInstanceFollowRedirects(true);
            httpConnection.setConnectTimeout(CONNECT_TIMEOUT);
            httpConnection.setReadTimeout(READ_TIMEOUT);

            if (message.saveRequired())
                message.saveChanges();

            MimeHeaders headers = message.getMimeHeaders();

            Iterator<?> it = headers.getAllHeaders();
            boolean hasAuth = false; // true if we find explicit Auth header
            while (it.hasNext()) {
                MimeHeader header = (MimeHeader) it.next();

                String[] values = headers.getHeader(header.getName());
                if (values.length == 1)
                    httpConnection.setRequestProperty(
                        header.getName(),
                        header.getValue());
                else {
                    StringBuilder concat = new StringBuilder();
                    int i = 0;
                    while (i < values.length) {
                        if (i != 0)
                            concat.append(',');
                        concat.append(values[i]);
                        i++;
                    }

                    httpConnection.setRequestProperty(
                        header.getName(),
                        concat.toString());
                }

                if ("Authorization".equals(header.getName())) {
                    hasAuth = true;
                    if (log.isLoggable(Level.FINE))
                        log.fine("SAAJ0091.p2p.https.auth.in.POST.true");
                }
            }

            if (!hasAuth && userInfo != null) {
                initAuthUserInfo(httpConnection, userInfo);
            }

            OutputStream out = httpConnection.getOutputStream();
            try {
                message.writeTo(out);
                out.flush();
            } finally {
                out.close();
            }

            httpConnection.connect();

            try {

                responseCode = httpConnection.getResponseCode();

                // let HTTP_INTERNAL_ERROR (500) and HTTP_BAD_REQUEST (400) through because it is used for SOAP faults
                if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR || responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                    isFailure = true;
                }
                //else if (responseCode != HttpURLConnection.HTTP_OK)
                //else if (!(responseCode >= HttpURLConnection.HTTP_OK && responseCode < 207))
                else if ((responseCode / 100) != 2) {
                    log.log(Level.SEVERE,
                            "SAAJ0008.p2p.bad.response",
                            new String[] {httpConnection.getResponseMessage()});
                    throw new SOAPExceptionImpl(
                        "Bad response: ("
                            + responseCode
                            + httpConnection.getResponseMessage());

                }
            } catch (IOException e) {
                // on JDK1.3.1_01, we end up here, but then getResponseCode() succeeds!
                responseCode = httpConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR || responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                    isFailure = true;
                } else {
                    throw e;
                }

            }

        } catch (SOAPException ex) {
            throw ex;
        } catch (Exception ex) {
            log.severe("SAAJ0009.p2p.msg.send.failed");
            throw new SOAPExceptionImpl("Message send failed", ex);
        }

        SOAPMessage response = null;
        InputStream httpIn = null;
        if (responseCode == HttpURLConnection.HTTP_OK || isFailure) {
            try {
                MimeHeaders headers = new MimeHeaders();

                String key, value;

                // Header field 0 is the status line so we skip it.

                int i = 1;

                while (true) {
                    key = httpConnection.getHeaderFieldKey(i);
                    value = httpConnection.getHeaderField(i);

                    if (key == null && value == null)
                        break;

                    if (key != null) {
                        StringTokenizer values =
                            new StringTokenizer(value, ",");
                        while (values.hasMoreTokens())
                            headers.addHeader(key, values.nextToken().trim());
                    }
                    i++;
                }

                httpIn =
                    (isFailure
                        ? httpConnection.getErrorStream()
                        : httpConnection.getInputStream());

                byte[] bytes = readFully(httpIn);

                int length =
                    httpConnection.getContentLength() == -1
                        ? bytes.length
                        : httpConnection.getContentLength();

                // If no reply message is returned,
                // content-Length header field value is expected to be zero.
                if (length == 0) {
                    response = null;
                    log.warning("SAAJ0014.p2p.content.zero");
                } else {
                    ByteInputStream in = new ByteInputStream(bytes, length);
                    response = messageFactory.createMessage(headers, in);
                }

            } catch (SOAPException ex) {
                throw ex;
            } catch (Exception ex) {
                log.log(Level.SEVERE,"SAAJ0010.p2p.cannot.read.resp", ex);
                throw new SOAPExceptionImpl(
                    "Unable to read response: " + ex.getMessage());
            } finally {
               if (httpIn != null)
                   httpIn.close();
               httpConnection.disconnect();
            }
        }
        return response;
    }

    // Object identifies where the request should be sent.
    // It is required to support objects of type String and java.net.URL.

    @Override
    public SOAPMessage get(Object endPoint) throws SOAPException {
        if (closed) {
            log.severe("SAAJ0011.p2p.get.already.closed.conn");
            throw new SOAPExceptionImpl("Connection is closed");
        }

        if (endPoint instanceof java.lang.String) {
            try {
                endPoint = new URL((String) endPoint);
            } catch (MalformedURLException mex) {
                log.severe("SAAJ0006.p2p.bad.URL");
                throw new SOAPExceptionImpl("Bad URL: " + mex.getMessage());
            }
        }

        if (endPoint instanceof URL)
            try {
                SOAPMessage response = doGet((URL)endPoint);
                return response;
            } catch (Exception ex) {
                throw new SOAPExceptionImpl(ex);
            } else
            throw new SOAPExceptionImpl("Bad endPoint type " + endPoint);
    }

    SOAPMessage doGet(URL endPoint) throws SOAPException, IOException {
        boolean isFailure = false;

        URL url = null;
        HttpURLConnection httpConnection = null;

        int responseCode = 0;
        try {
            // Process the URL
            URI uri = new URI(endPoint.toString());
            String userInfo = uri.getRawUserInfo();

            url = endPoint;

            if (dL > 0)
                d("uri: " + userInfo + " " + url + " " + uri);

            // TBD
            //    Will deal with https later.
            if (!url.getProtocol().equalsIgnoreCase("http")
                && !url.getProtocol().equalsIgnoreCase("https")) {
                log.severe("SAAJ0052.p2p.protocol.mustbe.http.or.https");
                throw new IllegalArgumentException(
                    "Protocol "
                        + url.getProtocol()
                        + " not supported in URL "
                        + url);
            }
            httpConnection = createConnection(url);

            httpConnection.setRequestMethod("GET");

            httpConnection.setDoOutput(true);
            httpConnection.setDoInput(true);
            httpConnection.setUseCaches(false);
            httpConnection.setInstanceFollowRedirects(true);
            httpConnection.setConnectTimeout(CONNECT_TIMEOUT);
            httpConnection.setReadTimeout(READ_TIMEOUT);

            httpConnection.connect();

            try {

                responseCode = httpConnection.getResponseCode();

                // let HTTP_INTERNAL_ERROR (500) and HTTP_BAD_REQUEST (400) through because it is used for SOAP faults
                if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR || responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                    isFailure = true;
                } else if ((responseCode / 100) != 2) {
                    log.log(Level.SEVERE,
                            "SAAJ0008.p2p.bad.response",
                            new String[] { httpConnection.getResponseMessage()});
                    throw new SOAPExceptionImpl(
                        "Bad response: ("
                            + responseCode
                            + httpConnection.getResponseMessage());

                }
            } catch (IOException e) {
                // on JDK1.3.1_01, we end up here, but then getResponseCode() succeeds!
                responseCode = httpConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR || responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                    isFailure = true;
                } else {
                    throw e;
                }

            }

        } catch (SOAPException ex) {
            throw ex;
        } catch (Exception ex) {
            log.severe("SAAJ0012.p2p.get.failed");
            throw new SOAPExceptionImpl("Get failed", ex);
        }

        SOAPMessage response = null;
        InputStream httpIn = null;
        if (responseCode == HttpURLConnection.HTTP_OK || isFailure) {
            try {
                MimeHeaders headers = new MimeHeaders();

                String key, value;

                // Header field 0 is the status line so we skip it.

                int i = 1;

                while (true) {
                    key = httpConnection.getHeaderFieldKey(i);
                    value = httpConnection.getHeaderField(i);

                    if (key == null && value == null)
                        break;

                    if (key != null) {
                        StringTokenizer values =
                            new StringTokenizer(value, ",");
                        while (values.hasMoreTokens())
                            headers.addHeader(key, values.nextToken().trim());
                    }
                    i++;
                }

                httpIn =
                        (isFailure
                        ? httpConnection.getErrorStream()
                        : httpConnection.getInputStream());
                // If no reply message is returned,
                // content-Length header field value is expected to be zero.
                // java SE 6 documentation says :
                // available() : an estimate of the number of bytes that can be read
                //(or skipped over) from this input stream without blocking
                //or 0 when it reaches the end of the input stream.
                if ((httpIn == null )
                        || (httpConnection.getContentLength() == 0)
                        || (httpIn.available() == 0)) {
                    response = null;
                    log.warning("SAAJ0014.p2p.content.zero");
                } else {
                    response = messageFactory.createMessage(headers, httpIn);
                }

            } catch (SOAPException ex) {
                throw ex;
            } catch (Exception ex) {
                log.log(Level.SEVERE,
                        "SAAJ0010.p2p.cannot.read.resp",
                        ex);
                throw new SOAPExceptionImpl(
                    "Unable to read response: " + ex.getMessage());
            } finally {
               if (httpIn != null)
                   httpIn.close();
               httpConnection.disconnect();
            }
        }
        return response;
    }

    private byte[] readFully(InputStream istream) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int num = 0;

        while ((num = istream.read(buf)) != -1) {
            bout.write(buf, 0, num);
        }

        byte[] ret = bout.toByteArray();

        return ret;
    }

    private void initAuthUserInfo(HttpURLConnection conn, String userInfo) {
        String user;
        String password;
        if (userInfo != null) { // get the user and password
            //System.out.println("UserInfo= " + userInfo );
            int delimiter = userInfo.indexOf(':');
            if (delimiter == -1) {
                user = ParseUtil.decode(userInfo);
                password = null;
            } else {
                user = ParseUtil.decode(userInfo.substring(0, delimiter++));
                password = ParseUtil.decode(userInfo.substring(delimiter));
            }

            String plain = user + ":";
            byte[] nameBytes = plain.getBytes();
            byte[] passwdBytes = (password == null ? new byte[0] : password
                    .getBytes());

            // concatenate user name and password bytes and encode them
            byte[] concat = new byte[nameBytes.length + passwdBytes.length];

            System.arraycopy(nameBytes, 0, concat, 0, nameBytes.length);
            System.arraycopy(
                passwdBytes,
                0,
                concat,
                nameBytes.length,
                passwdBytes.length);
            String auth = "Basic " + new String(Base64.encode(concat));
            conn.setRequestProperty("Authorization", auth);
            if (dL > 0)
                d("Adding auth " + auth);
        }
    }

    private static final int dL = 0;
    private void d(String s) {
        log.log(Level.SEVERE,
                "SAAJ0013.p2p.HttpSOAPConnection",
                new String[] { s });
        System.err.println("HttpSOAPConnection: " + s);
    }

    private java.net.HttpURLConnection createConnection(URL endpoint)
        throws IOException {
        return (HttpURLConnection) endpoint.openConnection();
    }

}
