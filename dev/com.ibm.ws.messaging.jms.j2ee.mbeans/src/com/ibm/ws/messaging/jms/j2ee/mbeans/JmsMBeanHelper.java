/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.jms.j2ee.mbeans;

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This is helper class JMS Mbeans
 */
public class JmsMBeanHelper {

    // because we use a pack-info.java for trace options our group and message file is already there
    // We just need to register the class here
    private static final TraceComponent tc = Tr.register(JmsMBeanHelper.class);

    public static final String KEY_J2EETYPE = "j2eeType";
    public static final String KEY_JMSRESOURCE = "JMSResource";
    public static final String KEY_JMS_PARENT = "J2EEServer";
    public static final String KEY_NAME = "name";
    public static final String DOMAIN_NAME = "WebSphere";

    /**
     * Constructs Objectname as per JSR 77 spec i.e
     * "WebSphere:j2eeType=JMSResource,J2EEServer=serverName,name=JmsResourceName"
     * for JMSResource parent-type is mandatory and it is J2EEServer.
     * 
     * @param serverName
     * @param jmsResourceName
     * @param jmsResourceType
     * @return
     */
    static ObjectName getMBeanObject(String serverName, String jmsResourceName) {

        ObjectName jmsMBeanObjectName = null;
        Hashtable<String, String> properties = new Hashtable<String, String>();

        //construct JMSProvider MBean object name.
        properties.put(KEY_J2EETYPE, KEY_JMSRESOURCE);

        //as per JSR 77 spec,J2EEServer(i.e server name) is parent for JMSResource.
        properties.put(KEY_JMS_PARENT, serverName);

        //actual JMSResource name
        properties.put(KEY_NAME, jmsResourceName);

        try {
            jmsMBeanObjectName = new ObjectName(DOMAIN_NAME, properties);
        } catch (MalformedObjectNameException e) {
            // ignore exceptions - This will never happen
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getMBeanObject", e);
        }

        return jmsMBeanObjectName;
    }
}
