/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import javax.ejb.TimedObject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;

import test.common.ComponentContextMockery;
import test.common.SharedOutputManager;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.ws.container.service.annotations.ModuleAnnotations;
import com.ibm.ws.container.service.metadata.internal.J2EENameFactoryImpl;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.osgi.EJBRuntimeVersion;
import com.ibm.ws.ejbcontainer.osgi.MDBRuntime;
import com.ibm.ws.ejbcontainer.osgi.ManagedBeanRuntime;
import com.ibm.ws.ejbcontainer.osgi.SessionBeanRuntime;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.Interceptor1;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.Interceptor2;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.Interceptor3;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.LocalAnnotatedIntf;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.LocalCompIntf;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.LocalHomeIntf;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.LocalIntf1;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.LocalIntf2;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.RemoteAnnotatedIntf;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.RemoteCompIntf;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.RemoteHomeIntf;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.RemoteIntf1;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.RemoteIntf2;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestActivationConfigProperties;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestAnnotatedLocal;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestAnnotatedLocalAndRemoteError;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestAnnotatedLocalNonEmptyError;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestAnnotatedRemote;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestAnnotatedRemoteNonEmptyError;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestBMT;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestBasic;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestDefaultLocal;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestDefaultLocalTwoImplements;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestDefaultMessageListener;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestDefaultMessageListenerXML;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestDependsOn;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestEmptyLocal;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestEmptyLocalAndAnnotatedLocal;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestEmptyLocalAndAnnotatedRemote;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestEmptyLocalAndLocalBean;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestEmptyLocalAndLocalHome;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestEmptyLocalAndRemoteError;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestEmptyLocalNoImplementsError;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestEmptyLocalTwoImplements;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestEmptyRemote;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestEmptyRemoteAndAnnotatedLocal;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestEmptyRemoteAndAnnotatedRemote;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestEmptyRemoteAndLocalBean;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestEmptyRemoteNoImplementsError;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestEmptyRemoteTwoImplements;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestInterceptors;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestInterfaces;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestLocal;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestLocalAndImplements;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestLocalAndRemoteError;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestManagedBean;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestManagedBeanInterceptors;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestManagedBeanNamed;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestManagedBeanTimedObject;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestManagedBeanTimeout;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestMessageDriven;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestMessageDrivenNamed;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestMessageListener;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestMultipleNamed;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestRemote;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestRemoteAndImplements;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestSchedule;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestSchedules;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestSingleton;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestSingletonNamed;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestStartup;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestStateful;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestStatefulNamed;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestStatefulPassivationCapableFalse;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestStateless;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestStatelessClassError;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestStatelessMBNamed;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestStatelessNamed;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestStatelessTypeError;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestTimeout;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestWebService;
import com.ibm.ws.ejbcontainer.osgi.internal.ejb.TestWebServiceProvider;
import com.ibm.ws.ejbcontainer.osgi.internal.ejbbnddd.EJBJarBndMockery;
import com.ibm.ws.ejbcontainer.osgi.internal.ejbdd.EJBJarMockery;
import com.ibm.ws.ejbcontainer.osgi.internal.ejbextdd.EJBJarExtMockery;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejb.Entity;
import com.ibm.ws.javaee.dd.ejb.Session;
import com.ibm.ws.javaee.dd.ejbbnd.EJBJarBnd;
import com.ibm.ws.javaee.dd.ejbext.EJBJarExt;
import com.ibm.ws.javaee.dd.managedbean.ManagedBeanBnd;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.application.handler.ApplicationInformation;

public class ModuleInitDataFactoryTest {
    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("EJBContainer=all");

    private final Mockery mockery = new Mockery();
    private final ComponentContextMockery ccMockery = new ComponentContextMockery(mockery);
    private final EJBJarMockery ejbJarMockery = new EJBJarMockery(mockery);
    private final EJBJarExtMockery ejbJarExtMockery = new EJBJarExtMockery(mockery);
    private final EJBJarBndMockery ejbJarBndMockery = new EJBJarBndMockery(mockery);
    private final ComponentContext cc = mockery.mock(ComponentContext.class);
    private final AnnoMockery annoMockery = new AnnoMockery(mockery);
    private ModuleInitDataFactory factory;
    private ServiceReference<ManagedBeanRuntime> managedBeanRuntimeRef;

    @Before
    public void before() throws Exception {
        factory = new ModuleInitDataFactory();

        J2EENameFactory j2eeNameFactory = new J2EENameFactoryImpl();
        factory.setJ2eeNameFactory(ccMockery.mockService(cc, "j2eeNameFactory", j2eeNameFactory));

        SessionBeanRuntime sessionBeanRuntime = mockery.mock(SessionBeanRuntime.class);
        ServiceReference<SessionBeanRuntime> sessionBeanRuntimeRef = ccMockery.mockService(cc, "sessionBeanRuntime", sessionBeanRuntime);
        factory.setSessionBeanRuntime(sessionBeanRuntimeRef);

        MDBRuntime mdbRuntime = mockery.mock(MDBRuntime.class);
        ServiceReference<MDBRuntime> mdbRuntimeRef = ccMockery.mockService(cc, "mdbRuntime", mdbRuntime);
        factory.setMDBRuntime(mdbRuntimeRef);

        ManagedBeanRuntime managedBeanRuntime = mockery.mock(ManagedBeanRuntime.class);
        managedBeanRuntimeRef = ccMockery.mockService(cc, "managedBeanRuntime", managedBeanRuntime);

        annoMockery.addClass(TimedObject.class);
    }

    private ModuleInitDataImpl createModuleInitData(final EJBJar ejbJar) throws Exception {
        return createModuleInitData(ejbJar, null, null);
    }

    private ModuleInitDataImpl createModuleInitData(final EJBJar ejbJar, final EJBJarExt ejbJarExt) throws Exception {
        return createModuleInitData(ejbJar, ejbJarExt, null);
    }

    private ModuleInitDataImpl createModuleInitData(final EJBJar ejbJar, final EJBJarBnd ejbJarBnd) throws Exception {
        return createModuleInitData(ejbJar, null, ejbJarBnd);
    }

    private ModuleInitDataImpl createModuleInitData(final EJBJar ejbJar, final EJBJarExt ejbJarExt, final EJBJarBnd ejbJarBnd) throws Exception {
        factory.setAnnoService(ccMockery.mockService(cc, "annoService", annoMockery.mockAnnotationService()));
        factory.activate(cc);

        @SuppressWarnings("unchecked")
        final ApplicationInformation<EJBModuleMetaDataImpl> appInfo = mockery.mock(ApplicationInformation.class);
        final Container container = mockery.mock(Container.class);

        final ModuleAnnotations moduleAnno = annoMockery.mockModuleAnnotations();

        mockery.checking(new Expectations() {
            {
                allowing(appInfo).getName();
                will(returnValue("test"));
                allowing(appInfo).getContainer();
                will(returnValue(container));

                @SuppressWarnings("unused")
                Collection<URL> getUri = allowing(container).getURLs();
                will(returnValue(Collections.singleton(new URL("file:test.jar"))));

                allowing(container).adapt(EJBJar.class);
                will(returnValue(ejbJar));

                allowing(container).adapt(EJBJarExt.class);
                will(returnValue(ejbJarExt));

                allowing(container).adapt(EJBJarBnd.class);
                will(returnValue(ejbJarBnd));

                allowing(container).adapt(ModuleAnnotations.class);
                will(returnValue(moduleAnno));

                allowing(container).adapt(ManagedBeanBnd.class);
                will(returnValue(null));

            }
        });

        String name = appInfo.getName();
        return factory.createModuleInitData(appInfo.getContainer(),
                                            ModuleInitDataChecker.mockClassLoader,
                                            name, name, name,
                                            null, null, false);
    }

