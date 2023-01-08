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
package io.openliberty.jcache.internal.fat.docker;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.SimpleLogConsumer;

/**
 * See the following link for details on the infinispan/server image.
 *
 * https://github.com/infinispan/infinispan-images.
 */
public class InfinispanContainer extends GenericContainer<InfinispanContainer> {
    private Class<?> CLASS = InfinispanContainer.class;

    /*
     * The version should match that used in the build.gradle file.
     */
    private static final String IMAGE_NAME = "infinispan/server";
    private static final String IMAGE_VERSION = "13.0.10.Final";

    private static final int INFINISPAN_PORT = 11222; // Hot Rod and REST
    public static final String ADMIN_USER = "admin";
    public static final String ADMIN_PASS = "password";

    /**
     * Construct a new {@link InfinispanContainer}.
     */
    public InfinispanContainer() {
        this(IMAGE_NAME + ":" + IMAGE_VERSION);
    }

    /**
     * Construct a new {@link InfinispanContainer}.
     *
     * @param imageName The name of the image.
     */
    public InfinispanContainer(final String imageName) {
        super(imageName);
        withEnv("USER", ADMIN_USER);
        withEnv("PASS", ADMIN_PASS);
        withStartupTimeout(Duration.ofMillis(20000));
        waitingFor(new LogMessageWaitStrategy().withRegEx(".*Infinispan Server.*started in.*\\s"));
        withLogConsumer(new SimpleLogConsumer(CLASS, "INFINISPAN"));
        withExposedPorts(INFINISPAN_PORT);
    }

    /**
     * Get the remote port on the container.
     *
     * @return
     */
    public Integer getRemotePort() {
        return getMappedPort(INFINISPAN_PORT);
    }

    /**
     * Get the HOTROD URI for the Infinispan server.
     *
     * @return The HOTROD URI.
     */
    public String getHotRodUri() {
        return "hotrod://" + ADMIN_USER + ":" + ADMIN_PASS + "@" + getHost() + ":" + getRemotePort();
    }

    /**
     * Get the REST endpoint for the Infinispan server.
     *
     * @return The REST endpoint.
     */
    public String getRESTEndpoint() {
        return "http://" + getHost() + ":" + getRemotePort() + "/rest/v2/";
    }

    /**
     * Delete all the caches from the Infinispan server (excluding internal caches).
     *
     * @throws Exception If there was an error deleting all the caches.
     */
    public void deleteAllCaches() throws Exception {
        List<String> cacheNames = listCaches();
        for (int idx = 0; idx < 3; idx++) {
            for (String cache : cacheNames) {
                deleteCache(cache);
            }

            /*
             * See if we succeeded. There are times where we the Infinispan instance will return
             * a cache to us but we will get a 404 when we try to delete it. So in that instance, we
             * will try again (and again perhaps).
             */
            cacheNames = listCaches();
            if (cacheNames.isEmpty()) {
                break;
            }
            Thread.sleep(100);
        }
    }

