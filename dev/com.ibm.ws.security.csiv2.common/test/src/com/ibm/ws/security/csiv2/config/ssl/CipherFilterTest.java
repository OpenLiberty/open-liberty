/*
 * Copyright 2015, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.security.csiv2.config.ssl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;

import javax.net.ssl.SSLServerSocketFactory;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.security.csiv2.config.ssl.SSLConfig;

/**
 *
 */
public class CipherFilterTest {

//    private static final Pattern p = Pattern.compile("((SSL)|(TLS))_([A-Z0-9]*)(_[\\w]*)*_WITH_(\\w*)_((SHA(\\d*))|(MD5))");

//    private static final Pattern p = Pattern.compile("((SSL)|(TLS))_([A-Z0-9]*)(_[\\w]*)*_WITH_([A-Z0-9]*)(_(\\d*))?([_a-zA-Z0-9]*)?_((SHA(\\d*))|(MD5))");

//    private static final Pattern p = Pattern.compile("((SSL)|(TLS))_([A-Z0-9]*)(_anon)?(_[\\w]*)*(_EXPORT)?_WITH_([A-Z0-9]*)(_(\\d*))?([_a-zA-Z0-9]*)?_((SHA(\\d*))|(MD5))");
//    private static final Pattern p = Pattern.compile("((SSL)|(TLS))_([A-Z0-9]*)(_anon)?(_[\\w]*)?(_EXPORT)?_WITH_([A-Z0-9]*)(_(\\d*))?([_a-zA-Z0-9]*)?_((SHA(\\d*))|(MD5))");
//    private static final Pattern p = Pattern.compile("((SSL)|(TLS))_([A-Z0-9]*)(_anon)?(_[a-zA-Z0-9]*)?(_EXPORT)?_WITH_([A-Z0-9]*)(_(\\d*))?([_a-zA-Z0-9]*)?_((SHA(\\d*))|(MD5))");
//    private static final Pattern p = Pattern.compile("(?:(SSL)|(TLS))_([A-Z0-9]*)(_anon)?(_[a-zA-Z0-9]*)?(_EXPORT)?_WITH_([A-Z0-9]*)(?:_(\\d*))?([_a-zA-Z0-9]*)?_(?:(?:(SHA)(\\d*))|(MD5))");

//    private static final Pattern p = Pattern.compile("((SSL)|(TLS))_([A-Z0-9]*)(_[a-zA-Z0-9]*)?(_[a-zA-Z0-9_]*)?_WITH_(\\w*)_((SHA(\\d*))|(MD5))");

    //Handy to see what your jvm supports
//    @Test
    public void testPattern() {
        System.out.println(SSLConfig.p);
        SSLServerSocketFactory serverSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        String[] suites = serverSocketFactory.getSupportedCipherSuites();
        for (String suite : suites) {
            System.out.println(suite);
            Matcher m = SSLConfig.p.matcher(suite);
            if (m.matches()) {
                List<String> bits = new ArrayList<String>();
                for (int i = 1; i <= m.groupCount(); i++) {
                    bits.add(m.group(i));
                }
                System.out.println(bits);
            } else {
                System.out.println("no match");
            }
        }

    }

    private static final EnumSet<SSLConfig.Options> ALL = EnumSet.allOf(SSLConfig.Options.class);
    private static final String TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256 = "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256";
    private static final String TLS_RSA_WITH_AES_128_CBC_SHA256 = "TLS_RSA_WITH_AES_128_CBC_SHA256";

    private static final EnumSet<SSLConfig.Options> WEAK = EnumSet.of(SSLConfig.Options.integrity, SSLConfig.Options.confidentiality, SSLConfig.Options.establishTrustInTarget,
                                                                      SSLConfig.Options.noexport, SSLConfig.Options.tls);
    private static final String TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA = "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA";
    private static final String TLS_ECDH_ECDSA_WITH_RC4_128_SHA = "TLS_ECDH_ECDSA_WITH_RC4_128_SHA";

    private static final EnumSet<SSLConfig.Options> ANON_STRONG = EnumSet.of(SSLConfig.Options.integrity, SSLConfig.Options.confidentiality, SSLConfig.Options.strong,
                                                                             SSLConfig.Options.noexport, SSLConfig.Options.tls);
    private static final String TLS_DH_anon_WITH_AES_128_CBC_SHA256 = "TLS_DH_anon_WITH_AES_128_CBC_SHA256";

    private static final EnumSet<SSLConfig.Options> SSL_ANON_WEAK = EnumSet.of(SSLConfig.Options.integrity, SSLConfig.Options.confidentiality,
                                                                               SSLConfig.Options.noexport);
    private static final String SSL_DH_anon_WITH_RC4_128_MD5 = "SSL_DH_anon_WITH_RC4_128_MD5";

