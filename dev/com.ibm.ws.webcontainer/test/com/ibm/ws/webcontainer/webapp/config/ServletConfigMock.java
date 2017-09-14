/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.webapp.config;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.ws.container.service.annotations.FragmentAnnotations;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.config.WebFragmentInfo;
import com.ibm.ws.container.service.metadata.internal.J2EENameFactoryImpl;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.WebFragment;
import com.ibm.ws.javaee.dd.webbnd.WebBnd;
import com.ibm.ws.javaee.dd.webext.WebExt;
import com.ibm.ws.javaee.version.ServletVersion;
import com.ibm.ws.webcontainer.osgi.WebContainer;
import com.ibm.ws.webcontainer.osgi.container.config.WebAppConfigurator;
import com.ibm.ws.webcontainer.osgi.container.config.WebAppConfiguratorHelperFactory;
//import com.ibm.ws.webcontainer.osgi.container.config.internal.WebAppConfiguratorFactoryImpl;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 *
 */
public class ServletConfigMock {

    public static WebAppConfiguration mergeTestXmls(final int version, String webXml, String... webFragments) throws Exception {

        final Mockery context = new Mockery();
        int mockId = 1;

        final WebApp mainDD = WebAppTestBase.parseWebApp(webXml, 30);

        final Container moduleContainer = context.mock(Container.class, "root" + mockId++);
        final WebAnnotations webAnnotations = context.mock(WebAnnotations.class, "webanno" + mockId++);

        TestingMemoryCache memoryCache = new TestingMemoryCache();

        //MUST be LinkedHashMap since we NEED ordering preserved.
        final LinkedHashMap<WebFragment, WebFragmentInfo> fragmentMap = new LinkedHashMap<WebFragment, WebFragmentInfo>();
        final List<WebFragmentInfo> orderedItems = new ArrayList<WebFragmentInfo>();

        for (String fragment : webFragments) {
            final WebFragment fragmentDD = WebAppTestBase.parseWebFragment(fragment, 30);

            final WebFragmentInfo webFragmentInfo = context.mock(WebFragmentInfo.class, "webFragInfo" + mockId++);

            fragmentMap.put(fragmentDD, webFragmentInfo);
        }

        final WebModuleInfo moduleInfo = context.mock(WebModuleInfo.class, "webModuleInfo" + mockId++);

        memoryCache.addToCache(WebModuleInfo.class, moduleInfo);

        final ExtendedApplicationInfo appInfo = context.mock(ExtendedApplicationInfo.class, "appInfo" + mockId++);

        final FragmentAnnotations fragmentAnnotations = context.mock(FragmentAnnotations.class, "fragAnnotation" + mockId++);
        final AnnotationTargets_Targets targets = context.mock(AnnotationTargets_Targets.class, "annoTargets" + mockId++);

        J2EENameFactory nameFactory = new J2EENameFactoryImpl();
        final J2EEName name = nameFactory.create("app", "mod", "web");

        final Set<String> annotatedClasses = new HashSet<String>();

        WebContainer webContainer = new WebContainer();
        final ServiceReference<ServletVersion> versionRef = context.mock(ServiceReference.class, "sr" + mockId++);

        context.checking(new Expectations() {
            {
                allowing(versionRef).getProperty(ServletVersion.VERSION);
                will(returnValue(version));

                // The Fragment merging requires annotations to function. We're
                // mocking out all use of annotations and making this XML only,
                // so some stubs are required.
                allowing(webAnnotations).openInfoStore();
                allowing(webAnnotations).closeInfoStore();

                allowing(webAnnotations).getAnnotationTargets();
                will(returnValue(targets));

                allowing(webAnnotations).getFragmentAnnotations(with(any(WebFragmentInfo.class)));
                will(returnValue(fragmentAnnotations));

                allowing(fragmentAnnotations).selectAnnotatedClasses((Class<?>) with(any(Object.class)));
                will(returnValue(annotatedClasses));

                // Prior to 168418, WebAnnotationsHelper would test 'isPartial' on target classes,
                // expecting 'false' to be returned.
                //
                // Starting with 168418, WebAnnotationsHelper tests 'isExcluded' on the
                // class.  Since the goal is to prevent further processing on target classes,
                // we mock a 'true' return value from 'isExcluded'.
                //
                // That is not entirely consistent with !isPartial, but is the best which
                // is possible without increasing the mock objects considerably.

                allowing(webAnnotations).isIncludedClass(with(any(String.class)));
                will(returnValue(false));

                allowing(webAnnotations).isPartialClass(with(any(String.class)));
                will(returnValue(false));

                allowing(webAnnotations).isExcludedClass(with(any(String.class)));
                will(returnValue(true));

                allowing(webAnnotations).isExternalClass(with(any(String.class)));
                will(returnValue(false));

                for (WebFragment fragmentDD : fragmentMap.keySet()) {

                    //This is the key call we want from the WebFragmentInfo. Return the DD for processing.
                    allowing(fragmentMap.get(fragmentDD)).getWebFragment();
                    will(returnValue(fragmentDD));

                    //Dummy text. Not sure the function.
                    allowing(fragmentMap.get(fragmentDD)).getLibraryURI();
                    will(returnValue("fragment"));

                    //Pretty sure this is correct. Another dummy variable.
                    allowing(fragmentMap.get(fragmentDD)).isPartialFragment();
                    will(returnValue(false));

                    orderedItems.add(fragmentMap.get(fragmentDD));
                }

                //The fragment infos are the *ordered* list of fragments we'll process.
                //We're removing ordering from this testing scheme by making an explicit one.
                allowing(webAnnotations).getOrderedItems();
                will(returnValue(orderedItems));

                allowing(moduleContainer).adapt(WebAnnotations.class);
                will(returnValue(webAnnotations));

                //Mock up the DD object to be returned from our mock container.
                allowing(moduleContainer).adapt(WebApp.class);
                will(returnValue(mainDD));
                allowing(moduleContainer).adapt(WebBnd.class);
                will(returnValue(null));
                allowing(moduleContainer).adapt(WebExt.class);
                will(returnValue(null));

                //Dummy value. Don't care about the manifest.
                allowing(moduleContainer).getEntry("/META-INF/MANIFEST.MF");
                will(returnValue(null));

                //Dummy variables. Dont' care about these.
                allowing(moduleInfo).getContextRoot();
                will(returnValue("ContextRoot"));
                allowing(moduleInfo).getName();
                will(returnValue("name"));
                allowing(moduleInfo).getURI();
                will(returnValue("uri"));

                //MockApplicationMetaData pulled from the EJB code. We just need the object made.
                allowing(appInfo).getMetaData();
                will(returnValue(new MockApplicationMetaData(name)));

                allowing(appInfo).getDeploymentName();
                will(returnValue(name.getApplication()));

                //Satisfying another call.
                allowing(moduleInfo).getApplicationInfo();
                will(returnValue(appInfo));

            }
        });

        Class<WebContainer> clazz = (Class<WebContainer>) webContainer.getClass();

        Method versionSetter = clazz.getDeclaredMethod("setVersion", ServiceReference.class);

        versionSetter.setAccessible(true);
        versionSetter.invoke(webContainer, versionRef);

        //With the above object structure, we pass in a mock container with a LIVE nonpersistent cache.
        //The cache is a simple in-memory set of hashmaps used for storing data. 
        WebAppConfigurator configurator = new WebAppConfigurator(moduleContainer, memoryCache, null);

        // Configure the factory to be used.
        // Servlet 4 TODO: We need to figure out how to mock the Factory so we can run this test
        // or we can move this and the tests that use it to the 3.0 factories project
        //WebAppConfiguratorHelperFactory factory = new WebAppConfiguratorFactoryImpl();
        //configurator.configureWebAppHelperFactory(factory, null);

        //Actually do the configuration. The mocking above should boil this down to two steps:
        //configureFromWebApp
        //configureFromWebFragment
        //and turn the rest into no-ops.
        configurator.configure();

        WebAppConfiguration finalConfig = (WebAppConfiguration) memoryCache.getFromCache(WebAppConfig.class);

        memoryCache.deleteAll();

        return finalConfig;

    }
}
