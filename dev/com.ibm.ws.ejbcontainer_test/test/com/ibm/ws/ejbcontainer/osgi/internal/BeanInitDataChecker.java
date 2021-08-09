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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.Assert;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.ejbcontainer.EJBEndpoint;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.ManagedBeanEndpoint;
import com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean;
import com.ibm.ws.metadata.ejb.BeanInitData;
import com.ibm.ws.metadata.ejb.ModuleInitData;

public class BeanInitDataChecker {
    private static BeanInitDataImpl findBeanInitDataImpl(ModuleInitData mid, String name) {
        for (BeanInitData bid : mid.ivBeans) {
            if (bid.ivName.equals(name)) {
                return (BeanInitDataImpl) bid;
            }
        }

        return null;
    }

    private static EJBEndpoint findEJBEndpoint(ModuleInitDataImpl mid, String name) {
        for (EJBEndpoint ep : mid.getEJBEndpoints()) {
            if (ep.getName().equals(name)) {
                return ep;
            }
        }

        return null;
    }

    private static ManagedBeanEndpoint findMBEndpoint(ModuleInitDataImpl mid, String className) {
        for (ManagedBeanEndpoint ep : mid.getManagedBeanEndpoints()) {
            if (ep.getClassName().equals(className)) {
                return ep;
            }
        }

        return null;
    }

    private final String name;
    private boolean xml;
    private boolean xmlExt;
    private boolean xmlBnd;
    final int type;
    private final String className;
    private int cmpVersion;
    private String remoteHome;
    private String remote;
    private String localHome;
    private String local;
    private final Set<String> remoteBusiness = new HashSet<String>();
    private final Set<String> localBusiness = new HashSet<String>();
    private boolean localBean;
    private String messageListener;
    private final Properties activationConfigProperties = new Properties();
    private boolean webService;
    private String webServiceEndpoint;
    private boolean bmt;
    private boolean scheduleTimers;
    private boolean passivationCapable = true;
    private boolean startup;
    private List<String> dependsOn;

    private BeanInitDataChecker(Void v, String name, int type, String className) {
        this.name = name;
        this.type = type;
        this.className = className;
        if (type == InternalConstants.TYPE_MANAGED_BEAN) {
            localBean();
            bmt();
        }
    }

    BeanInitDataChecker(String name, int type, String className) {
        this(null, name, type, className);
    }

    BeanInitDataChecker(String name, int type, Class<?> klass) {
        this(name, type, klass.getName());
    }

    BeanInitDataChecker(int type, Class<?> klass) {
        this(null, type == InternalConstants.TYPE_MANAGED_BEAN ? '$' + klass.getName() : klass.getSimpleName(), type, klass.getName());
    }

    BeanInitDataChecker xml() {
        this.xml = true;
        return this;
    }

    BeanInitDataChecker xmlExt() {
        this.xmlExt = true;
        return this;
    }

    BeanInitDataChecker xmlBnd() {
        this.xmlBnd = true;
        return this;
    }

    BeanInitDataChecker cmpVersion(int value) {
        this.cmpVersion = value;
        return this;
    }

    BeanInitDataChecker remote(String home, String intf) {
        this.remoteHome = home;
        this.remote = intf;
        return this;
    }

    BeanInitDataChecker local(String home, String intf) {
        this.localHome = home;
        this.local = intf;
        return this;
    }

    BeanInitDataChecker remoteBusiness(String... intf) {
        this.remoteBusiness.addAll(Arrays.asList(intf));
        return this;
    }

    BeanInitDataChecker remoteBusinessIf(boolean condition, String... intf) {
        return condition ? remoteBusiness(intf) : this;
    }

    BeanInitDataChecker localBusiness(String... intf) {
        this.localBusiness.addAll(Arrays.asList(intf));
        return this;
    }

    BeanInitDataChecker localBusinessIf(boolean condition, String... intf) {
        return condition ? localBusiness(intf) : this;
    }

    BeanInitDataChecker localBean() {
        this.localBean = true;
        return this;
    }

    BeanInitDataChecker messageListener(String intf) {
        this.messageListener = intf;
        return this;
    }

    BeanInitDataChecker activationConfigProperty(String key, String value) {
        this.activationConfigProperties.put(key, value);
        return this;
    }

    BeanInitDataChecker webService() {
        this.webService = true;
        return this;
    }

    BeanInitDataChecker webServiceEndpoint(String intf) {
        this.webServiceEndpoint = intf;
        return this;
    }

