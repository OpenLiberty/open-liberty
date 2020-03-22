/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.acme.docker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;

/**
 * Testcontainer implementation for Pebble and Boulder Certificate Authorities.
 * 
 */
public abstract class CAContainer extends GenericContainer<CAContainer> {

    private final String testDomain1 = "domain1.com";
    private final String testDomain2 = "domain2.com";
    private final String testDomain3 = "domain3.com";
    private final String testDomain4 = "domain4.com";
    
    /** 
	 * The port that is used to answer the HTTP-01 challenges
     * The port used to listen for incoming ACME requests.
     * The REST management API port. 
     */
    private final int httpPort, listenPort, managementPort;
    
    /**
     * This constructor is used by BoulderContainer
     */
    public CAContainer(String image, int httpPort, int listenPort, int managementPort) {
        super(image);
        this.httpPort = httpPort;
        this.listenPort = listenPort;
        this.managementPort = managementPort;
    }

    /**
     * This constructor is used by PebbleContainer.
     */
    public CAContainer(ImageFromDockerfile image, int httpPort, int listenPort, int managementPort) {
        super(image);
        this.httpPort = httpPort;
        this.listenPort = listenPort;
        this.managementPort = managementPort;
    }

    /**
     * Get CA's intermediate certificate.
     * 
     * @return CA's intermediate certificate in the form of a PEM file.
     * @throws Exception
     *             If we failed to receive the certificate.
     */
    public InputStream getAcmeCaIntermediateCertificate() throws Exception {
        final String METHOD_NAME = "getAcmeCaIntermediateCertificate()";
        String url = "https://" + this.getContainerIpAddress() + ":" + this.getMappedPort(managementPort)
                + "/intermediates/0";

        try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {
            /*
             * Create a GET request to the ACME CA server.
             */
            HttpGet httpGet = new HttpGet(url);

            /*
             * Send the GET request and process the response.
             */
            try (final CloseableHttpResponse response = httpclient.execute(httpGet)) {
                AcmeFatUtils.logHttpResponse(CAContainer.class, METHOD_NAME, httpGet, response);

                StatusLine statusLine = response.getStatusLine();

                if (statusLine.getStatusCode() != 200) {
                    throw new IOException(
                            METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
                }

                byte[] result = EntityUtils.toByteArray(response.getEntity());

                Log.info(CAContainer.class, METHOD_NAME, new String(result));
                return  new ByteArrayInputStream(result);
            }
        }
    }

    /**
     * Get the root certificate.
     * 
     * @param fileName
     *            The name of the file to save the certificate to.
     * @return Certificate Authority's root certificate in the form of a PEM file.
     * @throws Exception
     *             If we failed to receive the certificate.
     */
    public byte[] getAcmeCaRootCertificate() throws Exception {
        final String METHOD_NAME = "getAcmeCaRootCertificate()";
        String url = "https://" + this.getContainerIpAddress() + ":" + this.getMappedPort(managementPort) + "/roots/0";

        try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {
            /*
             * Create a GET request to the ACME CA server.
             */
            HttpGet httpGet = new HttpGet(url);

            /*
             * Send the GET request and process the response.
             */
            try (final CloseableHttpResponse response = httpclient.execute(httpGet)) {
                AcmeFatUtils.logHttpResponse(CAContainer.class, METHOD_NAME, httpGet, response);

                StatusLine statusLine = response.getStatusLine();

                if (statusLine.getStatusCode() != 200) {
                    throw new IOException(
                            METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
                }

                byte[] result = EntityUtils.toByteArray(response.getEntity());

                Log.info(CAContainer.class, METHOD_NAME, new String(result));
                return result;
            }
        }
    }
    
    /**
     * Get the status of the certificate from the ACME CA server.
     * 
     * @param certificate
     *            The certificate to check.
     * @return The status of the certificate.
     * @throws Exception
     */
    public String getAcmeCertificateStatus(X509Certificate certificate) throws Exception {
        final String METHOD_NAME = "getAcmeCertificateStatus()";
        String url = "https://" + this.getContainerIpAddress() + ":" + this.getMappedPort(managementPort)
                + "/cert-status-by-serial/" + certificate.getSerialNumber().toString(16);

        try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {
            /*
             * Create a GET request to the ACME CA server.
             */
            HttpGet httpGet = new HttpGet(url);

            /*
             * Send the GET request and process the response.
             */
            try (final CloseableHttpResponse response = httpclient.execute(httpGet)) {
                AcmeFatUtils.logHttpResponse(CAContainer.class, METHOD_NAME, httpGet, response);

                StatusLine statusLine = response.getStatusLine();

                if (statusLine.getStatusCode() != 200) {
                    throw new IOException(
                            METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
                }

                String result = EntityUtils.toString(response.getEntity());

                /*
                 * The result is in JSON, lets just parse out the status.
                 */
                Pattern p = Pattern.compile(".*\"Status\": \"(\\w+)\",.*", Pattern.DOTALL);
                Matcher m = p.matcher(result);
                if (m.find()) {
                    result = m.group(1);
                } else {
                    throw new Exception(
                            "Certificate status response was not in expected JSON format. Response: " + result);
                }

                Log.info(CAContainer.class, METHOD_NAME, new String(result));
                return result;
            }
        }
    }
    
    /**
     * Retrieves the client host's IP address that is reachable from the
     * container.
     * 
     * @return The client host's IP address that is reachable from the
     *         container.
     * @throws IllegalStateException
     *             If the address was not found.
     */
    public String getClientHost() throws IllegalStateException {
        for (String extraHost : this.getExtraHosts()) {
            if (extraHost.startsWith("host.testcontainers.internal:")) {
                return extraHost.replace("host.testcontainers.internal:", "");
            }
        }

        throw new IllegalStateException(
                "Unable to resolve local host from docker container. Could not find 'host.testcontainers.internal' property.");
    }
    
    /**
     * Add A and AAAA records.
     */
    public void addARecords() {
        try {
            for (String domain : new String[] { testDomain1, testDomain2, testDomain3, testDomain4 }) {
                /*
                 * Disable the IPv6 responses for this domain. The Boulder CA server
                 * responds on AAAA (IPv6) responses before A (IPv4) responses, and
                 * we don't currently have the testcontainer host's IPv6 address.
                 */
                addARecord(domain, getClientHost());
                addAAAARecord(domain, "");
            }
        } catch (Exception e) {
            Log.error(CAContainer.class, "addARecords", e);
        }
    }
    
    /**
     * Convenience method for adding A records.
     * @param host
     *            The host / domain to redirect requests for.
     */
    public void addARecord(String domain) throws IOException {
        addARecord(domain, getClientHost());
    }
    
    /**
     * Add an A record to the mock DNS server. This will allow us to redirect
     * requests to a named domain to the IPv4 address of our choice.
     * 
     * @param host
     *            The host / domain to redirect requests for.
     * @param address
     *            The address to direct the requests for that host to.
     * @throws IOException
     */
    public void addARecord(String host, String address) throws IOException {
        final String METHOD_NAME = "addARecord";

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {

            /*
             * Generate the JSON request. The request can support multiple
             * addresses but for the time being we will only support sending
             * one.
             */
            String jsonString = "{\"host\":\"" + host + "\",\"addresses\":[\"" + address + "\"]}";
            StringEntity requestEntity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);

            /*
             * Create a POST request to the mock DNS server.
             */
            HttpPost httpPost = new HttpPost(getManagementAddress() + "/add-a");
            httpPost.setEntity(requestEntity);
            /*
             * Send the POST request and process the response.
             */
            try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
                AcmeFatUtils.logHttpResponse(CAContainer.class, METHOD_NAME, httpPost, response);

                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() != 200) {
                    throw new IOException(
                            METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
                }
            }
        }
    }
    
    /**
     * Add an AAAA record to the mock DNS server. This will allow us to redirect
     * requests to a named domain to the IPv6 address of our choice.
     * 
     * @param host
     *            The host / domain to redirect requests for.
     * @param address
     *            The address to direct the requests for that host to.
     * @throws IOException
     */
    public void addAAAARecord(String host, String address) throws IOException {
        final String METHOD_NAME = "addAAAARecord";

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {

            /*
             * Generate the JSON request. The request can support multiple
             * addresses but for the time being we will only support sending
             * one.
             */
            String jsonString = "{\"host\":\"" + host + "\",\"addresses\":[\"" + address + "\"]}";
            StringEntity requestEntity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);

            /*
             * Create a POST request to the mock DNS server.
             */
            HttpPost httpPost = new HttpPost(getManagementAddress() + "/add-aaaa");
            httpPost.setEntity(requestEntity);

            /*
             * Send the POST request and process the response.
             */
            try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
                AcmeFatUtils.logHttpResponse(CAContainer.class, METHOD_NAME, httpPost, response);

                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() != 200) {
                    throw new IOException(
                            METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
                }
            }
        }
    }
    
    /**
     * Clear a previously added AAAA record that was added to the mock DNS
     * server.
     * 
     * @param host
     *            The host / domain to redirect requests for.
     * @throws IOException
     */
    public void clearAAAARecord(String host) throws IOException {
        final String METHOD_NAME = "clearAAAARecord(String)";

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            /*
             * Generate the JSON request.
             */
            String jsonString = "{\"host\":\"" + host + "\"}";
            StringEntity requestEntity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);

            /*
             * Create a POST request to the mock DNS server.
             */
            HttpPost httpPost = new HttpPost(getManagementAddress() + "/clear-aaaa");
            httpPost.setEntity(requestEntity);

            /*
             * Send the POST request and process the response.
             */
            try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
                AcmeFatUtils.logHttpResponse(CAContainer.class, METHOD_NAME, httpPost, response);

                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() != 200) {
                    throw new IOException(
                            METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
                }
            }
        }
    }

    /**
     * Clear a previously added A record that was added to the mock DNS server.
     * 
     * @param host
     *            The host / domain to redirect requests for.
     * @throws IOException
     */
    public void clearARecord(String host) throws IOException {
        final String METHOD_NAME = "clearMockARecord(String)";

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            /*
             * Generate the JSON request.
             */
            String jsonString = "{\"host\":\"" + host + "\"}";
            StringEntity requestEntity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);

            /*
             * Create a POST request to the mock DNS server.
             */
            HttpPost httpPost = new HttpPost(getManagementAddress() + "/clear-a");
            httpPost.setEntity(requestEntity);

            /*
             * Send the POST request and process the response.
             */
            try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
                AcmeFatUtils.logHttpResponse(CAContainer.class, METHOD_NAME, httpPost, response);

                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() != 200) {
                    throw new IOException(
                            METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
                }
            }
        }
    }
	
    /** 
     * return the port that is used to answer the HTTP-01 challenges  
     */
    public int getHttpPort() { return httpPort; }

    /** 
     * return the REST management API port. 
     */
    protected int getManagementPort() { return managementPort; }

    /** 
     * return the port used to listen for incoming ACME requests. 
     */
    protected int getListenPort() { return listenPort; }
    
    /**
     * The HTTP address that can be used the reach the REST management API for
     * the server.
     * 
     * @return The HTTP address to the REST management endpoint.
     */
    public abstract String getManagementAddress();
    
    /**
     * Get the URI to the ACME CA's directory.
     * 
     * @param usePebbleURI
     *            Use the "acme://pebble" style URI instead of the generic
     *            "https:" URI. This param is ignored for Boulder.
     * @return The URI to the ACME CA's directory.
     */
    public abstract String getAcmeDirectoryURI(boolean usePebbleURI);
    
    /**
     * Get the IP address for the container as seen from the container network.
     * 
     * @return The IP address for the container on the container network.
     */
    public abstract String getIntraContainerIP();
}
