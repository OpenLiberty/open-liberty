/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
import static org.junit.Assert.fail;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
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
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;

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
    private BeanManager bm, bm1, bm2;
    private CDIService cs;
    private CDIHelperTestWrapper chtw;
    private Bean bean1, bean2;
    private CreationalContext cc;
    private WebModuleMetaData wmmd;

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
        bm = mockery.mock(BeanManager.class, "bm");
        bm1 = mockery.mock(BeanManager.class, "bm1");
        bm2 = mockery.mock(BeanManager.class, "bm2");
        cs = mockery.mock(CDIService.class);
        bean1 = mockery.mock(Bean.class, "bean1");
        bean2 = mockery.mock(Bean.class, "bean2");
        cc = mockery.mock(CreationalContext.class);
        chtw = new CDIHelperTestWrapper(mockery, bm2);
        chtw.setCDIService(cs);
        mpu = new ModulePropertiesUtilsDouble();
        wmmd = mockery.mock(WebModuleMetaData.class, "wmmd");
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
    public void testGetJ2EEModuleNameWithComponentMetaData() {
        final String MODULENAME = "ModuleName";
        withComponentMetaDataModuleName(MODULENAME);
        mpu.setComponentMetaData(cmd);
        mpu.setWebModuleMetaData(null);
        assertTrue("A module name should be returned.", mpu.getJ2EEModuleName().equals(MODULENAME));
    }

    /**
     *
     */
    @Test
    public void testGetJ2EEModuleNameWithWebModuleMetaData() {
        final String MODULENAME = "ModuleName";
        withWebModuleMetaDataModuleName(MODULENAME);
        mpu.setComponentMetaData(null);
        mpu.setWebModuleMetaData(wmmd);
        assertTrue("A module name should be returned.", mpu.getJ2EEModuleName().equals(MODULENAME));
    }

    /**
     *
     */
    @Test
    public void testGetJ2EEModuleNameCMDNull() {
        mpu.setComponentMetaData(null);
        mpu.setWebModuleMetaData(null);
        assertNull("null should be returned.", mpu.getJ2EEModuleName());
    }

    /**
     *
     */
    @Test
    public void testGetJ2EEApplicationNameWithComponentMetaData() {
        final String APPLNAME = "ApplicationName";
        withComponentMetaDataAppName(APPLNAME);
        mpu.setComponentMetaData(cmd);
        mpu.setWebModuleMetaData(null);
        assertTrue("A module name should be returned.", mpu.getJ2EEApplicationName().equals(APPLNAME));
    }

    /**
     *
     */
    @Test
    public void testGetJ2EEApplicationNameWithWebModuleMetaData() {
        final String APPLNAME = "ApplicationName";
        withWebModuleMetaDataAppName(APPLNAME);
        mpu.setComponentMetaData(null);
        mpu.setWebModuleMetaData(wmmd);
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
                one(cdi).getBeanManager();
                will(returnValue(bm));
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
        mpu.setComponentMetaData(cmd);
        mockery.checking(new Expectations() {
            {
                one(cdi).getBeanManager();
                will(returnValue(bm));
                one(cdi).select(ModulePropertiesProvider.class);
                will(returnValue(null));
            }
        });
        try {
            mpu.getHttpAuthenticationMechanism();
            fail("RuntimeException should be thrown.");
        } catch (RuntimeException e) {
            assertEquals("The message of RuntimeException does not match.", e.getMessage(), "ModulePropertiesProvider object cannot be identified.");
        }
    }

    /**
     *
     */
    @Test
    public void testIsHttpAuthenticationMechanismNoBM() {
        mpu.setComponentMetaData(cmd);
        mockery.checking(new Expectations() {
            {
                one(cdi).getBeanManager();
                will(returnValue(null));
            }
        });
        assertFalse("false should be returned.", mpu.isHttpAuthenticationMechanism());
    }

    /**
     *
     */
    @Test
    public void testGetHttpAuthenticationMechanismNoBM() {
        mpu.setComponentMetaData(cmd);
        mockery.checking(new Expectations() {
            {
                one(cdi).getBeanManager();
                will(returnValue(null));
            }
        });
        assertFalse("false should be returned.", mpu.isHttpAuthenticationMechanism());
    }

    /**
     *
     */
    @Test
    public void testGetHttpAuthenticationMechanismMPPUnsatisified() {
        mpu.setComponentMetaData(cmd);
        withModulePropertiesProvider(true, false);
        try {
            mpu.getHttpAuthenticationMechanism();
            fail("RuntimeException should be thrown.");
        } catch (RuntimeException e) {
            assertEquals("The message of RuntimeException does not match.", e.getMessage(), "ModulePropertiesProvider object cannot be identified.");
        }
    }

    /**
     *
     */
    @Test
    public void testGetHttpAuthenticationMechanismMPPambiguous() {
        mpu.setComponentMetaData(cmd);
        withModulePropertiesProvider(false, true);
        try {
            mpu.getHttpAuthenticationMechanism();
            fail("RuntimeException should be thrown.");
        } catch (RuntimeException e) {
            assertEquals("The message of RuntimeException does not match.", e.getMessage(), "ModulePropertiesProvider object cannot be identified.");
        }
    }

    /**
     *
     */
    @Test
    public void testGetHttpAuthenticationMechanismNoAuthMech() {
        final String APPLNAME = "ApplicationName";
        final String MODULENAME = "ModuleName";
        List<Class> list = new ArrayList<Class>();
        withComponentMetaDataModuleName(MODULENAME).withComponentMetaDataAppName(APPLNAME).withModulePropertiesProvider(false, false).withAuthMechClassList(list);
        mpu.setComponentMetaData(cmd);
        mpu.clearModuleTable();
        assertNull("null should be returned.", mpu.getHttpAuthenticationMechanism());
        // since one of multiple modules might not have a HAM configured, there is no error/warning message logged. Only a debug message.
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
        withComponentMetaDataModuleName(MODULENAME).withComponentMetaDataAppName(APPLNAME).withModulePropertiesProvider(false, false).withAuthMechClassList(list);
        mpu.setComponentMetaData(cmd);
        mpu.clearModuleTable();
        assertNull("null should be returned.", mpu.getHttpAuthenticationMechanism());
        // since CDI code checks this condition and log the error, only debug output is logged for this case.
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
        final Set<Bean<?>> hams = new HashSet<Bean<?>>();
        hams.add(bean1);
        withModulePropertiesProvider(false, false).withAuthMechClassList(list).withHAMSet(bm, String.class, hams).withScope(bean1, ApplicationScoped.class);

        mpu.setComponentMetaData(cmd);
        mpu.clearModuleTable();
        assertEquals("HAM should be returned.", mpu.getHttpAuthenticationMechanism(), ham);
    }

    /**
     *
     */
    @Test
    public void testGetHttpAuthenticationMechanismOneAuthMechNoGlobalHAMSameBM() {
        final String APPLNAME = "ApplicationName";
        final String MODULENAME = "ModuleName";
        List<Class> list = new ArrayList<Class>();
        list.add(String.class);
        withComponentMetaDataModuleName(MODULENAME).withComponentMetaDataAppName(APPLNAME).withModulePropertiesProvider(bm2, false,
                                                                                                                        false).withAuthMechClassList(list).withNoBean(bm2);
        mpu.setComponentMetaData(cmd);
        mpu.clearModuleTable();
        assertNull("null should be returned.", mpu.getHttpAuthenticationMechanism());
        assertTrue("CWWKS1912E  message with application and module name not logged",
                   outputMgr.checkForStandardErr("CWWKS1912E:.*" + MODULENAME + ".*" + APPLNAME + ".*"));
    }

    /**
     *
     */
    @Test
    public void testGetHttpAuthenticationMechanismOneAuthMechMultipleBeansSameBM() {
        final String APPLNAME = "ApplicationName";
        final String MODULENAME = "ModuleName";
        List<Class> list = new ArrayList<Class>();
        list.add(String.class);
        final Set<Bean<?>> hams = new HashSet<Bean<?>>();
        hams.add(bean1);
        hams.add(bean2);
        withComponentMetaDataModuleName(MODULENAME).withComponentMetaDataAppName(APPLNAME).withModulePropertiesProvider(bm2, false,
                                                                                                                        false).withAuthMechClassList(list).withHAMSet(bm2,
                                                                                                                                                                      String.class,
                                                                                                                                                                      hams);
        mpu.setComponentMetaData(cmd);
        mpu.clearModuleTable();
        assertNull("null should be returned.", mpu.getHttpAuthenticationMechanism());
        assertTrue("CWWKS1912E  message with application and module name not logged",
                   outputMgr.checkForStandardErr("CWWKS1912E:.*" + MODULENAME + ".*" + APPLNAME + ".*"));
    }

    @Test
    public void testGetHttpAuthenticationMechanismOneAuthMechMultipleGlobalHAMOneModuleHAM() {
        final List<Class> list = new ArrayList<Class>();
        list.add(String.class);
        final Set<Bean<?>> hams = new HashSet<Bean<?>>();
        hams.add(bean1);
        hams.add(bean2);
        final Set<Bean<?>> hams2 = new HashSet<Bean<?>>();
        hams2.add(bean1);
        withModulePropertiesProvider(bm1, false, false).withAuthMechClassList(list).withHAMSet(bm1, String.class, hams);
        withHAMSet(bm2, String.class, hams2).withScope(bean1, ApplicationScoped.class);
        mpu.setComponentMetaData(cmd);
        mpu.clearModuleTable();
        assertEquals("HAM should be returned.", mpu.getHttpAuthenticationMechanism(), ham);
    }

    @Test
    public void testGetHttpAuthenticationMechanismOneAuthMechMultipleGlobalHAMOneModuleHAMModuleToHamCacheHit() {
        final List<Class> list = new ArrayList<Class>();
        list.add(String.class);
        final Set<Bean<?>> hams = new HashSet<Bean<?>>();
        hams.add(bean1);
        hams.add(bean2);
        final Set<Bean<?>> hams2 = new HashSet<Bean<?>>();
        hams2.add(bean1);
        withModulePropertiesProvider(bm1, false, false).withAuthMechClassList(list).withHAMSet(bm1, String.class, hams);
        withHAMSet(bm2, String.class, hams2).withScope(bean1, ApplicationScoped.class);
        mpu.setComponentMetaData(cmd);
        mpu.clearModuleTable();
        assertEquals("HAM should be returned.", mpu.getHttpAuthenticationMechanism(), ham);
        withCacheHit();
        assertEquals("HAM should be returned.", mpu.getHttpAuthenticationMechanism(), ham);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testGetHttpAuthenticationMechanismOneAuthMechMultipleGlobalHAMOneModuleHAMModuleToHamLookupCacheHit() {
        final List<Class> list = new ArrayList<Class>();
        list.add(String.class);
        final Set<Bean<?>> hams = new HashSet<Bean<?>>();
        hams.add(bean1);
        hams.add(bean2);
        final Set<Bean<?>> hams2 = new HashSet<Bean<?>>();
        hams2.add(bean1);
        withModulePropertiesProvider(bm1, false, false).withAuthMechClassList(list).withHAMSet(bm1, String.class, hams);
        withHAMSet(bm2, String.class, hams2).withScope(bean1, RequestScoped.class);
        mpu.setComponentMetaData(cmd);
        mpu.clearModuleTable();
        assertEquals("HAM should be returned.", mpu.getHttpAuthenticationMechanism(), ham);
        withHAMSet(bm2, String.class, hams2);
        assertEquals("HAM should be returned.", mpu.getHttpAuthenticationMechanism(), ham);
    }

    @Test
    public void testGetHttpAuthenticationMechanismOneAuthMechNoGlobalHAMNoModuleHAM() {
        final String APPLNAME = "ApplicationName";
        final String MODULENAME = "ModuleName";
        final List<Class> list = new ArrayList<Class>();
        list.add(String.class);
        final Set<Bean<?>> hams = new HashSet<Bean<?>>();
        withComponentMetaDataModuleName(MODULENAME).withComponentMetaDataAppName(APPLNAME).withModulePropertiesProvider(bm1, false,
                                                                                                                        false).withAuthMechClassList(list).withNoBean(bm1).withNoBean(bm2);
        mpu.setComponentMetaData(cmd);
        mpu.clearModuleTable();
        assertNull("null should be returned.", mpu.getHttpAuthenticationMechanism());
        assertTrue("CWWKS1912E  message with application and module name not logged",
                   outputMgr.checkForStandardErr("CWWKS1912E:.*" + MODULENAME + ".*" + APPLNAME + ".*"));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testGetHttpAuthenticationMechanismOneAuthMechNoGlobalHAMMultipleModuleHAM() {
        final String APPLNAME = "ApplicationName";
        final String MODULENAME = "ModuleName";
        final List<Class> list = new ArrayList<Class>();
        list.add(String.class);
        final Set<Bean<?>> hams = new HashSet<Bean<?>>();
        hams.add(bean1);
        hams.add(bean2);
        withComponentMetaDataModuleName(MODULENAME).withComponentMetaDataAppName(APPLNAME).withModulePropertiesProvider(bm1, false,
                                                                                                                        false).withAuthMechClassList(list).withNoBean(bm1);
        withHAMSet(bm2, String.class, hams);
        mpu.setComponentMetaData(cmd);
        mpu.clearModuleTable();
        assertNull("null should be returned.", mpu.getHttpAuthenticationMechanism());
    }

    /*************** support methods **************/
    private ModulePropertiesUtilsTest withComponentMetaDataAppName(final String name) {
        mockery.checking(new Expectations() {
            {
                one(cmd).getJ2EEName();
                will(returnValue(j2n));
                one(j2n).getApplication();
                will(returnValue(name));
                never(wmmd).getJ2EEName();
            }
        });
        return this;
    }

    private ModulePropertiesUtilsTest withComponentMetaDataModuleName(final String name) {
        mockery.checking(new Expectations() {
            {
                one(cmd).getModuleMetaData();
                will(returnValue(mmd));
                one(mmd).getJ2EEName();
                will(returnValue(j2n));
                one(j2n).getModule();
                will(returnValue(name));
                never(wmmd).getJ2EEName();
            }
        });
        return this;
    }

    private ModulePropertiesUtilsTest withModulePropertiesProvider(boolean isUnsatisfied, boolean isAmbiguous) {
        return withModulePropertiesProvider(bm, isUnsatisfied, isAmbiguous);
    }

    @SuppressWarnings("unchecked")
    private ModulePropertiesUtilsTest withModulePropertiesProvider(BeanManager beanManager, boolean isUnsatisfied, boolean isAmbiguous) {
        mockery.checking(new Expectations() {
            {
                one(cdi).getBeanManager();
                will(returnValue(beanManager));
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

    @SuppressWarnings("rawtypes")
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private ModulePropertiesUtilsTest withHAMSet(final BeanManager bm, final Class bc, final Set<Bean<?>> hams) {
        switch (hams.size()) {
            case 1:
                mockery.checking(new Expectations() {
                    {
                        one(bm).getReference(with(any(Bean.class)), with(any(Type.class)), with(any(CreationalContext.class)));
                        will(returnValue(ham));
                    }
                });
                break;
            default:
                mockery.checking(new Expectations() {
                    {
                        never(bm).getReference(with(any(Bean.class)), with(any(Type.class)), with(any(CreationalContext.class)));
                    }
                });
                break;
        }
        mockery.checking(new Expectations() {
            {
                one(bm).getBeans(bc);
                will(returnValue(hams));
                allowing(bm).resolve(hams);
                will(returnValue(hams.iterator().next()));
                allowing(bm).createCreationalContext(with(any(Bean.class)));
                will(returnValue(cc));
            }
        });
        return this;
    }

    private ModulePropertiesUtilsTest withWebModuleMetaDataModuleName(final String name) {
        mockery.checking(new Expectations() {
            {
                one(wmmd).getJ2EEName();
                will(returnValue(j2n));
                one(j2n).getModule();
                will(returnValue(name));
                never(cmd).getModuleMetaData();
            }
        });
        return this;
    }

    private ModulePropertiesUtilsTest withWebModuleMetaDataAppName(final String name) {
        mockery.checking(new Expectations() {
            {
                one(wmmd).getJ2EEName();
                will(returnValue(j2n));
                one(j2n).getApplication();
                will(returnValue(name));
                never(cmd).getJ2EEName();
            }
        });
        return this;
    }

    private ModulePropertiesUtilsTest withCacheHit() {
        mockery.checking(new Expectations() {
            {
                never(cdi).getBeanManager();
            }
        });
        return this;
    }

    @SuppressWarnings("rawtypes")
    private ModulePropertiesUtilsTest withScope(final Bean bean, final Class scope) {
        mockery.checking(new Expectations() {
            {
                one(bean).getScope();
                will(returnValue(scope));
            }
        });
        return this;
    }

    private ModulePropertiesUtilsTest withNoBean(final BeanManager bm) {
        final Set<Bean<?>> hams = new HashSet<Bean<?>>();
        mockery.checking(new Expectations() {
            {
                one(bm).getBeans(String.class);
                will(returnValue(hams));
            }
        });
        return this;
    }

    class ModulePropertiesUtilsDouble extends ModulePropertiesUtils {
        ComponentMetaData cmd = null;
        WebModuleMetaData wmmd = null;

        @SuppressWarnings("rawtypes")
        @Override
        protected CDI getCDI() {
            return cdi;
        }

        @Override
        protected ComponentMetaData getComponentMetaData() {
            return cmd;
        }

        protected void setComponentMetaData(ComponentMetaData cmd) {
            this.cmd = cmd;
        }

        @Override
        protected WebModuleMetaData getWebModuleMetaData() {
            return wmmd;
        }

        protected void setWebModuleMetaData(WebModuleMetaData wmmd) {
            this.wmmd = wmmd;
        }

    };
}
