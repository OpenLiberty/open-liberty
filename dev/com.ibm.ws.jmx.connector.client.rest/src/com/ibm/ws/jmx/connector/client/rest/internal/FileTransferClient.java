/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.client.rest.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.ws.jmx.connector.client.rest.ClientProvider;
import com.ibm.ws.jmx.connector.client.rest.internal.ClientConstants.HttpMethod;
import com.ibm.ws.jmx.connector.client.rest.internal.resources.FileTransferClientMessagesUtil;
import com.ibm.ws.jmx.connector.converter.JSONConverter;

/**
 * This class provides the client-side file transfer support. It uses artifacts from the jmx rest connector client, such as the root URL and
 * security credentials. New URLs are appended to the file transfer REST calls, so that it maps to a different jax-rs resource on the server-side.
 */
class FileTransferClient {

    private static final Logger logger = Logger.getLogger(FileTransferClient.class.getName());

    private static final String OPERATION_DOWNLOAD = "downloadFile";
    private static final String OPERATION_UPLOAD = "uploadFile";
    private static final String OPERATION_DELETE = "deleteFile";
    private static final String OPERATION_DELETE_ALL = "deleteAll";
    private static final String QUERY_PARAM_UPLOAD_EXPAND = "expandOnCompletion";
    private static final String QUERY_PARAM_START_OFFSET = "startOffset";
    private static final String QUERY_PARAM_END_OFFSET = "endOffset";

    private static final String FILE_TRANSFER_URL_V2 = "/fileTransfer";
    private static final String FILE_TRANSFER_ROUTER_URL_V2 = FILE_TRANSFER_URL_V2 + "/router";

    private static final String COLLECTION_URL = "/collection";
    private static final String COLLECTION_OPERATION_DELETE = "delete";

    private static final String FILE_TRANSFER_CONTENT_TYPE = "application/gzip";
    private static final String FILE_TRANSFER_ZIP_MIME = "application/zip";
    private static final String FILE_TRANSFER_PAX_MIME = "application/pax";
    private static final String FILE_TRANSFER_JSON_MIME = "application/json";
    private static final int FILE_TRANSFER_DEFAULT_BUFFER_SIZE = 8 * 1024;
    private static final String RANDOM_FILE_ACCESS_READ_MODE = "r";
    private static final String RANDOM_FILE_ACCESS_READ_WRITE_MODE = "rw";

    private final RESTMBeanServerConnection restConnection;

