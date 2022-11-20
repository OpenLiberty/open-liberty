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
package com.ibm.ws.http.channel.test.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.http.channel.internal.cookies.SameSiteCookieUtils;

import test.common.SharedOutputManager;

/**
 * Test methods from the SameSiteCookieUtils class.
 */
public class SameSiteCookieUtilsTest {

    private static SharedOutputManager outputMgr;

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    /**
     *
     * Tests compatibility of SameSite=None on Chrome user agents.
     *
     * Specification - Versions of Chrome from Chrome 51 to Chrome 66 (inclusive on both
     * ends) will reject a cookie with SameSite=None. This also affects older versions of Chromium
     * derived browsers as well as Android WebView. Prior to Chrome 51, SameSite was ignored and
     * cookies were treated as if SameSite=None.
     */
    @Test
    public void testChromeCompatibility() {
        try {

            String userAgent;

            //Prior to Chrome 51, SameSite is ignored, therefore these user-agents are not strictly incompatible.
            //Expected to return false

            //Chrome 50
            userAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36";
            assertFalse(SameSiteCookieUtils.isSameSiteNoneIncompatible(userAgent));

            //Test Chrome Versions 51 - 66, including WebView
            //Expected to return true
            String[] incompatibleChromeUserAgents = new String[] {
                                                                   //Chrome 51
                                                                   "Mozilla/5.0 doogiePIM/1.0.4.2 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.84 Safari/537.36",
                                                                   //Chrome 52
                                                                   "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36",
                                                                   //Chrome 53
                                                                   "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2883.87 Safari/537.36",
                                                                   //Chrome 54
                                                                   "Mozilla/5.0 Chrome/54.0.2840.99 Safari/537.36",
                                                                   //Chrome 55
                                                                   "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95 Safari/537.36",
                                                                   //Chrome 56
                                                                   "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.76 Safari/537.36",
                                                                   //Chrome 57
                                                                   "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36",
                                                                   //Chrome 58
                                                                   "Mozilla/5.0 (Linux; Android 8.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Klar/1.0 Chrome/58.0.3029.121 Mobile Safari/537.36",
                                                                   //Chrome 59
                                                                   "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36",
                                                                   //Chrome 60
                                                                   "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.78 Safari/537.36",
                                                                   //Chrome 61
                                                                   "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.79 Safari/537.36",
                                                                   //Chrome 62
                                                                   "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3165.0 Safari/537.36",
                                                                   //Chrome 63
                                                                   "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3213.3 Safari/537.36",
                                                                   //Chrome 64
                                                                   "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.119 Safari/537.36",
                                                                   //Chrome 65"
                                                                   "Mozilla/5.0 (Win) AppleWebKit/1000.0 (KHTML, like Gecko) Chrome/65.663 Safari/1000.01",
                                                                   //Chrome 66
                                                                   "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3334.0 Safari/537.36",
                                                                   //Chrome 66 Webview
                                                                   "Mozilla/5.0 (Linux; Android 4.4.4; One Build/KTU84L.H4) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/66.0.0.0 Mobile Safari/537.36 [FB_IAB/FB4A;FBAV/28.0.0.20.16;]"

            };

            for (String currentUserAgent : incompatibleChromeUserAgents) {
                assertTrue(SameSiteCookieUtils.isSameSiteNoneIncompatible(currentUserAgent));
            }

            //Versions Chrome 67 forward support SameSite=None. Test a supported newer version.
            //Expected to return false.
            //Chrome 67
            userAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.2526.73 Safari/537.36";
            assertFalse(SameSiteCookieUtils.isSameSiteNoneIncompatible(userAgent));

            //Test UC browser before 12.13.2
            //Test UC browser on 12.13.2
            //Test UC browser after 12.13.2
            //test bad format of product

        } catch (Throwable t) {
            outputMgr.failWithThrowable("testChromeCompatibility", t);
        }
    }

