/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.security.javaeesec.CDIHelperTestWrapper;

import test.common.SharedOutputManager;

public class ModulePropertiesUtilsTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @SuppressWarnings("rawtypes")
    private CDI cdi;
    private ModulePropertiesUtilsDouble mpu;
    private ComponentMetaData cmd;
    private ModuleMetaData mmd;
    private J2EEName j2n;
    private HttpAuthenticationMechanism ham, ham2;
    private ModulePropertiesProvider mpp;
    private Instance<ModulePropertiesProvider> mppi;
    private Instance<HttpAuthenticationMechanism> hami;
    private BeanManager bm1, bm2;
    private CDIService cs;
    private CDIHelperTestWrapper chtw;
    private Bean bean1, bean2;
    private CreationalContext cc;

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.javaeesec.*=all");

    @Rule
    public final TestName testName = new TestName();

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
    }

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        cdi = mockery.mock(CDI.class);
        cmd = mockery.mock(ComponentMetaData.class);
        mmd = mockery.mock(ModuleMetaData.class);
        j2n = mockery.mock(J2EEName.class);
        ham = mockery.mock(HttpAuthenticationMechanism.class, "ham");
        ham2 = mockery.mock(HttpAuthenticationMechanism.class, "ham2");
        mpp = mockery.mock(ModulePropertiesProvider.class);
        mppi = mockery.mock(Instance.class, "mppi");
        hami = mockery.mock(Instance.class, "hami");
        bm1 = mockery.mock(BeanManager.class, "bm1");
        bm2 = mockery.mock(BeanManager.class, "bm2");
        cs = mockery.mock(CDIService.class);
        bean1 = mockery.mock(Bean.class, "bean1");
        bean2 = mockery.mock(Bean.class, "bean2");
        cc = mockery.mock(CreationalContext.class);
        chtw = new CDIHelperTestWrapper(mockery, bm2);
        chtw.setCDIService(cs);
        mpu = new ModulePropertiesUtilsDouble();
    }

    @After
    public void tearDown() throws Exception {
        chtw.unsetCDIService(cs);
        mockery.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    /**
     *
     */
    @Test
    public void testGetJ2EEModuleName() {
        final String MODULENAME = "ModuleName";
        withModuleName(MODULENAME);
        mpu.setComponentMetaData(cmd);
        assertTrue("A module name should be returned.", mpu.getJ2EEModuleName().equals(MODULENAME));
    }

    /**
     *
     */
    @Test
    public void testGetJ2EEModuleNameCMDNull() {
        mpu.setComponentMetaData(null);
        assertNull("null should be returned.", mpu.getJ2EEModuleName());
    }

    /**
     *
     */
    @Test
    public void testGetJ2EEApplicationName() {
        final String APPLNAME = "ApplicationName";
        withAppName(APPLNAME);
        mpu.setComponentMetaData(cmd);
        assertTrue("A module name should be returned.", mpu.getJ2EEApplicationName().equals(APPLNAME));
    }

    /**
     *
     */
    @Test
    public void testGetJ2EEApplicationNameCMDNull() {
        mpu.setComponentMetaData(null);
        assertNull("null should be returned.", mpu.getJ2EEApplicationName());
    }

    /**
     *
     */
    @Test
    public void testIsHttpAuthenticationMechanismNoMPP() {
        mpu.setComponentMetaData(cmd);
        mockery.checking(new Expectations() {
            {
                one(cdi).select(ModulePropertiesProvider.class);
                will(returnValue(null));
            }
        });
        assertFalse("false should be returned.", mpu.isHttpAuthenticationMechanism());
    }

    /**
     *
     */
    @Test
    public void testGetHttpAuthenticationMechanismNoMPP() {
        final String APPLNAME = "ApplicationName";
        withAppName(APPLNAME);
        mpu.setComponentMetaData(cmd);
        mockery.checking(new Expectations() {
            {
                one(cdi).select(ModulePropertiesProvider.class);
                will(returnValue(null));
            }
        });
        assertNull("null should be returned.", mpu.getHttpAuthenticationMechanism());
        assertTrue("CWWKS1913E  message was not logged", outputMgr.checkForStandardErr("CWWKS1913E:"));
    }

    /**
     *
     */
    @Test
    public void testGetHttpAuthenticationMechanismMPPUnsatisified() {
        final String APPLNAME = "ApplicationName";
        mpu.setComponentMetaData(cmd);
        withAppName(APPLNAME).withModulePropertiesProvider(true, false);
        assertNull("null should be returned.", mpu.getHttpAuthenticationMechanism());
        assertTrue("CWWKS1913E  message was not logged", outputMgr.checkForStandardErr("CWWKS1913E:"));
    }

    /**
     *
     */
    @Test
    public void testGetHttpAuthenticationMechanismMPPambiguous() {
        final String APPLNAME = "ApplicationName";
        mpu.setComponentMetaData(cmd);
        withAppName(APPLNAME).withModulePropertiesProvider(false, true);
        assertNull("null should be returned.", mpu.getHttpAuthenticationMechanism());
        assertTrue("CWWKS1913E  message was not logged", outputMgr.checkForStandardErr("CWWKS1913E:"));
    }

    /**
     *
     */
    @Test
    public void testGetHttpAuthenticationMechanismZeroAuthMech() {
        final String APPLNAME = "ApplicationName";
        final String MODULENAME = "ModuleName";
        List<Class> list = new ArrayList<Class>();
        withModuleName(MODULENAME).withAppName(APPLNAME).withModulePropertiesProvider(false, false).withAuthMechClassList(list);
        mpu.setComponentMetaData(cmd);
        assertNull("null should be returned.", mpu.getHttpAuthenticationMechanism());
        assertTrue("CWWKS1915E  message with application and module name, and list of classeswas not logged",
                   outputMgr.checkForStandardErr("CWWKS1915E:.*" + MODULENAME + ".*" + APPLNAME + ".*"));
    }

    /**
     *
     */
    @Test
    public void testGetHttpAuthenticationMechanismMultipleAuthMechs() {
        final String APPLNAME = "ApplicationName";
        final String MODULENAME = "ModuleName";
        List<Class> list = new ArrayList<Class>();
        list.add(String.class);
        list.add(String.class);
        withModuleName(MODULENAME).withAppName(APPLNAME).withModulePropertiesProvider(false, false).withAuthMechClassList(list);
        mpu.setComponentMetaData(cmd);
        assertNull("null should be returned.", mpu.getHttpAuthenticationMechanism());
        assertTrue("CWWKS1915E  message with application and module name, and list of classeswas not logged",
                   outputMgr.checkForStandardErr("CWWKS1915E:.*" + MODULENAME + ".*" + APPLNAME + ".*String.*String.*"));
    }

    /**
     *
     */
    @Test
    public void testGetHttpAuthenticationMechanismOneAuthMech() {
        final String APPLNAME = "ApplicationName";
        final String MODULENAME = "ModuleName";
        List<Class> list = new ArrayList<Class>();
        list.add(String.class);
        withModulePropertiesProvider(false, false).withAuthMechClassList(list).withAuthMechImpl(String.class);
        mpu.setComponentMetaData(cmd);
        assertEquals("HAM should be returned.", mpu.getHttpAuthenticationMechanism(), ham);
    }

    /**
     *
     */
    @Test
    public void testGetHttpAuthenticationMechanismOneAuthMechUnsatisfiedSameBM() {
        final String APPLNAME = "ApplicationName";
        final String MODULENAME = "ModuleName";
        List<Class> list = new ArrayList<Class>();
        list.add(String.class);
        withModuleName(MODULENAME).withAppName(APPLNAME).withModulePropertiesProvider(false, false).withAuthMechClassList(list).withAuthMechImplInstance(String.class, true, false);
        withBeanManager(bm2);
        mpu.setComponentMetaData(cmd);
        assertNull("null should be returned.", mpu.getHttpAuthenticationMechanism());
        assertTrue("CWWKS1912E  message with application and module name not logged",
                   outputMgr.checkForStandardErr("CWWKS1912E:.*" + MODULENAME + ".*" + APPLNAME + ".*"));
    }

    /**
     *
     */
    @Test
    public void testGetHttpAuthenticationMechanismOneAuthMechAmbiguousSameBM() {
        final String APPLNAME = "ApplicationName";
        final String MODULENAME = "ModuleName";
        List<Class> list = new ArrayList<Class>();
        list.add(String.class);
        withModuleName(MODULENAME).withAppName(APPLNAME).withModulePropertiesProvider(false, false).withAuthMechClassList(list).withAuthMechImplInstance(String.class, false, true);
        withBeanManager(bm2);
        mpu.setComponentMetaData(cmd);
        assertNull("null should be returned.", mpu.getHttpAuthenticationMechanism());
        assertTrue("CWWKS1912E  message with application and module name not logged",
                   outputMgr.checkForStandardErr("CWWKS1912E:.*" + MODULENAME + ".*" + APPLNAME + ".*"));
    }

    @Test
    public void testGetHttpAuthenticationMechanismOneAuthMechUnsatisfiedOneModuleHAM() {
        final List<Class> list = new ArrayList<Class>();
        list.add(String.class);
        final Set<Bean> hams = new HashSet<Bean>();
        hams.add(bean1);
        withModulePropertiesProvider(false, false).withAuthMechClassList(list).withAuthMechImplInstance(String.class, true, false);
        withBeanManager(bm1).withModuleHAM(String.class, hams);
        mpu.setComponentMetaData(cmd);
        assertEquals("HAM should be returned.", mpu.getHttpAuthenticationMechanism(), ham);
    }

    @Test
    public void testGetHttpAuthenticationMechanismOneAuthMechUnsatisfiedZeroModuleHAM() {
        final String APPLNAME = "ApplicationName";
        final String MODULENAME = "ModuleName";
        final List<Class> list = new ArrayList<Class>();
        list.add(String.class);
        final Set<Bean> hams = new HashSet<Bean>();
        withModuleName(MODULENAME).withAppName(APPLNAME).withModulePropertiesProvider(false, false).withAuthMechClassList(list).withAuthMechImplInstance(String.class, true, false);
        withBeanManager(bm1).withModuleHAM(String.class, hams);
        mpu.setComponentMetaData(cmd);
        assertNull("null should be returned.", mpu.getHttpAuthenticationMechanism());
        assertTrue("CWWKS1912E  message with application and module name not logged",
                   outputMgr.checkForStandardErr("CWWKS1912E:.*" + MODULENAME + ".*" + APPLNAME + ".*"));
    }

    @Test
    public void testGetHttpAuthenticationMechanismOneAuthMechAmbiguousMultipleModuleHAM() {
        final String APPLNAME = "ApplicationName";
        final String MODULENAME = "ModuleName";
        final List<Class> list = new ArrayList<Class>();
        list.add(String.class);
        final Set<Bean> hams = new HashSet<Bean>();
        hams.add(bean1);
        hams.add(bean2);
        withModuleName(MODULENAME).withAppName(APPLNAME).withModulePropertiesProvider(false, false).withAuthMechClassList(list).withAuthMechImplInstance(String.class, false, true);
        withBeanManager(bm1).withModuleHAM(String.class, hams);
        mpu.setComponentMetaData(cmd);
        assertNull("null should be returned.", mpu.getHttpAuthenticationMechanism());
        assertTrue("CWWKS1915E  message with application and module name, and list of classeswas not logged",
                   outputMgr.checkForStandardErr("CWWKS1915E:.*" + MODULENAME + ".*" + APPLNAME + ".*HttpAuthenticationMechanism.*HttpAuthenticationMechanism.*"));
    }

    /*************** support methods **************/
    private ModulePropertiesUtilsTest withAppName(final String name) {
        mockery.checking(new Expectations() {
            {
                one(cmd).getJ2EEName();
                will(returnValue(j2n));
                one(j2n).getApplication();
                will(returnValue(name));
            }
        });
        return this;
    }

    private ModulePropertiesUtilsTest withModuleName(final String name) {
        mockery.checking(new Expectations() {
            {
                one(cmd).getModuleMetaData();
                will(returnValue(mmd));
                one(mmd).getJ2EEName();
                will(returnValue(j2n));
                one(j2n).getModule();
                will(returnValue(name));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private ModulePropertiesUtilsTest withModulePropertiesProvider(boolean isUnsatisfied, boolean isAmbiguous) {
        mockery.checking(new Expectations() {
            {
                one(cdi).select(ModulePropertiesProvider.class);
                will(returnValue(mppi));
                allowing(mppi).isUnsatisfied();
                will(returnValue(isUnsatisfied));
                allowing(mppi).isAmbiguous();
                will(returnValue(isAmbiguous));
            }
        });
        return this;
    }

    private ModulePropertiesUtilsTest withAuthMechClassList(List<Class> list) {
        mockery.checking(new Expectations() {
            {
                one(mppi).get();
                will(returnValue(mpp));
                one(mpp).getAuthMechClassList();
                will(returnValue(list));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private ModulePropertiesUtilsTest withAuthMechImplInstance(Class implClass, boolean isUnsatisfied, boolean isAmbiguous) {
        mockery.checking(new Expectations() {
            {
                one(cdi).select(implClass);
                will(returnValue(hami));
                allowing(hami).isUnsatisfied();
                will(returnValue(isUnsatisfied));
                allowing(hami).isAmbiguous();
                will(returnValue(isAmbiguous));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private ModulePropertiesUtilsTest withAuthMechImpl(Class implClass) {
        mockery.checking(new Expectations() {
            {
                one(cdi).select(implClass);
                will(returnValue(hami));
                allowing(hami).isUnsatisfied();
                will(returnValue(false));
                allowing(hami).isAmbiguous();
                will(returnValue(false));
                one(hami).get();
                will(returnValue(ham));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private ModulePropertiesUtilsTest withBeanManager(final BeanManager bm) {
        mockery.checking(new Expectations() {
            {
                one(cdi).getBeanManager();
                will(returnValue(bm));
            }
        });
        return this;
    }

    @SuppressWarnings("unchecked")
    private ModulePropertiesUtilsTest withModuleHAM(Class bc, Set<Bean> hams) {
        switch (hams.size()) {
            case 0:
                mockery.checking(new Expectations() {
                    {
                        never(bm2).getReference(with(any(Bean.class)), with(any(Type.class)), with(any(CreationalContext.class)));
                    }
                });
                break;
            case 1:
                mockery.checking(new Expectations() {
                    {
                        one(bm2).getReference(with(any(Bean.class)), with(any(Type.class)), with(any(CreationalContext.class)));
                        will(returnValue(ham));
                    }
                });
                break;
            case 2:
                mockery.checking(new Expectations() {
                    {
                        one(bm2).getReference(with(any(Bean.class)), with(any(Type.class)), with(any(CreationalContext.class)));
                        will(returnValue(ham));
                        one(bm2).getReference(with(any(Bean.class)), with(any(Type.class)), with(any(CreationalContext.class)));
                        will(returnValue(ham2));
                    }
                });
                break;
        }
        mockery.checking(new Expectations() {
            {
                one(bm2).getBeans(bc);
                will(returnValue(hams));
                allowing(bm2).createCreationalContext(with(any(Bean.class)));
                will(returnValue(cc));
            }
        });
        return this;
    }

    class ModulePropertiesUtilsDouble extends ModulePropertiesUtils {
        ComponentMetaData meta = null;

        @Override
        protected CDI getCDI() {
            return cdi;
        }

        @Override
        protected ComponentMetaData getComponentMetaData() {
            return meta;
        }

        protected void setComponentMetaData(ComponentMetaData meta) {
            this.meta = meta;
        }
    };
}
