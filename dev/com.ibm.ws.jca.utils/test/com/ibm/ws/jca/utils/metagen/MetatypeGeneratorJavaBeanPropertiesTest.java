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
package com.ibm.ws.jca.utils.metagen;

import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.jca.utils.xml.metatype.Metatype;
import com.ibm.ws.jca.utils.xml.ra.RaActivationSpec;
import com.ibm.ws.jca.utils.xml.ra.RaAdminObject;
import com.ibm.ws.jca.utils.xml.ra.RaConfigProperty;
import com.ibm.ws.jca.utils.xml.ra.RaConnectionDefinition;
import com.ibm.ws.jca.utils.xml.ra.RaConnector;
import com.ibm.ws.jca.utils.xml.ra.RaInboundResourceAdapter;
import com.ibm.ws.jca.utils.xml.ra.RaMessageAdapter;
import com.ibm.ws.jca.utils.xml.ra.RaMessageListener;
import com.ibm.ws.jca.utils.xml.ra.RaOutboundResourceAdapter;
import com.ibm.ws.jca.utils.xml.ra.RaResourceAdapter;

public class MetatypeGeneratorJavaBeanPropertiesTest {
    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private Metatype generateMetatype(RaConnector connector) throws Exception {
        Map<String, Object> metagenConfig = new HashMap<String, Object>();
        metagenConfig.put(MetaGenConstants.KEY_ADAPTER_NAME, "test");
        metagenConfig.put(MetaGenConstants.KEY_GENERATION_MODE, MetaGenConstants.VALUE_GENERATION_MODE_RAR);
        metagenConfig.put(MetaGenConstants.KEY_RAR_CLASSLOADER, MetatypeGeneratorJavaBeanPropertiesTest.class.getClassLoader());
        metagenConfig.put(MetaGenConstants.KEY_TRANSLATE, false);
        metagenConfig.put(MetaGenConstants.RAR_DEPLOYMENT_DESCRIPTOR, connector);

        return MetatypeGenerator.generateMetatype(metagenConfig, null);
    }

    @Test
    public void testResourceAdapter() throws Exception {
        RaConnector connector = new RaConnector();
        RaResourceAdapter ra = new RaResourceAdapter();
        connector.setResourceAdapter(ra);
        ra.setResourceAdapterClass(TestProp.class.getName());
        RaConfigProperty updateDefaultProp = new RaConfigProperty();
        updateDefaultProp.setName("updateDefault");
        ra.getConfigProperties().add(updateDefaultProp);

        Assert.assertNull(ra.getConfigPropertyById("prop"));
        Assert.assertNull(ra.getConfigPropertyById("default"));
        generateMetatype(connector);

        RaConfigProperty prop = ra.getConfigPropertyById("prop");
        Assert.assertEquals(String.class.getName(), prop.getType());
        Assert.assertNull(prop.getDefault());
        RaConfigProperty defaultProp = ra.getConfigPropertyById("default");
        Assert.assertEquals(String.class.getName(), defaultProp.getType());
        Assert.assertEquals("default", defaultProp.getDefault());
        Assert.assertEquals(String.class.getName(), updateDefaultProp.getType());
        Assert.assertEquals("updated", updateDefaultProp.getDefault());
    }

    public static class TestProp extends TestResourceAdapter implements ManagedConnectionFactory, ActivationSpec {
        public void setProp(String value) {}

        public void setDefault(String value) {}

        public String getDefault() {
            return "default";
        }

        public void setUpdateDefault(String value) {}

        public String getUpdateDefault() {
            return "updated";
        }

        // ManagedConnectionFactory impl

        @Override
        public Object createConnectionFactory() throws ResourceException {
            return null;
        }

        @Override
        public Object createConnectionFactory(ConnectionManager arg0) throws ResourceException {
            return null;
        }

        @Override
        public ManagedConnection createManagedConnection(Subject arg0, ConnectionRequestInfo arg1) throws ResourceException {
            return null;
        }

        @Override
        public PrintWriter getLogWriter() throws ResourceException {
            return null;
        }

        @Override
        public ManagedConnection matchManagedConnections(Set arg0, Subject arg1, ConnectionRequestInfo arg2) throws ResourceException {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter arg0) throws ResourceException {

        }

