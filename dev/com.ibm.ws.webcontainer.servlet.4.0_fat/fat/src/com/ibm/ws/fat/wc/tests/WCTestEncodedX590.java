/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/*
 * Testing the URL encoded client certificate that may send from front servers.
 * The encoded client cert is already wrapped with the header and footer along with the encoded cert.
 * In that case, there is no need to armor it (i.e wrap with header and footer), just URL decode and return.
 */

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WCTestEncodedX590 {

    private static final Logger LOG = Logger.getLogger(WCTestEncodedX590.class.getName());
    private final String GOOD_ENCODED_CERT = "-----BEGIN%20CERTIFICATE-----%0AMIIDODCCAiCgAwIBAgIIHbO76YfCH1cwDQYJKoZIhvcNAQELBQAwOjELMAkGA1UE%0ABhMCVVMxDDAKBgNVBAoTA0lCTTEMMAoGA1UECxMDVFdTMQ8wDQYDVQQDEwZDbGll%0AbnQwHhcNMTUxMTAzMTYyODM1WhcNMzUxMDMwMTYyODM1WjA6MQswCQYDVQQGEwJV%0AUzEMMAoGA1UEChMDSUJNMQwwCgYDVQQLEwNUV1MxDzANBgNVBAMTBkNsaWVudDCC%0AASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAPU68OGswFeZypQ8Doty0XtE%0A5UhUFjCf%2Fpp7mhCTnY7sYL5ZZJVdlInxivyLwB9gZIpqcIF1argfLBQ9QJAlIti5%0AnZ6NPyAM9NavLM8EL5r2xXnFHmJleGX9ph13bGrlMRafz2A0o6CdoowfnbN1WUtn%0AzzuhCYMAHvQ43FloxEgiKtyJKtH%2BJkMMokPGTZY8G4x9x8Gna6xnTo9rtatvVd3r%0AONoIMTIQ8wITjVtNMFCmvDrcgV2gu9r2xYNvTTv%2Fg7qrmV95MFMut8ztJ3ap5KUA%0AdeoyOvB09qsuP1t8xNApyZ9b5LMdzfJkN0Dv4cRWNjOq5Z5o3J32QrIbRlU2VGkC%0AAwEAAaNCMEAwHQYDVR0OBBYEFGdrGhj4SscHjalpYC2KtlkOJsnCMB8GA1UdIwQY%0AMBaAFGdrGhj4SscHjalpYC2KtlkOJsnCMA0GCSqGSIb3DQEBCwUAA4IBAQBjbKHw%0ASlvWtCI9ByOKaRb4l6MzVqs3n8jvzBdaC0rzQAjAAoax9fNvhzSzqVRNykdqqQLz%0ABy0kerXw5wPBFWVDff3gxPna5XyMoJyDj0YICL3fAubjzB9aPJu0y1vwgnK3G39N%0Ax0MODjg2LQMb9MvpEHOg0JQbBBxMDLNrFqZLO2L2VnYh7qL%2BfisBVwxslCLeyNCd%0ATkfL%2F%2Bpo%2FF%2Bzi%2F0sfAUwRdfgMm%2FAKAGzwMQufOYeKCgMULtq14QAJQnLmq4M%2FM00%0AC5QyeYtdaTou%2BMsLmoa1tkq2VSDVxAcktJyRSRsox36G7EHDLV4U2gtR6xczNEjw%0A2%2Bsj772FjdAMXRSR%0A-----END%20CERTIFICATE-----%0A\n";
    private final String BAD_ENCODED_CERT = "-----BEGIN%20%0MIIDODCCAiCgAwIBAgIIHbO76YfCH1cwDQYJKoZIhvcNAQELBQAwOjELMAkGA1UE%0ABhMCVVMxDDAKBgNVBAoTA0lCTTEMMAoGA1UECxMDVFdTMQ8wDQYDVQQDEwZDbGll%0AbnQwHhcNMTUxMTAzMTYyODM1WhcNMzUxMDMwMTYyODM1WjA6MQswCQYDVQQGEwJV%0AUzEMMAoGA1UEChMDSUJNMQwwCgYDVQQLEwNUV1MxDzANBgNVBAMTBkNsaWVudDCC%0AASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAPU68OGswFeZypQ8Doty0XtE%0A5UhUFjCf%2Fpp7mhCTnY7sYL5ZZJVdlInxivyLwB9gZIpqcIF1argfLBQ9QJAlIti5%0AnZ6NPyAM9NavLM8EL5r2xXnFHmJleGX9ph13bGrlMRafz2A0o6CdoowfnbN1WUtn%0AzzuhCYMAHvQ43FloxEgiKtyJKtH%2BJkMMokPGTZY8G4x9x8Gna6xnTo9rtatvVd3r%0AONoIMTIQ8wITjVtNMFCmvDrcgV2gu9r2xYNvTTv%2Fg7qrmV95MFMut8ztJ3ap5KUA%0AdeoyOvB09qsuP1t8xNApyZ9b5LMdzfJkN0Dv4cRWNjOq5Z5o3J32QrIbRlU2VGkC%0AAwEAAaNCMEAwHQYDVR0OBBYEFGdrGhj4SscHjalpYC2KtlkOJsnCMB8GA1UdIwQY%0AMBaAFGdrGhj4SscHjalpYC2KtlkOJsnCMA0GCSqGSIb3DQEBCwUAA4IBAQBjbKHw%0ASlvWtCI9ByOKaRb4l6MzVqs3n8jvzBdaC0rzQAjAAoax9fNvhzSzqVRNykdqqQLz%0ABy0kerXw5wPBFWVDff3gxPna5XyMoJyDj0YICL3fAubjzB9aPJu0y1vwgnK3G39N%0Ax0MODjg2LQMb9MvpEHOg0JQbBBxMDLNrFqZLO2L2VnYh7qL%2BfisBVwxslCLeyNCd%0ATkfL%2F%2Bpo%2FF%2Bzi%2F0sfAUwRdfgMm%2FAKAGzwMQufOYeKCgMULtq14QAJQnLmq4M%2FM00%0AC5QyeYtdaTou%2BMsLmoa1tkq2VSDVxAcktJyRSRsox36G7EHDLV4U2gtR6xczNEjw%0A2%2Bsj772FjdAMXRSR%0A-----END%20CERTIFICATE-----%0A\n";
    private final String NO_ENCODED_CER = "MIIDODCCAiCgAwIBAgIIHbO76YfCH1cwDQYJKoZIhvcNAQELBQAwOjELMAkGA1UEBhMCVVMxDDAKBgNVBAoTA0lCTTEMMAoGA1UECxMDVFdTMQ8wDQYDVQQDEwZDbGllbnQwHhcNMTUxMTAzMTYyODM1WhcNMzUxMDMwMTYyODM1WjA6MQswCQYDVQQGEwJVUzEMMAoGA1UEChMDSUJNMQwwCgYDVQQLEwNUV1MxDzANBgNVBAMTBkNsaWVudDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAPU68OGswFeZypQ8Doty0XtE5UhUFjCf/pp7mhCTnY7sYL5ZZJVdlInxivyLwB9gZIpqcIF1argfLBQ9QJAlIti5nZ6NPyAM9NavLM8EL5r2xXnFHmJleGX9ph13bGrlMRafz2A0o6CdoowfnbN1WUtnzzuhCYMAHvQ43FloxEgiKtyJKtH+JkMMokPGTZY8G4x9x8Gna6xnTo9rtatvVd3rONoIMTIQ8wITjVtNMFCmvDrcgV2gu9r2xYNvTTv/g7qrmV95MFMut8ztJ3ap5KUAdeoyOvB09qsuP1t8xNApyZ9b5LMdzfJkN0Dv4cRWNjOq5Z5o3J32QrIbRlU2VGkCAwEAAaNCMEAwHQYDVR0OBBYEFGdrGhj4SscHjalpYC2KtlkOJsnCMB8GA1UdIwQYMBaAFGdrGhj4SscHjalpYC2KtlkOJsnCMA0GCSqGSIb3DQEBCwUAA4IBAQBjbKHwSlvWtCI9ByOKaRb4l6MzVqs3n8jvzBdaC0rzQAjAAoax9fNvhzSzqVRNykdqqQLzBy0kerXw5wPBFWVDff3gxPna5XyMoJyDj0YICL3fAubjzB9aPJu0y1vwgnK3G39Nx0MODjg2LQMb9MvpEHOg0JQbBBxMDLNrFqZLO2L2VnYh7qL+fisBVwxslCLeyNCdTkfL/+po/F+zi/0sfAUwRdfgMm/AKAGzwMQufOYeKCgMULtq14QAJQnLmq4M/M00C5QyeYtdaTou+MsLmoa1tkq2VSDVxAcktJyRSRsox36G7EHDLV4U2gtR6xczNEjw2+sj772FjdAMXRSR";

    @Server("servlet40_X590CertServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add TestX590EncodedCert.war to the server if not already present.");

        ShrinkHelper.defaultDropinApp(server, "TestX590EncodedCert.war", "encodedcert.servlet");

        LOG.info("Setup : complete, ready for Tests");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WCTestEncodedX590.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE8086E"); // expected error from test_Bad_EncodedX590ClientCert
        }
    }

    /**
     * send request with GOOD encoded client cert
     * see server.xml for additional setup in order for $WS header to work
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_Good_EncodedX590ClientCert() throws Exception {
        String expectedResponse = "FOUND Cert DN";

        LOG.info("\n #################### [test_Good_EncodedX590ClientCert]: BEGIN ####################");

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            String URLString = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestX590EncodedCert/TestCertificate";
            LOG.info("\nSending Request: [" + URLString + "]");

            HttpGet httpGet = new HttpGet(URLString);
            httpGet.setHeader("$WSCC", GOOD_ENCODED_CERT);

            try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
                System.out.println(response.getCode() + " " + response.getReasonPhrase());
                LOG.info("\nResponse code: " + response.getCode());

                HttpEntity entity = response.getEntity();
                String content = EntityUtils.toString(entity).trim();

                //fully consumed the underlying so the connection can be safely re-used
                EntityUtils.consume(entity);
                LOG.info("\nResponse content: " + content);

                assertTrue("Response did not contain expected response: ", content.contains(expectedResponse));
            }
        }

        LOG.info("\n #################### [test_Good_EncodedX590ClientCert] FINISH #################### ");
    }

    /**
     * send request with BAD encoded client cert (missing some data)
     * see server.xml for additional setup in order for $WS header to work
     *
     * Expected Error/warning SRVE8086E: Invalid peer certificate.
     * Expected FFDC java.security.cert.CertificateException
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "java.security.cert.CertificateException" })
    public void test_Bad_EncodedX590ClientCert() throws Exception {
        String expectedResponse = "Can NOT obtain Cert";

        LOG.info("\n #################### [test_Bad_EncodedX590ClientCert]: BEGIN ####################");

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            String URLString = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestX590EncodedCert/TestCertificate";
            LOG.info("\nSending Request: [" + URLString + "]");

            HttpGet httpGet = new HttpGet(URLString);
            httpGet.setHeader("$WSCC", BAD_ENCODED_CERT);

            try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
                System.out.println(response.getCode() + " " + response.getReasonPhrase());
                LOG.info("\nResponse code: " + response.getCode());

                HttpEntity entity = response.getEntity();
                String content = EntityUtils.toString(entity).trim();

                //fully consumed the underlying so the connection can be safely re-used
                EntityUtils.consume(entity);
                LOG.info("\nResponse content [" + content + "]");

                assertTrue("Response did not contain expected response: " + expectedResponse, content.equals(expectedResponse));
            }

        }

        LOG.info("\n #################### [test_Bad_EncodedX590ClientCert]  FINISH ####################");
    }

    /*
     * test normal cert with no percent encoding, no header and footer
     */
    @Test
    @Mode(TestMode.FULL)
    public void test_None_EncodedX590ClientCert() throws Exception {
        String expectedResponse = "FOUND Cert DN";

        LOG.info("\n #################### [test_None_EncodedX590ClientCert]: BEGIN ####################");

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            String URLString = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestX590EncodedCert/TestCertificate";
            LOG.info("\nSending Request: [" + URLString + "]");

            HttpGet httpGet = new HttpGet(URLString);
            httpGet.setHeader("$WSCC", NO_ENCODED_CER);

            try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
                System.out.println(response.getCode() + " " + response.getReasonPhrase());
                LOG.info("\nResponse code: " + response.getCode());

                HttpEntity entity = response.getEntity();
                String content = EntityUtils.toString(entity).trim();

                //fully consumed the underlying so the connection can be safely re-used
                EntityUtils.consume(entity);
                LOG.info("\nResponse content: " + content);

                assertTrue("Response did not contain expected response: ", content.contains(expectedResponse));
            }
        }

        LOG.info("\n #################### [test_None_EncodedX590ClientCert] FINISH #################### ");
    }
}
