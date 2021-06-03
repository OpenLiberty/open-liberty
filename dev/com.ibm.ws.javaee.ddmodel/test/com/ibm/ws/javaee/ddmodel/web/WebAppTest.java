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
import com.ibm.ws.javaee.ddmodel.DDParser;

/**
 * Servlet deployment descriptor parse tests.
 */
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
    
    // Servlet 3.0 cases ...

    @Test(expected = DDParser.ParseException.class)    
    public void testEE6Web22() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse( webApp(WebApp.VERSION_2_2) );
    }

    @Test(expected = DDParser.ParseException.class)    
    public void testEE6Web23() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse( webApp(WebApp.VERSION_2_3) );
    }

    @Test
    public void testEE6Web24() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse( webApp(WebApp.VERSION_2_4) );
    }
    
    @Test
    public void testEE6Web30() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse( webApp(WebApp.VERSION_3_0) );
    }    

    @Test(expected = DDParser.ParseException.class)
    public void testEE6Web31() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse( webApp(WebApp.VERSION_3_1) );
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE6Web40() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse( webApp(WebApp.VERSION_4_0) );
    }
    
    // Servlet 3.0 specific cases ...

    /**
     * Verify that spaces are trimmed from environment entry
     * names but not from environment entry values.
     */
    @Test
    public void testEE6Web30EnvEntryValueWhitespace() throws Exception {
        WebApp webApp = parse(
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
        WebApp webApp = parse( webApp( WebApp.VERSION_3_0, absOrder() ) ); 

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
        WebApp webApp = parse( webApp( WebApp.VERSION_3_0, othersAbsOrder() ) ); 
        
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
        @SuppressWarnings("unused")        
        WebApp webApp = parse( webApp( WebApp.VERSION_3_0, dupeAbsOrder() ) );
    }

    /**
     * Verify that empty deny-uncovered-http-methods are not allowed.
     */
    @Test(expected = DDParser.ParseException.class)
    public void testEE6Web30DenyUncoveredHttpMethods() throws Exception {
        @SuppressWarnings("unused")
        WebApp webApp = parse(
            webApp( WebApp.VERSION_3_0,
                    "<deny-uncovered-http-methods/>" ) );
    }

    // Partial header cases ...

    @Test(expected = DDParser.ParseException.class)
    public void testEE6Web30NoSchema() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse( noSchemaWebApp30() + webAppTail() ); 
    }
    
    @Test(expected = DDParser.ParseException.class)
    public void testEE6Web30NoSchemaInstance() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse( noSchemaInstanceWebApp30() + webAppTail() ); 
    }
    
    @Test
    public void testEE6Web30NoSchemaLocation() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse( noSchemaLocationWebApp30() + webAppTail() ); 
    }    
    
    @Test(expected = DDParser.ParseException.class)
    public void testEE6Web30NoVersion() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse( noVersionWebApp30() + webAppTail() ); 
    }        

    @Test
    public void testEE6Web30NoID() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse( noIDWebApp30() + webAppTail() ); 
    }

    // Servlet 3.1 cases ...

    @Test()
    public void testEE7Web24() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse( webApp(WebApp.VERSION_2_4), WebApp.VERSION_3_1 );
    }

    @Test()
    public void testEE7Web30() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse( webApp(WebApp.VERSION_3_0), WebApp.VERSION_3_1 );
    }

    @Test()
    public void testEE7Web31() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse( webApp(WebApp.VERSION_3_1), WebApp.VERSION_3_1 );        
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE7Web40() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse( webApp(WebApp.VERSION_4_0), WebApp.VERSION_3_1 );
    }

    // The prohibition against having more than one absolute ordering element
    // was added in JavaEE7.
    //
    // Duplicate elements are not allowed in EE7.

    /**
     * Verify that duplicate absolute ordering elements are
     * not allowed.  These were allowed in Servlet 3.0 / Java EE6,
     * but are not allowed in Servlet 3.1 / Java EE7.
     */    
    @Test(expected = DDParser.ParseException.class)
    public void testEE7Web31AbsoluteOrderingDuplicates() throws Exception {
        @SuppressWarnings("unused")
        WebApp webApp = parse(
            webApp( WebApp.VERSION_3_1, dupeAbsOrder() ),
            WebApp.VERSION_3_1 );
    }

    /**
     * Verify that empty deny-uncovered-http-methods are now allowed.
     * They are allowed in Servlet 3.1 / Java EE7.  They were not
     * allowed in Servlet 3.0 / Java EE6.
     */    
    @Test
    public void testEE7Web31DenyUncoveredHttpMethods() throws Exception {
        @SuppressWarnings("unused")
        WebApp webApp = parse(        
            webApp( WebApp.VERSION_3_1, "<deny-uncovered-http-methods/>" ),
            WebApp.VERSION_3_1 );
    }

    /**
     * Verify that non-empty deny-uncovered-http-methods are not allowed.
     */    
    @Test(expected = DDParser.ParseException.class)
    public void testEE7Web31DenyUncoveredHttpMethodsNotEmptyType() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse(                
            webApp( WebApp.VERSION_3_1,
                    "<deny-uncovered-http-methods>junk</deny-uncovered-http-methods>" ),
            WebApp.VERSION_3_1 );
    }

    // Servlet 4.0 cases ...

    @Test
    public void testEE8Web24() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse( webApp(WebApp.VERSION_2_4), WebApp.VERSION_4_0 );        
    }

    @Test
    public void testEE8Web30() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse( webApp(WebApp.VERSION_3_0), WebApp.VERSION_4_0 );        
    }

    @Test
    public void testEE8Web31() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse( webApp(WebApp.VERSION_3_1), WebApp.VERSION_4_0 );        
    }

    @Test
    public void testEE8Web40() throws Exception {
        @SuppressWarnings("unused")        
        WebApp webApp = parse( webApp(WebApp.VERSION_4_0), WebApp.VERSION_4_0 );        
    }
}
