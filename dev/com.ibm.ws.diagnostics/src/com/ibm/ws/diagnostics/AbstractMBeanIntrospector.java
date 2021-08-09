/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.diagnostics;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Base class for any introspector that can be implemented by
 * dumping MBean attributes. It is not perfect. It may not
 * completely traverse complex types and if the target is not
 * an MXBean, the attribute's toString is used.
 */
public class AbstractMBeanIntrospector {

    /**
     * The standard indent for the report.
     */
    private final static String INDENT = "    ";

    /**
     * GIven a JMX object name and a filter, introspect all matching
     * MBeans that are found associated with registered MBean servers.
     * 
     * @param objectName the object name pattern to provide
     *            to {@link javax.management.MBeanServer#queryNames(ObjectName, QueryExp)
     *            queryNames}
     * @param query the query expression to provide to {@code queryNames}
     */
    protected void introspect(ObjectName objectName, QueryExp query, PrintWriter writer) {
        // Iterate over the mbean servers, query for the beans, introspect
        for (MBeanServer mbeanServer : getMBeanServers()) {
            for (ObjectName mbean : mbeanServer.queryNames(objectName, query)) {
                introspectMBeanAttributes(mbeanServer, mbean, writer);
            }
        }
    }

    /**
     * Get the set MBean servers that includes the platform MBean server and any
     * that can be found with {@link MBeanServerFactory#findMBeanServer(String) findMBeanServer}.
     * 
     * @return the set of discovered mbean servers
     */
    Collection<MBeanServer> getMBeanServers() {
        Collection<MBeanServer> mbeanServers = new HashSet<MBeanServer>();
        mbeanServers.add(ManagementFactory.getPlatformMBeanServer());
        mbeanServers.addAll(MBeanServerFactory.findMBeanServer(null));
        return mbeanServers;
    }

    /**
     * Introspect an MBean's attributes. Introspection will capture the MBean's
     * description, its canonical object name, and its attributes. When attributes
     * are a complex {@link OpenType}, an attempt will be made to format the complex
     * type. Primitives and types that do not have metadata will be formatted
     * with {@linkplain String#valueOf}.
     * 
     * @param mbeanServer the mbean server hosting the mbean
     * @param mbean the mbean to introspect
     * @param writer the print writer to write the instrospection to
     */
    @FFDCIgnore(Throwable.class)
    void introspectMBeanAttributes(MBeanServer mbeanServer, ObjectName mbean, PrintWriter writer) {
        try {
            // Dump the description and the canonical form of the object name
            writer.println();
            writer.print(mbeanServer.getMBeanInfo(mbean).getDescription());
            writer.println(" (" + mbean.getCanonicalName() + ")");

            // Iterate over the attributes of the mbean.  For each, capture the name
            // and then attempt format the attribute.
            String indent = INDENT;
            MBeanAttributeInfo[] attrs = mbeanServer.getMBeanInfo(mbean).getAttributes();
            for (MBeanAttributeInfo attr : attrs) {
                StringBuilder sb = new StringBuilder();
                sb.append(indent).append(attr.getName()).append(": ");
                sb.append(getFormattedAttribute(mbeanServer, mbean, attr, indent));
                writer.println(sb.toString());
            }
        } catch (Throwable t) {
            Throwable rootCause = t;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("<<Introspection failed: ");
            sb.append(rootCause.getMessage() != null ? rootCause.getMessage() : rootCause);
            sb.append(">>");
            writer.println(sb.toString());
        }
    }

    /**
     * Attempt for format an MBean attribute. Composite and Tabular data types will
     * be recursively formatted, arrays will be formatted with each entry on one line,
     * while all others will rely on the value returned by {@code toString}.
     * 
     * @param mbeanServer the mbean server hosting the mbean
     * @param mbean the mbean associated with the attribute
     * @param attr the mbean attribute to format
     * @param indent the current indent level of the formatted report
     * 
     * @return the formatted attribute
     */
    @FFDCIgnore(Throwable.class)
    String getFormattedAttribute(MBeanServer mbeanServer, ObjectName mbean, MBeanAttributeInfo attr, String indent) {
        try {
            Object attribute = mbeanServer.getAttribute(mbean, attr.getName());
            if (attribute == null) {
                return "null";
            } else if (CompositeData.class.isAssignableFrom(attribute.getClass())) {
                return getFormattedCompositeData(CompositeData.class.cast(attribute), indent);
            } else if (TabularData.class.isAssignableFrom(attribute.getClass())) {
                return getFormattedTabularData(TabularData.class.cast(attribute), indent);
            } else if (attribute.getClass().isArray()) {
                return getFormattedArray(attribute, indent);
            } else {
                return String.valueOf(attribute);
            }
        } catch (Throwable t) {
            Throwable rootCause = t;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("<<Unavailable: ");
            sb.append(rootCause.getMessage() != null ? rootCause.getMessage() : rootCause);
            sb.append(">>");
            return sb.toString();
        }
    }