    private static final EnumSet<SSLConfig.Options> ANON_WEAK = EnumSet.of(SSLConfig.Options.integrity, SSLConfig.Options.confidentiality,
                                                                           SSLConfig.Options.noexport, SSLConfig.Options.tls);
    private static final String TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA = "TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA";

    private static final EnumSet<SSLConfig.Options> PUBLIC = EnumSet.of(SSLConfig.Options.integrity, SSLConfig.Options.establishTrustInTarget,
                                                                        SSLConfig.Options.noexport, SSLConfig.Options.tls);
    private static final String TLS_RSA_WITH_NULL_SHA256 = "TLS_RSA_WITH_NULL_SHA256";

    private static final EnumSet<SSLConfig.Options> EXPORT = EnumSet.of(SSLConfig.Options.integrity,
                                                                        SSLConfig.Options.confidentiality, SSLConfig.Options.establishTrustInTarget, SSLConfig.Options.tls);
    private static final String TLS_KRB5_EXPORT_WITH_RC4_40_SHA = "TLS_KRB5_EXPORT_WITH_RC4_40_SHA";

    private static final EnumSet<SSLConfig.Options> SSL_ANON_EXPORT = EnumSet.of(SSLConfig.Options.integrity,
                                                                                 SSLConfig.Options.confidentiality);
    private static final String SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA = "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA";

    private static final String[] CHOICES = { TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,
                                             TLS_RSA_WITH_AES_128_CBC_SHA256,
                                             TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                                             TLS_ECDH_ECDSA_WITH_RC4_128_SHA,
                                             TLS_DH_anon_WITH_AES_128_CBC_SHA256,
                                             SSL_DH_anon_WITH_RC4_128_MD5,
                                             TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA,
                                             TLS_RSA_WITH_NULL_SHA256,
                                             TLS_KRB5_EXPORT_WITH_RC4_40_SHA,
                                             SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA,
    };

    @Test
    public void testGetOptions() {
        Assert.assertEquals(ALL, SSLConfig.getOptions(TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256));
        Assert.assertEquals(ALL, SSLConfig.getOptions(TLS_RSA_WITH_AES_128_CBC_SHA256));
        Assert.assertEquals(WEAK, SSLConfig.getOptions(TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA));
        Assert.assertEquals(WEAK, SSLConfig.getOptions(TLS_ECDH_ECDSA_WITH_RC4_128_SHA));
        Assert.assertEquals(ANON_STRONG, SSLConfig.getOptions(TLS_DH_anon_WITH_AES_128_CBC_SHA256));
        Assert.assertEquals(SSL_ANON_WEAK, SSLConfig.getOptions(SSL_DH_anon_WITH_RC4_128_MD5));
        Assert.assertEquals(ANON_WEAK, SSLConfig.getOptions(TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA));
        Assert.assertEquals(PUBLIC, SSLConfig.getOptions(TLS_RSA_WITH_NULL_SHA256));
        Assert.assertEquals(EXPORT, SSLConfig.getOptions(TLS_KRB5_EXPORT_WITH_RC4_40_SHA));
        Assert.assertEquals(SSL_ANON_EXPORT, SSLConfig.getOptions(SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA));
    }
/*
    @Test
    public void testGetCompatible() {
        check(new String[] { TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,
                            TLS_RSA_WITH_AES_128_CBC_SHA256 }, ALL, ALL);

        check(new String[] { TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                            TLS_ECDH_ECDSA_WITH_RC4_128_SHA }, WEAK, WEAK);

        check(new String[] { TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,
                            TLS_RSA_WITH_AES_128_CBC_SHA256,
                            TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                            TLS_ECDH_ECDSA_WITH_RC4_128_SHA }, ALL, WEAK);

        check(new String[] { TLS_DH_anon_WITH_AES_128_CBC_SHA256 }, ANON_STRONG, ANON_STRONG);

        check(new String[] { TLS_DH_anon_WITH_AES_128_CBC_SHA256,
                            SSL_DH_anon_WITH_RC4_128_MD5,
                            TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA }, ANON_STRONG, SSL_ANON_WEAK);

        check(new String[] { TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,
                            TLS_RSA_WITH_AES_128_CBC_SHA256,
                            TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                            TLS_ECDH_ECDSA_WITH_RC4_128_SHA,
                            TLS_DH_anon_WITH_AES_128_CBC_SHA256,
                            TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA,
                            TLS_RSA_WITH_NULL_SHA256 }, ALL, EnumSet.of(SSLConfig.Options.integrity,
                                                                        SSLConfig.Options.noexport, SSLConfig.Options.tls));

    }

    private void check(String[] expected, EnumSet<SSLConfig.Options> supported, EnumSet<SSLConfig.Options> required) {
        String[] actual = SSLConfig.getCompatibleCipherSuites(CHOICES, supported, required);
        Assert.assertEquals("supported: " + supported + " required: " + required, Arrays.asList(expected), Arrays.asList(actual));
    }
*/
}