    public FileTransferClient(RESTMBeanServerConnection restConnection) throws IOException {
        this.restConnection = restConnection;

        if (logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, logger.getName(), "constructor",
                        FileTransferClientMessagesUtil.getMessage(FileTransferClientMessagesUtil.CLIENT_INIT, restConnection.connector.getConnectionId()));
        }
    }

    /**
     * Handle MBean invocation requests
     */
    public Object handleOperation(String operation, Object[] params) throws IOException {
        if (OPERATION_DOWNLOAD.equals(operation)) {
            if (params.length == 2) {
                downloadFile((String) params[0], (String) params[1]);
            } else {
                //partial download
                return downloadFile((String) params[0], (String) params[1], (Long) params[2], (Long) params[3]);
            }
        } else if (OPERATION_UPLOAD.equals(operation)) {
            uploadFile((String) params[0], (String) params[1], (Boolean) params[2]);
        } else if (OPERATION_DELETE.equals(operation)) {
            deleteFile((String) params[0]);
        } else if (OPERATION_DELETE_ALL.equals(operation)) {
            deleteAll((List<String>) params[0]);
        } else {
            throw logUnsupportedOperationError("handleOperation", operation);
        }

        //currently all operations are void, so return null.
        return null;
    }

    HttpsURLConnection getFileTransferConnection(URL url, HttpMethod method) throws IOException {
        return getFileTransferConnection(url, method, true);
    }

    HttpsURLConnection getFileTransferConnection(URL url, HttpMethod method, boolean setEncoding) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setDoInput(true);
        if (method == HttpMethod.POST) {
            if (restConnection.serverVersion == 2) {
                //In V2 we were setting the Content-Type to application/gzip.  In V3+ we corrected that
                //by not specifying a Content-Type and instead specifying that the Content-Encoding is gzip.
                connection.setRequestProperty("Content-Type", FILE_TRANSFER_CONTENT_TYPE);
            }
            connection.setDoOutput(true);
        } else {
            connection.setDoOutput(false);
        }
        connection.setUseCaches(false);
        connection.setRequestMethod(method.toString());
        connection.setReadTimeout(restConnection.connector.getReadTimeout());

        // fix 'Invalid CRLF found in header name' error
        if (restConnection.connector.getBasicAuthHeader() != null) {
            connection.setRequestProperty("Authorization", restConnection.connector.getBasicAuthHeader());
        }
        if (restConnection.connector.isHostnameVerificationDisabled()) {
            connection.setHostnameVerifier(restConnection.hostnameVerificationDisabler);
        }

        connection.setRequestProperty("User-Agent", RESTMBeanServerConnection.CLIENT_VERSION);

        //Set routing headers if applicable
        if (restConnection.isHostLevelRouting()) {
            connection.addRequestProperty(ClientProvider.ROUTING_KEY_HOST_NAME, (String) restConnection.mapRouting.get(ClientProvider.ROUTING_KEY_HOST_NAME));
        } else if (restConnection.isServerLevelRouting()) {
            connection.addRequestProperty(ClientProvider.ROUTING_KEY_HOST_NAME, (String) restConnection.mapRouting.get(ClientProvider.ROUTING_KEY_HOST_NAME));
            connection.addRequestProperty(ClientProvider.ROUTING_KEY_SERVER_USER_DIR, (String) restConnection.mapRouting.get(ClientProvider.ROUTING_KEY_SERVER_USER_DIR));
            connection.addRequestProperty(ClientProvider.ROUTING_KEY_SERVER_NAME, (String) restConnection.mapRouting.get(ClientProvider.ROUTING_KEY_SERVER_NAME));
        }

        if (restConnection.getConnector().getCustomSSLSocketFactory() != null) {
            connection.setSSLSocketFactory(restConnection.getConnector().getCustomSSLSocketFactory());
        }

        if (restConnection.serverVersion > 2 && setEncoding) {
            if (method == HttpMethod.POST) {
                connection.addRequestProperty("Content-Encoding", "gzip");
            } else {
                connection.addRequestProperty("Accept-Encoding", "gzip");
            }
        }

        return connection;
    }

    private String getEncodedFilePath(String path) throws IOException {
        return "/" + URLEncoder.encode(path, "UTF-8");
    }

    private String getFileTransferURL() throws IOException {
        if (restConnection.serverVersion == 2) {
            return restConnection.getRootURL() + FILE_TRANSFER_URL_V2;
        } else {
            return restConnection.getFileTransferURL().toString();
        }
    }

    private String getFileTransferRoutingURL() throws IOException {
        if (restConnection.serverVersion == 2) {
            return restConnection.getRootURL() + FILE_TRANSFER_ROUTER_URL_V2;
        } else {
            return restConnection.getFileTransferURL().toString();
        }
    }

    private URL getURL(Map<String, String> queryParameters, String additionalURL) throws IOException {

        String coreURLStr;

        if (restConnection.isServerLevelRouting() || restConnection.isHostLevelRouting()) {
            coreURLStr = getFileTransferRoutingURL() + additionalURL;
        } else {
            coreURLStr = getFileTransferURL() + additionalURL;
        }

        if (logger.isLoggable(Level.FINEST)) {
            logger.logp(Level.FINEST, logger.getName(), "getURL", "coreURLStr: " + coreURLStr);
        }

        try {
            if (queryParameters != null && !queryParameters.isEmpty()) {
                //Iterate through the query parameters and build a query string
                StringBuilder sb = new StringBuilder();
                Set<Entry<String, String>> params = queryParameters.entrySet();

                for (Entry<String, String> param : params) {
                    //Add preceding characters
                    if (sb.length() == 0) {
                        //first entry
                        sb.append('?');
                    } else {
                        //not the first entry
                        sb.append('&');
                    }

                    //Add parameter
                    sb.append(param.getKey());
                    sb.append('=');
                    sb.append(URLEncoder.encode(param.getValue(), "UTF-8"));
                }

                if (logger.isLoggable(Level.FINEST)) {
                    logger.logp(Level.FINEST, logger.getName(), "getURL", "queryURL: " + sb.toString());
                }

                return new URL(coreURLStr + sb.toString());
            }

            return new URL(coreURLStr);

        } catch (Exception e) {
            throw logClientException("getURL", e);
        }

    }

    public void downloadFile(String remoteSourceFile, String localTargetFile) throws IOException {
        downloadFile(remoteSourceFile, localTargetFile, 0, -1); //regular download (non-partial download)
    }

    public long downloadFile(String remoteSourceFile, String localTargetFile, long startOffset, long endOffset) throws IOException {
        final String methodName = "downloadFile(String remoteSourceFile, String localTargetFile, long startOffset, long endOffset)";

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), methodName, "Entering downloadFile with remoteSourceFile: " + remoteSourceFile + " ; localTargetFile: " + localTargetFile +
                                                                   " ; startOffset: " + startOffset + " and endOffset: " + endOffset);
        }

        Map<String, String> queryParams = new HashMap<String, String>();

        if (startOffset > 0) {
            queryParams.put(QUERY_PARAM_START_OFFSET, String.valueOf(startOffset));
        }

        if (endOffset > -1) {
            queryParams.put(QUERY_PARAM_END_OFFSET, String.valueOf(endOffset));
        }

        URL url = getURL(queryParams, getEncodedFilePath(remoteSourceFile));
        HttpsURLConnection connection = getFileTransferConnection(url, HttpMethod.GET);

        // Check response code from server
        final int responseCode = connection.getResponseCode();
        InputStream is = null;
        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:

                //Get the resulting stream
                is = connection.getInputStream();

                if (logger.isLoggable(Level.FINEST)) {
                    logger.logp(Level.FINEST, logger.getName(), methodName, "Received stream: " + FileTransferClientMessagesUtil.getObjID(is));
                }
                //Data is always compressed with this java client, so use GZIP stream reader
                is = new GZIPInputStream(is, FILE_TRANSFER_DEFAULT_BUFFER_SIZE);
                break;

            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                throw logServerException(methodName, connection);
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw logCredentialsException(methodName, responseCode, connection);
            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_NOT_FOUND:
                IOException ioe = logResponseCodeException(methodName, responseCode, connection);
                restConnection.recoverConnection(ioe);
                throw ioe;
            default:
                throw logResponseCodeException(methodName, responseCode, connection);
        }

        //Create our target output stream
        RandomAccessFile destFile = new RandomAccessFile(localTargetFile, RANDOM_FILE_ACCESS_READ_WRITE_MODE);
        long totalBytesRead = 0;

        try {
            //Transfer bytes to output file
            byte[] buf = new byte[FILE_TRANSFER_DEFAULT_BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = is.read(buf)) > 0) {
                destFile.write(buf, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
            destFile.setLength(totalBytesRead);
        } finally {
            tryToClose(is);
            tryToClose(destFile);
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, logger.getName(), methodName,
                        FileTransferClientMessagesUtil.getMessage(FileTransferClientMessagesUtil.DOWNLOAD_TO_FILE, remoteSourceFile, localTargetFile,
                                                                  restConnection.connector.getConnectionId()));
        }

        if (startOffset < 0) {
            startOffset = 0;
        }

        long nextStartOffset = startOffset + totalBytesRead;

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), methodName, "Exiting downloadFile with nextStartOffset=" + nextStartOffset);
        }

        return nextStartOffset;
    }

    public void uploadFile(String localSourceFile, String remoteTargetFile, boolean expandOnCompletion) throws IOException {
        final String methodName = "uploadFile(String localSourceFile, String remoteTargetFile, boolean expandOnCompletion)";

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), methodName, "Entering uploadFile with localSourceFile: " + localSourceFile + " and remoteTargetFile: " + remoteTargetFile);
        }

        Map<String, String> queryParams = new HashMap<String, String>();
        queryParams.put(QUERY_PARAM_UPLOAD_EXPAND, String.valueOf(expandOnCompletion));
        URL url = getURL(queryParams, getEncodedFilePath(remoteTargetFile));
        HttpsURLConnection connection = getFileTransferConnection(url, HttpMethod.POST);

        if (expandOnCompletion && restConnection.serverVersion >= 5) {
            //Starting with server V5 users aren't required to put an extension on the target file,
            //so for archives that will be expanded we place a proper Content-Type
            if (localSourceFile.endsWith(".pax")) {
                connection.setRequestProperty("Content-Type", FILE_TRANSFER_PAX_MIME);
            } else {
                //default is zip, since it's handled the same was WAR/EAR/JAR
                connection.setRequestProperty("Content-Type", FILE_TRANSFER_ZIP_MIME);
            }
        }

        //Grab file to be uploaded
        RandomAccessFile uploadFile = new RandomAccessFile(localSourceFile, RANDOM_FILE_ACCESS_READ_MODE);

        //Grab the outgoing stream
        OutputStream outStream = null;
        try {
            outStream = connection.getOutputStream();
        } catch (ConnectException ce) {
            restConnection.recoverConnection(ce);
            tryToClose(outStream);
            tryToClose(uploadFile);
            throw ce;
        }

        //We're always compressing the transfers from this java client, so use GZIP
        outStream = new GZIPOutputStream(outStream, FILE_TRANSFER_DEFAULT_BUFFER_SIZE);

        try {
            //Transfer bytes to output file
            byte[] buf = new byte[FILE_TRANSFER_DEFAULT_BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = uploadFile.read(buf)) > 0) {
                outStream.write(buf, 0, bytesRead);
            }
            outStream.flush();

        } finally {
            tryToClose(outStream);
            tryToClose(uploadFile);
        }

        // Check response code from server
        final int responseCode = connection.getResponseCode();
        switch (responseCode) {
            case HttpURLConnection.HTTP_NO_CONTENT:

                if (logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, logger.getName(), methodName,
                                FileTransferClientMessagesUtil.getMessage(FileTransferClientMessagesUtil.UPLOAD_FROM_FILE, localSourceFile, remoteTargetFile,
                                                                          restConnection.connector.getConnectionId()));
                }

                if (logger.isLoggable(Level.FINER)) {
                    logger.logp(Level.FINER, logger.getName(), methodName, "Exiting uploadFile");
                }

                return;

            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                throw logServerException(methodName, connection);

            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw logCredentialsException(methodName, responseCode, connection);

            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_NOT_FOUND:
                IOException ioe = logResponseCodeException(methodName, responseCode, connection);
                restConnection.recoverConnection(ioe);
                throw ioe;

            default:
                throw logResponseCodeException(methodName, responseCode, connection);
        }

    }

    public void deleteFile(String remoteSourceFile) throws IOException {
        final String methodName = "deleteFile(String remoteSourceFile)";

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), methodName, "Entering deleteFile with remoteSourceFile: " + remoteSourceFile);
        }

        URL url = getURL(null, getEncodedFilePath(remoteSourceFile));
        HttpsURLConnection connection = getFileTransferConnection(url, HttpMethod.DELETE);

        // Check response code from server
        final int responseCode = connection.getResponseCode();
        switch (responseCode) {
            case HttpURLConnection.HTTP_NO_CONTENT:

                if (logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, logger.getName(), methodName,
                                FileTransferClientMessagesUtil.getMessage(FileTransferClientMessagesUtil.DELETE_FILE, remoteSourceFile,
                                                                          restConnection.connector.getConnectionId()));
                }

                if (logger.isLoggable(Level.FINER)) {
                    logger.logp(Level.FINER, logger.getName(), methodName, "Exiting deleteFile");
                }

                return;

            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                throw logServerException(methodName, connection);

            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw logCredentialsException(methodName, responseCode, connection);

            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_NOT_FOUND:
                IOException ioe = logResponseCodeException(methodName, responseCode, connection);
                restConnection.recoverConnection(ioe);
                throw ioe;

            default:
                throw logResponseCodeException(methodName, responseCode, connection);
        }
    }

    public void deleteAll(List<String> remoteArtifacts) throws IOException {
        final String methodName = "deleteAll(List<String> remoteArtifacts)";

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), methodName, "Entering deleteAll with remoteArtifacts: " + remoteArtifacts);
        }

        URL url = getURL(null, COLLECTION_URL);
        HttpsURLConnection connection = getFileTransferConnection(url, HttpMethod.POST, false);
        connection.setRequestProperty("Content-Type", FILE_TRANSFER_JSON_MIME);

        //Grab the outgoing stream
        OutputStream outStream = null;
        try {
            outStream = connection.getOutputStream();
        } catch (ConnectException ce) {
            restConnection.recoverConnection(ce);
            tryToClose(outStream);
            throw ce;
        }

        try {
            // Generate JSON with following structure: { "delete": ["C:/temp/output.log", "C:/wlp", "C:/workarea"] }
            JSONArray artifactsArray = new JSONArray();
            for (String artifact : remoteArtifacts) {
                artifactsArray.add(artifact);
            }

            JSONObject contentObject = new JSONObject();
            contentObject.put(COLLECTION_OPERATION_DELETE, artifactsArray);

            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, logger.getName(), methodName, "Generated JSON: " + contentObject.toString());
            }
            outStream.write(contentObject.toString().getBytes("UTF-8"));
            outStream.flush();
        } finally {
            tryToClose(outStream);
        }

        // Check response code from server
        final int responseCode = connection.getResponseCode();

        switch (responseCode) {
            case HttpURLConnection.HTTP_NO_CONTENT:

                if (logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, logger.getName(), methodName,
                                FileTransferClientMessagesUtil.getMessage(FileTransferClientMessagesUtil.DELETE_ALL, remoteArtifacts,
                                                                          restConnection.connector.getConnectionId()));
                }

                if (logger.isLoggable(Level.FINER)) {
                    logger.logp(Level.FINER, logger.getName(), methodName, "Exiting deleteAll");
                }

                return;

            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                throw logServerException(methodName, connection);

            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw logCredentialsException(methodName, responseCode, connection);

            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_NOT_FOUND:
                IOException ioe = logResponseCodeException(methodName, responseCode, connection);
                restConnection.recoverConnection(ioe);
                throw ioe;

            default:
                throw logResponseCodeException(methodName, responseCode, connection);
        }
    }

    public static boolean tryToClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
                return true;
            } catch (IOException e) {
                // ignore
            }
        }
        return false;
    }

    private IOException logClientException(String methodName, Exception e) {
        String errorMsg = FileTransferClientMessagesUtil.getMessage(FileTransferClientMessagesUtil.CLIENT_ERROR, e.getLocalizedMessage(),
                                                                    restConnection.connector.getConnectionId());
        logger.logp(Level.SEVERE, logger.getName(), methodName, errorMsg, e);
        return new IOException(errorMsg, e);
    }

    private IOException logServerException(String methodName, HttpURLConnection connection) {
        Throwable t = null;
        JSONConverter converter = JSONConverter.getConverter();
        try {
            t = converter.readThrowable(connection.getErrorStream());
            logger.logp(Level.SEVERE, logger.getName(), methodName, t.getMessage());

        } catch (Exception e) {
            //Fallback into standard server message
            String responseMessage = "error";
            try {
                responseMessage = connection.getResponseMessage();
            } catch (IOException io) {
                // Use "error" for message..but shouldn't really happen
            }
            String errorMsg = FileTransferClientMessagesUtil.getMessage(FileTransferClientMessagesUtil.SERVER_ERROR, responseMessage,
                                                                        restConnection.connector.getConnectionId());
            t = new IOException(errorMsg);
            logger.logp(Level.SEVERE, logger.getName(), methodName, errorMsg);
        } finally {
            JSONConverter.returnConverter(converter);
        }

        return (t instanceof IOException) ? (IOException) t : new IOException(t);
    }

    private IOException logCredentialsException(String methodName, int responseCode, HttpURLConnection connection) {
        String responseMessage = null;
        try {
            responseMessage = connection.getResponseMessage();
        } catch (IOException io) {
            // Use null for message
        }
        String errorMsg = FileTransferClientMessagesUtil.getMessage(FileTransferClientMessagesUtil.BAD_CREDENTIALS, responseCode, responseMessage,
                                                                    restConnection.connector.getConnectionId());
        logger.logp(Level.SEVERE, logger.getName(), methodName, errorMsg);
        return new IOException(errorMsg);
    }

    private IOException logResponseCodeException(String methodName, int responseCode, HttpURLConnection connection) {
        String responseMessage = null;
        try {
            responseMessage = connection.getResponseMessage();
        } catch (IOException io) {
            // Use null for message
        }
        String errorMsg = FileTransferClientMessagesUtil.getMessage(FileTransferClientMessagesUtil.RESPONSE_CODE_ERROR, responseCode, responseMessage,
                                                                    restConnection.connector.getConnectionId());
        logger.logp(Level.SEVERE, logger.getName(), methodName, errorMsg);
        return new IOException(errorMsg);
    }

    private IOException logUnsupportedOperationError(String methodName, String operation) {
        String errorMsg = FileTransferClientMessagesUtil.getMessage(FileTransferClientMessagesUtil.UNSUPPORTED_OPERATION, operation, restConnection.connector.getConnectionId());
        logger.logp(Level.SEVERE, logger.getName(), methodName, errorMsg);
        return new IOException(errorMsg);
    }

}