    /**
     *
     * Tests compatibility of SameSite=None on UC user agents.
     *
     * Specification - Versions of UC Browser on Android prior to version 12.13.2 will
     * reject a cookie with SameSite=None. This behavior was corrected in newer versions of
     * UC Browser.
     *
     */
    @Test
    public void testUCBrowserCompatibility() {
        try {

            String userAgent;

            //Test a version prior to 12.13.2
            //UC Browser @ 10.7
            userAgent = "UCWEB/2.0 (MIDP-2.0; U; Adr 4.0.4; en-US; ZTE_U795) U2/1.0.0 UCBrowser/10.7.6.805 U2/1.0.0 Mobile";
            assertTrue(SameSiteCookieUtils.isSameSiteNoneIncompatible(userAgent));

            //Test a version on version 12, but before 12.13.2
            userAgent = "Mozilla/5.0 (Linux; U; Android 7.1.1; en-US; Lenovo K8 Note Build/NMB26.54-74) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.2987.108 UCBrowser/12.0.0.1088 Mobile Safari/537.36";
            assertTrue(SameSiteCookieUtils.isSameSiteNoneIncompatible(userAgent));

            //Border test on version where fix was delivered
            //UC Browser @ 12.13.2
            userAgent = "Mozilla/5.0 (Linux; U; Android 8.0.0; en-US; Pixel XL Build/OPR3.170623.007) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 UCBrowser/12.13.2.1005 U3/0.8.0 Mobile Safari/534.30";
            assertFalse(SameSiteCookieUtils.isSameSiteNoneIncompatible(userAgent));

            //Test on a version after fix was delivered
            //UC Browser @ 12.13.4
            userAgent = "Mozilla/5.0 (Linux; U; Android 8.0.0; en-US; Pixel XL Build/OPR3.170623.007) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 UCBrowser/12.13.4.1005 U3/0.8.0 Mobile Safari/534.30";
            assertFalse(SameSiteCookieUtils.isSameSiteNoneIncompatible(userAgent));

            //Test on a future major version
            //UC Browser @ 13.0.8
            userAgent = "Mozilla/5.0 (Linux; U; Android 9; en-US; Redmi Note 5 Pro Build/PKQ1.180904.001) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.2987.108 UCBrowser/13.0.8.1291 Mobile Safari/537.36";
            assertFalse(SameSiteCookieUtils.isSameSiteNoneIncompatible(userAgent));

        } catch (Throwable t) {
            outputMgr.failWithThrowable("testUCBrowserCompatibility", t);
        }
    }

    /**
     *
     * Tests compatibility of SameSite=None on MacOS and iOS.
     *
     * Specification - Versions of Safari and embedded browsers on MACOS 10.14 and all
     * browsers on iOS12 will erroneously treat cookies marked with SameSite=None as
     * if they were marked SameSite=Strict. This has been fixed on newer versions of
     * iOS and MacOS.
     *
     */
    @Test
    public void testMacOSAndIOSCompatibility() {
        try {

            String userAgent;

            //Test Safari @ MacOS 10.14 - Expected to return true.
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.0 Safari/605.1.15";
            assertTrue(SameSiteCookieUtils.isSameSiteNoneIncompatible(userAgent));

            //Test Embedded @ MacOS 10.14 - Expected to return true
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14) AppleWebKit/537.36 (KHTML, like Gecko)";
            assertTrue(SameSiteCookieUtils.isSameSiteNoneIncompatible(userAgent));

            //Test Safari  @ MacOS after 10.14 - Expected to return false
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15) AppleWebKit/601.1.39 (KHTML, like Gecko) Version/10.1.2 Safari/601.1.39";
            assertFalse(SameSiteCookieUtils.isSameSiteNoneIncompatible(userAgent));

            //Test Safari @ MacOS 10.15.1 - major.minor.release version format on a supported user-agent
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Safari/605.1.15";
            assertFalse(SameSiteCookieUtils.isSameSiteNoneIncompatible(userAgent));

            //Test Multiple Browsers in iOS 12 - Expected to return true for any browser.
            // Safari
            userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 12_0 like Mac OS X) AppleWebKit/ 604.1.21 (KHTML, like Gecko) Version/ 12.0 Mobile/17A6278a Safari/602.1.26";
            assertTrue(SameSiteCookieUtils.isSameSiteNoneIncompatible(userAgent));
            //Chrome
            userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 12_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/70.0.3538.75 Mobile/15E148 Safari/605.1";
            assertTrue(SameSiteCookieUtils.isSameSiteNoneIncompatible(userAgent));
            //Firefox
            userAgent = "Mozilla/5.0 (iPad; CPU OS 12_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) FxiOS/13.2b11866 Mobile/16A366 Safari/605.1.15";
            assertTrue(SameSiteCookieUtils.isSameSiteNoneIncompatible(userAgent));

            //Test a browser after iOS version 12 (@ IOS 13)
            userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/66.6 Mobile/14A5297c Safari/602.1";
            assertFalse(SameSiteCookieUtils.isSameSiteNoneIncompatible(userAgent));

            //TODO: considerations
            //Test user agent on Safari and macOS before 10.14

            //Test user agent on MacEmbedded browser and macOS before 10.14
            //Test user agent on MacEmbedded browser and macOS after 10.14
            //Test user agent on iOS before 12

        } catch (Throwable t) {
            outputMgr.failWithThrowable("testMACOSAndIOSCompatibility", t);
        }
    }

}