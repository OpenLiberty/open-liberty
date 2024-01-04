/*******************************************************************************
 * Copyright (c) 2013, 2023 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmodel.web;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.common.ContextService;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.javaee.dd.common.ManagedExecutor;
import com.ibm.ws.javaee.dd.common.ManagedScheduledExecutor;
import com.ibm.ws.javaee.dd.common.ManagedThreadFactory;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.common.AbsoluteOrdering;
import com.ibm.ws.javaee.dd.web.common.AttributeValue;
import com.ibm.ws.javaee.dd.web.common.CookieConfig;
import com.ibm.ws.javaee.ddmodel.DDJakarta10Elements;
import com.ibm.ws.javaee.ddmodel.DDJakarta11Elements;

public class WebAppTest extends WebAppTestBase {
    // Absolute ordering fragments.

    private static final String absOrder() {
        return "<absolute-ordering>" +
               "<name>Fragment1</name>" +
               "<name>Fragment2</name>" +
               "</absolute-ordering>";
    }

    private static final String othersAbsOrder() {
        return "<absolute-ordering>" +
               "<name>Fragment1</name>" +
               "<others/>" +
               "<name>Fragment2</name>" +
               "</absolute-ordering>";
    }

    private static final String dupeAbsOrder() {
        return "<absolute-ordering>" +
               "<name>Fragment1</name>" +
               "<name>Fragment2</name>" +
               "</absolute-ordering>" +
               "<absolute-ordering>" +
               "</absolute-ordering>";
    }

    @Test
    public void testWebApp() throws Exception {
        for (int schemaVersion : WebApp.VERSIONS) {
            for (int maxSchemaVersion : WebApp.VERSIONS) {
                // The WebApp parser uses a maximum schema
                // version of "max(version, WebApp.VERSION_3_0)".
                // Adjust the message expectations accordingly.
                //
                // See: com.ibm.ws.javaee.ddmodel.web.WebAppDDParser

                int effectiveMax;
                if (maxSchemaVersion < WebApp.VERSION_3_0) {
                    effectiveMax = WebApp.VERSION_3_0;
                } else {
                    effectiveMax = maxSchemaVersion;
                }

                String altMessage;
                String[] messages;
                if (schemaVersion > effectiveMax) {
                    altMessage = UNPROVISIONED_DESCRIPTOR_VERSION_ALT_MESSAGE;
                    messages = UNPROVISIONED_DESCRIPTOR_VERSION_MESSAGES;
                } else {
                    altMessage = null;
                    messages = null;
                }

                parseWebApp(webApp(schemaVersion, webAppBody()),
                            maxSchemaVersion,
                            altMessage, messages);
            }
        }
    }

    // Servlet 3.0 specific cases ...

    /**
     * Verify that spaces are trimmed from environment entry
     * names but not from environment entry values.
     */
    @Test
    public void testEE6Web30EnvEntryValueWhitespace() throws Exception {
        WebApp webApp = parseWebApp(
                                    webApp(WebApp.VERSION_3_0,
                                           "<env-entry>" +
                                                               "<env-entry-name> envName </env-entry-name>" +
                                                               "<env-entry-value> envValue </env-entry-value>" +
                                                               "</env-entry>"));

        List<EnvEntry> envEntries = webApp.getEnvEntries();
        Assert.assertNotNull(envEntries);
        Assert.assertEquals(envEntries.size(), 1);
        EnvEntry envEntry = envEntries.get(0);
        Assert.assertNotNull(envEntry);
        Assert.assertNotNull(envEntry.getName());
        Assert.assertEquals(envEntry.getName(), "envName");
        Assert.assertNotNull(envEntry.getValue());
        Assert.assertEquals(envEntry.getValue(), " envValue ");
    }

    /**
     * Verify the parsing of an absolute ordering element.
     */
    @Test
    public void testEE6Web30AbsoluteOrderingElement() throws Exception {
        WebApp webApp = parseWebApp(webApp(WebApp.VERSION_3_0, absOrder()));

        AbsoluteOrdering absOrder = webApp.getAbsoluteOrdering();
        Assert.assertNotNull(absOrder);
        Assert.assertFalse(absOrder.isSetOthers());
        List<String> beforeOthers = absOrder.getNamesBeforeOthers();
        Assert.assertEquals(beforeOthers.size(), 2);
        List<String> afterOthers = absOrder.getNamesAfterOthers();
        Assert.assertEquals(afterOthers.size(), 0);
    }

    /**
     * Verify the parsing of the absolute ordering element which
     * has an others element.
     */
    @Test
    public void testEE6Web30OthersElement() throws Exception {
        WebApp webApp = parseWebApp(webApp(WebApp.VERSION_3_0, othersAbsOrder()));

        AbsoluteOrdering absOrder = webApp.getAbsoluteOrdering();
        Assert.assertNotNull(absOrder);
        Assert.assertTrue(absOrder.isSetOthers());
        List<String> beforeOthers = absOrder.getNamesBeforeOthers();
        Assert.assertEquals(beforeOthers.size(), 1);
        List<String> afterOthers = absOrder.getNamesAfterOthers();
        Assert.assertEquals(afterOthers.size(), 1);
    }

    // The prohibition against having more than one absolute ordering element
    // was added in JavaEE7.
    //
    // Duplicate elements are allowed in EE6.

    /**
     * Verify that duplicate absolute ordering elements are
     * allowed. These are allowed in Servlet 3.0 / Java EE6.
     */
    @Test
    public void testEE6Web30AbsoluteOrderingDuplicateElements() throws Exception {
        parseWebApp(webApp(WebApp.VERSION_3_0, dupeAbsOrder()));
    }

    /**
     * Verify that empty deny-uncovered-http-methods are not allowed.
     */
    @Test
    public void testEE6Web30DenyUncoveredHttpMethods() throws Exception {
        parseWebApp(webApp(WebApp.VERSION_3_0, "<deny-uncovered-http-methods/>"),
                    "unexpected.child.element",
                    "CWWKC2259E", "deny-uncovered-http-methods", "web-app", "MyWar.war : WEB-INF/web.xml");
    }

    // Servlet 3.1 cases ...

    // The prohibition against having more than one absolute ordering element
    // was added in JavaEE7.
    //
    // Duplicate elements are not allowed in EE7.

    /**
     * Verify that duplicate absolute ordering elements are
     * not allowed. These were allowed in Servlet 3.0 / Java EE6,
     * but are not allowed in Servlet 3.1 / Java EE7.
     */
    @Test
    public void testEE7Web31AbsoluteOrderingDuplicates() throws Exception {
        parseWebApp(webApp(WebApp.VERSION_3_1, dupeAbsOrder()),
                    WebApp.VERSION_3_1,
                    "at.most.one.occurrence",
                    "CWWKC2266E", "absolute-ordering", "MyWar.war : WEB-INF/web.xml");
    }

    /**
     * Verify that empty deny-uncovered-http-methods are now allowed.
     * They are allowed in Servlet 3.1 / Java EE7. They were not
     * allowed in Servlet 3.0 / Java EE6.
     */
    @Test
    public void testEE7Web31DenyUncoveredHttpMethods() throws Exception {
        parseWebApp(webApp(WebApp.VERSION_3_1, "<deny-uncovered-http-methods/>"),
                    WebApp.VERSION_3_1);
    }

    /**
     * Verify that non-empty deny-uncovered-http-methods are not allowed.
     */
    @Test
    public void testEE7Web31DenyUncoveredHttpMethodsNotEmptyType() throws Exception {
        parseWebApp(webApp(WebApp.VERSION_3_1,
                           "<deny-uncovered-http-methods>junk</deny-uncovered-http-methods>"),
                    WebApp.VERSION_3_1,
                    "unexpected.content",
                    "CWWKC2257E", "deny-uncovered-http-methods", "MyWar.war : WEB-INF/web.xml");
    }

    // EE10 element testing ...

    @Test
    public void testEE10ContextServiceWeb31() throws Exception {
        parseWebApp(webApp(WebApp.VERSION_3_1, DDJakarta10Elements.CONTEXT_SERVICE_XML),
                    WebApp.VERSION_3_1,
                    "unexpected.child.element",
                    "CWWKC2259E", "context-service",
                    "MyWar.war : WEB-INF/web.xml");
    }

    @Test
    public void testEE10ManagedExecutorWeb31() throws Exception {
        parseWebApp(webApp(WebApp.VERSION_3_1, DDJakarta10Elements.MANAGED_EXECUTOR_XML),
                    WebApp.VERSION_3_1,
                    "unexpected.child.element",
                    "CWWKC2259E", "managed-executor",
                    "MyWar.war : WEB-INF/web.xml");
    }

    @Test
    public void testEE10ManagedScheduledExecutorWeb31() throws Exception {
        parseWebApp(webApp(WebApp.VERSION_3_1, DDJakarta10Elements.MANAGED_SCHEDULED_EXECUTOR_XML),
                    WebApp.VERSION_3_1,
                    "unexpected.child.element",
                    "CWWKC2259E", "managed-scheduled-executor",
                    "MyWar.war : WEB-INF/web.xml");
    }

    @Test
    public void testEE10ManagedThreadFactoryWeb31() throws Exception {
        parseWebApp(webApp(WebApp.VERSION_3_1, DDJakarta10Elements.MANAGED_THREAD_FACTORY_XML),
                    WebApp.VERSION_3_1,
                    "unexpected.child.element",
                    "CWWKC2259E", "managed-thread-factory",
                    "MyWar.war : WEB-INF/web.xml");
    }

    //

    @Test
    public void testEE10ContextServiceWeb50() throws Exception {
        parseWebApp(webApp(WebApp.VERSION_5_0, DDJakarta10Elements.CONTEXT_SERVICE_XML),
                    WebApp.VERSION_5_0,
                    "unexpected.child.element",
                    "CWWKC2259E", "context-service",
                    "MyWar.war : WEB-INF/web.xml");
    }

    @Test
    public void testEE10ManagedExecutorWeb50() throws Exception {
        parseWebApp(webApp(WebApp.VERSION_5_0, DDJakarta10Elements.MANAGED_EXECUTOR_XML),
                    WebApp.VERSION_5_0,
                    "unexpected.child.element",
                    "CWWKC2259E", "managed-executor",
                    "MyWar.war : WEB-INF/web.xml");
    }

    @Test
    public void testEE10ManagedScheduledExecutorWeb50() throws Exception {
        parseWebApp(webApp(WebApp.VERSION_5_0, DDJakarta10Elements.MANAGED_SCHEDULED_EXECUTOR_XML),
                    WebApp.VERSION_5_0,
                    "unexpected.child.element",
                    "CWWKC2259E", "managed-scheduled-executor",
                    "MyWar.war : WEB-INF/web.xml");
    }

    @Test
    public void testEE10ManagedThreadFactoryWeb50() throws Exception {
        parseWebApp(webApp(WebApp.VERSION_5_0, DDJakarta10Elements.MANAGED_THREAD_FACTORY_XML),
                    WebApp.VERSION_5_0,
                    "unexpected.child.element",
                    "CWWKC2259E", "managed-thread-factory",
                    "MyWar.war : WEB-INF/web.xml");
    }

    //

    @Test
    public void testEE10ContextServiceWeb60() throws Exception {
        WebApp webApp = parseWebApp(
                                    webApp(WebApp.VERSION_6_0, DDJakarta10Elements.CONTEXT_SERVICE_XML),
                                    WebApp.VERSION_6_0);

        List<String> names = DDJakarta10Elements.names("WebApp", "contextServices");

        List<ContextService> services = webApp.getContextServices();
        DDJakarta10Elements.verifySize(names, 1, services);
        DDJakarta10Elements.verify(names, services.get(0));
    }

    @Test
    public void testEE10ManagedExecutorWeb60() throws Exception {
        WebApp webApp = parseWebApp(
                                    webApp(WebApp.VERSION_6_0, DDJakarta10Elements.MANAGED_EXECUTOR_XML),
                                    WebApp.VERSION_6_0);

        List<String> names = DDJakarta10Elements.names("WebApp", "managedExecutors");

        List<ManagedExecutor> executors = webApp.getManagedExecutors();
        DDJakarta10Elements.verifySize(names, 1, executors);
        DDJakarta10Elements.verify(names, executors.get(0));
    }

    @Test
    public void testEE10ManagedScheduledExecutorWeb60() throws Exception {
        WebApp webApp = parseWebApp(
                                    webApp(WebApp.VERSION_6_0, DDJakarta10Elements.MANAGED_SCHEDULED_EXECUTOR_XML),
                                    WebApp.VERSION_6_0);

        List<String> names = DDJakarta10Elements.names("WebApp", "managedScheduledExecutors");

        List<ManagedScheduledExecutor> executors = webApp.getManagedScheduledExecutors();
        DDJakarta10Elements.verifySize(names, 1, executors);
        DDJakarta10Elements.verify(names, executors.get(0));
    }

    @Test
    public void testEE10ManagedThreadFactoryWeb60() throws Exception {
        WebApp webApp = parseWebApp(
                                    webApp(WebApp.VERSION_6_0, DDJakarta10Elements.MANAGED_THREAD_FACTORY_XML),
                                    WebApp.VERSION_6_0);

        List<String> names = DDJakarta10Elements.names("WebApp", "managedThreadFactories");

        List<ManagedThreadFactory> factories = webApp.getManagedThreadFactories();
        DDJakarta10Elements.verifySize(names, 1, factories);
        DDJakarta10Elements.verify(names, factories.get(0));
    }

    //

    @Test
    public void testEE11ContextServiceWeb61() throws Exception {
        WebApp webApp = parseWebApp(
                                    webApp(WebApp.VERSION_6_1, DDJakarta11Elements.CONTEXT_SERVICE_XML),
                                    WebApp.VERSION_6_1);

        List<String> names = DDJakarta10Elements.names("WebApp", "contextServices");

        List<ContextService> services = webApp.getContextServices();
        DDJakarta10Elements.verifySize(names, 1, services);
        DDJakarta10Elements.verify(names, services.get(0));
    }

    @Test
    public void testEE11ManagedExecutorWeb61() throws Exception {
        WebApp webApp = parseWebApp(
                                    webApp(WebApp.VERSION_6_1, DDJakarta11Elements.MANAGED_EXECUTOR_XML),
                                    WebApp.VERSION_6_1);

        List<String> names = DDJakarta10Elements.names("WebApp", "managedExecutors");

        List<ManagedExecutor> executors = webApp.getManagedExecutors();
        DDJakarta10Elements.verifySize(names, 1, executors);
        DDJakarta10Elements.verify(names, executors.get(0));
    }

    @Test
    public void testEE11ManagedScheduledExecutorWeb61() throws Exception {
        WebApp webApp = parseWebApp(
                                    webApp(WebApp.VERSION_6_1, DDJakarta11Elements.MANAGED_SCHEDULED_EXECUTOR_XML),
                                    WebApp.VERSION_6_1);

        List<String> names = DDJakarta10Elements.names("WebApp", "managedScheduledExecutors");

        List<ManagedScheduledExecutor> executors = webApp.getManagedScheduledExecutors();
        DDJakarta10Elements.verifySize(names, 1, executors);
        DDJakarta10Elements.verify(names, executors.get(0));
    }

    @Test
    public void testEE11ManagedThreadFactoryWeb61() throws Exception {
        WebApp webApp = parseWebApp(
                                    webApp(WebApp.VERSION_6_1, DDJakarta11Elements.MANAGED_THREAD_FACTORY_XML),
                                    WebApp.VERSION_6_1);

        List<String> names = DDJakarta10Elements.names("WebApp", "managedThreadFactories");

        List<ManagedThreadFactory> factories = webApp.getManagedThreadFactories();
        DDJakarta10Elements.verifySize(names, 1, factories);
        DDJakarta10Elements.verify(names, factories.get(0));
    }

    public static final String COOKIE_ATTRIBUTE_PREFIX = "<session-config>\n" +
                                                         "<cookie-config>\n" +
                                                         "<name>CookieConfigName_viaWebXML</name>\n" +
                                                         "<domain>CookieConfigDomain_viaWebXML</domain>\n" +
                                                         "<path>CookieConfigPath_viaWebXML</path>\n" +
                                                         "<max-age>2021</max-age>\n" +
                                                         "<http-only>true</http-only>\n" +
                                                         "<secure>true</secure>\n";

    public static final String COOKIE_ATTRIBUTE_SUFFIX = "</cookie-config>\n" +
                                                         "</session-config>\n";

    public static String cookieAttributes(String text) {
        return COOKIE_ATTRIBUTE_PREFIX + text + COOKIE_ATTRIBUTE_SUFFIX;
    }

    public WebApp parseCookieAttributes60(String cookieText) throws Exception {
        return parseWebApp(webApp(WebApp.VERSION_6_0, cookieAttributes(cookieText)),
                           WebApp.VERSION_6_0);
    }

    // Verify the parse of a cookie configuration which has no attributes.

    @Test
    public void testEE10CookieAttributesNone() throws Exception {
        WebApp webApp = parseCookieAttributes60("");

        CookieConfig cookieConfig = webApp.getSessionConfig().getCookieConfig();
        Assert.assertNotNull(cookieConfig);
        Assert.assertEquals(cookieConfig.getName(), "CookieConfigName_viaWebXML");

        List<AttributeValue> attributes = cookieConfig.getAttributes();
        Assert.assertEquals(attributes.size(), 0);
    }

    @Test
    public void testEE10CookieAttributesNoDescriptions() throws Exception {
        WebApp webApp = parseCookieAttributes60("<attribute>\n" +
                                                "<attribute-name>color</attribute-name>\n" +
                                                "<attribute-value>blue</attribute-value>\n" +
                                                "</attribute>\n");

        CookieConfig cookieConfig = webApp.getSessionConfig().getCookieConfig();
        Assert.assertNotNull(cookieConfig);

        List<AttributeValue> attributes = cookieConfig.getAttributes();
        Assert.assertEquals(attributes.size(), 1);

        AttributeValue attribute = attributes.get(0);

        Assert.assertEquals(attribute.getAttributeName(), "color");
        Assert.assertEquals(attribute.getAttributeValue(), "blue");
    }

    @Test
    public void testEE10CookieAttributesOneDescription() throws Exception {
        WebApp webApp = parseCookieAttributes60("<attribute>\n" +
                                                "<description xml:lang=\"en\">Hair color</description>\n" +
                                                "<attribute-name>color</attribute-name>\n" +
                                                "<attribute-value>blue</attribute-value>\n" +
                                                "</attribute>\n");

        CookieConfig cookieConfig = webApp.getSessionConfig().getCookieConfig();
        Assert.assertNotNull(cookieConfig);

        List<AttributeValue> attributes = cookieConfig.getAttributes();
        Assert.assertEquals(attributes.size(), 1);

        AttributeValue attribute = attributes.get(0);

        List<Description> descriptions = attribute.getDescriptions();
        Assert.assertEquals(descriptions.size(), 1);

        Description description = descriptions.get(0);
        Assert.assertEquals(description.getLang(), "en");
        Assert.assertEquals(description.getValue(), "Hair color");

        Assert.assertEquals(attribute.getAttributeName(), "color");
        Assert.assertEquals(attribute.getAttributeValue(), "blue");
    }

    @Test
    public void testEE10CookieAttributesTwoDescriptions() throws Exception {
        WebApp webApp = parseCookieAttributes60("<attribute>\n" +
                                                "<description xml:lang=\"en\">Hair color</description>\n" +
                                                "<description xml:lang=\"fr\">Couleur de cheveux</description>\n" +
                                                "<attribute-name>color</attribute-name>\n" +
                                                "<attribute-value>blue</attribute-value>\n" +
                                                "</attribute>\n");

        CookieConfig cookieConfig = webApp.getSessionConfig().getCookieConfig();
        Assert.assertNotNull(cookieConfig);

        List<AttributeValue> attributes = cookieConfig.getAttributes();
        Assert.assertEquals(attributes.size(), 1);

        AttributeValue attribute = attributes.get(0);

        List<Description> descriptions = attribute.getDescriptions();
        Assert.assertEquals(descriptions.size(), 2);

        Description description = descriptions.get(0);
        Assert.assertEquals(description.getLang(), "en");
        Assert.assertEquals(description.getValue(), "Hair color");

        description = descriptions.get(1);
        Assert.assertEquals(description.getLang(), "fr");
        Assert.assertEquals(description.getValue(), "Couleur de cheveux");

        Assert.assertEquals(attribute.getAttributeName(), "color");
        Assert.assertEquals(attribute.getAttributeValue(), "blue");
    }

    @Test
    public void testEE10CookieAttributesMultiple() throws Exception {
        WebApp webApp = parseCookieAttributes60("<attribute>\n" +
                                                "<description xml:lang=\"en\">Hair color</description>\n" +
                                                "<description xml:lang=\"fr\">Couleur de cheveux</description>\n" +
                                                "<attribute-name>color</attribute-name>\n" +
                                                "<attribute-value>blue</attribute-value>\n" +
                                                "</attribute>\n" +

                                                "<attribute>\n" +
                                                "<description xml:lang=\"en\">Facial complecxion</description>\n" +
                                                "<description xml:lang=\"fr\">Complexión facial</description>\n" +
                                                "<attribute-name>complexion</attribute-name>\n" +
                                                "<attribute-value>rough</attribute-value>\n" +
                                                "</attribute>\n");

        CookieConfig cookieConfig = webApp.getSessionConfig().getCookieConfig();
        Assert.assertNotNull(cookieConfig);

        List<AttributeValue> attributes = cookieConfig.getAttributes();
        Assert.assertEquals(attributes.size(), 2);

        AttributeValue attribute = attributes.get(0);

        List<Description> descriptions = attribute.getDescriptions();
        Assert.assertEquals(descriptions.size(), 2);

        Description description = descriptions.get(0);
        Assert.assertEquals(description.getLang(), "en");
        Assert.assertEquals(description.getValue(), "Hair color");

        description = descriptions.get(1);
        Assert.assertEquals(description.getLang(), "fr");
        Assert.assertEquals(description.getValue(), "Couleur de cheveux");

        Assert.assertEquals(attribute.getAttributeName(), "color");
        Assert.assertEquals(attribute.getAttributeValue(), "blue");

        attribute = attributes.get(1);

        descriptions = attribute.getDescriptions();
        Assert.assertEquals(descriptions.size(), 2);

        description = descriptions.get(0);
        Assert.assertEquals(description.getLang(), "en");
        Assert.assertEquals(description.getValue(), "Facial complecxion");

        description = descriptions.get(1);
        Assert.assertEquals(description.getLang(), "fr");
        Assert.assertEquals(description.getValue(), "Complexión facial");

        Assert.assertEquals(attribute.getAttributeName(), "complexion");
        Assert.assertEquals(attribute.getAttributeValue(), "rough");
    }

    // Only the last of attributes which have a duplicate name is used.
    // Each duplication produces a warning message.

    public static final String DUPLICATE_ATTRIBUTE_NAME_ALT_MESSAGE = "duplicate.session.config.attribute.name";
    public static final String[] DUPLICATE_ATTRIBUTE_NAME_MESSAGES = { "CWWCK27790W" };

    // The tests don't capture warnings.  The expected messages, above, are shown for reference.

    @Test
    public void testEE10CookieAttributesDuplicateName() throws Exception {
        WebApp webApp = parseCookieAttributes60("<attribute>\n" +
                                                "<description>Hair color</description>\n" +
                                                "<attribute-name>color</attribute-name>\n" +
                                                "<attribute-value>blue</attribute-value>\n" +
                                                "</attribute>\n" +

                                                "<attribute>\n" +
                                                "<description>Hair color</description>\n" +
                                                "<attribute-name>color</attribute-name>\n" +
                                                "<attribute-value>red</attribute-value>\n" +
                                                "</attribute>\n");

        CookieConfig cookieConfig = webApp.getSessionConfig().getCookieConfig();
        Assert.assertNotNull(cookieConfig);

        List<AttributeValue> attributes = cookieConfig.getAttributes();
        Assert.assertEquals(attributes.size(), 1);

        AttributeValue attribute = attributes.get(0);

        List<Description> descriptions = attribute.getDescriptions();
        Assert.assertEquals(descriptions.size(), 1);

        Description description = descriptions.get(0);
        Assert.assertEquals(description.getValue(), "Hair color");

        Assert.assertEquals(attribute.getAttributeName(), "color");
        Assert.assertEquals(attribute.getAttributeValue(), "red");
    }

    // Attributes which are missing the 'name' attribute are ignored by the
    // parser.  Each attribute which is missing 'name' produces a warning message.

    public static final String MISSING_ATTRIBUTE_NAME_ALT_MESSAGE = "missing.session.config.attribute.name";
    public static final String[] MISSING_ATTRIBUTE_NAME_MESSAGES = { "CWWCK27790W" };

    // The tests don't capture warnings.  The expected messages, above, are shown for reference.

    @Test
    public void testEE10CookieAttributesMissingName() throws Exception {
        WebApp webApp = parseCookieAttributes60("<attribute>\n" +
                                                "<description>Hair color</description>\n" +
                                                "<attribute-name>color</attribute-name>\n" +
                                                "<attribute-value>blue</attribute-value>\n" +
                                                "</attribute>\n" +

                                                "<attribute>\n" +
                                                "<description>Hair color</description>\n" +
                                                // "<attribute-name>color</attribute-name>\n" +
                                                "<attribute-value>red</attribute-value>\n" +
                                                "</attribute>\n");

        CookieConfig cookieConfig = webApp.getSessionConfig().getCookieConfig();
        Assert.assertNotNull(cookieConfig);

        List<AttributeValue> attributes = cookieConfig.getAttributes();
        Assert.assertEquals(attributes.size(), 1);

        AttributeValue attribute = attributes.get(0);

        List<Description> descriptions = attribute.getDescriptions();
        Assert.assertEquals(descriptions.size(), 1);

        Description description = descriptions.get(0);
        Assert.assertEquals(description.getValue(), "Hair color");

        Assert.assertEquals(attribute.getAttributeName(), "color");
        Assert.assertEquals(attribute.getAttributeValue(), "blue");
    }
}