    /**
     * Format an array.
     * 
     * Note: This doesn't properly format arrays of composite types.
     * 
     * @param attribute the mbean attribute to format
     * @param the current indent level of the formatted report
     * 
     * @return the formatted array
     */
    String getFormattedArray(Object attribute, String indent) {
        int arrayLength = Array.getLength(attribute);
        if (arrayLength == 0) {
            return "[]";
        }

        Class<?> componentType = attribute.getClass().getComponentType();

        // For the 8 primitive types, a cast is necessary when invoking
        // Arrays.toString.  The array data is then broken into separate
        // lines
        if (componentType.equals(boolean.class)) {
            return formatPrimitiveArrayString(Arrays.toString((boolean[]) attribute), indent);
        } else if (componentType.equals(byte.class)) {
            return formatPrimitiveArrayString(Arrays.toString((byte[]) attribute), indent);
        } else if (componentType.equals(char.class)) {
            return formatPrimitiveArrayString(Arrays.toString((char[]) attribute), indent);
        } else if (componentType.equals(double.class)) {
            return formatPrimitiveArrayString(Arrays.toString((double[]) attribute), indent);
        } else if (componentType.equals(float.class)) {
            return formatPrimitiveArrayString(Arrays.toString((float[]) attribute), indent);
        } else if (componentType.equals(int.class)) {
            return formatPrimitiveArrayString(Arrays.toString((int[]) attribute), indent);
        } else if (componentType.equals(long.class)) {
            return formatPrimitiveArrayString(Arrays.toString((long[]) attribute), indent);
        } else if (componentType.equals(short.class)) {
            return formatPrimitiveArrayString(Arrays.toString((short[]) attribute), indent);
        }

        // Arbitrary strings can have ', ' in them so we iterate here
        StringBuilder sb = new StringBuilder();
        indent += INDENT;

        Object[] array = (Object[]) attribute;
        for (int i = 0; i < array.length; i++) {
            sb.append("\n").append(indent).append("[").append(i).append("]: ");
            sb.append(String.valueOf(array[i]));
        }
        return sb.toString();
    }

    /**
     * Reformat the string produced by {@link Arrays.toString} to put each
     * element of the array on its own line with its index in front.
     * 
     * @param string the output of {@code Arrays.toString}
     * @param the current indent level of the formatted report
     * 
     * @return the formatted array
     */
    String formatPrimitiveArrayString(String string, String indent) {
        StringBuilder sb = new StringBuilder();
        indent += INDENT;

        // Split the data based on the command white space
        String[] data = string.substring(1, string.length() - 2).split(", ");
        for (int i = 0; i < data.length; i++) {
            sb.append("\n").append(indent).append("[").append(i).append("]: ");
            sb.append(data[i]);
        }
        return sb.toString();
    }

    /**
     * Format an open MBean composite data attribute.
     * 
     * @param cd the composite data attribute
     * @param indent the current indent level of the formatted report
     * 
     * @return the formatted composite data
     */
    String getFormattedCompositeData(CompositeData cd, String indent) {
        StringBuilder sb = new StringBuilder();
        indent += INDENT;
        CompositeType type = cd.getCompositeType();
        for (String key : type.keySet()) {
            sb.append("\n").append(indent);
            sb.append(key).append(": ");
            OpenType<?> openType = type.getType(key);
            if (openType instanceof SimpleType) {
                sb.append(cd.get(key));
            } else if (openType instanceof CompositeType) {
                CompositeData nestedData = (CompositeData) cd.get(key);
                sb.append(getFormattedCompositeData(nestedData, indent));
            } else if (openType instanceof TabularType) {
                TabularData tabularData = (TabularData) cd.get(key);
                sb.append(tabularData);
            }
        }
        return String.valueOf(sb);
    }

    /**
     * Format an open MBean tabular data attribute.
     * 
     * @param td the tabular data attribute
     * @param indent the current indent level of the formatted report
     * 
     * @return the formatted composite data
     */
    @SuppressWarnings("unchecked")
    String getFormattedTabularData(TabularData td, String indent) {
        StringBuilder sb = new StringBuilder();
        indent += INDENT;

        sb.append("{");
        Collection<CompositeData> values = (Collection<CompositeData>) td.values();
        int valuesRemaining = values.size();
        for (CompositeData cd : values) {
            sb.append(getFormattedCompositeData(cd, indent));
            if (--valuesRemaining > 0) {
                sb.append("\n").append(indent).append("}, {");
            }
        }
        sb.append("}");

        return String.valueOf(sb);
    }

}
