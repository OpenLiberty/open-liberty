/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.transport.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

import com.ibm.ws.repository.common.enums.AttachmentLinkType;
import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.StateAction;
import com.ibm.ws.repository.transport.exceptions.BadVersionException;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.Attachment;
import com.ibm.ws.repository.transport.model.AttachmentSummary;
import com.ibm.ws.repository.transport.model.StateUpdateAction;

/**
 * This class contains methods for making REST calls into the Massive server
 */
public class RestClient extends AbstractRepositoryClient implements RepositoryReadableClient, RepositoryWriteableClient {

    private final ClientLoginInfo loginInfo;

    /**
     * Newline string for ending lines when building an HTTP request
     */
    private static final String NEWLINE = "\r\n";

    /**
     * Bar (|) character encoded for URL
     */
    private static final String ENCODED_BAR = "%7C";

    private static final int REPOSITORY_SOCKET_READ_TIMEOUT = 300 * 1000;

    /**
     * Create a new instance of the client using the supplied userId and
     * password
     *
     * @param userId
     *            The user id to use in Massive
     * @param password
     *            The password to use in Massive
     */
    public RestClient(ClientLoginInfo loginInfo) {
        super();
        this.loginInfo = loginInfo;
    }

    /**
     * This method will issue a GET to all of the assets in massive
     *
     * @return A list of all of the assets in Massive
     * @throws IOException
     * @throws RequestFailureException
     */
    @Override
    public List<Asset> getAllAssets() throws IOException, RequestFailureException {
        HttpURLConnection connection = createHttpURLConnectionToMassive("/assets");
        connection.setRequestMethod("GET");
        testResponseCode(connection);
        return JSONAssetConverter.readValues(connection.getInputStream());
    }

    /**
     * Checks the repository availability
     *
     * @return This will return void if all is ok but will throw an exception if
     *         there are any problems
     * @throws RequestFailureException If the response code is not OK or the headers returned are missing
     *             the count field (which can happen if we hit a URL that returns 200 but is not a valid repository).
     * @throws IOException If there is a problem with the URL
     */
    @Override
    public void checkRepositoryStatus() throws IOException, RequestFailureException {
        HttpURLConnection connection = createHeadConnection("/assets");
        testResponseCode(connection);
        Map<String, List<String>> results = connection.getHeaderFields();
        if (results == null) {
            throw new RequestFailureException(connection.getResponseCode(), "No header returned, this does not look like a valid repository", connection.getURL(), null);
        }
        List<String> count = results.get("count");
        if (count == null) {
            throw new RequestFailureException(connection.getResponseCode(), "No count returned, this does not look like a valid repository", connection.getURL(), null);
        }
    }

    /**
     * This method will issue a HEAD to all of the assets in massive and returns the {@link HttpURLConnection#getHeaderFields()}.
     *
     * @return The {@link HttpURLConnection#getHeaderFields()} for the HEAD request
     * @throws IOException
     * @throws RequestFailureException if the response code is not OK
     */
    public Map<String, List<String>> getAllAssetsMetadata() throws IOException, RequestFailureException {
        HttpURLConnection connection = createHeadConnection("/assets");
        testResponseCode(connection);
        return connection.getHeaderFields();
    }

    /**
     * Creates a head request connection to the specified path
     *
     * @param path The relative URL path
     * @return An HttpURLConnection to the specified path
     * @throws IOException If there was a problem creating the connection or URL
     */
    private HttpURLConnection createHeadConnection(String path) throws IOException {
        HttpURLConnection connection = createHttpURLConnectionToMassive(path);
        connection.setRequestMethod("HEAD");
        return connection;
    }

    /**
     * Adds an asset into Massive. Note that Massive will set some fields (such
     * as ID) so it is important to switch to the returned object after calling
     * this method.
     *
     * @param asset
     *            The asset to add, it will not be modified by this method
     * @return The asset with information added by Massive
     * @throws IOException
     */
    @Override
    public Asset addAsset(final Asset asset) throws IOException, BadVersionException, RequestFailureException {

        HttpURLConnection connection = createHttpURLConnectionToMassive("/assets");
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        JSONAssetConverter.writeValue(connection.getOutputStream(), asset);
        testResponseCode(connection);
        Asset returnedAsset = JSONAssetConverter.readValue(connection.getInputStream());
        returnedAsset = getAsset(returnedAsset.get_id());
        return returnedAsset;
    }

