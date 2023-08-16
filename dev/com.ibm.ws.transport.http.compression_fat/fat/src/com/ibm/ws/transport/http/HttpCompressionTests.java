/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
package com.ibm.ws.transport.http;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Class to test the Auto Compression in Liberty
 * with the <autoCompression> configuration in the server.xml
 */
@RunWith(FATRunner.class)
public class HttpCompressionTests {

    private static final Class<?> ME = HttpCompressionTests.class;
    public static final String APP_NAME = "EndpointInformation";
    public static final String APP_SERVLET_NAME = "EndpointInformationServlet";
    private static final String APP_MESSAGE = "Endpoint Information Servlet Test";
    private static final String APP_STARTED_MESSAGE = "CWWKT0016I:.*EndpointInformation.*";

    //Accept-Encoding
    private static final String ACCEPT_ENCODING = "Accept-Encoding";

    //Base file location for configuration files
    private static final String CONFIGURATION_FILES_DIR = "compressionConfig" + File.separator;
    private static final String XML_EXTENSION = ".xml";
    private static final String LOG_EXTENSION = ".log";

    private enum ConfigurationFiles {
        DEFAULT("compression-server"),
        WITH_CONTENT_TYPES("compressionWithTypes-server"),
        WITH_CONFIG_REF("compressionCommonConfig-server"),
        WITH_CONFIG_PREF_ALGO("compressionWithPreferredAlgo-server"),
        WITH_CONFIG_PREF_ALGO_MISCONFIG("compressionWithPreferredAlgoMisconfiguration-server"),
        WITH_CONFIG_DUPLICATE_TYPES("compressionWithTypeDuplicate-server"),
        WITH_CONFIG_DUPLICATE_REMOVAL_TYPES("compressionWithTypeDuplicateRemoval-server"),
        WITH_CONFIG_ADD_REMOVE_DUPLICATE_TYPES("compressionWithTypeAddRemoveDuplicate-server"),
        WITH_CONFIG_OVERWRITE_ADD_TYPES("compressionWithTypeOverwriteAndAdd-server");

        ConfigurationFiles(String name) {
            this.name = name;
        }

        private String name;

    }

    private enum CompressionType {
        DEFLATE("deflate"),
        GZIP("gzip"),
        XGZIP("x-gzip"),
        ZLIB("zlib"),
        IDENTITY("identity");

        CompressionType(String name) {
            this.name = name;
        }

        private String name;
    }

    @Server("FATServer")
    public static LibertyServer server;

    @Rule
    public TestName name = new TestName();

    private final int httpDefaultPort = server.getHttpDefaultPort();
    private HttpClient client;

    // List of headers used for a particular request
    private static List<BasicHeader> headerList = new ArrayList<BasicHeader>();
    // Represents the server configuration file name
    private static String configurationFileName = null;
    // Represents the test's name
    private static String testName;
    // Represents a request's body as a string object
    private static String responseString;

    private static void cleanUp() {
        headerList.clear();
        configurationFileName = null;
        testName = null;
        responseString = null;
    }

    @BeforeClass
    public static void setupOnlyOnce() throws Exception {
        // Create a WebArchive that will have the file name 'EndpointInformation.war' once it's written to a file
        // Include the 'com.ibm.ws.transport.http.servlets' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.ws.transport.http.servlets");
    }

