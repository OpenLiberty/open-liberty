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
package com.ibm.ws.http.channel.internal.cookies;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.http.channel.internal.HttpMessages;

/**
 *
 */
public class SameSiteCookieUtils {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(CookieUtils.class,
                                                         HttpMessages.HTTP_TRACE_NAME,
                                                         HttpMessages.HTTP_BUNDLE);

    private static final int UNSUPPORTED_IOS_VERSION = 12;
    private static final int UNSUPPORTED_MACOS_MAJOR_VERSION = 10;
    private static final int UNSUPPORTED_MACOS_MINOR_VERSION = 14;
    private static final Pattern IosVersion = Pattern.compile(".*\\(ip.+; cpu .*os (\\d+)[_\\d]*.*\\) applewebkit\\/.*");
    private static final Pattern MacOsVersion = Pattern.compile(".*\\(macintosh;.*mac os x (\\d+)_(\\d+)[_\\d]*.*\\) applewebkit\\/.*");
    private static final Pattern Safari = Pattern.compile(".*version\\/.* safari\\/.*");
    private static final Pattern MacEmbeddedBrowser = Pattern.compile("^mozilla\\/[\\.\\d]+ \\(macintosh;.*mac os x [_\\d]+\\) applewebkit\\/[\\.\\d]+ \\(khtml, like gecko\\)$");

    private static final Pattern ucBrowser = Pattern.compile(".*ucbrowser/.*");
    private static final Pattern ucBrowserVersion = Pattern.compile(".*ucbrowser/(\\d+)\\.(\\d+)\\.(\\d+)[\\.\\d]*.*");

    private static final int UC_MAJOR_VERSION = 12;
    private static final int UC_MINOR_VERSION = 13;
    private static final int UC_BUILD_VERSION = 2;

    private static final Pattern chromiumBased = Pattern.compile(".*chrom(e|ium).*");
    private static final Pattern chromiumVersion = Pattern.compile(".*chrom[^\\/]+\\/(\\d+)[\\.\\d]*.*");

    private static final int CHROMIUM_MIN_MAJOR_VERSION = 51;
    private static final int CHROMIUM_MAX_MAJOR_VERSION = 66;

    /**
     * Default constructor for the utility class.
     */
    private SameSiteCookieUtils() {
        // nothing to do
    }

    /**
     * Compares the request's user agent against a regex of known incompatible
     * clients for the SameSite=None cookie attribute. If the client is incompatible,
     * this method will return true.
     *
     * @param userAgent
     * @return
     */
    public static boolean isSameSiteNoneIncompatible(String userAgent) {

        String normalizedUserAgent = userAgent.toLowerCase().trim();
        return hasWebKitSameSiteBug(normalizedUserAgent) || dropsUnrecognizedSameSiteAttribute(normalizedUserAgent);
    }

    /**
     * Compares the request's user agent against a regex of known clients that have
     * the Web Kit SameSite bug. Versions of Safari and embedded browsers on MACOS 10.14
     * and all browsers on iOS 12. These versions will erroneously treat cookies marked with
     * SameSite=None as if they were marked with SameSite=Strict.
     *
     * @param userAgent
     * @return
     */
    private static boolean hasWebKitSameSiteBug(String userAgent) {
        return isIosVersion(UNSUPPORTED_IOS_VERSION, userAgent) ||
               (isMacOSVersion(UNSUPPORTED_MACOS_MAJOR_VERSION, UNSUPPORTED_MACOS_MINOR_VERSION, userAgent) && (isSafari(userAgent) || isMacEmbeddedBrowser(userAgent)));
    }

    /**
     * Compares the request's user agent against a regex to identify if a specific IOS version
     * is being utilized.
     *
     * @param major
     * @param userAgent
     * @return
     */
    private static boolean isIosVersion(int major, String userAgent) {
        boolean isSameVersion = false;

        try {
            Matcher m = IosVersion.matcher(userAgent);
            if (m.matches()) {
                int version = Integer.parseInt(m.group(1));
                isSameVersion = (version == major);
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, SameSiteCookieUtils.class.getName() + ".isIosVersion", "1");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while parsing IOS User-Agent; " + userAgent);
            }
        }

