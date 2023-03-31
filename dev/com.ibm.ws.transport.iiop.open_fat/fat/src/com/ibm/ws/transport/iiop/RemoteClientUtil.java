/*******************************************************************************
 * Copyright (c) 2015-2023 IBM Corporation and others.
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
package com.ibm.ws.transport.iiop;

import java.io.IOException;
import java.util.Properties;

import javax.rmi.PortableRemoteObject;

import junit.framework.AssertionFailedError;

import org.junit.Assert;
import org.omg.CORBA.ORB;

import test.iiop.common.HelloService;
import test.iiop.common.util.IorData;

import componenttest.topology.impl.LibertyServer;

public class RemoteClientUtil {
    static <T> T getRemoteServiceFromServerLog(LibertyServer server, ORB clientOrb, Class<T> iface) {
        String ior = getServiceIor(server, iface);
        return getServiceFromIor(clientOrb, ior, iface);
    }

    private static <T> T getServiceFromIor(ORB clientOrb, String ior, Class<T> iface) {
        // use the IOR to create a stub
        org.omg.CORBA.Object service = clientOrb.string_to_object(ior);
        Assert.assertNotNull(service);
        // narrow the stub to the specified interface
        return iface.cast(PortableRemoteObject.narrow(service, iface));
    }

    private static <T> String getServiceIor(LibertyServer server, Class<T> iface) {
        String serviceName = iface.getSimpleName();
        // wait to see that the service is started
        server.waitForStringInLogUsingMark("### Activating " + serviceName);
        // now try to find the message containing the IOR
        String iorPattern = "### Exported " + serviceName + ": ";
        String logEntry = server.waitForStringInLogUsingMark(iorPattern);
        System.out.println("### Log string found: '" + logEntry + "'");
        Assert.assertNotNull(logEntry);
        // extract the IOR from the message
        String ior = logEntry.replaceFirst(".*" + iorPattern, "");
        Assert.assertFalse("Empty IOR found from log message: '" + logEntry + "'", ior.isEmpty());
        return ior;
    }

    static String getNameServiceUrl(LibertyServer server) {
        try {
            String ior = RemoteClientUtil.getServiceIor(server, HelloService.class); // could use any known iiop FAT test service
            IorData iorData = new IorData(ior);
            return "corbaname::" + iorData.host + ":" + iorData.port;
        } catch (IOException e) {
            throw (Error) new AssertionFailedError("Could not read ior").initCause(e);
        }
    }

    static ORB initOrb(Properties props, String... args) {
        return ORB.init(args, props);
    }
}
