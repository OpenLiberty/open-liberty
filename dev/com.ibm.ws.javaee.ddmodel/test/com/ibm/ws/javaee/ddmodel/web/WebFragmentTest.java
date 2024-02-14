/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmodel.web;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ibm.ws.javaee.dd.common.ContextService;
import com.ibm.ws.javaee.dd.common.ManagedExecutor;
import com.ibm.ws.javaee.dd.common.ManagedScheduledExecutor;
import com.ibm.ws.javaee.dd.common.ManagedThreadFactory;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.WebFragment;
import com.ibm.ws.javaee.ddmodel.DDJakarta10Elements;
import com.ibm.ws.javaee.ddmodel.DDJakarta11Elements;

/**
 * Web fragment descriptor parsing unit tests.
 */
public class WebFragmentTest extends WebFragmentTestBase {

    @Test
    public void testWebFragment() throws Exception {
        for ( int schemaVersion : WebFragment.VERSIONS ) {
            for ( int maxSchemaVersion : WebFragment.VERSIONS ) {
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

                parse( webFragment( schemaVersion, WebAppTestBase.webAppBody() ),
                       maxSchemaVersion,
                       altMessage, messages );
            }
        }
    }
    
    @Test
    public void testEE6WebFragment30OrderingElement() throws Exception {
        parse(webFragment30("<ordering/>"));
    }

    // The prohibition against having more than one ordering element
    // was added in JavaEE7.
    @Test
    public void testEE6WebFragment30OrderingDuplicates() throws Exception {
        parse(webFragment30("<ordering/>" + "<ordering/>"));
    }

    @Test
    public void testEE7WebFragment31OrderingDuplicates() throws Exception {
        parse(webFragment31("<ordering/>" + "<ordering/>"),
                WebApp.VERSION_3_1,
                "at.most.one.occurrence",
                "CWWKC2266E", "ordering",
                "MyWar.war : WEB-INF/lib/fragment1.jar : META-INF/web-fragment.xml" );        
    }
    
    // EE10 element testing ...

    @Test
    public void testEE10ContextServiceWebFragment31() throws Exception {
        parse( webFragment(WebApp.VERSION_3_1, DDJakarta10Elements.CONTEXT_SERVICE_XML),
                WebApp.VERSION_3_1,
                "unexpected.child.element",
                "CWWKC2259E", "context-service",
                "MyWar.war : WEB-INF/lib/fragment1.jar : META-INF/web-fragment.xml" );
    }

    @Test
    public void testEE10ManagedExecutorWebFragment31() throws Exception {
        parse( webFragment( WebApp.VERSION_3_1, DDJakarta10Elements.MANAGED_EXECUTOR_XML),
                WebApp.VERSION_3_1,
                "unexpected.child.element",
                "CWWKC2259E", "managed-executor",
                "MyWar.war : WEB-INF/lib/fragment1.jar : META-INF/web-fragment.xml" );
    }    
    
    @Test
    public void testEE10ManagedScheduledExecutorWebFragment31() throws Exception {
        parse( webFragment( WebApp.VERSION_3_1, DDJakarta10Elements.MANAGED_SCHEDULED_EXECUTOR_XML),
                WebApp.VERSION_3_1,
                "unexpected.child.element",
                "CWWKC2259E", "managed-scheduled-executor",
                "MyWar.war : WEB-INF/lib/fragment1.jar : META-INF/web-fragment.xml" );
    }        

    @Test
    public void testEE10ManagedThreadFactoryWebFragment31() throws Exception {
        parse( webFragment( WebApp.VERSION_3_1, DDJakarta10Elements.MANAGED_THREAD_FACTORY_XML),
                WebApp.VERSION_3_1,
                "unexpected.child.element",
                "CWWKC2259E", "managed-thread-factory",
                "MyWar.war : WEB-INF/lib/fragment1.jar : META-INF/web-fragment.xml" );
    }    

    //
    
    @Test
    public void testEE10ContextServiceWebFragment50() throws Exception {
        parse( webFragment(WebApp.VERSION_5_0, DDJakarta10Elements.CONTEXT_SERVICE_XML),
                WebApp.VERSION_5_0,
                "unexpected.child.element",
                "CWWKC2259E", "context-service",
                "MyWar.war : WEB-INF/lib/fragment1.jar : META-INF/web-fragment.xml" );
    }    

    @Test
    public void testEE10ManagedExecutorWebFragment50() throws Exception {
        parse( webFragment( WebApp.VERSION_5_0, DDJakarta10Elements.MANAGED_EXECUTOR_XML),
                WebApp.VERSION_5_0,
                "unexpected.child.element",
                "CWWKC2259E", "managed-executor",
                "MyWar.war : WEB-INF/lib/fragment1.jar : META-INF/web-fragment.xml" );
    }    

    @Test
    public void testEE10ManagedScheduledExecutorWebFragment50() throws Exception {
        parse( webFragment( WebApp.VERSION_5_0, DDJakarta10Elements.MANAGED_SCHEDULED_EXECUTOR_XML),
                WebApp.VERSION_5_0,
                "unexpected.child.element",
                "CWWKC2259E", "managed-scheduled-executor",
                "MyWar.war : WEB-INF/lib/fragment1.jar : META-INF/web-fragment.xml" );
    }        

