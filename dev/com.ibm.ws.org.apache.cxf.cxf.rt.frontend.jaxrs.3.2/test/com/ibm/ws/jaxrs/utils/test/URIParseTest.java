/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.utils.test;

import java.net.URI;

import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.junit.Test;

import junit.framework.Assert;

/**
 *
 */
public class URIParseTest {


    @Test
    public void testURIs() throws Exception {
        //queryString
        String u = "http://myhost:9080/abc/xyz?abc=123";
        parse(u);
        //domain
        u = "https://www.google.com/";
        parse(u);        
        //domain
        u = "https://www.google.com";
        parse(u);        
        //empty host
        u = "ftp:///abc/xyz#wut";
        parse(u);
        //empty scheme
        u = "//myhost/xyz#wut";
        parse(u);
        u = "ftp://ftp.is.co.za/rfc/rfc1808.txt";
        parse(u);
        u = "gopher://spinaltap.micro.umn.edu/00/Wea%20ther/California/Los%20Angeles";
        parse(u);
        u = "http://www.math.uio.no/faq/compression-faq/part1.html";
        parse(u);
        u = "mailto:mduerst@ifi.unizh.ch";
        parse(u);
        u = "news:comp.infosystems.www.servers.unix/";
        parse(u);
        u = "telnet://melvyl.ucop.edu/";
        parse(u);
        u = "http://00:10:23:45:9080/abc&zyx";
        parse(u);
        u = "http://user@[00:10:23:45:67:78:89:90]/?abc&zyx";
        parse(u);
        u = "http://user@2001:db8:1234::/48/?abc&zyx";
        parse(u);
    }
    
    @Test
    public void testInvalidURIs() throws Exception {
        //queryString
        String u = "http://myhost:908a0/abc/xyz?abc=123";
        parse(u);
        //domain
        u = "https://www.google.com:abc/";
        parse(u);        
        //domain
        u = "https://www.google.com:80";
        parse(u);        
        //empty host
        u = "ftp:///abc*/xyz#wut";
        parse(u);
        //empty scheme
        u = "abc//myhost/xyz#wut";
        parse(u);
        u = "ftpv://ftp.is.co.za/rfc/rfc1808.txt";
        parse(u);
        u = "gopher/://spinaltap.micro.umn.edu/00/Wea%20ther/California/Los%20Angeles";
        parse(u);
        u = "http#://www.math.uio.no/faq/compression-faq/part1.html";
        parse(u);
        u = "mailto:mduerst@ifi.unizh.ch";
        parse(u);
        u = "news:comp.infosystems.www.servers.unix/";
        parse(u);
        u = "telnet?://melvyl.ucop.edu/";
        parse(u);
        u = ":http://00:10:23:45:9080/abc&zyx";
        try {
            parse(u);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // caught proper exception, now need to make sure we get null from parseURI call
            Assert.assertNull(HttpUtils.parseURI(u, true));
            Assert.assertNull(HttpUtils.parseURI(u, false));
        }
        u = "http://";
        try {
            parse(u);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // caught proper exception, now need to make sure we get null from parseURI call
            Assert.assertNull(HttpUtils.parseURI(u, true));
            Assert.assertNull(HttpUtils.parseURI(u, false));
        }
        u = "http://user@2001:db8:1234::/48/?abc&zyx";
        parse(u);
        u = null;
        try {
            parse(u);
            Assert.fail();
        } catch (NullPointerException e) {
            try {
                HttpUtils.parseURI(u, true);
                Assert.fail();
            } catch (NullPointerException ex) {
            }
        }
    }

    private void parse(String u) {
        URI uri = URI.create(u);
        String[] p = HttpUtils.parseURI(u, true);
        Assert.assertEquals(uri.getScheme(), p[0]);
        Assert.assertEquals(uri.getRawAuthority(), p[1]);
        p = HttpUtils.parseURI(u, false);
        Assert.assertEquals(uri.getScheme(), p[0]);
        Assert.assertEquals(uri.getRawPath(), p[1]);
    }
}