    private ServiceReference<EJBRuntimeVersion> createEJBRuntimeVersionServiceReference(final Version version) {
        @SuppressWarnings("unchecked")
        final ServiceReference<EJBRuntimeVersion> ref = mockery.mock(ServiceReference.class, ServiceReference.class.getName() + "-EJBRuntimeVersion");
        mockery.checking(new Expectations() {
            {
                allowing(ref).getProperty(EJBRuntimeVersion.VERSION);
                will(returnValue(version.toString()));
            }
        });
        return ref;
    }

    private static final int TYPE_STATELESS = InternalConstants.TYPE_STATELESS_SESSION;
    private static final int TYPE_STATEFUL = InternalConstants.TYPE_STATEFUL_SESSION;
    private static final int TYPE_SINGLETON = InternalConstants.TYPE_SINGLETON_SESSION;
    private static final int TYPE_MESSAGE_DRIVEN = InternalConstants.TYPE_MESSAGE_DRIVEN;
    private static final int TYPE_BEAN_MANAGED_ENTITY = InternalConstants.TYPE_BEAN_MANAGED_ENTITY;
    private static final int TYPE_CONTAINER_MANAGED_ENTITY = InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY;
    private static final int TYPE_MANAGED_BEAN = InternalConstants.TYPE_MANAGED_BEAN;