    /**
     * Updates an asset in Massive. The {@link Asset#get_id()} must return the
     * correct ID for this asset. Note that Massive will set some fields (such
     * as last update date) so it is important to switch to the returned object
     * after calling this method.
     *
     * @param asset
     *            The asset to add, it will not be modified by this method
     * @return The asset with information added by Massive
     * @throws IOException
     * @throws RequestFailureException
     */
    @Override
    public Asset updateAsset(final Asset asset) throws IOException, BadVersionException, RequestFailureException {
        HttpURLConnection connection = createHttpURLConnectionToMassive("/assets/"
                                                                        + asset.get_id());
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        JSONAssetConverter.writeValue(connection.getOutputStream(), asset);

        /*
         * Force the PUT to take place by getting the response code and making sure it is ok
         */
        testResponseCode(connection, true);

        /*
         * PUTs don't return an (they just return "1" - not sure what that
         * means) so go and grab it so we have an updated one with the right
         * last update date in case they use it for optimistic locking
         */
        return getAsset(asset.get_id());
    }

    /**
     * Deletes an asset with the given ID. Note, this will not delete any
     * attachments associated with the asset.
     *
     * @see #deleteAssetAndAttachments(String)
     * @param id
     *            The ID to delete
     * @return <code>true</code> if the delete is successful
     * @throws IOException
     * @throws RequestFailureException
     */
    private void deleteAsset(String id) throws IOException, RequestFailureException {
        HttpURLConnection connection = createHttpURLConnectionToMassive("/assets/"
                                                                        + id);
        connection.setRequestMethod("DELETE");
        testResponseCode(connection, true);
    }

    /**
     * Find assets based on the <code>searchString</code>.
     *
     * NOTE: TODO at the moment this only works when called against an unauthenticated Client
     * due to a problem with how the stores are defined (the company values are defined
     * incorrectly).
     *
     * @param searchString The string to search for
     * @param types The types to filter the results for
     * @return The assets that match the search string and type
     * @throws IOException
     * @throws RequestFailureException
     */
    @Override
    public List<Asset> findAssets(String searchString, Collection<ResourceType> types) throws IOException, RequestFailureException {

        String encodedSearchString = URLEncoder.encode(searchString, "UTF-8");

        StringBuffer url = new StringBuffer("/assets?q=" + encodedSearchString);
        if (types != null && !types.isEmpty()) {
            Collection<String> typeValues = new HashSet<String>();
            for (ResourceType type : types) {
                typeValues.add(type.getValue());
            }
            url.append("&" + createListFilter(FilterableAttribute.TYPE, typeValues));
        }

        // Call massive to run the query
        HttpURLConnection connection = createHttpURLConnectionToMassive(url.toString());
        connection.setRequestMethod("GET");
        testResponseCode(connection);
        InputStream is = connection.getInputStream();

        // take the returned input stream and convert it to assets
        List<Asset> assets = JSONAssetConverter.readValues(is);
        return assets;
    }

    private byte[] getStartBytes(final AttachmentSummary attSummary, String boundary) throws IOException {
        final String name = attSummary.getName();
        final File fileToWrite = attSummary.getFile();
        final Attachment attach = attSummary.getAttachment();
        ByteArrayOutputStream startOutputStream = new ByteArrayOutputStream();

        try {
            OutputStreamWriter writer = new OutputStreamWriter(startOutputStream, Charset.forName("UTF-8"));
            writer.write("--" + boundary + NEWLINE);
            writer.write("Content-Disposition: form-data; name=\"attachmentInfo\"" + NEWLINE);
            writer.write("Content-Type: application/json" + NEWLINE);
            writer.write(NEWLINE);
            writer.flush();
            JSONAssetConverter.writeValue(startOutputStream, attach);
            writer.write(NEWLINE);

            writer.write("--" + boundary + NEWLINE);
            writer.write("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileToWrite.getName() + "\"" + NEWLINE);
            String uploadType = "application/binary"; // default value
            if (attach.getType() == null) {
                // Attachments should have a Type specified
                throw new IllegalArgumentException("Attachments must have a Type specified");
            } else {
                switch (attach.getType()) {
                    case LICENSE:
                        uploadType = "text/html";
                        break;
                    case LICENSE_AGREEMENT:
                    case LICENSE_INFORMATION:
                        uploadType = "text/plain";
                        break;
                    default:
                        break;
                }
            }
            writer.write("Content-Type: " + uploadType + NEWLINE);
            writer.write(NEWLINE);
            writer.close();
        } finally {
            if (startOutputStream != null) {
                startOutputStream.close();
            }
        }
        return startOutputStream.toByteArray();
    }

