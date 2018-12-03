/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import static com.ibm.wsspi.classloading.ApiType.API;
import static com.ibm.wsspi.classloading.ApiType.SPEC;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.classloading.configuration.GlobalClassloadingConfiguration;
import com.ibm.ws.classloading.internal.providers.Providers;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.GatewayConfiguration;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.library.LibraryChangeListener;

final class TestUtil {
    protected enum ClassSource {
        A, B
    }

    static final String SERVLET_JAR_LOCATION = "servlet.jar.location";

    static final Container buildMockContainer(final String name, final URL url) {
        return new MockContainer(name, url);
    }

    static synchronized ClassLoadingServiceImpl getClassLoadingService(ClassLoader parentClassLoader) throws BundleException, InvalidSyntaxException {
        return getClassLoadingService(parentClassLoader, null, false, null);
    }

    static synchronized ClassLoadingServiceImpl getClassLoadingService(ClassLoader parentClassLoader, boolean failResolve) throws BundleException, InvalidSyntaxException {
        return getClassLoadingService(parentClassLoader, GetLibraryAction.NO_LIBS, failResolve, null);
    }

    static synchronized ClassLoadingServiceImpl getClassLoadingService(ClassLoader parentClassLoader,
                                                                       GetLibraryAction getLibraries) throws BundleException, InvalidSyntaxException {
        return getClassLoadingService(parentClassLoader, getLibraries, false, null);
    }

    static ClassLoadingServiceImpl getClassLoadingService(ClassLoader parentClassLoader,
                                                          ComponentContextExpectationProvider expectations) throws BundleException, InvalidSyntaxException {
        return getClassLoadingService(parentClassLoader, null, false, expectations);
    }

    @SuppressWarnings("unchecked")
    static ClassLoadingServiceImpl getClassLoadingService(final ClassLoader parentClassLoader,
                                                          final GetLibraryAction getLibraries,
                                                          final boolean failResolve,
                                                          final ComponentContextExpectationProvider expectations) throws BundleException, InvalidSyntaxException {
        final ClassLoadingServiceImpl cls = new ClassLoadingServiceImpl();

        cls.setGlobalClassloadingConfiguration(new GlobalClassloadingConfiguration());

        final Mockery mockery = new Mockery();
        final ComponentContext componentContext = mockery.mock(ComponentContext.class);
        final BundleContext myBundleContext = mockery.mock(BundleContext.class, "myBundleContext");
        setFinalStatic(Providers.class, "bundleContext", myBundleContext);
        final Bundle mainBundle = mockery.mock(Bundle.class, "mainBundle");
        final BundleContext mainBundleContext = mockery.mock(BundleContext.class, "systemBundleContext");
        final Bundle gatewayBundle = mockery.mock(Bundle.class, "gatewayBundle");
        final FrameworkWiring frameworkWiring = mockery.mock(FrameworkWiring.class);
        final BundleWiring bundleWiring = mockery.mock(BundleWiring.class);
        final FrameworkStartLevel frameworkStartLevel = mockery.mock(FrameworkStartLevel.class);
        final BundleStartLevel bundleStartLevel = mockery.mock(BundleStartLevel.class);

        mockery.checking(new Expectations() {
            {

                // componentContext.getBundleContext() should return bCtx
                allowing(componentContext).getBundleContext();
                will(returnValue(myBundleContext));
                // bundleContext.getBundle("System Bundle") should return mainBundle
                allowing(myBundleContext).getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
                will(returnValue(mainBundle));
                allowing(mainBundle).getBundleContext();
                will(returnValue(mainBundleContext));
                // allow bundle listener
                allowing(mainBundleContext).addBundleListener(with(any(BundleListener.class)));
                allowing(mainBundleContext).removeBundleListener(with(any(BundleListener.class)));

                // mainBundle.adapt(FrameworkWiring.class) should return frameworkWiring
                allowing(mainBundle).adapt(FrameworkWiring.class);
                will(returnValue(frameworkWiring));
                // bundleContext.installBundle(String, InputStream) should return gatewayBundle
                allowing(myBundleContext).installBundle(with(any(String.class)), with(any(InputStream.class)));
                will(returnValue(gatewayBundle));

                allowing(myBundleContext).getBundle(with(new BaseMatcher<String>() {

                    @Override
                    public boolean matches(Object o) {
                        if (o instanceof String) {
                            return ((String) o).startsWith("WSClassLoadingService@");
                        }
                        return false;
                    }

                    @Override
                    public void describeTo(Description arg0) {
                        // nothing
                    }
                }));
                will(returnValue(gatewayBundle));

                allowing(gatewayBundle).getHeaders(with(""));
                will(returnValue(new Hashtable<String, String>()));

                allowing(gatewayBundle).getLocation();
                will(returnValue(""));
                allowing(gatewayBundle).getBundleId();
                will(returnValue((long) 5150));

                allowing(gatewayBundle).stop();
                allowing(gatewayBundle).uninstall();
                allowing(gatewayBundle).update(with(any(InputStream.class)));

                //Bundle.start()
                allowing(gatewayBundle).start(Bundle.START_ACTIVATION_POLICY);
                if (failResolve) {
                    will(throwException(new BundleException("Some resolution message", BundleException.RESOLVE_ERROR)));
                }
                // Bundle.getState() should return ACTIVE
                allowing(gatewayBundle).getState();
                if (failResolve) {
                    will(returnValue(Bundle.INSTALLED));
                } else {
                    will(returnValue(Bundle.ACTIVE));
                }
                // gatewayBundle.adapt(BundleWiring.class) should return bundleWiring
                //Bundle.getState() called by trace
                allowing(gatewayBundle).getState();
                will(returnValue(Bundle.INSTALLED));
                allowing(gatewayBundle).adapt(BundleWiring.class);
                will(returnValue(bundleWiring));
                // bundleWiring.getClassLoader() should return parentClassLoader
                allowing(bundleWiring).getClassLoader();
                will(returnValue(parentClassLoader));
                allowing(frameworkWiring).getBundle();
                will(returnValue(mainBundle));
                allowing(mainBundle).adapt(FrameworkStartLevel.class);
                will(returnValue(frameworkStartLevel));
                allowing(gatewayBundle).adapt(BundleStartLevel.class);
                will(returnValue(bundleStartLevel));
                allowing(frameworkStartLevel).getStartLevel();
                will(returnValue(18));
                allowing(bundleStartLevel).getStartLevel();
                will(returnValue(20));
                allowing(bundleStartLevel).setStartLevel(18);
                // allow look up of libraries
                allowing(myBundleContext).registerService(with(LibraryChangeListener.class), with(any(LibraryChangeListener.class)), with(any(Dictionary.class)));
                will(getLibraries);
                allowing(myBundleContext).getServiceReferences(with(Library.class), with(any(String.class)));
                will(getLibraries);
                allowing(myBundleContext).getService(with(any(MockServiceReference.class)));
                will(getLibraries);
            }
        });
        if (expectations != null) {
            expectations.addExpectations(mockery, componentContext);
        }
        cls.activate(componentContext, null);
        return cls;
    }

