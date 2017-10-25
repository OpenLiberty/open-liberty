/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012, 2017
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.wsspi.http;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

/**
 *
 */
public class URLEscapingUtilsTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule rule = outputMgr;

    /**
     * Test strings with embedded script/dangerous characters:
     * All special characters should be stripped.
     * Test method for {@link com.ibm.wsspi.http.URLEscapingUtils#toSafeString(java.lang.String)}.
     */
    @Test
    public void testToSafeString() {

        String s = "http://safestring.com/";
        String result = URLEscapingUtils.toSafeString(s);
        String safe = "http&#058;&#047;&#047;safestring&#046;com&#047;";
        System.out.println(s);
        System.out.println(result);
        System.out.println(safe);
        Assert.assertEquals("s: " + s, safe, result);

        s = "<>&\"\t!#$%\'()*+,-./:;=?@";
        result = URLEscapingUtils.toSafeString(s);
        safe = "&lt;&gt;&amp;&quot;&#009;&#033;&#035;&#036;&#037;&#039;&#040;&#041;&#042;&#043;&#044;&#045;&#046;&#047;&#058;&#059;&#061;&#063;&#064;";
        Assert.assertEquals("s: " + s, safe, result);

        s = "[\\]^_`{|}~";
        result = URLEscapingUtils.toSafeString(s);
        safe = "&#091;&#092;&#093;&#094;&#095;&#096;&#123;&#124;&#125;&#126;";
        Assert.assertEquals("s: " + s, safe, result);

        s = "http://localhost:58080/sccwebclient/\"><script>alert(46219)</script>";
        result = URLEscapingUtils.toSafeString(s);
        safe = "http&#058;&#047;&#047;localhost&#058;58080&#047;sccwebclient&#047;&quot;&gt;&lt;script&gt;alert&#040;46219&#041;&lt;&#047;script&gt;";
        System.out.println(s);
        System.out.println(result);
        System.out.println(safe);
        Assert.assertEquals("s: " + s, safe, result);
    }
}