    private byte[] getEndBytes(final AttachmentSummary attSummary, String boundary) throws IOException {
        ByteArrayOutputStream endOutputStream = new ByteArrayOutputStream();
        // Data to stream after file is uploaded
        try {
            OutputStreamWriter writer = new OutputStreamWriter(endOutputStream, Charset.forName("UTF-8"));
            writer.write(NEWLINE);
            writer.write("--" + boundary + "--" + NEWLINE);
            writer.close();
        } finally {
            if (endOutputStream != null) {
                endOutputStream.close();
            }
        }
        return endOutputStream.toByteArray();
    }

    @Override
    public Attachment addAttachment(final String assetId, final AttachmentSummary attSummary) throws IOException, BadVersionException, RequestFailureException {
        final Attachment attach = attSummary.getAttachment();
        final String name = attSummary.getName();
        // Info about the attachment goes into the URL
        String urlString = "/assets/" + assetId + "/attachments?name=" + name;
        if (attach.getType() != null) {
            urlString = urlString + "&type=" + attach.getType().toString();
        }

        HttpURLConnection connection = createHttpURLConnectionToMassive(urlString);
        if (attSummary.getURL() == null) {
            writeMultiPart(assetId, attSummary, connection);
        } else {
            writeSinglePart(assetId, attSummary, connection);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        testResponseCode(connection);
        InputStream is = connection.getInputStream();
        int len = 0;
        while ((len = is.read()) != -1) {
            baos.write((byte) len);
        }
        is.close();
        baos.close();

        Attachment attachment = JSONAssetConverter.readValue(
                                                             new ByteArrayInputStream(baos.toByteArray()), Attachment.class);

        return attachment;
    }

    /**
     * Adds a new attachment to an asset
     *
     * @param assetId
     * @param name
     * @param attach
     * @return
     * @throws IOException
     * @throws RequestFailureException
     */
    private void writeMultiPart(final String assetId, final AttachmentSummary attSummary,
                                HttpURLConnection connection) throws IOException, BadVersionException, RequestFailureException {
        final File fileToWrite = attSummary.getFile();

        String boundary = "---------------------------287032381131322";

        byte[] startBytes = getStartBytes(attSummary, boundary);
        byte[] endBytes = getEndBytes(attSummary, boundary);

        long fileSize;
        fileSize = AccessController.doPrivileged(new PrivilegedAction<Long>() {
            @Override
            public Long run() {
                return attSummary.getFile().length();
            }
        });
        long contentLength = startBytes.length + endBytes.length + fileSize;

        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        connection.setRequestProperty("Content-Length", "" + contentLength);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        OutputStream httpStream = connection.getOutputStream();

        // Write the header
        httpStream.write(startBytes);
        httpStream.flush();

        FileInputStream inputStream = null;
        try {
            try {
                inputStream = AccessController.doPrivileged(new PrivilegedExceptionAction<FileInputStream>() {
                    @Override
                    public FileInputStream run() throws IOException {
                        return new FileInputStream(fileToWrite);
                    }
                });
            } catch (PrivilegedActionException e) {
                throw (IOException) e.getCause();
            }
            byte[] buffer = new byte[1024];
            int read;
            int total = 0;
            while ((read = inputStream.read(buffer)) != -1) {
                httpStream.write(buffer, 0, read);
                total += read;
            }
            if (total != fileSize) {
                throw new IOException("File size was " + fileSize + " but we only uploaded " + total + " bytes");
            }
            httpStream.flush();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }

        httpStream.write(endBytes);
        httpStream.flush();
        httpStream.close();

    }

    private void writeSinglePart(final String assetId, final AttachmentSummary attSummary,
                                 HttpURLConnection connection) throws IOException, BadVersionException, RequestFailureException {

        final Attachment attach = attSummary.getAttachment();

        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        OutputStream httpStream = connection.getOutputStream();

        JSONAssetConverter.writeValue(httpStream, attach);
        httpStream.flush();
        httpStream.close();
    }

    /**
     * Returns the contents of an attachment
     *
     * @param assetId
     *            The ID of the asset owning the attachment
     * @param attachmentId
     *            The ID of the attachment
     * @return The input stream for the attachment
     * @throws IOException
     * @throws RequestFailureException
     */
    @Override
    public InputStream getAttachment(final Asset asset, final Attachment attachment) throws IOException, BadVersionException, RequestFailureException {

        // accept license for type CONTENT
        HttpURLConnection connection;
        if (attachment.getType() == AttachmentType.CONTENT) {
            connection = createHttpURLConnection(attachment.getUrl() + "?license=agree");
        } else {
            connection = createHttpURLConnection(attachment.getUrl());
        }

        // If the attachment was a link and we have a basic auth userid + password specified
        // we are attempting to access the files staged from a protected site so authorise for it
        if (attachment.getLinkType() == AttachmentLinkType.DIRECT) {
            if ((loginInfo.getAttachmentBasicAuthUserId() != null) && (loginInfo.getAttachmentBasicAuthPassword() != null)) {
                String userpass = loginInfo.getAttachmentBasicAuthUserId() + ":" + loginInfo.getAttachmentBasicAuthPassword();
                String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes(Charset.forName("UTF-8")));
                connection.setRequestProperty("Authorization", basicAuth);
            }
        }

        connection.setRequestMethod("GET");
        testResponseCode(connection);
        return connection.getInputStream();
    }