    @Test
    public void testStatelessAnnotation() throws Exception {
        annoMockery.addClass(TestStateless.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestStateless.class)
                                        .localBean())
                        .check();
    }

    @Test
    public void testStatelessMixed() throws Exception {
        annoMockery.addClass(TestStateless.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestStateless.class.getSimpleName())
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestStateless.class)
                                        .xml()
                                        .localBean())
                        .check();
    }

    @Test
    public void testStatelessMixedExplicitClass() throws Exception {
        annoMockery.addClass(TestStateless.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestStateless.class.getSimpleName())
                                        .ejbClass(TestStateless.class.getName())
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestStateless.class)
                                        .xml()
                                        .localBean())
                        .check();
    }

    /**
     * Test that a session bean can be specified in XML without a session-type,
     * which should be inferred from the component-defining annotation on the
     * ejb-class.
     */
    @Test
    public void testStatelessTypeMixed() throws Exception {
        annoMockery.addClass(TestStateless.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session("XML")
                                        .ejbClass(TestStateless.class.getName())
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker("XML", TYPE_STATELESS, TestStateless.class)
                                        .xml()
                                        .localBean())
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestStateless.class)
                                        .localBean())
                        .check();
    }

    @Test
    public void testStatefulAnnotation() throws Exception {
        annoMockery.addClass(TestStateful.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATEFUL, TestStateful.class)
                                        .localBean())
                        .check();
    }

    @Test
    public void testSingletonAnnotation() throws Exception {
        annoMockery.addClass(TestSingleton.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_SINGLETON, TestSingleton.class)
                                        .localBean())
                        .check();
    }

    @Test
    public void testMessageDrivenAnnotation() throws Exception {
        annoMockery.addClass(TestMessageDriven.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_MESSAGE_DRIVEN, TestMessageDriven.class))
                        .check();
    }

    @Test
    public void testManagedBeanAnnotation() throws Exception {
        factory.setManagedBeanRuntime(managedBeanRuntimeRef);
        annoMockery.addClass(TestManagedBean.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_MANAGED_BEAN, TestManagedBean.class))
                        .check();
    }

    @Test
    public void testStatelessNamedAnnotation() throws Exception {
        annoMockery.addClass(TestStatelessNamed.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TestStatelessNamed.NAME, TYPE_STATELESS, TestStatelessNamed.class)
                                        .localBean())
                        .check();
    }

    @Test
    public void testStatelessMBNamedAnnotation() throws Exception {
        annoMockery.addClass(TestStatelessMBNamed.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TestStatelessMBNamed.NAME, TYPE_STATELESS, TestStatelessMBNamed.class)
                                        .localBean())
                        .check();
    }

    @Test
    public void testStatefulNamedAnnotation() throws Exception {
        annoMockery.addClass(TestStatefulNamed.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TestStatefulNamed.NAME, TYPE_STATEFUL, TestStatefulNamed.class)
                                        .localBean())
                        .check();
    }

    @Test
    public void testSingletonNamedAnnotation() throws Exception {
        annoMockery.addClass(TestSingletonNamed.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TestSingletonNamed.NAME, TYPE_SINGLETON, TestSingletonNamed.class)
                                        .localBean())
                        .check();
    }

    @Test
    public void testMessageDrivenNamedAnnotation() throws Exception {
        annoMockery.addClass(TestMessageDrivenNamed.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TestMessageDrivenNamed.NAME, TYPE_MESSAGE_DRIVEN, TestMessageDrivenNamed.class))
                        .check();
    }

    @Test
    public void testManagedBeanNamedAnnotation() throws Exception {
        factory.setManagedBeanRuntime(managedBeanRuntimeRef);
        annoMockery.addClass(TestManagedBeanNamed.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TestManagedBeanNamed.NAME, TYPE_MANAGED_BEAN, TestManagedBeanNamed.class))
                        .check();
    }

    @Test
    public void testStatelessAndManagedBeanAnnotations() throws Exception {
        factory.setManagedBeanRuntime(managedBeanRuntimeRef);
        annoMockery.addClass(TestStateless.class);
        annoMockery.addClass(TestManagedBeanNamed.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestStateless.class)
                                        .localBean())
                        .bean(new BeanInitDataChecker(TestManagedBeanNamed.NAME, TYPE_MANAGED_BEAN, TestManagedBeanNamed.class))
                        .check();
    }

    @Test
    public void testStatelessXMLOverrideManagedBean() throws Exception {
        factory.setManagedBeanRuntime(managedBeanRuntimeRef);
        annoMockery.addClass(TestManagedBean.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestManagedBean.class.getSimpleName())
                                        .ejbClass(TestManagedBean.class.getName())
                                        .type(Session.SESSION_TYPE_STATELESS)
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestManagedBean.class)
                                        .xml()
                                        .localBean())
                        .check();
    }

    @Test
    public void testMultipleNamedAnnotations() throws Exception {
        annoMockery.addClass(TestMultipleNamed.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TestMultipleNamed.STATELESS_NAME, TYPE_STATELESS, TestMultipleNamed.class)
                                        .localBean())
                        .bean(new BeanInitDataChecker(TestMultipleNamed.STATEFUL_NAME, TYPE_STATEFUL, TestMultipleNamed.class)
                                        .localBean())
                        .bean(new BeanInitDataChecker(TestMultipleNamed.SINGLETON_NAME, TYPE_SINGLETON, TestMultipleNamed.class)
                                        .localBean())
                        .bean(new BeanInitDataChecker(TestMultipleNamed.MESSAGE_DRIVEN_NAME, TYPE_MESSAGE_DRIVEN, TestMultipleNamed.class))
                        .check();
    }

    @Test(expected = EJBConfigurationException.class)
    public void testDuplicateEJBXMLError() throws Exception {
        annoMockery.addClass(TestStateless.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session("session")
                                        .ejbClass(TestStateless.class.getName())
                                        .mock())
                        .enterpriseBean(ejbJarMockery.session("session")
                                        .ejbClass(TestStateless.class.getName())
                                        .mock())
                        .mock();
        outputMgr.expectError("CNTR4100E");
        createModuleInitData(ejbJar);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testMissingEJBClassError() throws Exception {
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session("session")
                                        .mock())
                        .mock();
        outputMgr.expectError("CNTR4101E");
        createModuleInitData(ejbJar);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testInvalidEJBClassError() throws Exception {
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session("session")
                                        .ejbClass("com.ibm.DoesNotExist")
                                        .mock())
                        .mock();
        outputMgr.expectError("CNTR4115E");
        createModuleInitData(ejbJar);
    }

    @Test
    public void testInvalidEJBClassMetadataComplete() throws Exception {
        EJBJar ejbJar = ejbJarMockery
                        .metadataComplete()
                        .enterpriseBean(ejbJarMockery.session("session")
                                        .type(Session.SESSION_TYPE_STATELESS)
                                        .ejbClass("com.ibm.DoesNotExist")
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .metadataComplete()
                        .bean(new BeanInitDataChecker("session", TYPE_STATELESS, "com.ibm.DoesNotExist")
                                        .xml())
                        .check();
    }

    @Test(expected = EJBConfigurationException.class)
    public void testIncompatibleEJBClassError() throws Exception {
        annoMockery.addClass(TestStateless.class);
        annoMockery.addClass(TestStatelessClassError.class);
        outputMgr.expectError("CNTR4106E");
        createModuleInitData(null);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testIncompatibleEJBClassXMLError() throws Exception {
        annoMockery.addClass(TestBasic.class);
        annoMockery.addClass(TestStatelessClassError.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session("TestStateless")
                                        .ejbClass(TestBasic.class.getName())
                                        .type(Session.SESSION_TYPE_STATELESS)
                                        .mock())
                        .mock();
        outputMgr.expectError("CNTR4114E");
        createModuleInitData(ejbJar);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testMissingSessionTypeError() throws Exception {
        annoMockery.addClass(TestBasic.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session("session")
                                        .ejbClass(TestBasic.class.getName())
                                        .mock())
                        .mock();
        outputMgr.expectError("CNTR4102E");
        createModuleInitData(ejbJar);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testIncompatibleKindError() throws Exception {
        annoMockery.addClass(TestStateless.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.messageDriven("TestStateless")
                                        .mock())
                        .mock();
        outputMgr.expectError("CNTR4103E");
        createModuleInitData(ejbJar);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testIncompatibleTypeAnnotationError() throws Exception {
        annoMockery.addClass(TestStatelessTypeError.class);
        outputMgr.expectError("CNTR4104E");
        createModuleInitData(null);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testIncompatibleTypeXMLError() throws Exception {
        annoMockery.addClass(TestStateless.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session("TestStateless")
                                        .type(Session.SESSION_TYPE_STATEFUL)
                                        .mock())
                        .mock();
        outputMgr.expectError("CNTR4105E");
        createModuleInitData(ejbJar);
    }

    @Test
    public void testEntityXML() throws Exception {
        annoMockery.addClass(TestBasic.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.entity("entity", Entity.PERSISTENCE_TYPE_BEAN)
                                        .ejbClass(TestBasic.class.getName())
                                        .remote(RemoteHomeIntf.class.getName(), RemoteCompIntf.class.getName())
                                        .local(LocalHomeIntf.class.getName(), LocalCompIntf.class.getName())
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker("entity", TYPE_BEAN_MANAGED_ENTITY, TestBasic.class)
                                        .xml()
                                        .remote(RemoteHomeIntf.class.getName(), RemoteCompIntf.class.getName())
                                        .local(LocalHomeIntf.class.getName(), LocalCompIntf.class.getName()))
                        .check();
    }

    @Test
    public void testEntityCMPVersion1X() throws Exception {
        annoMockery.addClass(TestBasic.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.entity("entity", Entity.PERSISTENCE_TYPE_CONTAINER)
                                        .ejbClass(TestBasic.class.getName())
                                        .cmpVersion(Entity.CMP_VERSION_1_X)
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker("entity", TYPE_CONTAINER_MANAGED_ENTITY, TestBasic.class)
                                        .cmpVersion(InternalConstants.CMP_VERSION_1_X)
                                        .xml())
                        .check();
    }

    @Test
    public void testEntityCMPVersion2X() throws Exception {
        annoMockery.addClass(TestBasic.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.entity("entity", Entity.PERSISTENCE_TYPE_CONTAINER)
                                        .ejbClass(TestBasic.class.getName())
                                        .cmpVersion(Entity.CMP_VERSION_2_X)
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker("entity", TYPE_CONTAINER_MANAGED_ENTITY, TestBasic.class)
                                        .cmpVersion(InternalConstants.CMP_VERSION_2_X)
                                        .xml())
                        .check();
    }

    @Test
    public void testInterfaces() throws Exception {
        annoMockery.addClass(TestInterfaces.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestInterfaces.class)
                                        .remote(RemoteHomeIntf.class.getName(), null)
                                        .local(LocalHomeIntf.class.getName(), null)
                                        .remoteBusiness(RemoteIntf1.class.getName(), RemoteIntf2.class.getName())
                                        .localBusiness(LocalIntf1.class.getName(), LocalIntf2.class.getName())
                                        .localBean())
                        .check();
    }

    @Test(expected = EJBConfigurationException.class)
    public void testEmptyLocalAndRemoteError() throws Exception {
        annoMockery.addClass(TestEmptyLocalAndRemoteError.class);
        outputMgr.expectError("CNTR4107E");
        createModuleInitData(null);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testEmptyLocalNoImplementsError() throws Exception {
        annoMockery.addClass(TestEmptyLocalNoImplementsError.class);
        outputMgr.expectError("CNTR4108E");
        createModuleInitData(null);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testEmptyRemoteNoImplementsError() throws Exception {
        annoMockery.addClass(TestEmptyRemoteNoImplementsError.class);
        outputMgr.expectError("CNTR4108E");
        createModuleInitData(null);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testLocalAndRemoteError() throws Exception {
        annoMockery.addClass(TestLocalAndRemoteError.class);
        outputMgr.expectError("CNTR4110E");
        createModuleInitData(null);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testLocalAndRemoteXMLError() throws Exception {
        EJBJar ejbJar = ejbJarMockery
                        .metadataComplete()
                        .enterpriseBean(ejbJarMockery.session(TestBasic.class.getSimpleName())
                                        .ejbClass(TestBasic.class.getName())
                                        .type(Session.SESSION_TYPE_STATELESS)
                                        .remoteBusiness(RemoteIntf1.class.getName())
                                        .localBusiness(RemoteIntf1.class.getName())
                                        .mock())
                        .mock();
        outputMgr.expectError("CNTR4110E");
        createModuleInitData(ejbJar);
    }

    @Test(expected = EJBConfigurationException.class)
    public void testAnnotatedLocalAndRemoteError() throws Exception {
        annoMockery.addClass(TestAnnotatedLocalAndRemoteError.class);
        outputMgr.expectError("CNTR4110E");
        createModuleInitData(null);
    }

    @Test
    public void testStatelessXML() throws Exception {
        EJBJar ejbJar = ejbJarMockery
                        .metadataComplete()
                        .enterpriseBean(ejbJarMockery.session(TestBasic.class.getSimpleName())
                                        .ejbClass(TestBasic.class.getName())
                                        .type(Session.SESSION_TYPE_STATELESS)
                                        .remote(RemoteHomeIntf.class.getName(), RemoteCompIntf.class.getName())
                                        .local(LocalHomeIntf.class.getName(), LocalCompIntf.class.getName())
                                        .remoteBusiness(RemoteIntf1.class.getName(), RemoteIntf2.class.getName())
                                        .localBusiness(LocalIntf1.class.getName(), LocalIntf2.class.getName())
                                        .localBean()
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .metadataComplete()
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestBasic.class)
                                        .xml()
                                        .remote(RemoteHomeIntf.class.getName(), RemoteCompIntf.class.getName())
                                        .local(LocalHomeIntf.class.getName(), LocalCompIntf.class.getName())
                                        .remoteBusiness(RemoteIntf1.class.getName(), RemoteIntf2.class.getName())
                                        .localBusiness(LocalIntf1.class.getName(), LocalIntf2.class.getName())
                                        .localBean())
                        .check();
    }

    @Test
    public void testDefaultLocalInterface() throws Exception {
        annoMockery.addClass(TestDefaultLocal.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestDefaultLocal.class)
                                        .localBusiness(LocalIntf1.class.getName()))
                        .timers()
                        .check();
    }

    @Test
    public void testDefaultLocalTwoImplementsInterfaces() throws Exception {
        annoMockery.addClass(TestDefaultLocalTwoImplements.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestDefaultLocalTwoImplements.class)
                                        .localBusiness(LocalIntf1.class.getName())
                                        .localBusiness(LocalIntf2.class.getName()))
                        .timers()
                        .check();
    }

    @Test
    public void testDefaultLocalAndXMLLocalInterfaces() throws Exception {
        annoMockery.addClass(TestDefaultLocal.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestDefaultLocal.class.getSimpleName())
                                        .localBusiness(LocalIntf2.class.getName())
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestDefaultLocal.class)
                                        .xml()
                                        .localBusiness(LocalIntf2.class.getName()))
                        .timers()
                        .check();
    }

    @Test
    public void testDefaultLocalAndXMLRemoteInterfaces() throws Exception {
        annoMockery.addClass(TestDefaultLocal.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestDefaultLocal.class.getSimpleName())
                                        .remoteBusiness(RemoteIntf2.class.getName())
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestDefaultLocal.class)
                                        .xml()
                                        .remoteBusiness(RemoteIntf2.class.getName()))
                        .timers()
                        .check();
    }

    @Test
    public void testLocalInterface() throws Exception {
        annoMockery.addClass(TestLocal.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestLocal.class)
                                        .localBusiness(LocalIntf1.class.getName()))
                        .check();
    }

    @Test
    public void testLocalAndImplementsInterface() throws Exception {
        annoMockery.addClass(TestLocalAndImplements.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestLocalAndImplements.class)
                                        .localBusiness(LocalIntf1.class.getName()))
                        .check();
    }

    @Test
    public void testEmptyLocalInterface() throws Exception {
        annoMockery.addClass(TestEmptyLocal.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestEmptyLocal.class)
                                        .localBusiness(LocalIntf1.class.getName()))
                        .check();
    }

    @Test
    public void testEmptyLocalTwoImplementsInterfaces() throws Exception {
        annoMockery.addClass(TestEmptyLocalTwoImplements.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestEmptyLocalTwoImplements.class)
                                        .localBusiness(LocalIntf1.class.getName())
                                        .localBusiness(LocalIntf2.class.getName()))
                        .timers()
                        .check();
    }

    @Test
    public void testEmptyLocalAndAnnotatedLocalInterfaces() throws Exception {
        annoMockery.addClass(TestEmptyLocalAndAnnotatedLocal.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestEmptyLocalAndAnnotatedLocal.class)
                                        .localBusiness(LocalAnnotatedIntf.class.getName()))
                        .check();
    }

    @Test
    public void testEmptyLocalAndXMLLocalInterfacesStrict() throws Exception {
        factory.setEJBRuntimeVersion(createEJBRuntimeVersionServiceReference(EJBRuntimeVersion.VERSION_3_2));
        testEmptyLocalAndXMLLocalInterfacesCompatibility();
    }

    @Test
    public void testEmptyLocalAndXMLLocalInterfacesCompatibility() throws Exception {
        annoMockery.addClass(TestEmptyLocal.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestEmptyLocal.class.getSimpleName())
                                        .localBusiness(LocalIntf2.class.getName())
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestEmptyLocal.class)
                                        .xml()
                                        .localBusinessIf(factory.isEmptyAnnotationIgnoresExplicitInterfaces(), LocalIntf1.class.getName())
                                        .localBusiness(LocalIntf2.class.getName()))
                        .check();
    }

    @Test
    public void testEmptyLocalAndXMLRemoteInterfacesConflictStrict() throws Exception {
        factory.setEJBRuntimeVersion(createEJBRuntimeVersionServiceReference(EJBRuntimeVersion.VERSION_3_2));
        testEmptyLocalAndXMLRemoteInterfacesConflictCompatibility();
    }

    @Test
    public void testEmptyLocalAndXMLRemoteInterfacesConflictCompatibility() throws Exception {
        annoMockery.addClass(TestEmptyLocal.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestEmptyLocal.class.getSimpleName())
                                        .remoteBusiness(LocalIntf1.class.getName())
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestEmptyLocal.class)
                                        .xml()
                                        .remoteBusiness(LocalIntf1.class.getName()))
                        .check();
    }

    @Test
    public void testEmptyLocalAndAnnotatedRemoteInterfaces() throws Exception {
        annoMockery.addClass(TestEmptyLocalAndAnnotatedRemote.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestEmptyLocalAndAnnotatedRemote.class)
                                        .remoteBusiness(RemoteAnnotatedIntf.class.getName()))
                        .check();
    }

    @Test
    public void testEmptyLocalAndLocalBeanInterfacesStrict() throws Exception {
        factory.setEJBRuntimeVersion(createEJBRuntimeVersionServiceReference(EJBRuntimeVersion.VERSION_3_2));
        testEmptyLocalAndLocalBeanInterfacesCompatibility();
    }

    @Test
    public void testEmptyLocalAndLocalBeanInterfacesCompatibility() throws Exception {
        annoMockery.addClass(TestEmptyLocalAndLocalBean.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestEmptyLocalAndLocalBean.class)
                                        .localBean()
                                        .localBusinessIf(factory.isEmptyAnnotationIgnoresExplicitInterfaces(), LocalIntf1.class.getName()))
                        .check();
    }

    @Test
    public void testEmptyLocalAndXMLLocalBeanInterfacesStrict() throws Exception {
        factory.setEJBRuntimeVersion(createEJBRuntimeVersionServiceReference(EJBRuntimeVersion.VERSION_3_2));
        testEmptyLocalAndXMLLocalBeanInterfacesCompatibility();
    }

    @Test
    public void testEmptyLocalAndXMLLocalBeanInterfacesCompatibility() throws Exception {
        annoMockery.addClass(TestEmptyLocal.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestEmptyLocal.class.getSimpleName())
                                        .localBean()
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestEmptyLocal.class)
                                        .xml()
                                        .localBean()
                                        .localBusinessIf(factory.isEmptyAnnotationIgnoresExplicitInterfaces(), LocalIntf1.class.getName()))
                        .check();
    }

    @Test
    public void testEmptyLocalAndLocalHomeInterfaces() throws Exception {
        annoMockery.addClass(TestEmptyLocalAndLocalHome.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestEmptyLocalAndLocalHome.class)
                                        .local(LocalHomeIntf.class.getName(), null)
                                        .localBusiness(LocalIntf1.class.getName()))
                        .check();
    }

    @Test
    public void testEmptyLocalAndXMLLocalHomeInterfaces() throws Exception {
        annoMockery.addClass(TestEmptyLocal.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestEmptyLocal.class.getSimpleName())
                                        .local(LocalHomeIntf.class.getName(), LocalCompIntf.class.getName())
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestEmptyLocal.class)
                                        .xml()
                                        .local(LocalHomeIntf.class.getName(), LocalCompIntf.class.getName())
                                        .localBusiness(LocalIntf1.class.getName()))
                        .check();
    }

    @Test
    public void testRemoteInterface() throws Exception {
        annoMockery.addClass(TestRemote.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestRemote.class)
                                        .remoteBusiness(RemoteIntf1.class.getName()))
                        .check();
    }

    @Test
    public void testRemoteAndImplementsInterface() throws Exception {
        annoMockery.addClass(TestRemoteAndImplements.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestRemoteAndImplements.class)
                                        .remoteBusiness(RemoteIntf1.class.getName()))
                        .check();
    }

    @Test
    public void testEmptyRemoteInterface() throws Exception {
        annoMockery.addClass(TestEmptyRemote.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestEmptyRemote.class)
                                        .remoteBusiness(RemoteIntf1.class.getName()))
                        .check();
    }

    @Test
    public void testEmptyRemoteTwoImplementsInterfaces() throws Exception {
        annoMockery.addClass(TestEmptyRemoteTwoImplements.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestEmptyRemoteTwoImplements.class)
                                        .remoteBusiness(RemoteIntf1.class.getName())
                                        .remoteBusiness(RemoteIntf2.class.getName()))
                        .timers()
                        .check();
    }

    @Test
    public void testEmptyRemoteAndAnnotatedLocalInterfaces() throws Exception {
        annoMockery.addClass(TestEmptyRemoteAndAnnotatedLocal.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestEmptyRemoteAndAnnotatedLocal.class)
                                        .localBusiness(LocalAnnotatedIntf.class.getName()))
                        .check();
    }

    @Test
    public void testEmptyRemoteAndAnnotatedRemoteInterfaces() throws Exception {
        annoMockery.addClass(TestEmptyRemoteAndAnnotatedRemote.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestEmptyRemoteAndAnnotatedRemote.class)
                                        .remoteBusiness(RemoteAnnotatedIntf.class.getName()))
                        .check();
    }

    @Test
    public void testEmptyRemoteAndXMLLocalInterfacesConflictStrict() throws Exception {
        factory.setEJBRuntimeVersion(createEJBRuntimeVersionServiceReference(EJBRuntimeVersion.VERSION_3_2));
        testEmptyRemoteAndXMLLocalInterfacesConflictCompatibility();
    }

    @Test
    public void testEmptyRemoteAndXMLLocalInterfacesConflictCompatibility() throws Exception {
        annoMockery.addClass(TestEmptyRemote.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestEmptyRemote.class.getSimpleName())
                                        .localBusiness(RemoteIntf1.class.getName())
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestEmptyRemote.class)
                                        .xml()
                                        .localBusiness(RemoteIntf1.class.getName()))
                        .check();
    }

    @Test
    public void testEmptyRemoteAndXMLRemoteInterfacesStrict() throws Exception {
        factory.setEJBRuntimeVersion(createEJBRuntimeVersionServiceReference(EJBRuntimeVersion.VERSION_3_2));
        testEmptyRemoteAndXMLRemoteInterfacesCompatibility();
    }

    @Test
    public void testEmptyRemoteAndXMLRemoteInterfacesCompatibility() throws Exception {
        annoMockery.addClass(TestEmptyRemote.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestEmptyRemote.class.getSimpleName())
                                        .remoteBusiness(RemoteIntf2.class.getName())
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestEmptyRemote.class)
                                        .xml()
                                        .remoteBusinessIf(factory.isEmptyAnnotationIgnoresExplicitInterfaces(), RemoteIntf1.class.getName())
                                        .remoteBusiness(RemoteIntf2.class.getName()))
                        .check();
    }

    @Test
    public void testEmptyRemoteAndLocalBeanInterfacesStrict() throws Exception {
        factory.setEJBRuntimeVersion(createEJBRuntimeVersionServiceReference(EJBRuntimeVersion.VERSION_3_2));
        testEmptyRemoteAndLocalBeanInterfacesCompatibility();
    }

    @Test
    public void testEmptyRemoteAndLocalBeanInterfacesCompatibility() throws Exception {
        annoMockery.addClass(TestEmptyRemoteAndLocalBean.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestEmptyRemoteAndLocalBean.class)
                                        .localBean()
                                        .remoteBusinessIf(factory.isEmptyAnnotationIgnoresExplicitInterfaces(), RemoteIntf1.class.getName()))
                        .check();
    }

    @Test
    public void testEmptyRemoteAndXMLLocalBeanInterfacesStrict() throws Exception {
        factory.setEJBRuntimeVersion(createEJBRuntimeVersionServiceReference(EJBRuntimeVersion.VERSION_3_2));
        testEmptyRemoteAndXMLLocalBeanInterfacesCompatibility();
    }

    @Test
    public void testEmptyRemoteAndXMLLocalBeanInterfacesCompatibility() throws Exception {
        annoMockery.addClass(TestEmptyRemote.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestEmptyRemote.class.getSimpleName())
                                        .localBean()
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestEmptyRemote.class)
                                        .xml()
                                        .localBean()
                                        .remoteBusinessIf(factory.isEmptyAnnotationIgnoresExplicitInterfaces(), RemoteIntf1.class.getName()))
                        .check();
    }

    @Test
    public void testEmptyRemoteAndXMLRemoteHomeInterfaces() throws Exception {
        annoMockery.addClass(TestEmptyRemote.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestEmptyRemote.class.getSimpleName())
                                        .remote(RemoteHomeIntf.class.getName(), RemoteCompIntf.class.getName())
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestEmptyRemote.class)
                                        .xml()
                                        .remote(RemoteHomeIntf.class.getName(), RemoteCompIntf.class.getName())
                                        .remoteBusiness(RemoteIntf1.class.getName()))
                        .check();
    }

    @Test
    public void testAnnotatedLocalInterface() throws Exception {
        annoMockery.addClass(TestAnnotatedLocal.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestAnnotatedLocal.class)
                                        .localBusiness(LocalAnnotatedIntf.class.getName()))
                        .timers()
                        .check();
    }

    @Test(expected = EJBConfigurationException.class)
    public void testAnnotatedLocalNonEmptyInterfaceError() throws Exception {
        annoMockery.addClass(TestAnnotatedLocalNonEmptyError.class);
        outputMgr.expectError("CNTR4111E");
        createModuleInitData(null);
    }

    @Test
    public void testAnnotatedRemoteInterface() throws Exception {
        annoMockery.addClass(TestAnnotatedRemote.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestAnnotatedRemote.class)
                                        .remoteBusiness(RemoteAnnotatedIntf.class.getName()))
                        .timers()
                        .check();
    }

    @Test(expected = EJBConfigurationException.class)
    public void testAnnotatedRemoteNonEmptyInterfaceError() throws Exception {
        annoMockery.addClass(TestAnnotatedRemoteNonEmptyError.class);
        outputMgr.expectError("CNTR4111E");
        createModuleInitData(null);
    }

    @Test
    public void testMessageListenerInterface() throws Exception {
        annoMockery.addClass(TestMessageListener.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_MESSAGE_DRIVEN, TestMessageListener.class)
                                        .messageListener(LocalIntf1.class.getName()))
                        .check();
    }

    @Test
    public void testDefaultMessageListenerInterface() throws Exception {
        annoMockery.addClass(TestDefaultMessageListener.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_MESSAGE_DRIVEN, TestDefaultMessageListener.class)
                                        .messageListener(LocalIntf1.class.getName()))
                        .timers()
                        .check();
    }

    @Test
    public void testDefaultMessageListenerInterfaceXML() throws Exception {
        annoMockery.addClass(TestDefaultMessageListenerXML.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.messageDriven(TestDefaultMessageListenerXML.class.getSimpleName())
                                        .ejbClass(TestDefaultMessageListenerXML.class.getName())
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_MESSAGE_DRIVEN, TestDefaultMessageListenerXML.class)
                                        .xml()
                                        .messageListener(LocalIntf1.class.getName()))
                        .timers()
                        .check();
    }

    @Test
    public void testWebService() throws Exception {
        annoMockery.addClass(TestWebService.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestWebService.class)
                                        .webService())
                        .check();
    }

    @Test
    public void testWebServiceProvider() throws Exception {
        annoMockery.addClass(TestWebServiceProvider.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestWebServiceProvider.class)
                                        .webService())
                        .check();
    }

    @Test
    public void testBMT() throws Exception {
        annoMockery.addClass(TestBMT.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestBMT.class)
                                        .localBean()
                                        .bmt())
                        .check();
    }

    @Test
    public void testCMTMixed() throws Exception {
        annoMockery.addClass(TestStateless.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestStateless.class.getSimpleName())
                                        .transactionType(Session.TRANSACTION_TYPE_CONTAINER)
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestStateless.class)
                                        .xml()
                                        .localBean())
                        .check();
    }

    @Test
    public void testBMTMixed() throws Exception {
        annoMockery.addClass(TestStateless.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestStateless.class.getSimpleName())
                                        .transactionType(Session.TRANSACTION_TYPE_BEAN)
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestStateless.class)
                                        .xml()
                                        .localBean()
                                        .bmt())
                        .check();
    }

    @Test
    public void testStatefulPassivationCapableFalseV31() throws Exception {
        annoMockery.addClass(TestStatefulPassivationCapableFalse.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATEFUL, TestStatefulPassivationCapableFalse.class)
                                        .localBean())
                        .check();
    }

    @Test
    public void testStatefulPassivationCapableFalseV32() throws Exception {
        factory.setEJBRuntimeVersion(createEJBRuntimeVersionServiceReference(EJBRuntimeVersion.VERSION_3_2));
        annoMockery.addClass(TestStatefulPassivationCapableFalse.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATEFUL, TestStatefulPassivationCapableFalse.class)
                                        .localBean()
                                        .passivationIncapable())
                        .check();
    }

    @Test
    public void testStatefulPassivationCapableMixedDefault() throws Exception {
        annoMockery.addClass(TestStateful.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestStateful.class.getSimpleName())
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATEFUL, TestStateful.class)
                                        .xml()
                                        .localBean())
                        .check();
    }

    @Test
    public void testStatefulPassivationCapableMixedFalse() throws Exception {
        annoMockery.addClass(TestStateful.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestStateful.class.getSimpleName())
                                        .passivationCapable(false)
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATEFUL, TestStateful.class)
                                        .xml()
                                        .passivationIncapable()
                                        .localBean())
                        .check();
    }

    @Test
    public void testStatefulPassivationCapableMixedTrue() throws Exception {
        annoMockery.addClass(TestStateful.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestStateful.class.getSimpleName())
                                        .passivationCapable(true)
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATEFUL, TestStateful.class)
                                        .xml()
                                        .localBean())
                        .check();
    }

    @Test
    public void testStartup() throws Exception {
        annoMockery.addClass(TestStartup.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_SINGLETON, TestStartup.class)
                                        .localBean()
                                        .startup())
                        .check();
    }

    @Test
    public void testStartupMixed() throws Exception {
        annoMockery.addClass(TestSingleton.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestSingleton.class.getSimpleName())
                                        .initOnStartup(true)
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_SINGLETON, TestSingleton.class)
                                        .xml()
                                        .localBean()
                                        .startup())
                        .check();
    }

    @Test
    public void testStartupMixedOverride() throws Exception {
        annoMockery.addClass(TestStartup.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestStartup.class.getSimpleName())
                                        .initOnStartup(false)
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_SINGLETON, TestStartup.class)
                                        .xml()
                                        .localBean())
                        .check();
    }

    @Test
    public void testDependsOn() throws Exception {
        annoMockery.addClass(TestDependsOn.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_SINGLETON, TestDependsOn.class)
                                        .localBean()
                                        .dependsOn(TestDependsOn.DEPENDS_ON_1, TestDependsOn.DEPENDS_ON_2))
                        .check();
    }

    @Test
    public void testDependsOnMixed() throws Exception {
        annoMockery.addClass(TestSingleton.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestSingleton.class.getSimpleName())
                                        .dependsOn(TestDependsOn.DEPENDS_ON_1, TestDependsOn.DEPENDS_ON_2)
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_SINGLETON, TestSingleton.class)
                                        .xml()
                                        .localBean()
                                        .dependsOn(TestDependsOn.DEPENDS_ON_1, TestDependsOn.DEPENDS_ON_2))
                        .check();
    }

    @Test
    public void testActivationConfigProperties() throws Exception {
        annoMockery.addClass(TestActivationConfigProperties.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_MESSAGE_DRIVEN, TestActivationConfigProperties.class)
                                        .activationConfigProperty(TestActivationConfigProperties.NAME1, TestActivationConfigProperties.VALUE1)
                                        .activationConfigProperty(TestActivationConfigProperties.NAME2, TestActivationConfigProperties.VALUE2))
                        .check();
    }

    @Test
    public void testActivationConfigPropertiesMixed() throws Exception {
        annoMockery.addClass(TestMessageDriven.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.messageDriven(TestMessageDriven.class.getSimpleName())
                                        .activationConfigProperty(TestActivationConfigProperties.NAME1, "override")
                                        .activationConfigProperty(TestActivationConfigProperties.NAME2, TestActivationConfigProperties.VALUE2)
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_MESSAGE_DRIVEN, TestMessageDriven.class)
                                        .xml()
                                        .activationConfigProperty(TestActivationConfigProperties.NAME1, "override")
                                        .activationConfigProperty(TestActivationConfigProperties.NAME2, TestActivationConfigProperties.VALUE2))
                        .check();
    }

    @Test
    public void testTimeout() throws Exception {
        annoMockery.addClass(TestTimeout.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestTimeout.class)
                                        .localBean())
                        .timers()
                        .check();
    }

    @Test
    public void testStatelessTimeoutMixed() throws Exception {
        annoMockery.addClass(TestStateless.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestStateless.class.getSimpleName())
                                        .timeoutMethod("timeout")
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestStateless.class)
                                        .xml()
                                        .localBean())
                        .timers()
                        .check();
    }

    @Test
    public void testMessageDrivenTimeoutMixed() throws Exception {
        annoMockery.addClass(TestMessageDriven.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.messageDriven(TestMessageDriven.class.getSimpleName())
                                        .timeoutMethod("timeout")
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_MESSAGE_DRIVEN, TestMessageDriven.class)
                                        .xml())
                        .timers()
                        .check();
    }

    @Test
    public void testSchedule() throws Exception {
        annoMockery.addClass(TestSchedule.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestSchedule.class)
                                        .localBean()
                                        .scheduleTimers())
                        .timers()
                        .check();
    }

    @Test
    public void testStatelessScheduleMixed() throws Exception {
        annoMockery.addClass(TestStateless.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestStateless.class.getSimpleName())
                                        .timer()
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestStateless.class)
                                        .xml()
                                        .localBean()
                                        .scheduleTimers())
                        .timers()
                        .check();
    }

    @Test
    public void testMessageDrivenScheduleMixed() throws Exception {
        annoMockery.addClass(TestMessageDriven.class);
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.messageDriven(TestMessageDriven.class.getSimpleName())
                                        .timer()
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_MESSAGE_DRIVEN, TestMessageDriven.class)
                                        .xml()
                                        .scheduleTimers())
                        .timers()
                        .check();
    }

    @Test
    public void testSchedules() throws Exception {
        annoMockery.addClass(TestSchedules.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestSchedules.class)
                                        .localBean()
                                        .scheduleTimers())
                        .timers()
                        .check();
    }

    @Test
    public void testManagedBeanTimers() throws Exception {
        factory.setManagedBeanRuntime(managedBeanRuntimeRef);
        annoMockery.addClass(TestManagedBeanTimedObject.class);
        annoMockery.addClass(TestManagedBeanTimeout.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_MANAGED_BEAN, TestManagedBeanTimedObject.class))
                        .bean(new BeanInitDataChecker(TYPE_MANAGED_BEAN, TestManagedBeanTimeout.class))
                        .check();
    }

    @Test
    public void testMetadataCompleteVersion21() throws Exception {
        annoMockery.addClass(TestStateless.class);
        EJBJar ejbJar = ejbJarMockery.versionId(EJBJar.VERSION_2_1)
                        .enterpriseBean(ejbJarMockery.versionId(21).session(TestStateless.class.getSimpleName())
                                        .ejbClass(TestStateless.class.getName())
                                        .type(Session.SESSION_TYPE_STATEFUL)
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar)).metadataComplete()
                        .bean(new BeanInitDataChecker(TYPE_STATEFUL, TestStateless.class)
                                        .xml())
                        .check();
    }

    @Test
    public void testMetadataCompleteVersion30() throws Exception {
        annoMockery.addClass(TestStateless.class);
        EJBJar ejbJar = ejbJarMockery.metadataComplete()
                        .versionId(EJBJar.VERSION_3_0)
                        .enterpriseBean(ejbJarMockery.session(TestStateless.class.getSimpleName())
                                        .ejbClass(TestStateless.class.getName())
                                        .type(Session.SESSION_TYPE_STATEFUL)
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .metadataComplete()
                        .bean(new BeanInitDataChecker(TYPE_STATEFUL, TestStateless.class)
                                        .xml())
                        .check();
    }

    @Test
    public void testMetadataCompleteManagedBean() throws Exception {
        factory.setManagedBeanRuntime(managedBeanRuntimeRef);
        annoMockery.addClass(TestManagedBean.class);
        EJBJar ejbJar = ejbJarMockery
                        .metadataComplete()
                        .enterpriseBean(ejbJarMockery.session(TestBasic.class.getSimpleName())
                                        .ejbClass(TestBasic.class.getName())
                                        .type(Session.SESSION_TYPE_STATELESS)
                                        .remote(RemoteHomeIntf.class.getName(), RemoteCompIntf.class.getName())
                                        .local(LocalHomeIntf.class.getName(), LocalCompIntf.class.getName())
                                        .remoteBusiness(RemoteIntf1.class.getName(), RemoteIntf2.class.getName())
                                        .localBusiness(LocalIntf1.class.getName(), LocalIntf2.class.getName())
                                        .localBean()
                                        .mock())
                        .mock();
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .metadataComplete()
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestBasic.class)
                                        .xml()
                                        .remote(RemoteHomeIntf.class.getName(), RemoteCompIntf.class.getName())
                                        .local(LocalHomeIntf.class.getName(), LocalCompIntf.class.getName())
                                        .remoteBusiness(RemoteIntf1.class.getName(), RemoteIntf2.class.getName())
                                        .localBusiness(LocalIntf1.class.getName(), LocalIntf2.class.getName())
                                        .localBean())
                        .bean(new BeanInitDataChecker(TYPE_MANAGED_BEAN, TestManagedBean.class))
                        .check();
    }

    @Test
    public void testMatchingExtName() throws Exception {
        EJBJar ejbJar = ejbJarMockery
                        .metadataComplete()
                        .enterpriseBean(ejbJarMockery.session(TestBasic.class.getSimpleName())
                                        .ejbClass(TestBasic.class.getName())
                                        .type(Session.SESSION_TYPE_STATELESS)
                                        .mock())
                        .mock();

        EJBJarExt ejbJarExt = ejbJarExtMockery
                        .sessionBean(ejbJarExtMockery.session(TestBasic.class.getSimpleName())
                                        .mock())
                        .sessionBean(ejbJarExtMockery.session("UndefinedSessionBeanName")
                                        .mock())
                        .mock();

        // Because the name and type of the Enterprise Bean in the ejb-jar.xml file matches
        // the name and type of *an* Enterprise Bean defined in the ibm-ejb-jar-ext.xml file,
        // the BeanInitDataImpl's enterpriseBeanExt field should not be null --> so we'll call .xmlExt()
        new ModuleInitDataChecker(createModuleInitData(ejbJar, ejbJarExt))
                        .metadataComplete()
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestBasic.class)
                                        .xml()
                                        .xmlExt())
                        .check();
    }

    @Test
    public void testNonMatchingExtName() throws Exception {
        EJBJar ejbJar = ejbJarMockery
                        .metadataComplete()
                        .enterpriseBean(ejbJarMockery.session(TestBasic.class.getSimpleName())
                                        .ejbClass(TestBasic.class.getName())
                                        .type(Session.SESSION_TYPE_STATELESS)
                                        .mock())
                        .mock();

        EJBJarExt ejbJarExt = ejbJarExtMockery
                        .sessionBean(ejbJarExtMockery.session("UndefinedSessionBeanName")
                                        .mock())
                        .mock();

        // Because the name of the Enterprise Bean in the ejb-jar.xml does not match
        // any Enterprise Bean in the ibm-ejb-jar-ext.xml file of that name. Do not call xmlExt().
        new ModuleInitDataChecker(createModuleInitData(ejbJar, ejbJarExt))
                        .metadataComplete()
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestBasic.class)
                                        .xml())
                        .check();
    }

    @Test
    public void testNonMatchingExtType() throws Exception {
        EJBJar ejbJar = ejbJarMockery
                        .metadataComplete()
                        .enterpriseBean(ejbJarMockery.session(TestBasic.class.getSimpleName())
                                        .ejbClass(TestBasic.class.getName())
                                        .type(Session.SESSION_TYPE_STATELESS)
                                        .mock())
                        .mock();

        EJBJarExt ejbJarExt = ejbJarExtMockery
                        .messageDrivenBean(ejbJarExtMockery.messageDriven(TestBasic.class.getName())
                                        .mock())
                        .mock();

        // Because the type of the "name"d Enterprise Bean in the ejb-jar.xml does not match
        // the type of the same "name"d Enterprise Bean in the ibm-ejb-jar-ext.xml file.
        // Do not call xmlExt().
        new ModuleInitDataChecker(createModuleInitData(ejbJar, ejbJarExt))
                        .metadataComplete()
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestBasic.class)
                                        .xml())
                        .check();
    }

    @Test
    public void testMatchingBndName() throws Exception {
        EJBJar ejbJar = ejbJarMockery
                        .metadataComplete()
                        .enterpriseBean(ejbJarMockery.session(TestBasic.class.getSimpleName())
                                        .ejbClass(TestBasic.class.getName())
                                        .type(Session.SESSION_TYPE_STATELESS)
                                        .mock())
                        .mock();

        EJBJarBnd ejbJarBnd = ejbJarBndMockery
                        .sessionBean(ejbJarBndMockery.session(TestBasic.class.getSimpleName())
                                        .mock())
                        .sessionBean(ejbJarBndMockery.session("UndefinedSessionBeanName")
                                        .mock())
                        .mock();

        new ModuleInitDataChecker(createModuleInitData(ejbJar, ejbJarBnd))
                        .metadataComplete()
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestBasic.class)
                                        .xml()
                                        .xmlBnd())
                        .check();
    }

    @Test
    public void testNonMatchingBndName() throws Exception {
        EJBJar ejbJar = ejbJarMockery
                        .metadataComplete()
                        .enterpriseBean(ejbJarMockery.session(TestBasic.class.getSimpleName())
                                        .ejbClass(TestBasic.class.getName())
                                        .type(Session.SESSION_TYPE_STATELESS)
                                        .mock())
                        .mock();

        EJBJarBnd ejbJarBnd = ejbJarBndMockery
                        .sessionBean(ejbJarBndMockery.session("UndefinedSessionBeanName")
                                        .mock())
                        .mock();

        new ModuleInitDataChecker(createModuleInitData(ejbJar, ejbJarBnd))
                        .metadataComplete()
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestBasic.class)
                                        .xml())
                        .check();
    }

    @Test
    public void testNonMatchingBndType() throws Exception {
        EJBJar ejbJar = ejbJarMockery
                        .metadataComplete()
                        .enterpriseBean(ejbJarMockery.session(TestBasic.class.getSimpleName())
                                        .ejbClass(TestBasic.class.getName())
                                        .type(Session.SESSION_TYPE_STATELESS)
                                        .mock())
                        .mock();

        EJBJarBnd ejbJarBnd = ejbJarBndMockery
                        .messageDrivenBean(ejbJarBndMockery.messageDriven(TestBasic.class.getName())
                                        .mock())
                        .mock();

        new ModuleInitDataChecker(createModuleInitData(ejbJar, ejbJarBnd))
                        .metadataComplete()
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestBasic.class)
                                        .xml())
                        .check();
    }

    @Test
    public void testMatchingBndExtName() throws Exception {
        EJBJar ejbJar = ejbJarMockery
                        .metadataComplete()
                        .enterpriseBean(ejbJarMockery.session(TestStateless.class.getSimpleName())
                                        .ejbClass(TestStateless.class.getName())
                                        .type(Session.SESSION_TYPE_STATELESS)
                                        .mock())
                        .mock();

        EJBJarExt ejbJarExt = ejbJarExtMockery
                        .sessionBean(ejbJarExtMockery.session(TestStateless.class.getSimpleName())
                                        .mock())
                        .mock();

        EJBJarBnd ejbJarBnd = ejbJarBndMockery
                        .sessionBean(ejbJarBndMockery.session(TestStateless.class.getSimpleName())
                                        .mock())
                        .mock();

        new ModuleInitDataChecker(createModuleInitData(ejbJar, ejbJarExt, ejbJarBnd))
                        .metadataComplete()
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestStateless.class)
                                        .xml()
                                        .xmlExt()
                                        .xmlBnd())
                        .check();
    }

    @Test
    public void testInterceptorsFromXML() throws Exception {
        EJBJar ejbJar = ejbJarMockery
                        .enterpriseBean(ejbJarMockery.session(TestStateless.class.getSimpleName())
                                        .ejbClass(TestStateless.class.getName())
                                        .type(Session.SESSION_TYPE_STATELESS)
                                        .mock())
                        .interceptors(Interceptor1.class.getName(), Interceptor2.class.getName())
                        .mock();
        annoMockery.addClass(TestStateless.class);
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestStateless.class)
                                        .localBean()
                                        .xml())
                        .ejbInterceptors(Interceptor1.class.getName(), Interceptor2.class.getName())
                        .check();
    }

    @Test
    public void testInterceptorsFromAnnotations() throws Exception {
        annoMockery.addClass(TestInterceptors.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestInterceptors.class)
                                        .localBusiness(LocalIntf1.class.getName()))
                        .ejbInterceptors(Interceptor1.class.getName(), Interceptor2.class.getName())
                        .check();
    }

    @Test
    public void testInterceptorsFromBoth() throws Exception {
        EJBJar ejbJar = ejbJarMockery
                        .interceptors(Interceptor3.class.getName())
                        .mock();

        annoMockery.addClass(TestInterceptors.class);
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestInterceptors.class)
                                        .localBusiness(LocalIntf1.class.getName()))
                        .ejbInterceptors(Interceptor1.class.getName(), Interceptor2.class.getName(), Interceptor3.class.getName())
                        .check();
    }

    @Test
    public void testInterceptorsMetadataComplete() throws Exception {
        EJBJar ejbJar = ejbJarMockery
                        .metadataComplete()
                        .enterpriseBean(ejbJarMockery.session(TestInterceptors.class.getSimpleName())
                                        .ejbClass(TestInterceptors.class.getName())
                                        .type(Session.SESSION_TYPE_STATELESS)
                                        .mock())
                        .interceptors(Interceptor3.class.getName())
                        .mock();
        annoMockery.addClass(TestInterceptors.class);
        new ModuleInitDataChecker(createModuleInitData(ejbJar))
                        .metadataComplete()
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestInterceptors.class)
                                        .xml())
                        .ejbInterceptors(Interceptor3.class.getName())
                        .check();
    }

    @Test
    public void testInterceptorsFromManagedBeans() throws Exception {
        factory.setManagedBeanRuntime(managedBeanRuntimeRef);
        annoMockery.addClass(TestManagedBeanInterceptors.class);
        new ModuleInitDataChecker(createModuleInitData(null))
                        .bean(new BeanInitDataChecker(TYPE_MANAGED_BEAN, TestManagedBeanInterceptors.class))
                        .mbInterceptors(Interceptor1.class.getName(), Interceptor2.class.getName())
                        .check();
    }

    @Test
    public void testInterceptorBindings() throws Exception {
        EJBJar ejbJar = ejbJarMockery
                        .metadataComplete()
                        .enterpriseBean(ejbJarMockery.session(TestStateless.class.getSimpleName())
                                        .ejbClass(TestStateless.class.getName())
                                        .type(Session.SESSION_TYPE_STATELESS)
                                        .mock())
                        .mock();

        EJBJarBnd ejbJarBnd = ejbJarBndMockery
                        .sessionBean(ejbJarBndMockery.session(TestStateless.class.getSimpleName())
                                        .mock())
                        .interceptor(ejbJarBndMockery.interceptor("InterceptorClassName1")
                                        .mock())
                        .interceptor(ejbJarBndMockery.interceptor("InterceptorClassName2")
                                        .mock())
                        .mock();

        new ModuleInitDataChecker(createModuleInitData(ejbJar, ejbJarBnd))
                        .metadataComplete()
                        .bean(new BeanInitDataChecker(TYPE_STATELESS, TestStateless.class)
                                        .xml()
                                        .xmlBnd())
                        .check();
    }
}