    BeanInitDataChecker bmt() {
        this.bmt = true;
        return this;
    }

    BeanInitDataChecker scheduleTimers() {
        this.scheduleTimers = true;
        return this;
    }

    BeanInitDataChecker passivationIncapable() {
        this.passivationCapable = false;
        return this;
    }

    BeanInitDataChecker startup() {
        this.startup = true;
        return this;
    }

    BeanInitDataChecker dependsOn(String... dependsOn) {
        if (this.dependsOn == null) {
            this.dependsOn = new ArrayList<String>();
        }
        this.dependsOn.addAll(Arrays.asList(dependsOn));
        return this;
    }

    void check(ModuleInitDataImpl mid) {
        BeanInitDataImpl bid = findBeanInitDataImpl(mid, name);
        EJBEndpoint ep = findEJBEndpoint(mid, name);
        ManagedBeanEndpoint mbep = findMBEndpoint(mid, className);
        Assert.assertNotNull(name, bid);

        Assert.assertEquals(name, mid, bid.ivModuleInitData);
        J2EEName j2eeName = bid.ivJ2EEName;
        Assert.assertEquals(name + " J2EEName.getApplication", mid.ivJ2EEName.getApplication(), j2eeName.getApplication());
        Assert.assertEquals(name + " J2EEName.getModule", mid.ivJ2EEName.getModule(), j2eeName.getModule());
        Assert.assertEquals(name + " J2EEName.getComponent", name, j2eeName.getComponent());
        Assert.assertEquals(name + " type", type, bid.ivType);
        Assert.assertEquals(name + " class", className, bid.ivClassName);
        Assert.assertEquals(name + " CMP version", cmpVersion, bid.ivCMPVersion);

        if (type == InternalConstants.TYPE_MANAGED_BEAN) {
            Assert.assertNull(name + " EJBEndpoint", ep);
            Assert.assertNotNull(name + " ManagedBeanEndpoint", mbep);
            Assert.assertEquals(name + " ManagedBeanEndpoint.getName", (name.startsWith("$") ? null : name), mbep.getName());
            Assert.assertEquals(name + " ManagedBeanEndpoint.getJ2EEName", j2eeName, mbep.getJ2EEName());
            Assert.assertEquals(name + " ManagedBeanEndpoint.getClassName", className, mbep.getClassName());
        } else {
            Assert.assertNull(name + " ManagedBeanEndpoint", mbep);
            Assert.assertNotNull(name + " EJBEndpoint", ep);
            Assert.assertEquals(name + " EJBEndpoint.getName", name, ep.getName());
            Assert.assertEquals(name + " EJBEndpoint.getEJBType", type, ep.getEJBType().value());
            Assert.assertEquals(name + " EJBEndpoint.getClassName", className, ep.getClassName());
            Assert.assertEquals(name + " EJBEndpoint.getJ2EEName", j2eeName, ep.getJ2EEName());
        }

        if (xml) {
            Assert.assertNotNull(name + " javaee.dd.ejb.EnterpriseBean", bid.ivEnterpriseBean);
            Assert.assertEquals(name + " javaee.dd.ejb.EnterpriseBean.getName", name, bid.ivEnterpriseBean.getName());
        } else {
            Assert.assertNull(name + " javaee.dd.ejb.EnterpriseBean", bid.ivEnterpriseBean);
        }
        if (xmlExt) {
            // If we have extension data for the bean, the Enterprise Bean's name and type matched.
            Assert.assertNotNull(name + " javaee.dd.ejbext.EnterpriseBean", bid.enterpriseBeanExt);
            Assert.assertEquals(name + " javaee.dd.ejbext.EnterpriseBean.getName", name, bid.enterpriseBeanExt.getName());
            // Extension bean type must be either MessageDriven or Session.
            if (bid.enterpriseBeanExt instanceof com.ibm.ws.javaee.dd.ejbext.Session) {
                Assert.assertTrue(name + "Session Extension type", bid.getEJBType().isSession());
            } else if (bid.enterpriseBeanExt instanceof com.ibm.ws.javaee.dd.ejbext.MessageDriven) {
                Assert.assertTrue(name + "MessageDriven Extension type", bid.getEJBType().isMessageDriven());
            }
        } else {
            Assert.assertNull(name + " javaee.dd.ejbext.EnterpriseBean", bid.enterpriseBeanExt);
        }
        if (xmlBnd) {
            // If we have binding data for the bean, the Enterprise Bean's name and type matched.
            Assert.assertNotNull(name + " javaee.dd.ejbbnd.EnterpriseBean", bid.beanBnd);

            // Binding bean type must be either MessageDriven or Session.
            if (bid.beanBnd instanceof com.ibm.ws.javaee.dd.ejbbnd.Session) {
                Assert.assertTrue(name + " should have Session binding type", bid.getEJBType().isSession());
            } else if (bid.beanBnd instanceof com.ibm.ws.javaee.dd.ejbbnd.MessageDriven) {
                Assert.assertTrue(name + " MessageDriven binding type", bid.getEJBType().isMessageDriven());
            }
            Assert.assertEquals(name + " javaee.dd.ejbbnd.EnterpriseBean.getName", name, ((EnterpriseBean) bid.beanBnd).getName());
        } else {
            Assert.assertNull(name + " javaee.dd.ejbbnd.EnterpriseBean", bid.beanBnd);
        }
        Assert.assertEquals(name + " remote home", remoteHome, bid.ivRemoteHomeInterfaceName);
        Assert.assertEquals(name + " remote component", remote, bid.ivRemoteInterfaceName);
        Assert.assertEquals(name + " local home", localHome, bid.ivLocalHomeInterfaceName);
        Assert.assertEquals(name + " local component", local, bid.ivLocalInterfaceName);
        if (remoteBusiness.isEmpty()) {
            Assert.assertNull(name + " remote business: " + Arrays.toString(bid.ivRemoteBusinessInterfaceNames), bid.ivRemoteBusinessInterfaceNames);
        } else {
            Assert.assertEquals(name + " remote business", remoteBusiness, new HashSet<String>(Arrays.asList(bid.ivRemoteBusinessInterfaceNames)));
        }
        if (localBusiness.isEmpty()) {
            Assert.assertNull(name + " local business: " + Arrays.toString(bid.ivLocalBusinessInterfaceNames), bid.ivLocalBusinessInterfaceNames);
        } else {
            Assert.assertEquals(name + " local business", localBusiness, new HashSet<String>(Arrays.asList(bid.ivLocalBusinessInterfaceNames)));
        }
        Assert.assertEquals(name + " local bean", localBean, bid.ivLocalBean);
        if (type != InternalConstants.TYPE_MANAGED_BEAN) {
            Assert.assertEquals(name + " EJBEndpoint.getLocalBusinessInterfaceNames", localBusiness, new HashSet<String>(ep.getLocalBusinessInterfaceNames()));
            assertUnmodifiable(name + " EJBEndpoint.getLocalBusinessInterfaceNames", ep.getLocalBusinessInterfaceNames());
            Assert.assertEquals(name + " EJBEndpoint.isLocalBean", localBean, ep.isLocalBean());
        }
        Assert.assertEquals(name + " message listener interface", messageListener, bid.ivMessageListenerInterfaceName);
        if (activationConfigProperties.isEmpty()) {
            Assert.assertTrue(name + " activation config properties: " + bid.ivActivationConfigProperties,
                              bid.ivActivationConfigProperties == null || bid.ivActivationConfigProperties.isEmpty());
        } else {
            Assert.assertEquals(name + " activation config properties", activationConfigProperties, bid.ivActivationConfigProperties);
        }
        Assert.assertEquals(name + " webservice", webService, bid.ivWebServiceEndpoint);
        Assert.assertEquals(name + " webservice interface", webServiceEndpoint, bid.ivWebServiceEndpointInterfaceName);
        if (type != InternalConstants.TYPE_MANAGED_BEAN) {
            Assert.assertEquals(name + " EJBEndpoint.isWebService", webService || webServiceEndpoint != null, ep.isWebService());
        }
        Assert.assertEquals(name + " BMT", bmt, bid.ivBeanManagedTransaction);

        // Core container handles null ivHasScheduleTimers, but for performance, it should always be set.
        Assert.assertEquals(name + " schedule", scheduleTimers, bid.ivHasScheduleTimers);
        Assert.assertEquals(name + " passivation capable", passivationCapable, bid.ivPassivationCapable);
        Assert.assertEquals(name + " startup", startup, bid.ivStartup);
        Assert.assertEquals(name + " depends on", dependsOn, bid.ivDependsOn == null ? null : new ArrayList<String>(bid.ivDependsOn));
    }

    private static void assertUnmodifiable(String name, Collection<?> list) {
        try {
            list.add(null);
            Assert.fail(name + " should be unmodifiable");
        } catch (UnsupportedOperationException e) {
        }
    }
}