    /**
     * Returns the meta data about an attachment
     *
     * @param assetId
     *            The ID of the asset owning the attachment
     * @param attachmentId
     *            The ID of the attachment
     * @return The attachment meta data
     * @throws IOException
     * @throws RequestFailureException
     */
    public Attachment getAttachmentMetaData(String assetId, String attachmentId) throws IOException, BadVersionException, RequestFailureException {
        // At the moment can only get all attachments
        Asset ass = getAsset(assetId);
        List<Attachment> allAttachments = ass.getAttachments();
        for (Attachment attachment : allAttachments) {
            if (attachmentId.equals(attachment.get_id())) {
                return attachment;
            }
        }

        // Didn't find it so just return null
        return null;
    }

    /**
     * Delete an attachment from an asset
     *
     * @param assetId
     *            The ID of the asset containing the attachment
     * @param attachmentId
     *            The ID of the attachment to delete
     * @return <code>true</code> if the delete was successful
     * @throws IOException
     * @throws RequestFailureException
     */
    @Override
    public void deleteAttachment(final String assetId, final String attachmentId) throws IOException, RequestFailureException {
        HttpURLConnection connection = createHttpURLConnectionToMassive("/assets/"
                                                                        + assetId + "/attachments/" + attachmentId);
        connection.setRequestMethod("DELETE");
        testResponseCode(connection, true);
    }

    /**
     * This will delete an asset and all its attachments
     *
     * @param assetId
     *            The id of the asset
     * @return <code>true</code> if the asset and all its attachments are
     *         deleted. Note this is not atomic so some attachments may be
     *         deleted and <code>false</code> returned.
     * @throws IOException
     * @throws RequestFailureException
     */
    @Override
    public void deleteAssetAndAttachments(final String assetId) throws IOException, RequestFailureException {
        Asset ass = getUnverifiedAsset(assetId);
        List<Attachment> attachments = ass.getAttachments();
        if (attachments != null) {
            for (Attachment attachment : attachments) {
                deleteAttachment(assetId, attachment.get_id());
            }
        }
        // Now delete the asset
        deleteAsset(assetId);
    }