    @Test
    public void testEE10ManagedThreadFactoryWebFragment50() throws Exception {
        parse( webFragment( WebApp.VERSION_5_0, DDJakarta10Elements.MANAGED_THREAD_FACTORY_XML),
                WebApp.VERSION_5_0,
                "unexpected.child.element",
                "CWWKC2259E", "managed-thread-factory",
                "MyWar.war : WEB-INF/lib/fragment1.jar : META-INF/web-fragment.xml" );
    }    

    //

    @Test
    public void testEE10ContextServiceWeb60() throws Exception {
        WebFragment webFragment = parse(
                webFragment( WebApp.VERSION_6_0, DDJakarta10Elements.CONTEXT_SERVICE_XML),
                WebApp.VERSION_6_0);

        List<String> names = new ArrayList<String>(5);
        names.add("WebFragment");
        names.add("contextServices");

        List<ContextService> services = webFragment.getContextServices();
        DDJakarta10Elements.verifySize(names, 1, services);
        DDJakarta10Elements.verify(names, services.get(0));        
    }   
    
    @Test
    public void testEE10ManagedExecutorWebFragment60() throws Exception {
        WebFragment webFragment = parse(
                webFragment( WebApp.VERSION_6_0, DDJakarta10Elements.MANAGED_EXECUTOR_XML),
                WebApp.VERSION_6_0);

        List<String> names = new ArrayList<String>(5);
        names.add("WebFragment");
        names.add("managedExecutors");

        List<ManagedExecutor> executors = webFragment.getManagedExecutors();
        DDJakarta10Elements.verifySize(names, 1, executors);
        DDJakarta10Elements.verify(names, executors.get(0));        
    }    

    @Test
    public void testEE10ManagedScheduledExecutorWebFragment60() throws Exception {
        WebFragment webFragment = parse(
                webFragment( WebApp.VERSION_6_0, DDJakarta10Elements.MANAGED_SCHEDULED_EXECUTOR_XML),
                WebApp.VERSION_6_0);

        List<String> names = new ArrayList<String>(5);
        names.add("WebFragment");
        names.add("managedScheduledExecutors");
        
        List<ManagedScheduledExecutor> executors = webFragment.getManagedScheduledExecutors();
        DDJakarta10Elements.verifySize(names, 1, executors);
        DDJakarta10Elements.verify(names, executors.get(0));        
    }        

    @Test
    public void testEE10ManagedThreadFactoryWebFragment60() throws Exception {
        WebFragment webFragment = parse(
                webFragment( WebApp.VERSION_6_0, DDJakarta10Elements.MANAGED_THREAD_FACTORY_XML),
                WebApp.VERSION_6_0);

        List<String> names = new ArrayList<String>(5);
        names.add("WebFragment");
        names.add("managedThreadFactories");
        
        List<ManagedThreadFactory> factories = webFragment.getManagedThreadFactories();
        DDJakarta10Elements.verifySize(names, 1, factories);
        DDJakarta10Elements.verify(names, factories.get(0));
    }    
    
    // EE11 element testing

    @Test
    public void testEE11ContextServiceWeb61() throws Exception {
        WebFragment webFragment = parse(
                webFragment( WebApp.VERSION_6_1, DDJakarta11Elements.CONTEXT_SERVICE_XML),
                WebApp.VERSION_6_1);

        List<String> names = new ArrayList<String>(5);
        names.add("WebFragment");
        names.add("contextServices");

        List<ContextService> services = webFragment.getContextServices();
        DDJakarta11Elements.verifySize(names, 1, services);
        DDJakarta11Elements.verify(names, services.get(0));        
    }   
    
    @Test
    public void testEE11ManagedExecutorWebFragment61() throws Exception {
        WebFragment webFragment = parse(
                webFragment( WebApp.VERSION_6_1, DDJakarta11Elements.MANAGED_EXECUTOR_XML),
                WebApp.VERSION_6_1);

        List<String> names = new ArrayList<String>(5);
        names.add("WebFragment");
        names.add("managedExecutors");

        List<ManagedExecutor> executors = webFragment.getManagedExecutors();
        DDJakarta11Elements.verifySize(names, 1, executors);
        DDJakarta11Elements.verify(names, executors.get(0));        
    }    

    @Test
    public void testEE11ManagedScheduledExecutorWebFragment61() throws Exception {
        WebFragment webFragment = parse(
                webFragment( WebApp.VERSION_6_1, DDJakarta11Elements.MANAGED_SCHEDULED_EXECUTOR_XML),
                WebApp.VERSION_6_1);

        List<String> names = new ArrayList<String>(5);
        names.add("WebFragment");
        names.add("managedScheduledExecutors");
        
        List<ManagedScheduledExecutor> executors = webFragment.getManagedScheduledExecutors();
        DDJakarta11Elements.verifySize(names, 1, executors);
        DDJakarta11Elements.verify(names, executors.get(0));        
    }        

    @Test
    public void testEE11ManagedThreadFactoryWebFragment61() throws Exception {
        WebFragment webFragment = parse(
                webFragment( WebApp.VERSION_6_1, DDJakarta11Elements.MANAGED_THREAD_FACTORY_XML),
                WebApp.VERSION_6_1);

        List<String> names = new ArrayList<String>(5);
        names.add("WebFragment");
        names.add("managedThreadFactories");
        
        List<ManagedThreadFactory> factories = webFragment.getManagedThreadFactories();
        DDJakarta11Elements.verifySize(names, 1, factories);
        DDJakarta11Elements.verify(names, factories.get(0));
    }
}