    /**
     * Get a list of the caches on the Infinispan server.
     *
     * @return The list of caches (excluding internal caches).
     * @throws Exception If there was an error getting the list of caches.
     */
    public List<String> listCaches() throws Exception {
        final String METHOD_NAME = "listCaches()";
        HttpGet request = new HttpGet(getRESTEndpoint() + "caches/");

        List<String> caches = new ArrayList<String>();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            /*
             * Get the challenge header and generate a HttpClientContext in response
             * to the challenge header.
             */
            Header challengeHeader = getAuthChallengeHeader(httpClient, request);
            HttpClientContext context = getHttpClientContext(challengeHeader);

            /*
             * Resend the request with the authentication challenge response.
             */
            try (CloseableHttpResponse response = httpClient.execute(request, context)) {
                logHttpResponse(InfinispanContainer.class, METHOD_NAME, request, response);

                assertEquals("Expected 200 as the response code.", 200, response.getStatusLine().getStatusCode());

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);

                    /*
                     * The result is returned as a JSON array. For now just format manually.
                     * Eventually I could use a JSON library.
                     */
                    for (String c : result.split(",")) {
                        c = c.replace("[", "").replace("]", "").replace("\"", "").trim();

                        /* Skip internal caches. */
                        if (!c.startsWith("___")) {
                            caches.add(c);
                        }
                    }
                }
            }
        }

        Log.info(getClass(), METHOD_NAME, "Existing caches (REST): " + caches);
        printCachesJavaView(); // TODO REMOVE
        return caches;
    }

    /**
     * Delete a cache from the Infinispan server.
     *
     * @param name The name of the cache.
     * @throws Exception If there was an error deleting the cache.
     */
    public void deleteCache(String name) throws Exception {
        final String METHOD_NAME = "deleteCache(String)";

        /*
         * Session cache appears to double escape the "/" in their cache names, and the cache name
         * results in "%2F", which when we pass it in here gets escaped again and doesn't match the
         * literal "%2F" in the cache name. Fix it here.
         */
        if (name.startsWith("com.ibm.ws.session")) {
            name = name.replace("%", "%25");
        }

        HttpDelete request = new HttpDelete(getRESTEndpoint() + "caches/" + name);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            /*
             * Get the challenge header and generate a HttpClientContext in response
             * to the challenge header.
             */
            Header challengeHeader = getAuthChallengeHeader(httpClient, request);
            HttpClientContext context = getHttpClientContext(challengeHeader);

            /*
             * Resend the request with the authentication challenge response.
             */
            try (CloseableHttpResponse response = httpClient.execute(request, context)) {
                logHttpResponse(InfinispanContainer.class, METHOD_NAME, request, response);
                assertThat("Expected 200 or 404 as the response code while deleting cache " + name + ".",
                           response.getStatusLine().getStatusCode(), anyOf(is(200), is(404)));
            }
        }
    }

    private void printCachesJavaView() {
        final String METHOD_NAME = "test()";

        /*
         * Connect to the remote Infinispan server and insert a value into the cache.
         */
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.uri(getHotRodUri()).security().authentication().realm("default").saslMechanism("DIGEST-MD5");
        RemoteCacheManager cacheManager = new RemoteCacheManager(builder.build(), true);
        Set<String> caches = cacheManager.getCacheNames();
        cacheManager.close();

        /* Skip internal caches. */
        Set<String> filtered = new HashSet<String>();
        for (String c : caches) {
            if (!c.startsWith("___")) {
                filtered.add(c);
            }
        }

        Log.info(getClass(), METHOD_NAME, "Existing caches (JAVA): " + filtered);
    }

    /**
     * Get the authorization challenge header from the server.
     *
     * @param httpClient The {@link HttpClient}.
     * @param request    The request to get the challenge for.
     * @return The {@link Header} that contains the challenge.
     * @throws Exception If there was an error getting the challenge header.
     */
    private static Header getAuthChallengeHeader(CloseableHttpClient httpClient, HttpUriRequest request) throws Exception {
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            assertEquals("REST endpoint didn't respond with HTTP 401.", HttpStatus.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
            assertTrue("REST endpoint responded with HTTP 401, but didn't send a WWW-Authenticate header.", response.containsHeader("WWW-Authenticate"));
            return response.getFirstHeader("WWW-Authenticate");
        }
    }

    /**
     * Get the {@link HttpClientContext} for the challenge header.
     *
     * @param challengeHeader The challenge header from a previous request.
     * @return The {@link HttpClientContext} with the response for the challenge header.
     * @throws Exception If there was an issue generating the context.
     */
    private HttpClientContext getHttpClientContext(Header challengeHeader) throws Exception {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY,
                                     new UsernamePasswordCredentials(ADMIN_USER, ADMIN_PASS));

        /*
         * Create AuthCache instance
         */
        AuthCache authCache = new BasicAuthCache();

        /*
         * Generate DIGEST scheme object, initialize it and add it to the local auth cache
         */
        DigestScheme digestAuth = new DigestScheme();
        digestAuth.processChallenge(challengeHeader);
        authCache.put(new HttpHost(getHost(), getRemotePort()), digestAuth);

        /*
         * Add AuthCache to the execution context
         */
        HttpClientContext context = HttpClientContext.create();
        context.setCredentialsProvider(credsProvider);
        context.setAuthCache(authCache);

        return context;
    }

    /**
     * Log HTTP responses in a standard form.
     *
     * @param clazz      The class that is asking to log the response.
     * @param methodName The method name that is asking to log the response.
     * @param request    The request that was made.
     * @param response   The response that was received.
     */
    public static void logHttpResponse(Class<?> clazz, String methodName, HttpRequestBase request,
                                       CloseableHttpResponse response) {
        StatusLine statusLine = response.getStatusLine();
        Log.info(clazz, methodName, request.getMethod() + " " + request.getURI() + " ---> " + statusLine.getStatusCode()
                                    + " " + statusLine.getReasonPhrase());
    }
}