    /**
     * Gets a single asset
     *
     * @param assetId
     *            The ID of the asset to obtain
     * @return The Asset
     * @throws IOException
     * @throws RequestFailureException
     */
    @Override
    public Asset getAsset(final String assetId) throws IOException, BadVersionException, RequestFailureException {
        HttpURLConnection connection = createHttpURLConnectionToMassive("/assets/"
                                                                        + assetId);
        connection.setRequestMethod("GET");
        testResponseCode(connection);
        return JSONAssetConverter.readValue(connection.getInputStream());
    }

    /**
     * Get an asset without verififying it (e.g., checking it has the right version)
     *
     * @param localName
     * @param provider
     * @param type
     * @return
     * @throws IOException
     * @throws RequestFailureException
     */
    private Asset getUnverifiedAsset(String assetId) throws IOException, RequestFailureException {
        HttpURLConnection connection = createHttpURLConnectionToMassive("/assets/"
                                                                        + assetId);
        connection.setRequestMethod("GET");
        testResponseCode(connection);
        return JSONAssetConverter.readUnverifiedValue(connection.getInputStream());
    }

    /**
     * This method will update the state of an object by taking the supplied
     * action.
     *
     * @param assetId
     *            The ID of the asset to update
     * @param action
     *            The action to take to modify the state
     * @return <code>true</code> if the update was successful (currently always
     *         returns <code>true</code> from Massive)
     * @throws IOException
     * @throws RequestFailureException
     */
    @Override
    public void updateState(final String assetId, final StateAction action) throws IOException, RequestFailureException {
        StateUpdateAction newState = new StateUpdateAction(action);
        HttpURLConnection connection = createHttpURLConnectionToMassive("/assets/"
                                                                        + assetId + "/state");
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        JSONAssetConverter.writeValue(connection.getOutputStream(), newState);

        // Make sure it was ok
        testResponseCode(connection, true);
    }

    /**
     * Create an {@link HttpURLConnection} that is set up with the security
     * information to connect to massive using the versioned URL as the base URL
     *
     * @param path
     *            The path within massive to connect to (note this does not need
     *            the API key or base path of "/ma/v1")
     * @return The {@link HttpURLConnection}
     * @throws IOException
     */
    private HttpURLConnection createHttpURLConnectionToMassive(String path) throws IOException {
        return createHttpURLConnection(loginInfo.getRepositoryUrl() + path);
    }

    /**
     * <p>
     * Create an {@link HttpURLConnection} that is set up with the security
     * information to connect to massive
     * </p>
     *
     * @param url
     *            The complete URL to use (does not include the apiKey)
     * @return the {@link HttpURLConnection} with the api key and security
     *         information added
     * @throws IOException
     */
    private HttpURLConnection createHttpURLConnection(final String urlString) throws IOException {
        // Add the api key, might already have query parameters so check
        final String connectingString = urlString.contains("?") ? "&" : "?";
        return createRealHttpURLConnection(urlString + connectingString + "apiKey=" + loginInfo.getApiKey());
    }

    /**
     * Create a URL in a doPriv
     *
     * @param urlString
     * @return
     * @throws MalformedURLException
     */
    private URL createURL(final String urlString) throws MalformedURLException {

        URL url;
        try {
            url = AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {

                @Override
                public URL run() throws MalformedURLException {
                    return new URL(urlString);
                }
            });
        } catch (PrivilegedActionException e) {
            throw (MalformedURLException) e.getCause();
        }

