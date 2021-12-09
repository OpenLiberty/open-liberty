/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.web;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.common.AbsoluteOrdering;

public class WebAppTest extends WebAppTestBase {
    // Absolute ordering fragments.

    private static final String absOrder() {     
        return "<absolute-ordering>" +
                   "<name>Fragment1</name>" +
                   "<name>Fragment2</name>" +
               "</absolute-ordering>";
    }

    private static final String othersAbsOrder() {         
        return "<absolute-ordering>" +
                   "<name>Fragment1</name>" +
                   "<others/>" +
                   "<name>Fragment2</name>" +
               "</absolute-ordering>";
    }

    private static final String dupeAbsOrder() { 
        return "<absolute-ordering>" +
                   "<name>Fragment1</name>" +
                   "<name>Fragment2</name>" +
               "</absolute-ordering>" +
               "<absolute-ordering>" +
               "</absolute-ordering>";
    }

    @Test
    public void testWebApp() throws Exception {
        for ( int schemaVersion : WebApp.VERSIONS ) {
            for ( int maxSchemaVersion : WebApp.VERSIONS ) {
                // The WebApp parser uses a maximum schema
                // version of "max(version, WebApp.VERSION_3_0)".
                // Adjust the message expectations accordingly.
                //
                // See: com.ibm.ws.javaee.ddmodel.web.WebAppDDParser

                int effectiveMax;
                if ( maxSchemaVersion < WebApp.VERSION_3_0 ) {
                    effectiveMax = WebApp.VERSION_3_0;
                } else {
                    effectiveMax = maxSchemaVersion;
                }

                String altMessage;
                String[] messages;
                if ( schemaVersion > effectiveMax ) {
                    altMessage = UNPROVISIONED_DESCRIPTOR_VERSION_ALT_MESSAGE;
                    messages = UNPROVISIONED_DESCRIPTOR_VERSION_MESSAGES;
                } else {
                    altMessage = null;
                    messages = null;
                }

                parseWebApp( webApp( schemaVersion, webAppBody() ),
                             maxSchemaVersion,
                             altMessage, messages );
            }
        }
    }
    
    // Servlet 3.0 specific cases ...

    /**
     * Verify that spaces are trimmed from environment entry
     * names but not from environment entry values.
     */
    @Test
    public void testEE6Web30EnvEntryValueWhitespace() throws Exception {
        WebApp webApp = parseWebApp(
            webApp( WebApp.VERSION_3_0,
                    "<env-entry>" +
                        "<env-entry-name> envName </env-entry-name>" +
                        "<env-entry-value> envValue </env-entry-value>" +
                    "</env-entry>") );

        List<EnvEntry> envEntries = webApp.getEnvEntries();
        Assert.assertNotNull(envEntries);
        Assert.assertEquals(envEntries.size(), 1);
        EnvEntry envEntry = envEntries.get(0);
        Assert.assertNotNull(envEntry);
        Assert.assertNotNull(envEntry.getName());
        Assert.assertEquals(envEntry.getName(), "envName");
        Assert.assertNotNull(envEntry.getValue());
        Assert.assertEquals(envEntry.getValue(), " envValue ");
    }

    /**
     * Verify the parsing of an absolute ordering element.
     */
    @Test
    public void testEE6Web30AbsoluteOrderingElement() throws Exception {
        WebApp webApp = parseWebApp( webApp( WebApp.VERSION_3_0, absOrder() ) ); 

        AbsoluteOrdering absOrder = webApp.getAbsoluteOrdering();
        Assert.assertNotNull(absOrder);
        Assert.assertFalse( absOrder.isSetOthers() );
        List<String> beforeOthers = absOrder.getNamesBeforeOthers();
        Assert.assertEquals( beforeOthers.size(), 2 );
        List<String> afterOthers = absOrder.getNamesAfterOthers();
        Assert.assertEquals( afterOthers.size(), 0 );        
    }

    /**
     * Verify the parsing of the absolute ordering element which
     * has an others element.
     */    
    @Test
    public void testEE6Web30OthersElement() throws Exception {
        WebApp webApp = parseWebApp( webApp( WebApp.VERSION_3_0, othersAbsOrder() ) ); 
        
        AbsoluteOrdering absOrder = webApp.getAbsoluteOrdering();
        Assert.assertNotNull(absOrder);
        Assert.assertTrue( absOrder.isSetOthers() );
        List<String> beforeOthers = absOrder.getNamesBeforeOthers();
        Assert.assertEquals( beforeOthers.size(), 1 );
        List<String> afterOthers = absOrder.getNamesAfterOthers();
        Assert.assertEquals( afterOthers.size(), 1 );        
    }

    // The prohibition against having more than one absolute ordering element
    // was added in JavaEE7.
    //
    // Duplicate elements are allowed in EE6.

    /**
     * Verify that duplicate absolute ordering elements are
     * allowed.  These are allowed in Servlet 3.0 / Java EE6.
     */
    @Test
    public void testEE6Web30AbsoluteOrderingDuplicateElements() throws Exception {
        parseWebApp( webApp( WebApp.VERSION_3_0, dupeAbsOrder() ) );
    }

    /**
     * Verify that empty deny-uncovered-http-methods are not allowed.
     */
    @Test
    public void testEE6Web30DenyUncoveredHttpMethods() throws Exception {
        parseWebApp( webApp( WebApp.VERSION_3_0, "<deny-uncovered-http-methods/>" ),
                "unexpected.child.element",
                "CWWKC2259E", "deny-uncovered-http-methods", "web-app", "MyWar.war : WEB-INF/web.xml" );        
    }

    // Servlet 3.1 cases ...

    // The prohibition against having more than one absolute ordering element
    // was added in JavaEE7.
    //
    // Duplicate elements are not allowed in EE7.

    /**
     * Verify that duplicate absolute ordering elements are
     * not allowed.  These were allowed in Servlet 3.0 / Java EE6,
     * but are not allowed in Servlet 3.1 / Java EE7.
     */    
    @Test
    public void testEE7Web31AbsoluteOrderingDuplicates() throws Exception {
        parseWebApp( webApp( WebApp.VERSION_3_1, dupeAbsOrder() ),
                     WebApp.VERSION_3_1,
                     "at.most.one.occurrence",
                     "CWWKC2266E", "absolute-ordering", "MyWar.war : WEB-INF/web.xml" );
    }

    /**
     * Verify that empty deny-uncovered-http-methods are now allowed.
     * They are allowed in Servlet 3.1 / Java EE7.  They were not
     * allowed in Servlet 3.0 / Java EE6.
     */    
    @Test
    public void testEE7Web31DenyUncoveredHttpMethods() throws Exception {
        parseWebApp( webApp( WebApp.VERSION_3_1, "<deny-uncovered-http-methods/>" ),
                     WebApp.VERSION_3_1 );
    }

    /**
     * Verify that non-empty deny-uncovered-http-methods are not allowed.
     */    
    @Test
    public void testEE7Web31DenyUncoveredHttpMethodsNotEmptyType() throws Exception {
        parseWebApp( webApp( WebApp.VERSION_3_1,
                             "<deny-uncovered-http-methods>junk</deny-uncovered-http-methods>" ),
                     WebApp.VERSION_3_1,
                     "unexpected.content",
                     "CWWKC2257E", "deny-uncovered-http-methods", "MyWar.war : WEB-INF/web.xml" );                     
    }
}
