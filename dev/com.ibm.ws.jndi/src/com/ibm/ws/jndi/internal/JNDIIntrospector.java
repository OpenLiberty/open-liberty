/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;

import com.ibm.wsspi.logging.Introspector;

@Component(property = "service.vendor=IBM")
public class JNDIIntrospector implements Introspector {

    @Override
    public String getIntrospectorName() {
        return "JNDIDefaultNamespace";
    }

    @Override
    public String getIntrospectorDescription() {
        return "JNDI default namespace";
    }

    @Override
    public void introspect(PrintWriter writer) throws IOException {
        writer.println(); // add a blank line
        writer.println("======================================================================================");
        writer.println("Beginning of Dump");
        writer.println("======================================================================================");
        writer.println(); // add a blank line

        //Output columns titles
        writer.printf("%-15s %-45s %s", "Service.id", "Service.name", "ObjectClass").println();

        ContextNode root = JNDIServiceBinderManager.JNDIServiceBinderHolder.HELPER.root;

        writer.println("Total number of items: " + outputEntries(writer, root, ""));
        writer.println();

        //Output indication of end of dump.
        writer.println("======================================================================================");
        writer.println("End of Dump");
        writer.println("======================================================================================");
    }

    private int outputEntries(PrintWriter pw, ContextNode node, String parentNamePrefix) {

        int numberOfEntries = 0;

        for (Entry<String, Object> entry : new TreeMap<String, Object>(node.children).entrySet()) {

            String key = parentNamePrefix + entry.getKey();

            Object value = entry.getValue();

            if (value instanceof ContextNode) {
                key = key.concat("/");
                numberOfEntries += outputEntries(pw, ((ContextNode) value), key);
            }
            else {
                numberOfEntries++;

                Object serviceID = null, objectClass = null;

                Object lastEntry = value instanceof AutoBindNode ? ((AutoBindNode) value).getLastEntry() : null;
                if (lastEntry instanceof ServiceReference<?>) {
                    serviceID = ((ServiceReference<?>) lastEntry).getProperty(Constants.SERVICE_ID);
                    objectClass = ((ServiceReference<?>) lastEntry).getProperty(Constants.OBJECTCLASS);
                    if (objectClass instanceof Object[]) {
                        objectClass = ((Object[]) objectClass)[0];
                    }
                } else {
                    serviceID = null;
                    objectClass = lastEntry != null ? lastEntry : value;
                }

                pw.printf("%-15s %-45s %s", serviceID, key, objectClass).println();
            }
        }

        return numberOfEntries;
    }
}