        return url;
    }

    /**
     *
     * @param urlString This string should include the ApiKey but not any auth data.
     * @return
     * @throws IOException
     */
    private HttpURLConnection createRealHttpURLConnection(final String urlString) throws IOException {

        URL url = createURL(urlString);

        // If an HTTP proxy is defined, open the connection with a java.net.Proxy.  Authentication,
        // is handled by the system default authenticator
        HttpURLConnection connection = null;

        // Check if we are using a proxy
        if (loginInfo.getProxy() != null) {
            final LoginInfoClientProxy clientProxy = loginInfo.getProxy();

            if (clientProxy.isHTTPorHTTPS()) {

                LoginInfoClientProxy loginProxy = loginInfo.getProxy();
                Proxy javaNetProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(loginProxy.getProxyURL().getHost(), loginProxy.getProxyURL().getPort()));
                connection = (HttpURLConnection) url.openConnection(javaNetProxy);

            } else {
                // The proxy is not an HTTP or HTTPS proxy we do not support this
                UnsupportedOperationException ue = new UnsupportedOperationException("Non-HTTP proxy not supported");
                throw new IOException(ue);
            }
        } else {
            // we are not using a proxy just create a connection.
            connection = (HttpURLConnection) url.openConnection();
        }

        connection.setReadTimeout(REPOSITORY_SOCKET_READ_TIMEOUT);

        addAuthToConnection(connection);

        String userAgent = loginInfo.getUserAgent();
        if (userAgent != null && !userAgent.isEmpty()) {
            connection.setRequestProperty("User-Agent", userAgent);
        }

        return connection;
    }

    private void addAuthToConnection(HttpURLConnection connection) {
        String basicAuthUserPass = null;

        if (loginInfo.getSoftlayerUserId() != null) {
            // SoftLayer expects credentials to be supplied to basic auth Authorization header
            // so if SoftLayer credentials are supplied then that's what we put in Authorization
            // header
            basicAuthUserPass = loginInfo.getSoftlayerUserId() + ":" + loginInfo.getSoftlayerPassword();
        } else if (loginInfo.getUserId() != null && loginInfo.getPassword() != null) {
            // If SoftLayer credentials are *not* supplied then we can put userId
            // and password into the Authorization header. This is where LARS expects
            // its credentials to be.
            basicAuthUserPass = loginInfo.getUserId() + ":" + loginInfo.getPassword();
        }

        if (basicAuthUserPass != null) {
            String basicAuth = "Basic " + encode(basicAuthUserPass.getBytes(Charset.forName("UTF-8")));
            connection.setRequestProperty("Authorization", basicAuth);
        }

        // Regardless of what we have put into the Authorization header, we put the user
        // and password (if we ahve one) into Massive's custom userId and password headers.
        // These will be read by Massive but not by LARS.
        if (loginInfo.getUserId() != null) {
            connection.addRequestProperty("userId", loginInfo.getUserId());
        }
        if (loginInfo.getPassword() != null) {
            connection.addRequestProperty("password", loginInfo.getPassword());
        }
    }

    /**
     * This method will update an existing attachment on an asset. Note that
     * Massive currently doesn't support update attachment so this will do a
     * delete and an add.
     *
     * @param assetId
     *            The ID of the asset that the attachment is attached to
     * @param name
     *            The name of the attachment to update
     * @param file
     *            The file to attach
     * @param attach
     *            Attachment metadata
     * @return
     * @throws IOException
     * @throws RequestFailureException
     */
    @Override
    public Attachment updateAttachment(String assetId, AttachmentSummary summary) throws IOException, BadVersionException, RequestFailureException {
        // First find the attachment to update
        Asset ass = getAsset(assetId);
        List<Attachment> attachments = ass.getAttachments();

        if (attachments != null) {
            for (Attachment attachment : attachments) {
                if (attachment.getName().equals(summary.getName())) {
                    this.deleteAttachment(assetId, attachment.get_id());
                    break;
                }
            }
        }
        return this.addAttachment(assetId, summary);
    }

    /**
     * This will obtain assets from Massive using the supplied filters. The map can contain <code>null</code> or empty collections of values, in which case they will not be used in
     * the filter.
     *
     * @param filters A map of attributes to filter on mapped to the values to use
     * @return The filtered assets
     * @throws IOException
     * @throws RequestFailureException
     */
    @Override
    public Collection<Asset> getFilteredAssets(final Map<FilterableAttribute, Collection<String>> filters) throws IOException, RequestFailureException {
        // Were any filters defined?
        if (filters == null || allFiltersAreEmpty(filters)) {
            return getAllAssets();
        }

        // Build up a filter string
        Collection<String> filterStrings = new HashSet<String>();
        for (Map.Entry<FilterableAttribute, Collection<String>> filter : filters.entrySet()) {
            Collection<String> values = filter.getValue();
            if (values != null && !values.isEmpty()) {
                filterStrings.add(createListFilter(filter.getKey(), values));
            }
        }

        StringBuilder filterString = new StringBuilder("?");
        boolean isFirst = true;
        for (String filter : filterStrings) {
            if (isFirst) {
                isFirst = false;
            } else {
                filterString.append("&");
            }
            filterString.append(filter);
        }

        // Now do the filtered call into massive
        HttpURLConnection connection = createHttpURLConnectionToMassive("/assets" + filterString.toString());
        connection.setRequestMethod("GET");
        testResponseCode(connection);
        return JSONAssetConverter.readValues(connection.getInputStream());
    }

    /**
     * Creates a URL filter for the <code>attribute</code> where the <code>values</code> are the valid values.
     *
     * @param attributeName The attributes to use in the filter
     * @param values The valid values. Must not be <code>null</code> or empty
     * @return The filter strings
     */
    private String createListFilter(FilterableAttribute attribute, Collection<String> values) {
        /*
         * This is more complicated than you'd think... Some attribute values caused incompatible changes to the JSON data model so are actually stored in a different object in the
         * JSON. Therefore the filter that we are constructing maybe pointing to one or two attributes in the JSON. We create a filter for both possible attributes and then only
         * add the ones that we used.
         */
        boolean firstFilter1Value = true;
        boolean firstFilter2Value = true;
        StringBuilder filter1 = new StringBuilder(attribute.getAttributeName()).append("=");
        StringBuilder filter2 = new StringBuilder();
        Collection<String> filter2Values = attribute.getValuesInSecondaryAttributeName() == null ? Collections.<String> emptySet() : attribute.getValuesInSecondaryAttributeName();
        if (attribute.getSecondaryAttributeName() != null) {
            filter2 = filter2.append(attribute.getSecondaryAttributeName()).append("=");
        }
        for (String value : values) {
            if (filter2Values.contains(value)) {
                try {
                    value = URLEncoder.encode(value, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // If UTF-8 encoding isn't supported we'll just have to try the unencoded string
                }
                if (firstFilter2Value) {
                    firstFilter2Value = false;
                } else {
                    // OR all types so we get them all
                    filter2.append(ENCODED_BAR);
                }
                filter2.append(value);
            } else {
                try {
                    value = URLEncoder.encode(value, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // If UTF-8 encoding isn't supported we'll just have to try the unencoded string
                }
                if (firstFilter1Value) {
                    firstFilter1Value = false;
                } else {
                    // OR all types so we get them all
                    filter1.append(ENCODED_BAR);
                }
                filter1.append(value);
            }
        }
        if (!firstFilter1Value && !firstFilter2Value) {
            throw new IllegalArgumentException("Unable to filter values that come from two different JSON objects, attempted to filter " + attribute + " using values " + values);
        }
        if (!firstFilter1Value) {
            return filter1.toString();
        }
        if (!firstFilter2Value) {
            return filter2.toString();
        }
        return null;
    }

    /**
     * This method will test the return type to make sure that it is between 200 (inclusive) and 300 (exclusive), i.e. that it is "successful". If it is not then it will throw an
     * {@link RequestFailureException} with the response code and message from the error stream.
     *
     * @param connection
     * @throws RequestFailureException
     * @throws IOException
     * @see HttpURLConnection#getResponseCode()
     * @see HttpURLConnection#getErrorStream()
     */
    private void testResponseCode(HttpURLConnection connection) throws RequestFailureException, IOException {
        testResponseCode(connection, false);
    }

    /**
     * This method will test the return type to make sure that it is between 200 (inclusive) and 300 (exclusive), i.e. that it is "successful". If it is not then it will throw an
     * {@link RequestFailureException} with the response code and message from the error stream.
     * <br>
     * if clearInputStream is true, then on a 'good' response code (2xx), any content on the
     * input stream will be read and thrown away. This should help Java to re-use connections
     * http://docs.oracle.com/javase/6/docs/technotes/guides/net/http-keepalive.html
     *
     * @param connection
     * @param clearInputStream if true the connections input stream will be read and discarded.
     * @throws RequestFailureException
     * @throws IOException
     * @see HttpURLConnection#getResponseCode()
     * @see HttpURLConnection#getErrorStream()
     */

    private void testResponseCode(HttpURLConnection connection, boolean clearInputStream) throws RequestFailureException, IOException {

        int responseCode = connection.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            if (clearInputStream) {
                clearInputStream(connection);
            }
            return;
        }

        // Not one of the OK response codes so get the message off the error stream and throw an exception
        InputStream errorStream = connection.getErrorStream();
        String errorStreamString = null;
        String message = null;
        if (errorStream != null) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int read = -1;
            while ((read = errorStream.read()) != -1) {
                outputStream.write(read);
            }
            errorStreamString = outputStream.toString(getCharset(connection.getContentType()));

            // The content of the error stream coming from Massive seems to vary in structure
            // (HTML, JSON) see if it is is JSON and get a good message or use it as is
            message = parseErrorObject(errorStreamString);
        } else {
            // No error stream returned (this can happen on LARS if the error is returned by liberty rather than the application)
            // Set the message to the HTTP response message so that we have something slightly helpful to show to the user
            message = connection.getResponseMessage();
        }

        throw new RequestFailureException(responseCode, message, connection.getURL(), errorStreamString);

    }

    /**
     * Read the input stream from the connection, throw it away and close
     * the connection, swallow all exceptions.
     *
     * @param conn
     */
    private void clearInputStream(HttpURLConnection conn) {
        InputStream is = null;
        byte[] buffer = new byte[1024];
        try {
            is = conn.getInputStream();
            while (is.read(buffer) != -1) {
                continue;
            }
        } catch (IOException e) {
            // Don't care.
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Don't care.
                }
            }
        }
    }

    /**
     * Utility method to get the charset from a url connection's content type
     *
     * @return The charset name
     */
    public static String getCharset(final String contentType) {
        // Default to UTF-8
        String charset = "UTF-8";
        try {
            // Content type is in the form:
            // Content-Type: text/html; charset=utf-8
            // Where they can be lots of params separated by ; characters
            if (contentType != null && !contentType.isEmpty()) {
                if (contentType.contains(";")) {
                    String[] params = contentType.substring(contentType.indexOf(";")).split(";");
                    for (String param : params) {
                        param = param.trim();
                        if (param.indexOf("=") > 0) {
                            String paramName = param.substring(0, param.indexOf("=")).trim();
                            if ("charset".equals(paramName) && param.length() > (param.indexOf("=") + 1)) {
                                String paramValue = param.substring(param.indexOf("=") + 1).trim();
                                if (paramValue != null && !paramValue.isEmpty() && Charset.isSupported(paramValue)) {
                                    charset = paramValue;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            // Ignore, we really don't want this util killing anything!
        }
        return charset;
    }

    /**
     * This treats the supplied string as a JSON object and looks for the message attribute inside it. If it is not valid JSON or does not contain a message the original string is
     * returned.
     *
     * @param errorObject
     * @return
     */
    private String parseErrorObject(String errorObject) {
        if (errorObject == null) {
            return null;
        }
        try {
            // Just use JsonObject parse directly instead of DataModelSerializer as we only want one attribute
            InputStream inputStream = new ByteArrayInputStream(errorObject.getBytes(Charset.forName("UTF-8")));
            JsonReader jsonReader = Json.createReader(inputStream);
            JsonObject parsedObject = jsonReader.readObject();
            jsonReader.close();
            Object errorMessage = parsedObject.get("message");
            if (errorMessage != null && errorMessage instanceof JsonString && !((JsonString) errorMessage).getString().isEmpty()) {
                return ((JsonString) errorMessage).getString();
            } else {
                return errorObject;
            }
        } catch (JsonException e) {
            return errorObject;
        }
    }

    /**
     * Print the base 64 string differently depending on JDK level because
     * on JDK 7/8 we have JAX-B, and on JDK 8+ we have java.util.Base64
     */
    private static String encode(byte[] bytes) {
        try {
            if (System.getProperty("java.version").startsWith("1.")) {
                // return DatatypeConverter.printBase64Binary(str);
                Class<?> DatatypeConverter = Class.forName("javax.xml.bind.DatatypeConverter");
                return (String) DatatypeConverter.getMethod("printBase64Binary", byte[].class).invoke(null, bytes);
            } else {
                // return Base64.getEncoder().encode();
                Class<?> Base64 = Class.forName("java.util.Base64");
                Object encodeObject = Base64.getMethod("getEncoder").invoke(null);
                return (String) encodeObject.getClass().getMethod("encodeToString", byte[].class).invoke(encodeObject, bytes);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