        return isSameVersion;
    }

    /**
     * Compares the request's user agent against a regex to identify if the MAC OS version
     * matches a specific version (major.minor format).
     *
     * @param major
     * @param minor
     * @param userAgent
     * @return
     */
    private static boolean isMacOSVersion(int major, int minor, String userAgent) {
        boolean isSameVersion = false;
        try {
            Matcher m = MacOsVersion.matcher(userAgent);

            if (m.matches()) {
                int majorVersion = Integer.parseInt(m.group(1));
                int minorVersion = Integer.parseInt(m.group(2));

                isSameVersion = (majorVersion == major && minorVersion == minor);
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, SameSiteCookieUtils.class.getName() + ".isMacOSVersion", "1");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while parsing IOS User-Agent; " + userAgent);
            }
        }
        return isSameVersion;
    }

    private static boolean isSafari(String userAgent) {
        return Safari.matcher(userAgent).matches() && !isChromiumBased(userAgent);
    }

    private static boolean isMacEmbeddedBrowser(String userAgent) {
        return MacEmbeddedBrowser.matcher(userAgent).matches();
    }

    /**
     * Compares the request's user agent against a regex of known clients that
     * incorrectly handle the SameSite attribute.
     *
     * Versions of Chrome from Chrome 51 to Chrome 66 (inclusive on both ends)
     * will reject cookies with SameSite=None. This also affects older versions
     * of Chromium derived browsers, as well as Android WebView.
     *
     * Versions of UC browser on Android prior to version 12.13.2 reject cookies
     * with SameSite=None.
     *
     * @param userAgent
     * @return
     */
    private static boolean dropsUnrecognizedSameSiteAttribute(String userAgent) {

        boolean isUnsupportedUserAgent = false;

        if (isUcBrowser(userAgent)) {
            isUnsupportedUserAgent = !isUcBrowserSameSiteCompatible(userAgent);
        }

        else if (isChromiumBased(userAgent)) {
            isUnsupportedUserAgent = !isChromiumVersionSameSiteCompatible(userAgent);
        }

        return isUnsupportedUserAgent;
    }

    /**
     * Evaluates the User-Agent to verify if it is a UC browser client.
     *
     * @param userAgent
     * @return
     */
    private static boolean isUcBrowser(String userAgent) {
        return ucBrowser.matcher(userAgent).matches();
    }

    /**
     * Evaluates a UC Browser User-Agent to identify if the version is
     * supports the use of the SameSite=None attribute. Any version prior to
     * version 12.13.2 will be considered to be unsupported.
     *
     * @param userAgent
     * @return
     */
    private static boolean isUcBrowserSameSiteCompatible(String userAgent) {

        boolean supportsSameSite = false;
        try {
            Matcher m = ucBrowserVersion.matcher(userAgent);
            if (m.matches()) {

                int majorVersion = Integer.parseInt(m.group(1));
                int minorVersion = Integer.parseInt(m.group(2));
                int buildVersion = Integer.parseInt(m.group(3));

                if (majorVersion != UC_MAJOR_VERSION) {
                    supportsSameSite = majorVersion > UC_MAJOR_VERSION;
                } else if (minorVersion != UC_MINOR_VERSION) {
                    supportsSameSite = minorVersion > UC_MINOR_VERSION;
                }

                else {
                    supportsSameSite = buildVersion >= UC_BUILD_VERSION;
                }

            }
        } catch (Exception e) {
            FFDCFilter.processException(e, SameSiteCookieUtils.class.getName() + ".isUcBrowserSameSiteCompatible", "1");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while parsing UC based User-Agent; " + userAgent);
            }
            supportsSameSite = true;
        }

        return supportsSameSite;

    }

    /**
     * Evaluates the User-Agent to verify if it is a Chromium based client.
     *
     * @param userAgent
     * @return
     */
    private static boolean isChromiumBased(String userAgent) {
        return chromiumBased.matcher(userAgent).matches();
    }

    /**
     * Evaluates a Chromium based User-Agent to identify if the version
     * supports the use of the SameSite=None attribute. Any version from
     * version 51 to 66 (inclusive on both ends) will be considered to be
     * unsupported. This applies to both Chrome and Chromium derived browsers.
     *
     * @param userAgent
     * @return
     */
    private static boolean isChromiumVersionSameSiteCompatible(String userAgent) {
        boolean supportsSameSite = true;

        try {
            Matcher m = chromiumVersion.matcher(userAgent);
            if (m.matches()) {
                int majorVersion = Integer.parseInt(m.group(1));
                supportsSameSite = !((majorVersion >= CHROMIUM_MIN_MAJOR_VERSION) &&
                                     (majorVersion <= CHROMIUM_MAX_MAJOR_VERSION));

            }
        } catch (Exception e) {
            FFDCFilter.processException(e, SameSiteCookieUtils.class.getName() + ".isChromiumVersionSameSiteCompatible", "1");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while parsing Chromium based User-Agent; " + userAgent);
            }
        }

        return supportsSameSite;
    }

}
