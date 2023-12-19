/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Assert;

import com.ibm.ws.javaee.dd.common.ContextService;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRefs;
import com.ibm.ws.javaee.dd.common.ManagedExecutor;
import com.ibm.ws.javaee.dd.common.ManagedScheduledExecutor;
import com.ibm.ws.javaee.dd.common.ManagedThreadFactory;
import com.ibm.ws.javaee.dd.common.Property;

/*
 * jakartaee_11.xsd - jakartaee_10.xsd
 *   <xsd:import namespace="http://www.w3.org/XML/1998/namespace"
              schemaLocation="https://www.w3.org/2001/xml.xsd"/>

<xsd:complexType name="context-serviceType">
      <xsd:element name="qualifier"
                   type="jakartaee:fully-qualified-classType"
                   minOccurs="0"
                   maxOccurs="unbounded">
      </xsd:element>
</xsd:complexType>

<xsd:complexType name="managed-executorType">
      <xsd:element name="virtual"
                   type="jakartaee:true-falseType"
                   minOccurs="0"
                   maxOccurs="1">
      </xsd:element>
      <xsd:element name="qualifier"
                   type="jakartaee:fully-qualified-classType"
                   minOccurs="0"
                   maxOccurs="unbounded">
      </xsd:element>
</xsd:complexType>

<xsd:complexType name="managed-scheduled-executorType">
      <xsd:element name="virtual"
                   type="jakartaee:true-falseType"
                   minOccurs="0"
                   maxOccurs="1">
      </xsd:element>
      <xsd:element name="qualifier"
                   type="jakartaee:fully-qualified-classType"
                   minOccurs="0"
                   maxOccurs="unbounded">
      </xsd:element>
</xsd:complexType>

<xsd:complexType name="managed-thread-factoryType">
      <xsd:element name="virtual"
                   type="jakartaee:true-falseType"
                   minOccurs="0"
                   maxOccurs="1">
      </xsd:element>
      <xsd:element name="qualifier"
                   type="jakartaee:fully-qualified-classType"
                   minOccurs="0"
                   maxOccurs="unbounded">
      </xsd:element>
</xsd:complexType>
*/
public class DDJakarta11Elements {

    public static List<String> names(String... name0) {
        return new ArrayList<String>(Arrays.asList(name0));
    }

    public static void withName(List<String> names, String name, Consumer<List<String>> action) {
        names.add(name);
        try {
            action.accept(names);
        } finally {
            names.remove(names.size() - 1);
        }
    }