    /**
     * NOTE: apache auto-decompresses responses by default, so client needs to be
     * configured to not do so for these tests.
     *
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        cleanUp();
        RequestConfig requestConfig = RequestConfig.custom()
                        .setLocalAddress(InetAddress.getByName("127.0.0.1"))
                        .setConnectionRequestTimeout(30000)
                        .setConnectTimeout(30000)
                        .setSocketTimeout(30000)
                        .build();
        client = HttpClientBuilder
                        .create()
                        .setRetryHandler(new DefaultHttpRequestRetryHandler())
                        .setDefaultRequestConfig(requestConfig)
                        .disableContentCompression()
                        .evictExpiredConnections()
                        .build();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted())
            server.stopServer("CWWKT0029W", "CWWKT0030W", "CWWKT0031W", "CWWKT0032W", "CWWKT0033W");
    }

    /**
     * Test q-value by choosing different weights and verifying that the
     * chosen compression type is the highest value. This test will set
     * deflate to a compression q-value of 0.75 and gzip to a value of
     * 0.5. The first request will set the Accept-Encoding header with
     * deflate first, followed by gzip. The second request will flip
     * the ordering.
     *
     * Expected outcome: both responses should contain a Content-Encoding
     * set to deflate.
     *
     *
     * @throws Exception
     */
    @Test
    public void testQValueWeight() throws Exception {

        configurationFileName = ConfigurationFiles.DEFAULT.name;
        testName = name.getMethodName();

        startServer(configurationFileName, testName);

        //First Request
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate;q=0.75, gzip;q=0.5"));

        HttpResponse httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.DEFLATE);