        // ActivationSpec impl

        @Override
        public ResourceAdapter getResourceAdapter() {
            return null;
        }

        @Override
        public void setResourceAdapter(ResourceAdapter arg0) throws ResourceException {

        }

        @Override
        public void validate() throws InvalidPropertyException {

        }
    }

    @Test
    public void testConnectionDefinition() throws Exception {
        RaConnector connector = new RaConnector();
        RaResourceAdapter ra = new RaResourceAdapter();
        connector.setResourceAdapter(ra);
        RaOutboundResourceAdapter outboundAdapter = new RaOutboundResourceAdapter();
        ra.setOutboundResourceAdapter(outboundAdapter);
        RaConnectionDefinition cd = new RaConnectionDefinition();
        outboundAdapter.getConnectionDefinitions().add(cd);
        cd.setManagedConnectionFactoryClass(TestProp.class.getName());
        cd.setConnectionFactoryImplClass("Impl");
        cd.setConnectionFactoryInterface("Intf");

        Assert.assertNull(cd.getConfigPropertyById("prop"));
        Assert.assertNull(cd.getConfigPropertyById("default"));
        generateMetatype(connector);

        RaConfigProperty prop = cd.getConfigPropertyById("prop");
        Assert.assertEquals(String.class.getName(), prop.getType());
        Assert.assertNull(prop.getDefault());
        RaConfigProperty defaultProp = cd.getConfigPropertyById("default");
        Assert.assertEquals(String.class.getName(), defaultProp.getType());
        Assert.assertNull(defaultProp.getDefault());
    }

    @Test
    public void testAdminObject() throws Exception {
        RaConnector connector = new RaConnector();
        RaResourceAdapter ra = new RaResourceAdapter();
        connector.setResourceAdapter(ra);
        RaAdminObject ao = new RaAdminObject();
        ra.getAdminObjects().add(ao);
        ao.setAdminObjectClass(TestProp.class.getName());
        ao.setAdminObjectInterface("Intf");

        Assert.assertNull(ao.getConfigPropertyById("prop"));
        Assert.assertNull(ao.getConfigPropertyById("default"));
        generateMetatype(connector);

        RaConfigProperty prop = ao.getConfigPropertyById("prop");
        Assert.assertEquals(String.class.getName(), prop.getType());
        Assert.assertNull(prop.getDefault());
        RaConfigProperty defaultProp = ao.getConfigPropertyById("default");
        Assert.assertEquals(String.class.getName(), defaultProp.getType());
        Assert.assertNull(defaultProp.getDefault());
    }

    @Test
    public void testActivationSpec() throws Exception {
        RaConnector connector = new RaConnector();
        RaResourceAdapter ra = new RaResourceAdapter();
        connector.setResourceAdapter(ra);
        RaInboundResourceAdapter inboundAdapter = new RaInboundResourceAdapter();
        ra.setInboundResourceAdapter(inboundAdapter);
        RaMessageAdapter messageAdapter = new RaMessageAdapter();
        inboundAdapter.setMessageAdapter(messageAdapter);
        RaMessageListener listener = new RaMessageListener();
        messageAdapter.getMessageListeners().add(listener);
        RaActivationSpec as = new RaActivationSpec();
        listener.setActivationSpec(as);
        as.setActivationSpecClass(TestProp.class.getName());

        Assert.assertNull(as.getConfigPropertyById("prop"));
        Assert.assertNull(as.getConfigPropertyById("default"));
        generateMetatype(connector);

        RaConfigProperty prop = as.getConfigPropertyById("prop");
        Assert.assertEquals(String.class.getName(), prop.getType());
        Assert.assertNull(prop.getDefault());
        RaConfigProperty defaultProp = as.getConfigPropertyById("default");
        Assert.assertEquals(String.class.getName(), defaultProp.getType());
        Assert.assertNull(defaultProp.getDefault());
    }

    @Test
    public void testMissingClass() throws Exception {
        RaConnector connector = new RaConnector();
        RaResourceAdapter ra = new RaResourceAdapter();
        connector.setResourceAdapter(ra);
        ra.setResourceAdapterClass("class.does.not.exist");

        try {
            generateMetatype(connector);
            fail("generateMetatype should have thrown a ClassNotFoundException for class.does.not.exist");
        } catch (ClassNotFoundException e) {
        }
        outputMgr.expectWarning("J2CA9919W");
    }