    public static String dotNames(List<String> names) {
        int numNames = names.size();
        if (numNames == 0) {
            return "";
        }

        String name0 = names.get(0);
        if (numNames == 1) {
            return name0;
        }

        String name1 = names.get(1);
        if (numNames == 2) {
            return name0 + '.' + name1;
        }

        String name2 = names.get(2);
        if (numNames == 3) {
            return name0 + '.' + name1 + '.' + name2;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(name0).append('.').append(name1).append('.').append(name2);
        for (int nameNo = 3; nameNo < numNames; nameNo++) {
            builder.append('.');
            builder.append(names.get(nameNo));
        }
        return builder.toString();
    }

    public static void verify(List<String> names, String expected, String actual) {
        if (((expected == null) && (actual != null)) ||
            ((expected != null) && !expected.equals(actual))) {
            Assert.assertEquals(dotNames(names), expected, actual);
        }
    }

    public static void verify(List<String> names, int expected, int actual) {
        if (expected != actual) {
            Assert.assertEquals(dotNames(names), expected, actual);
        }
    }

    public static void verify(List<String> names, boolean expected, boolean actual) {
        if (expected != actual) {
            Assert.assertEquals(dotNames(names), expected, actual);
        }
    }

    public static void verify(
                              List<String> names,
                              String expectedLang, String expectedValue,
                              List<Description> actualDescriptions) {

        if (actualDescriptions.size() != 1) {
            Assert.assertEquals(
                                "Descriptions should be a singleton [ " + actualDescriptions + " ] ",
                                1, actualDescriptions.size());
        }

        Description actualDescription = actualDescriptions.get(0);
        withName(names, "lang",
                 (useNames) -> verify(useNames, expectedLang, actualDescription.getLang()));
        withName(names, "value",
                 (useNames) -> verify(useNames, expectedValue, actualDescription.getValue()));
    }

    public static void verify(
                              List<String> names,
                              String expectedName, String expectedValue,
                              Property actualProperty) {

        withName(names, "name",
                 (useNames) -> verify(useNames, expectedName, actualProperty.getName()));
        withName(names, "value",
                 (useNames) -> verify(useNames, expectedValue, actualProperty.getValue()));
    }

    public static void verifySize(List<String> names, int expectedSize, List<?> actualList) {
        withName(names, "size",
                 (useNames) -> verify(useNames, expectedSize, actualList.size()));
    }

    public static void verifySize(List<String> names, int expectedSize, String[] actualList) {
        withName(names, "size",
                 (useNames) -> verify(useNames, expectedSize, actualList.length));
    }

    public static void verifyDoubleton(
                                       List<String> names,
                                       String expectedName0, String expectedValue0,
                                       String expectedName1, String expectedValue1,
                                       List<? extends Property> properties) {

        verifySize(names, 2, properties);
        withName(names, "[0]",
                 (useNames) -> verify(useNames, expectedName0, expectedValue0, properties.get(0)));
        withName(names, "[1]",
                 (useNames) -> verify(useNames, expectedName1, expectedValue1, properties.get(1)));
    }

    public static final void verify(List<String> names, List<String> actual, String... expected) {
        verifySize(names, expected.length, actual);

        for (int elementNo = 0; elementNo < expected.length; elementNo++) {
            final int finalElementNo = elementNo; // Needed by the compiler.
            withName(names, "[" + elementNo + "]",
                     (useNames) -> verify(useNames, expected[finalElementNo], actual.get(finalElementNo)));
        }
    }

    public static final void verify(List<String> names, String[] actual, String... expected) {
        verifySize(names, expected.length, actual);

        for (int elementNo = 0; elementNo < expected.length; elementNo++) {
            final int finalElementNo = elementNo; // Needed by the compiler.
            withName(names, "[" + elementNo + "]",
                     (useNames) -> verify(useNames, expected[finalElementNo], actual[finalElementNo]));
        }
    }

    public static final String CONTEXT_SERVICE_XML = "<context-service id=\"CS01\">\n" +
                                                     "  <description>CS01-desc</description>\n" +
                                                     "  <name>CS01:name</name>\n" +
                                                     "  <cleared>Application</cleared>\n" +
                                                     "  <cleared>Security</cleared>\n" +
                                                     "  <cleared>Transaction</cleared>\n" +
                                                     "  <cleared>Remaining</cleared>\n" +
                                                     "  <cleared>Other</cleared>\n" +
                                                     "  <propagated>Application</propagated>\n" +
                                                     "  <propagated>Security</propagated>\n" +
                                                     "  <propagated>Remaining</propagated>\n" +
                                                     "  <propagated>Other</propagated>\n" +
                                                     "  <unchanged>Application</unchanged>\n" +
                                                     "  <unchanged>Security</unchanged>\n" +
                                                     "  <unchanged>Transaction</unchanged>\n" +
                                                     "  <unchanged>Remaining</unchanged>\n" +
                                                     "  <unchanged>Other</unchanged>\n" +
                                                     "  <qualifier>com.ibm.test.Qualifier1</qualifier>\n" +
                                                     "  <qualifier>com.ibm.test.Qualifier2</qualifier>\n" +
                                                     "  <property>\n" +
                                                     "    <name>CS01</name>\n" +
                                                     "    <value>CS01-value</value>\n" +
                                                     "  </property>\n" +
                                                     "  <property>\n" +
                                                     "    <name>CS02</name>\n" +
                                                     "    <value>CS02-value</value>\n" +
                                                     "  </property>\n" +
                                                     "</context-service>\n";

    public static void verify(List<String> names, ContextService contextService) {
        // withName(names, "id",
        //        (useNames) -> verify("CS01", contextService.getID())); // Can't be accessed here.
        withName(names, "description",
                 (useNames) -> verify(useNames, null, "CS01-desc", contextService.getDescriptions()));

        withName(names, "name",
                 (useNames) -> verify(useNames, "CS01:name", contextService.getName()));
        withName(names, "cleared",
                 (useNames) -> verify(useNames,
                                      contextService.getCleared(),
                                      "Application", "Security", "Transaction", "Remaining", "Other"));
        withName(names, "propagated",
                 (useNames) -> verify(useNames,
                                      contextService.getPropagated(),
                                      "Application", "Security", "Remaining", "Other"));
        withName(names, "unchanged",
                 (useNames) -> verify(useNames,
                                      contextService.getUnchanged(),
                                      "Application", "Security", "Transaction", "Remaining", "Other"));
        withName(names, "qualifier",
                 (useNames) -> verify(useNames,
                                      contextService.getQualifiers(),
                                      "com.ibm.test.Qualifier1", "com.ibm.test.Qualifier2"));
        withName(names, "properties",
                 (useNames) -> verifyDoubleton(useNames,
                                               "CS01", "CS01-value", "CS02", "CS02-value",
                                               contextService.getProperties()));
    }

    public static final String MANAGED_EXECUTOR_XML = "<managed-executor id=\"ME01\">\n" +
                                                      "  <description>ME01-desc</description>\n" +
                                                      "  <name>ME01:name</name>\n" +
                                                      "  <context-service-ref>java:comp</context-service-ref>\n" +
                                                      "  <max-async>10</max-async>\n" +
                                                      "  <hung-task-threshold>1000</hung-task-threshold>\n" +
                                                      "  <virtual>true</virtual>\n" +
                                                      "  <qualifier>com.ibm.test.Qualifier3</qualifier>\n" +
                                                      "  <property>\n" +
                                                      "    <name>ME01</name>\n" +
                                                      "    <value>ME01-value</value>\n" +
                                                      "  </property>\n" +
                                                      "  <property>\n" +
                                                      "    <name>ME02</name>\n" +
                                                      "    <value>ME02-value</value>\n" +
                                                      "  </property>\n" +
                                                      "</managed-executor>\n";

    public static void verify(List<String> names, ManagedExecutor executor) {
        // withName(names, "id",
        //        (useNames) -> verify("ME01", executor.getID())); // Can't be accessed here.
        withName(names, "description",
                 (useNames) -> verify(useNames, null, "ME01-desc", executor.getDescriptions()));
        withName(names, "name",
                 (useNames) -> verify(useNames, "ME01:name", executor.getName()));
        withName(names, "contextServiceRef",
                 (useNames) -> verify(useNames, "java:comp", executor.getContextServiceRef()));
        withName(names, "maxAsync",
                 (useNames) -> verify(useNames, 10, executor.getMaxAsync()));
        withName(names, "hungTaskThreshold",
                 (useNames) -> verify(useNames, 1000, (int) executor.getHungTaskThreshold()));
        withName(names, "virtual",
                 (useNames) -> verify(useNames, true, executor.isVirtual()));
        withName(names, "qualifier",
                 (useNames) -> verify(useNames,
                                      executor.getQualifiers(),
                                      "com.ibm.test.Qualifier3"));
        withName(names, "properties",
                 (useNames) -> verifyDoubleton(useNames,
                                               "ME01", "ME01-value", "ME02", "ME02-value",
                                               executor.getProperties()));
    }

    public static final String MANAGED_SCHEDULED_EXECUTOR_XML = "<managed-scheduled-executor id=\"MSE01\">\n" +
                                                                "  <description>MSE01-desc</description>\n" +
                                                                "  <name>MSE01:name</name>\n" +
                                                                "  <context-service-ref>java:module</context-service-ref>\n" +
                                                                "  <max-async>11</max-async>\n" +
                                                                "  <hung-task-threshold>1100</hung-task-threshold>\n" +
                                                                "  <qualifier>com.ibm.test.Qualifier4</qualifier>\n" +
                                                                "  <qualifier>com.ibm.test.Qualifier5</qualifier>\n" +
                                                                "  <qualifier>com.ibm.test.Qualifier6</qualifier>\n" +
                                                                "  <property>\n" +
                                                                "    <name>MSE01</name>\n" +
                                                                "    <value>MSE01-value</value>\n" +
                                                                "  </property>\n" +
                                                                "  <property>\n" +
                                                                "    <name>MSE02</name>\n" +
                                                                "    <value>MSE02-value</value>\n" +
                                                                "  </property>\n" +
                                                                "</managed-scheduled-executor>\n";

    public static void verify(List<String> names, ManagedScheduledExecutor executor) {
        // withName(names, "id",
        //        (useNames) -> verify("MSE01", executor.getID())); // Can't be accessed here.
        withName(names, "description",
                 (useNames) -> verify(useNames, null, "MSE01-desc", executor.getDescriptions()));
        withName(names, "name",
                 (useNames) -> verify(useNames, "MSE01:name", executor.getName()));
        withName(names, "contextServiceRef",
                 (useNames) -> verify(useNames, "java:module", executor.getContextServiceRef()));
        withName(names, "maxAsync",
                 (useNames) -> verify(useNames, 11, executor.getMaxAsync()));
        withName(names, "hungTaskThreshold",
                 (useNames) -> verify(useNames, 1100, (int) executor.getHungTaskThreshold()));
        withName(names, "virtual",
                 (useNames) -> verify(useNames, false, executor.isVirtual())); //Default value should be false
        withName(names, "qualifier",
                 (useNames) -> verify(useNames,
                                      executor.getQualifiers(),
                                      "com.ibm.test.Qualifier4", "com.ibm.test.Qualifier5", "com.ibm.test.Qualifier6"));
        withName(names, "properties",
                 (useNames) -> verifyDoubleton(useNames,
                                               "MSE01", "MSE01-value", "MSE02", "MSE02-value",
                                               executor.getProperties()));
    }

    public static final String MANAGED_THREAD_FACTORY_XML = "<managed-thread-factory id=\"MTF01\">\n" +
                                                            "  <description>MTF01-desc</description>\n" +
                                                            "  <name>MTF01:name</name>\n" +
                                                            "  <context-service-ref>java:app</context-service-ref>\n" +
                                                            "  <priority>10</priority>\n" +
                                                            "  <virtual>false</virtual>\n" +
                                                            "  <property>\n" +
                                                            "    <name>MTF01</name>\n" +
                                                            "    <value>MTF01-value</value>\n" +
                                                            "  </property>\n" +
                                                            "  <property>\n" +
                                                            "    <name>MTF02</name>\n" +
                                                            "    <value>MTF02-value</value>\n" +
                                                            "  </property>\n" +
                                                            "</managed-thread-factory>\n";

    public static void verify(List<String> names, ManagedThreadFactory factory) {
        // withName(names, "id",
        //        (useNames) -> verify("MTF01", factory.getID())); // Can't be accessed here.
        withName(names, "description",
                 (useNames) -> verify(useNames, null, "MTF01-desc", factory.getDescriptions()));
        withName(names, "name",
                 (useNames) -> verify(useNames, "MTF01:name", factory.getName()));
        withName(names, "contextServiceRef",
                 (useNames) -> verify(useNames, "java:app", factory.getContextServiceRef()));
        withName(names, "priority",
                 (useNames) -> verify(useNames, 10, factory.getPriority()));
        withName(names, "virtual",
                 (useNames) -> verify(useNames, false, factory.isVirtual()));
        withName(names, "qualifier",
                 (useNames) -> verify(useNames,
                                      factory.getQualifiers())); //None provided
        withName(names, "properties",
                 (useNames) -> verifyDoubleton(useNames,
                                               "MTF01", "MTF01-value", "MTF02", "MTF02-value",
                                               factory.getProperties()));
    }

    //

    public static void verifyEE11(List<String> names, JNDIEnvironmentRefs refs) {
        List<ContextService> services = refs.getContextServices();
        DDJakarta11Elements.verifySize(names, 1, services);
        DDJakarta11Elements.verify(names, services.get(0));

        List<ManagedExecutor> executors = refs.getManagedExecutors();
        DDJakarta11Elements.verifySize(names, 1, executors);
        DDJakarta11Elements.verify(names, executors.get(0));

        List<ManagedScheduledExecutor> scheduledExecutors = refs.getManagedScheduledExecutors();
        DDJakarta11Elements.verifySize(names, 1, scheduledExecutors);
        DDJakarta11Elements.verify(names, scheduledExecutors.get(0));

        List<ManagedThreadFactory> factories = refs.getManagedThreadFactories();
        DDJakarta11Elements.verifySize(names, 1, factories);
        DDJakarta11Elements.verify(names, factories.get(0));
    }
}