        //Second Request
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "gzip;q=0.5, deflate;q=0.75"));
        httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.DEFLATE);

        //Third Request
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "identity, deflate; q=0.75"));

        httpResponse = execute(headerList, testName);
        assertNotCompressed(httpResponse);

        //Fourth Request
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, ""));

        httpResponse = execute(headerList, testName);
        assertNotCompressed(httpResponse);

        //Fifth Request
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "*"));

        httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.GZIP);

    }

    /**
     * Test that the server honors q-value set to 0 when no explicit encodings
     * are set and a wildcard is provided. The server should first check to see
     * if able to set gzip. If unable, it will try to set deflate. If unable to
     * do neither, no compression should be set.
     *
     * This test will send three requests:
     * 1. Accept-Encoding: gzip;q=0, *
     * Expected outcome: Response is compressed as deflate.
     * 2. Accept-Encoding: *
     * Expected outcome: Response is compressed as gzip (favored over deflate and identity)
     * 3. Accept-Encoding: gzip;q=0,deflate;q=0, *
     * Expected outcome: Response is not compressed.
     *
     * @throws Exception
     */
    @Test
    public void testAcceptEncodingStarAndQV0Interactions() throws Exception {

        configurationFileName = ConfigurationFiles.DEFAULT.name;
        testName = name.getMethodName();

        startServer(configurationFileName, testName);

        //First Request
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "gzip;q=0, *"));

        HttpResponse httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.DEFLATE);

        //Second Request
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "*"));
        httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.GZIP);

        //Third Request
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "gzip;q=0,deflate;q=0,*"));

        httpResponse = execute(headerList, testName);
        assertNotCompressed(httpResponse);

    }

    /**
     * Test that the server honors q-value set to 0 when no explicit encodings
     * are set and a wildcard is provided. This will also set the server preferred
     * algorithm to 'deflate'.
     *
     * This test will send two requests:
     * 1. Accept-Encoding: deflate;q=0, *
     * Expected outcome: Response is compressed as gzip.
     * 2. Accept-Encoding: gzip;q=0,deflate;q=0, *
     * Expected outcome: Response is not compressed.
     *
     * @throws Exception
     */
    @Test
    public void testAcceptEncodingStarAndQV0InteractionsWithPreferredAlgo() throws Exception {

        configurationFileName = ConfigurationFiles.WITH_CONFIG_PREF_ALGO.name;
        testName = name.getMethodName();

        startServer(configurationFileName, testName);

        //First Request
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate;q=0, *"));

        HttpResponse httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.GZIP);

        //Second Request
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "gzip;q=0,deflate;q=0,*"));

        httpResponse = execute(headerList, testName);
        assertNotCompressed(httpResponse);

    }

    /**
     * Test that the server will favor compression of gzip over other
     * supported compression types when they share the same q-value. This
     * is achieved by sending two supported compression types - gzip &
     * deflate - set with q-values of 1. The first request will set the
     * Accept-Encoding header with deflate first; the second request will
     * flip the ordering. This is done to prove that GZIP is chosen
     * regardless of how the header is defined.
     *
     * Expected outcome: both responses should contain a Content-Encoding
     * set to gzip.
     *
     *
     * @throws Exception
     */
    @Test
    public void testCompressionFavorsGZIP() throws Exception {

        configurationFileName = ConfigurationFiles.DEFAULT.name;
        testName = name.getMethodName();

        startServer(configurationFileName, testName);

        //First Request
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate;q=1, gzip;q=1"));

        HttpResponse httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.GZIP);

        //Second Request
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "gzip;q=1, deflate;q=1"));
        httpResponse = execute(headerList, testName);

        assertCompressed(httpResponse, CompressionType.GZIP);

    }

    /**
     * Test that the server will not default to compressing when
     * no supported compression algorithm is present in the Accept-Encoding
     * request header.
     *
     * Expected outcome: response will not be compressed and the
     * default app message will be plain text.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testNoSupportedCompressionAlgorithm() throws Exception {

        configurationFileName = ConfigurationFiles.DEFAULT.name;
        testName = name.getMethodName();

        startServer(configurationFileName, testName);

        //First Request
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "MostEfficientCompressionAlgorithm"));

        HttpResponse httpResponse = execute(headerList, testName);

        assertNotCompressed(httpResponse);

    }

    /**
     * Test that the server will compress the supported compression algorithms:
     * gzip, xgzip, deflate, zlib, and identity.
     *
     * Expected outcome: response will be compressed for each of the algorithms.
     * In the case of identity, the response is not compressed.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSupportedCompressionAlgorithms() throws Exception {

        configurationFileName = ConfigurationFiles.DEFAULT.name;
        testName = name.getMethodName();

        startServer(configurationFileName, testName);

        //First Request
        headerList.add(new BasicHeader(ACCEPT_ENCODING, CompressionType.GZIP.name));

        HttpResponse httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.GZIP);

        //Second Request
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, CompressionType.XGZIP.name));
        httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.XGZIP);

        //Second Request
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, CompressionType.DEFLATE.name));
        httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.DEFLATE);

        //Fourth Request
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, CompressionType.ZLIB.name));
        httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.DEFLATE);

        //Fifth Request
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, CompressionType.IDENTITY.name));
        httpResponse = execute(headerList, testName);
        assertNotCompressed(httpResponse);

    }

    /**
     * Test that the server properly handles malformed Accept-Encoding
     * header values.
     *
     * The first request will test when multiple semi-colon ";" characters
     * are put into the q-value definition
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMalformedAcceptEncodingHeader() throws Exception {

        configurationFileName = ConfigurationFiles.DEFAULT.name;
        testName = name.getMethodName();

        startServer(configurationFileName, testName);

        //First request - test multiple ";"
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate;;q=1"));

        HttpResponse httpResponse = execute(headerList, testName);
        assertNotCompressed(httpResponse);

        //Second request - test bad q-value
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate;qq=1, gzip;q=NOT_A_NUMBER"));

        httpResponse = execute(headerList, testName);
        assertNotCompressed(httpResponse);

        //Third request - contains ";" but no q-value
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate;"));

        httpResponse = execute(headerList, testName);
        assertNotCompressed(httpResponse);

        //Fourth request - no compression type with defined q value
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, ";q=1"));

        httpResponse = execute(headerList, testName);
        assertNotCompressed(httpResponse);

        //Fifth request - combination of good compression type with malformed
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate;q=1, gzip;q=NOT_A_NUMBER"));

        httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.DEFLATE);

    }

    /**
     * Tests that the server properly handles the intended range, 0-1, of
     * quality values. It is allowed to send up to three decimal spaces
     * when defining this weight.
     *
     * First Request sends a negative q-value.
     * Expected Result: Server will not compress response
     *
     * Second Request sends a q-value that is larger than 1
     * Expected Result: Server will not compress response
     *
     * Third Request sends a q-value with one decimal digit - 0.1
     * Expected Result: Server will compress
     *
     * Fourth Request sends a q-value with two decimal digits - 0.11
     * Expected Result: Server will compress
     *
     * Fifth Request sends a q-value with three decimal digits - 0.111
     * Expected Result: Server will compress
     *
     * Sixth Request sends a q-value with four decimal digits - 0.1111
     * Expected Result: Server will not compress (out of range)
     *
     * Seventh Request sends a q-value of 1 followed by three decimal digits - 1.000
     * Expected Result: Server will compress (It is allowed to send up to three '0'
     * after a value of 1.
     *
     * Eighth Request sends a q-value of 0 with no digits
     * Expected Result: Server will not compress
     *
     * @throws Exception
     */
    @Test
    public void testQValueRange() throws Exception {
        configurationFileName = ConfigurationFiles.DEFAULT.name;
        testName = name.getMethodName();

        startServer(configurationFileName, testName);

        HttpResponse httpResponse = null;

        //First Request - Negative Number QValue
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate;q=-1"));

        httpResponse = execute(headerList, testName);
        assertNotCompressed(httpResponse);

        //Second Request - Larger than 1 QValue
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate;q=1.1"));

        httpResponse = execute(headerList, testName);
        assertNotCompressed(httpResponse);

        //Third Request - 0 followed by one digit
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate;q=0.1"));

        httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.DEFLATE);

        //Fourth Request - 0 followed by two digits
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate;q=0.11"));

        httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.DEFLATE);

        //Fifth Request - 0 followed by three digits
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate;q=0.111"));

        httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.DEFLATE);

        //Sixth Request - 0 followed by four digits
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate;q=0.1111"));

        httpResponse = execute(headerList, testName);
        assertNotCompressed(httpResponse);

        //Seventh Request - 1 followed by three 0 digits
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate;q=1.000"));

        httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.DEFLATE);

        //Eighth Request - 0 followed by no digits
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate;q=0"));

        httpResponse = execute(headerList, testName);
        assertNotCompressed(httpResponse);

    }

    /**
     * Test that the server properly defaults to a q-value of 1
     * when it is not specified explicitly in the Accept-Encoding
     * header. The request will set 'deflate' with a q-value of
     * 0.999 and gzip to no specific value.
     *
     * Expected Result: Server sets q-value of gzip to 1 and
     * sets the Content-Encoding to 'gzip'.
     *
     * @throws Exception
     */
    @Test
    public void testDefaultQValue() throws Exception {
        configurationFileName = ConfigurationFiles.DEFAULT.name;
        testName = name.getMethodName();

        Header contentEncodingHeader = null;
        startServer(configurationFileName, testName);

        HttpResponse httpResponse = null;

        //First Request - Deflate set to 0.999, gzip set to no q-value
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate;q=0.999, gzip"));

        httpResponse = execute(headerList, testName);
        contentEncodingHeader = getContentEncodingHeader(httpResponse);
        assertTrue("Response does not contain Content-Encoding: gzip", "gzip".equals(contentEncodingHeader.getValue()));

    }

    /**
     * Tests that the server is properly specifying a Vary header
     * with the value of Accept-Encoding when evaluating whether
     * to compress a response.
     *
     * Expected Result: Server sets the Vary header to
     * 'Vary: Accept-Encoding'
     *
     * If the Vary header already existed, server will instead
     * append to the existing header the value. During the second request, the
     * application will set a value of 'test' to the Vary header.
     *
     * Expected Result, Server appends (comma delimited) the
     * 'Accept-Encoding'. Header should look like 'Vary:test, Accept-Encoding'
     *
     * If the Vary header already existed with the 'Accept-Encoding' value,
     * the server should not try to append anything to the header. During the third
     * request, the application will set a value of 'Accept-Encoding' to the Vary header.
     *
     * @throws Exception
     */
    @Test
    public void testVaryHeader() throws Exception {
        configurationFileName = ConfigurationFiles.DEFAULT.name;
        testName = name.getMethodName();

        Header varyHeader = null;
        startServer(configurationFileName, testName);

        HttpResponse httpResponse = null;

        //First Request - ask for compression, look for Vary header
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate"));

        httpResponse = execute(headerList, testName);
        varyHeader = httpResponse.getFirstHeader("Vary");

        assertTrue("Response does not contain a Vary header with Accept-Encoding set", varyHeader != null && ACCEPT_ENCODING.equals(varyHeader.getValue()));

        //Second Request - set Vary to 'example', then ask for compression
        headerList.clear();
        headerList.add(new BasicHeader("TestCondition", "Vary"));
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate"));

        httpResponse = execute(headerList, testName);
        varyHeader = httpResponse.getFirstHeader("Vary");

        assertTrue("Response does not contain a comma delimited Vary header. Value was: " + varyHeader, varyHeader != null &&
                                                                                                        "test, Accept-Encoding".equalsIgnoreCase(varyHeader.getValue()));
    }

    /**
     * Test that the server will honor not compressing small responses.
     * If the content-length is known to be less than 2048 bytes,
     * the server should not try to compress.
     *
     * First request will set an app-specific header, "TestCondition: smallSize",
     * that will tell the app to set the Content-Length to 35 bytes.
     *
     * Expected Result: Since this is less than 2048 bytes, the message should not
     * be compressed.
     *
     * Second request will set an app-specific header, "TestCondition: regularSize",
     * that will tell the app to set the Content-Length to 2235 bytes.
     *
     * Expected Result: Message will be compressed using the deflate encoding.
     *
     * @throws Exception
     */
    @Test
    public void testCompressionSizeCompliance() throws Exception {

        configurationFileName = ConfigurationFiles.DEFAULT.name;
        testName = name.getMethodName();
        HttpResponse httpResponse = null;

        startServer(configurationFileName, testName);

        //First Request - Ask for compression, set response to small
        headerList.clear();
        headerList.add(new BasicHeader("TestCondition", "smallSize"));
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate"));

        httpResponse = execute(headerList, testName);
        assertNotCompressed(httpResponse);

        //Second Request - Ask for compression, set response to larger than 2048 bytes
        headerList.clear();
        headerList.add(new BasicHeader("TestCondition", "regularSize"));
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate"));

        httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.DEFLATE);

    }

    /**
     * Test that the server will honor compression of types configured in the
     * the server <compression> types attribute. The server is configured to add
     * "test" to the acceptable compressable content types; while "text/plain" is
     * set as an excluded type.
     *
     * First request will set an app-specific header, "TestCondition: testContentType",
     * that will tell the app to set the Content-Type to "test"
     *
     * Expected Result: Since the server.xml specifies "test" as an acceptable type
     * for compression, server should compress with the specified Accept-Encoding
     * algorithm.
     *
     * Second request will set "text/plain" as the content type.
     *
     * Expected Result: Since this is not included in the configured types
     * attribute, no compression should happen.
     *
     * @throws Exception
     */
    @Test
    public void testCompressionTypesOption() throws Exception {
        configurationFileName = ConfigurationFiles.WITH_CONTENT_TYPES.name;
        testName = name.getMethodName();
        HttpResponse httpResponse = null;

        startServer(configurationFileName, testName);

        //First Request - Ask the server for compression, set app to add
        //a Content-Type of "test", which should normally not compress. Since
        //this was added as a custom filter, it should compress.
        headerList.clear();
        headerList.add(new BasicHeader("TestCondition", "testContentType"));
        headerList.add(new BasicHeader(ACCEPT_ENCODING, CompressionType.DEFLATE.name));

        httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.DEFLATE);

        //Second Request - Ask the server for compression, app will behave as
        //usual, setting Content-Type to text/plain. Since filter types do
        //not include text/plain, this should not be compressed.
        headerList.clear();
        headerList.add(new BasicHeader("TestCondition", "regularSize"));
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate"));

        httpResponse = execute(headerList, testName);
        assertNotCompressed(httpResponse);

    }

    /**
     * Test that the server will honor the server preferred algorithm when
     * configuring the serverPreferredAlgorithm attribute of the <compression>
     * element. The server should use the preferred algorithm if the Accept-Encoding
     * indicates the client supports it, othewise use the highest quality ranked
     * value that is supported by the server.
     *
     *
     * @throws Exception
     */
    @Test
    public void testCompressionServerPrefAlgorithm() throws Exception {
        configurationFileName = ConfigurationFiles.WITH_CONFIG_PREF_ALGO.name;
        testName = name.getMethodName();
        HttpResponse httpResponse = null;

        startServer(configurationFileName, testName);

        headerList.clear();
        //Server Preferred Algorithm is set to deflate. Setting a smaller (yet
        //larger than zero) quality value than gzip should still compress as
        //deflate.
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate;q=0.5, gzip"));

        httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.DEFLATE);

        //Second Request - With the server configured to prefer deflate, a request
        //with no deflate algorithm should default to  the highest compression
        //algorithm in the accept encoding header
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "identity; q=0.5, gzip"));

        httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.GZIP);

        //Third Request - With the server configured to prefer deflate, send a request
        //with an Accept-Encoding that sets both deflate and gzip to the same quality
        //value. While the server would typically chose gzip over deflate in ties,
        //the server should now pick deflate.
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "deflate, gzip"));

        httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.DEFLATE);

        //Fourth Request - With the server configured to prefer deflate, send a
        //request with an Accept-Encoding that sets '*' as the value. While the server
        //would typically choose gzip as the default algorithm when '*' is set, deflate
        //should now be set.
        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "*"));

        httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.DEFLATE);
    }

    /**
     * Test that the server will not attempt compressing a response that already
     * contains a Content-Encoded response.
     *
     * Request will set an app-specific header, "TestCondition: testContentEncoding",
     * that will tell the app to set the Content-Encoded header to "gzip". This
     * will also set the Vary header to a value of "Accept-Encoding"
     *
     * Expected Result: Response contains a Content-Encoded header with the value
     * of gzip. Server should not attempt to compress the response, and as such,
     * payload should contain plain text expected message. The vary header is expected
     * to have the value of "Accept-Encoding".
     *
     * @throws Exception
     */
    @Test
    public void testCompressionContentEncodedServlet() throws Exception {
        configurationFileName = ConfigurationFiles.DEFAULT.name;
        testName = name.getMethodName();
        HttpResponse httpResponse = null;
        Header varyHeader = null;
        startServer(configurationFileName, testName);

        headerList.clear();
        headerList.add(new BasicHeader("TestCondition", "testContentEncoding"));
        headerList.add(new BasicHeader(ACCEPT_ENCODING, "gzip"));

        httpResponse = execute(headerList, testName);
        varyHeader = httpResponse.getFirstHeader("Vary");
        //Servlet should set Content-Encoding to gzip, which the server should
        //not try to compress. As a result, we should see the APP_MESSAGE here.
        responseString = getResponseAsString(httpResponse);
        assertTrue("Response is compressed.", responseString != null && responseString.contains(APP_MESSAGE));

        Header contentEncodingHeader = getContentEncodingHeader(httpResponse);
        assertTrue("Response does not contain Content-Encoding: gzip", "gzip".equalsIgnoreCase(contentEncodingHeader.getValue()));

        //Vary header is added by the servlet, ensure that it contains exactly "Accept-Encoding"

        assertTrue("Response expected to be [Accept-Encoding] but was: " + varyHeader, varyHeader != null && "Accept-Encoding".equalsIgnoreCase(varyHeader.getValue()));

    }

    /**
     * Test that the configuration supports setting the compression element as
     * a standalone element through reference ID.
     *
     *
     * Expected Result: Server is configured for compression and response payload
     * is compressed using the indicated compression algorithm (deflate in this
     * case)
     *
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testCompressionRefConfig() throws Exception {
        configurationFileName = ConfigurationFiles.WITH_CONFIG_REF.name;
        testName = name.getMethodName();
        HttpResponse httpResponse = null;

        startServer(configurationFileName, testName);

        headerList.clear();
        headerList.add(new BasicHeader(ACCEPT_ENCODING, CompressionType.DEFLATE.name));

        httpResponse = execute(headerList, testName);
        assertCompressed(httpResponse, CompressionType.DEFLATE);
    }

    /**
     * Test that a misconfiguration of the types attribute by attempting to add (+) the same
     * content type more than once, will result in the CWWKT0031W warning message
     *
     * Expected Result: The CWWKT0031W is obtained from the server's logs.
     *
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testCompressionConfigDuplicateTypes() throws Exception {
        configurationFileName = ConfigurationFiles.WITH_CONFIG_DUPLICATE_TYPES.name;
        testName = name.getMethodName();

        startServer(configurationFileName, testName);

        String stringToSearchFor = "CWWKT0029W";

        // There should be a match so fail if there is not.
        assertNotNull("The following string was not found in the access log: " + stringToSearchFor,
                      server.waitForStringInLog(stringToSearchFor));

    }

    /**
     * Test that a misconfiguration of the types attribute by attempting to remove (-) the same
     * content type more than once, will result in the CWWKT0031W warning message
     *
     * Expected Result: The CWWKT0031W is obtained from the server's logs.
     *
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testCompressionConfigRemoveDuplicateTypes() throws Exception {
        configurationFileName = ConfigurationFiles.WITH_CONFIG_DUPLICATE_REMOVAL_TYPES.name;
        testName = name.getMethodName();

        startServer(configurationFileName, testName);

        String stringToSearchFor = "CWWKT0030W";

        // There should be a match so fail if there is not.
        assertNotNull("The following string was not found in the access log: " + stringToSearchFor,
                      server.waitForStringInLog(stringToSearchFor));

    }

    /**
     * Test that a misconfiguration of the types attribute by attempting to add (+) and remove (-)
     * the same content type, will result in the CWWKT0031W warning message
     *
     * Expected Result: The CWWKT0031W is obtained from the server's logs.
     *
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testCompressionConfigAddRemoveDuplicateTypes() throws Exception {
        configurationFileName = ConfigurationFiles.WITH_CONFIG_ADD_REMOVE_DUPLICATE_TYPES.name;
        testName = name.getMethodName();

        startServer(configurationFileName, testName);

        String stringToSearchFor = "CWWKT0031W";

        // There should be a match so fail if there is not.
        assertNotNull("The following string was not found in the access log: " + stringToSearchFor,
                      server.waitForStringInLog(stringToSearchFor));

    }

    /**
     * Test that a misconfiguration of the types attribute by attempting to both overwrite and use
     * the add (+) option, will result in the CWWKT0032W warning message
     *
     * Expected Result: The CWWKT0032W is obtained from the server's logs.
     *
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testCompressionConfigOverWriteAndAddTypes() throws Exception {
        configurationFileName = ConfigurationFiles.WITH_CONFIG_OVERWRITE_ADD_TYPES.name;
        testName = name.getMethodName();

        startServer(configurationFileName, testName);

        String stringToSearchFor = "CWWKT0032W";

        // There should be a match so fail if there is not.
        assertNotNull("The following string was not found in the access log: " + stringToSearchFor,
                      server.waitForStringInLog(stringToSearchFor));

    }

    /**
     * Test that a misconfiguration of the serverPreferredAlgorithm attribute will
     * result in the CWWKT0033W warning message
     *
     * Expected Result: The CWWKT0033W is obtained from the server's logs.
     *
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testCompressionConfigPrefAlgoMisconfiguration() throws Exception {
        configurationFileName = ConfigurationFiles.WITH_CONFIG_PREF_ALGO_MISCONFIG.name;
        testName = name.getMethodName();

        startServer(configurationFileName, testName);

        String stringToSearchFor = "CWWKT0033W";

        // There should be a match so fail if there is not.
        assertNotNull("The following string was not found in the access log: " + stringToSearchFor,
                      server.waitForStringInLog(stringToSearchFor));

    }

    /**
     * Private method to start a server.
     *
     * Please look at publish/files/remoteIPConfig directory for the different server names.
     *
     * @param variation The name of the server that needs to be appended to "-server.xml"
     * @param testname  The name of the test that is starting the server. This allows for easier test
     *                      debug as the server console log will contain the test name.
     * @throws Exception
     */
    private void startServer(String variation, String testName) throws Exception {
        server.setServerConfigurationFile(CONFIGURATION_FILES_DIR + variation + XML_EXTENSION);
        server.startServer(testName + LOG_EXTENSION);
        server.waitForStringInLogUsingMark(APP_STARTED_MESSAGE);
    }

    /**
     * Private method to execute/drive an HTTP request and obtain an HTTP response
     *
     * @param headerList A list of headers to be added in the request
     * @param testName   The name of the test being executed
     * @return The HTTP response for the request
     * @throws Exception
     */
    private HttpResponse execute(List<BasicHeader> headerList, String testName) throws Exception {

        String urlString = "http://" + server.getHostname() + ":" + httpDefaultPort + "/" + APP_NAME + "/" + APP_SERVLET_NAME;
        URI uri = URI.create(urlString);
        Log.info(ME, testName, "Execute request to " + uri);

        HttpGet request = new HttpGet(uri);

        Log.info(ME, testName, "Header list: " + headerList.toString());

        for (BasicHeader header : headerList) {
            request.addHeader(header.getName(), header.getValue());
        }

        HttpResponse response = client.execute(request);
        Log.info(ME, testName, "Returned: " + response.getStatusLine());
        return response;
    }

    /**
     * Get the HTTP response as a String
     *
     * @param response
     * @return A String that contains the response
     * @throws IOException
     */
    private String getResponseAsString(HttpResponse response) throws IOException {

        assertNotNull("HttpResponse is null", response);

        final HttpEntity entity = response.getEntity();
        assertNotNull("Response HttpEntity is null", entity);

        return EntityUtils.toString(entity);
    }

    /**
     * Retrieves the <Header> object corresponding to Content-Encoding,
     * if present. Otherwise, null will be returned.
     *
     * @param response
     * @return
     * @throws IOException
     */
    private Header getContentEncodingHeader(HttpResponse response) throws IOException {

        assertNotNull("HttpResponse is null", response);

        final HttpEntity entity = response.getEntity();
        assertNotNull("Response HttpEntity is null", entity);

        return entity.getContentEncoding();
    }

    /**
     * Asserts that the response is compressed by checking if the string
     * representation contains the APP constant response message. When compressed,
     * this message would not be present as plain text.
     *
     * @throws IOException
     */
    private void assertCompressed(HttpResponse httpResponse, CompressionType type) throws IOException {
        responseString = getResponseAsString(httpResponse);
        assertTrue("Response is not compressed.", responseString != null && !responseString.contains(APP_MESSAGE));

        Header contentEncodingHeader = getContentEncodingHeader(httpResponse);
        assertTrue("Response expected Content-Encoding of [" + type.name + "] but got [" + contentEncodingHeader.getValue() + "]",
                   type.name.equals(contentEncodingHeader.getValue()));

    }

    private void assertNotCompressed(HttpResponse httpResponse) throws IOException {
        //Since it is not encoded, there should not be a Content-Encoding header
        Header contentEncodingHeader = getContentEncodingHeader(httpResponse);
//        if (contentEncodingHeader != null) {
//            assertTrue("Response contained a Content-Encoding that was not Identity", "identity".equalsIgnoreCase(contentEncodingHeader.getValue()));
//        } else {
        assertTrue("Response contained a Content-Encoding", contentEncodingHeader == null);
        //       }

        //This also means that we should be able to see the plain text default response
        //body provided by the application
        responseString = getResponseAsString(httpResponse);
        Log.info(ME, testName, "Response: " + responseString);
        assertTrue("Response does not contain default message", responseString != null && responseString.contains(APP_MESSAGE));

    }

}