    @Test
    public void testReadOnly() throws Exception {
        RaConnector connector = new RaConnector();
        RaResourceAdapter ra = new RaResourceAdapter();
        connector.setResourceAdapter(ra);
        ra.setResourceAdapterClass(TestReadOnly.class.getName());

        generateMetatype(connector);
        Assert.assertEquals(ra.getConfigProperties().toString(), 0, ra.getConfigProperties().size());
    }

    public static class TestReadOnly extends TestResourceAdapter {
        public String getProp() {
            return null;
        }
    }

    @Test
    public void testPrimitive() throws Exception {
        RaConnector connector = new RaConnector();
        RaResourceAdapter ra = new RaResourceAdapter();
        connector.setResourceAdapter(ra);
        ra.setResourceAdapterClass(TestPrimitive.class.getName());

        generateMetatype(connector);
        Assert.assertEquals(Integer.class.getName(), ra.getConfigPropertyById("prop").getType());
    }

    public static class TestPrimitive extends TestResourceAdapter {
        public void setProp(int value) {}
    }

    @Test
    public void testUnknownType() throws Exception {
        RaConnector connector = new RaConnector();
        RaResourceAdapter ra = new RaResourceAdapter();
        connector.setResourceAdapter(ra);
        ra.setResourceAdapterClass(TestUnknownType.class.getName());

        generateMetatype(connector);
        Assert.assertEquals(ra.getConfigProperties().toString(), 0, ra.getConfigProperties().size());
    }

    public static class TestUnknownType extends TestResourceAdapter {
        public void setReference(Reference ref) {}
    }

    @Test
    public void testDefaultValuePrivateConstructor() throws Exception {
        RaConnector connector = new RaConnector();
        RaResourceAdapter ra = new RaResourceAdapter();
        connector.setResourceAdapter(ra);
        ra.setResourceAdapterClass(TestDefaultValuePrivateConstructor.class.getName());

        generateMetatype(connector);
        outputMgr.expectWarning("J2CA9936W");
    }

    public static class TestDefaultValuePrivateConstructor extends TestResourceAdapter {
        private TestDefaultValuePrivateConstructor() {}

        public void setProp(int value) {}

        public int getProp() {
            return 0;
        }
    }

    @Test
    public void testDefaultValueConstructorError() throws Exception {
        RaConnector connector = new RaConnector();
        RaResourceAdapter ra = new RaResourceAdapter();
        connector.setResourceAdapter(ra);
        ra.setResourceAdapterClass(TestDefaultValueConstructorError.class.getName());

        generateMetatype(connector);
        outputMgr.expectWarning("J2CA9936W");
    }

    public static class TestDefaultValueConstructorError extends TestResourceAdapter {
        public TestDefaultValueConstructorError() {
            throw new Error("expected");
        }

        public void setProp(int value) {}

        public int getProp() {
            return 0;
        }
    }

    @Test
    public void testDefaultValueGetterError() throws Exception {
        RaConnector connector = new RaConnector();
        RaResourceAdapter ra = new RaResourceAdapter();
        connector.setResourceAdapter(ra);
        ra.setResourceAdapterClass(TestDefaultValueGetterError.class.getName());

        generateMetatype(connector);
        outputMgr.expectWarning("J2CA9936W");
    }

    public static class TestDefaultValueGetterError extends TestResourceAdapter {
        public void setProp(int value) {}

        public int getProp() {
            throw new Error("expected");
        }
    }

    public static class TestResourceAdapter implements ResourceAdapter {

        @Override
        public void endpointActivation(MessageEndpointFactory arg0, ActivationSpec arg1) throws ResourceException {

        }

        @Override
        public void endpointDeactivation(MessageEndpointFactory arg0, ActivationSpec arg1) {

        }

        @Override
        public XAResource[] getXAResources(ActivationSpec[] arg0) throws ResourceException {
            return null;
        }

        @Override
        public void start(BootstrapContext arg0) throws ResourceAdapterInternalException {

        }

        @Override
        public void stop() {

        }

    }
}