    public static URL getOtherClassesURL(ClassSource otherSource) throws MalformedURLException {
        URL loc = getTestClassesURL();
        String mainClassesDir = System.getProperty("main.classesDir", "bin").replace("\\", "/");
        String mainClassesSuffix = mainClassesDir.replaceAll(".*/com.ibm.ws.classloading_test/", "");
        return new URL(loc.toString().replaceAll("classloading_test/.*", "classloading_test.jar" + otherSource + "/" + mainClassesSuffix + "/"));
    }

    public static TestUtilClassLoader getLoaderFor(ClassSource source) throws MalformedURLException {
        URL[] urls = { getOtherClassesURL(source) };
        return new TestUtilClassLoader(urls);
    }

    /**
     * This returns a URL that points to the classes for this project.
     *
     * @return The URL pointing to the classes for this project
     * @throws MalformedURLException
     */
    public static URL getTestClassesURL() throws MalformedURLException {
        URL loc = TestUtil.class.getProtectionDomain().getCodeSource().getLocation();
        return loc;
    }

    public static URL getServletJarURL() throws MalformedURLException, FileNotFoundException {
        File servletJar = new File(System.getProperty(SERVLET_JAR_LOCATION));

        // check it exists!
        if (!!!servletJar.exists()) {
            String absolutePath = servletJar.getAbsolutePath();
            System.err.println("Cannot find servlet jar in expected location: " + absolutePath);
            throw new FileNotFoundException(absolutePath);
        }
        URL servletJarURL = servletJar.toURI().toURL();
        return servletJarURL;
    }

    public static URL getTestJarURL() throws MalformedURLException {
        URL loc = getTestClassesURL();
        return new URL(loc.toString() + "test.jar");
    }

    public static AppClassLoader createAppClassloader(String id, URL url,
                                                      boolean parentLast) throws MalformedURLException, FileNotFoundException, BundleException, InvalidSyntaxException {
        return createAppClassloader(id, url, parentLast, GetLibraryAction.NO_LIBS);
    }

    public static AppClassLoader createAppClassloader(String id, URL url, boolean parentLast,
                                                      GetLibraryAction getLibraries) throws MalformedURLException, FileNotFoundException, BundleException, InvalidSyntaxException {
        // find the servlet jar
        URL[] urlsForParentClassLoader = { TestUtil.getServletJarURL() };
        // get a classloader service that thinks it is in a framework
        ClassLoader parentLoader = new URLClassLoader(urlsForParentClassLoader);
        ClassLoadingServiceImpl service = TestUtil.getClassLoadingService(parentLoader, getLibraries);
        // configure up a classloader
        GatewayConfiguration gwConfig = service.createGatewayConfiguration().setApiTypeVisibility(SPEC, API);
        ClassLoaderConfiguration config = service.createClassLoaderConfiguration().setSharedLibraries(getLibraries.getPrivateLibs()).setCommonLibraries(getLibraries.getCommonLibs()).setDelegateToParentAfterCheckingLocalClasspath(parentLast).setId(service.createIdentity("UnitTest",
                                                                                                                                                                                                                                                                              id));

        Mockery mockery = new Mockery();
        Container containerForURL = buildMockContainer(id, url);

        // retrieve the classloader, making sure it can see the 'other' set of classes
        return service.createTopLevelClassLoader(Arrays.asList(containerForURL), gwConfig, config);
    }

    static void setFinalStatic(Class<?> clazz, String fieldName, Object newValue) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);

            Field modifiersField;
            modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.set(null, newValue);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }

}
