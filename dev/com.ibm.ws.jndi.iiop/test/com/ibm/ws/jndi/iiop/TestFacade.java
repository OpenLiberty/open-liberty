/* ***************************************************************************
 * Copyright (c) 2015,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ***************************************************************************/
package com.ibm.ws.jndi.iiop;

import static javax.naming.Context.INITIAL_CONTEXT_FACTORY;
import static javax.naming.Context.PROVIDER_URL;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.util.Properties;

import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

import org.apache.yoko.orb.spi.naming.NameServiceInitializer;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

public class TestFacade {
    private static TestFacadeDelegate instance;
    private static short nextPort = 12809;

    public static void setup() throws Exception {
        assertThat("Should be using the Yoko ORB", ORB.init().getClass().getName(), containsString("yoko"));
        System.out.println("### Creating new TestFacade using port " + nextPort);
        instance = new TestFacadeDelegate(nextPort++);
    }

    public static void tearDown() {
        instance.tearDown();
        instance = null;
    }

    public static Context jndiContext() {
        return instance.jndiContext();
    }

    public static void useSubcontext(String name) throws Exception {
        instance.useSubcontext(name);
    }

    public static void bindCosNamingContext(String name) throws Exception {
        instance.bindCosNamingContext(name);
    }

    public static void bindCosNamingObject(String string) throws Exception {
        instance.bindCosNamingObject(string);
    }

    public static <STUB extends Testable & Object> STUB createTestableStub(String id) throws Exception {
        return instance.createTestableStub(id);
    }

    public static Object stringToObjectUsingCorbaname(String name) {
        return instance.stringToObjectUsingCorbaname(name);
    }

    public static TestableImpl createTestableImpl(final String id) {
        TestableImpl testable = new TestableImpl(id);
        return testable;
    }

    private static NameComponent[] cosName(String s) throws Exception {
        CompoundName jndiName = JndiUtil.compound(s);
        NameComponent[] result = new NameComponent[jndiName.size()];
        for (int i = 0; i < result.length; i++) {
            String[] parts = jndiName.get(i).split("\\.", 2);
            String kind = parts.length < 2 ? "" : parts[1];
            String id = parts[0];
            result[i] = new NameComponent(id, kind);
        }
        return result;
    }

    private static class TestFacadeDelegate {

        final short port;
        final String host;
        final String endpoint;
        final String corbalocUrl;
        final ORB serverOrb;
        NamingContext cosNamingContext;
        Context jndiContext;
        String rootPrefix;

        final Properties jndiProps;

        public Context jndiContext() {
            return jndiContext;
        }

        public TestFacadeDelegate(short port) throws Exception {
            this.port = port;
            this.host = "localhost";
            this.endpoint = "iiop --host " + host + " --port " + port;
            this.corbalocUrl = "corbaloc::" + host + ":" + port + "/NameService";
            this.jndiProps = new Properties();
            this.jndiProps.put(INITIAL_CONTEXT_FACTORY, InitialContextFactoryImpl.class.getName());
            this.jndiProps.put(PROVIDER_URL, corbalocUrl);
            this.serverOrb = new OrbMaker()
                            .initializer(NameServiceInitializer.class)
                            .prop("yoko.orb.oa.endpoint", endpoint)
                            .makeServer();
            retrieveContexts();
            rootPrefix = "";
        }

        private void retrieveContexts() throws InvalidName, NamingException {
            cosNamingContext = NamingContextHelper.narrow(serverOrb.resolve_initial_references("NameService"));
            cosNamingContext = NamingContextHelper.narrow(serverOrb.string_to_object(corbalocUrl));
            jndiContext = new InitialContext(jndiProps);
        }

        public void useSubcontext(String name) throws Exception {
            rootPrefix += name + "/";
            cosNamingContext = cosNamingContext.bind_new_context(cosName(name));
            jndiContext = (Context) jndiContext.lookup(name);
        }

        public void bindCosNamingContext(String name) throws Exception {
            cosNamingContext.bind_new_context(cosName(name));
        }

        public void bindCosNamingObject(String string) throws Exception {
            final NameComponent[] cosName = cosName(string);
            final String id = cosName[cosName.length - 1].id;
            cosNamingContext.bind(cosName, createTestableStub(id));
        }

        /**
         * @param <STUB> Possibly generated stub class that extends Object and implement Testable.
         * @return a stub to a new RMI-IIOP object exported via {@link #svrOrb}
         */
        @SuppressWarnings("unchecked")
        public <STUB extends Testable & org.omg.CORBA.Object>
                        STUB createTestableStub(final String id) throws Exception {
            _TestableImpl_Tie tie = new _TestableImpl_Tie();
            tie.setTarget(createTestableImpl(id));
            POA poa = POAHelper.narrow(serverOrb.resolve_initial_references("RootPOA"));
            poa.activate_object(tie);
            org.omg.CORBA.Object ref = poa.servant_to_reference(tie);
            return (STUB) PortableRemoteObject.narrow(ref, Testable.class);
        }

        public void tearDown() {
            serverOrb.shutdown(true);
            serverOrb.destroy();
        }

        public org.omg.CORBA.Object stringToObjectUsingCorbaname(String name) {
            String corbaname = "corbaname::" + host + ":" + port + "/NameService#" + rootPrefix + name;
            org.omg.CORBA.Object o = serverOrb.string_to_object(corbaname);
            return o;
        }
    }
}