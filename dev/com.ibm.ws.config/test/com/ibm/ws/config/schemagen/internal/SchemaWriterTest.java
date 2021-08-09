/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.schemagen.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.metatype.AttributeDefinition;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.ibm.websphere.metatype.MetaTypeFactory;
import com.ibm.ws.config.xml.internal.MockAttributeDefinition;
import com.ibm.ws.config.xml.internal.MockBundle;
import com.ibm.ws.config.xml.internal.MockMetaTypeInformation;
import com.ibm.ws.config.xml.internal.MockObjectClassDefinition;
import com.ibm.ws.config.xml.internal.XMLConfigConstants;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;

/**
 *
 */
public class SchemaWriterTest {

    static SharedOutputManager outputMgr;

    static XMLOutputFactory xmlOutputFactory;
    static DocumentBuilderFactory domFactory;
    static XPathFactory xPathFactory;
    static XSDNamespaceContext xsdNamespaceContext;
    static SchemaFactory schemaFactory;
    static Schema xsdSchema;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();

        xmlOutputFactory = XMLOutputFactory.newInstance();

        schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        xsdSchema = schemaFactory.newSchema(SchemaWriterTest.class.getResource("/com/ibm/ws/config/schemagen/internal/XMLSchema.xsd"));

        domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);

        xPathFactory = XPathFactory.newInstance();

        xsdNamespaceContext = new XSDNamespaceContext();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();

        // Restore back to old kernel and let next test case set to new kernel
        // as needed
        SharedLocationManager.resetWsLocationAdmin();
    }

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation, this keeps
        // things sane
        outputMgr.resetStreams();
    }

    private MockObjectClassDefinition createAttributeMap(String ocdID, int cardinality, boolean required) {
        MockObjectClassDefinition objectClass = new MockObjectClassDefinition(ocdID);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testBoolean", AttributeDefinition.BOOLEAN, cardinality), required);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testInteger", AttributeDefinition.INTEGER, cardinality), required);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testLong", AttributeDefinition.LONG, cardinality), required);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testShort", AttributeDefinition.SHORT, cardinality), required);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testString", AttributeDefinition.STRING, cardinality), required);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testCharacter", AttributeDefinition.CHARACTER, cardinality), required);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testByte", AttributeDefinition.BYTE, cardinality), required);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testDouble", AttributeDefinition.DOUBLE, cardinality), required);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testFloat", AttributeDefinition.FLOAT, cardinality), required);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testDuration", MetaTypeFactory.DURATION_TYPE, cardinality), required);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testDurationH", MetaTypeFactory.DURATION_H_TYPE, cardinality), required);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testDurationM", MetaTypeFactory.DURATION_M_TYPE, cardinality), required);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testDurationS", MetaTypeFactory.DURATION_S_TYPE, cardinality), required);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("testToken", MetaTypeFactory.TOKEN_TYPE, cardinality), required);
        return objectClass;
    }

    @Test
    public void testAttributeType() throws Exception {
        String optionalId = "attribute.type.optional";
        String requiredId = "attribute.type.required";
        MockObjectClassDefinition ocdOptional = createAttributeMap(optionalId, 0, false);
        ocdOptional.setAlias("ocdopt");
        MockObjectClassDefinition ocdRequired = createAttributeMap(requiredId, 0, true);
        ocdRequired.setAlias("ocdreq");
        MockMetaTypeInformation metatype = new MockMetaTypeInformation();
        String pidOptional = "pid.optional";
        String pidRequired = "pid.required";
        metatype.add(pidOptional, false, ocdOptional);
        metatype.add(pidRequired, false, ocdRequired);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        checkAttributes(xpath, document, pidOptional, false);
        checkAttributes(xpath, document, pidRequired, true);

        checkAttribute(getElement(xpath, document, "serverType", "xsd:choice", pidOptional), "type", pidOptional);
        checkAttribute(getElement(xpath, document, "serverType", "xsd:choice", pidRequired), "type", pidRequired);
    }

    /**
     * Test for defect WS 86934 -- confirm that whitespace is accepted in empty elements, for the sake of
     * pretty-printing, by validating a test document against the written schema.
     * Adapted from testAttributeType, for no especially good reason except that this schema pattern occurs there
     * (among other places).
     *
     * Throws exception if
     */
    @Test
    public void testWhitespaceInEmptyElement() throws Exception {
        String optionalId = "attribute.type.optional";
        String requiredId = "attribute.type.required";
        MockObjectClassDefinition ocdOptional = createAttributeMap(optionalId, 0, false);
        ocdOptional.setAlias("pidopt");
        MockObjectClassDefinition ocdRequired = createAttributeMap(requiredId, 0, true);
        ocdRequired.setAlias("pidreq");
        MockMetaTypeInformation metatype = new MockMetaTypeInformation();
        String pidOptional = "pid.optional";
        String pidRequired = "pid.required";
        metatype.add(pidOptional, false, ocdOptional);
        metatype.add(pidRequired, false, ocdRequired);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Schema thisSchema = schemaFactory.newSchema(new StreamSource(new ByteArrayInputStream(out.toByteArray())));
        String testDoc = "<server>\n" +
                         "  <!-- Comments and whitespace should now be allowed -->\n" +
                         "  <pid.required testDurationH='1h' testString='string' testDouble='17.1415926535897932384626' testDuration='2h3m' " +
                         "testDurationM='4m' testShort='5' testFloat='3.1415926535897932384626' " +
                         "testInteger='17' testDurationS='17s' testLong='1234567890' testBoolean='true' " +
                         "testCharacter='c' testToken='abc ' testByte='127' >\n" +
                         "  </pid.required>\n" +
                         "  <pid.optional>\n" +
                         "    <!-- Comments and whitespace should now be allowed -->\n" +
                         "  </pid.optional>\n" +
                         "</server>\n";
        try {
            thisSchema.newValidator().validate(new StreamSource(new StringReader(testDoc)));
        } catch (Exception e) {
            fail("testWhitespaceInEmptyElement: " + e);
        }
    }

    private void checkAttributes(XPath xpath, Document document, String typeName, boolean required) throws XPathExpressionException {
        checkAttribute(xpath, document, typeName, "testBoolean", "booleanType", required);
        checkAttribute(xpath, document, typeName, "testInteger", "intType", required);
        checkAttribute(xpath, document, typeName, "testLong", "longType", required);
        checkAttribute(xpath, document, typeName, "testShort", "shortType", required);
        checkAttribute(xpath, document, typeName, "testString", "xsd:string", required);
        checkAttribute(xpath, document, typeName, "testByte", "byteType", required);
        checkAttribute(xpath, document, typeName, "testFloat", "floatType", required);
        checkAttribute(xpath, document, typeName, "testDouble", "doubleType", required);
        checkAttribute(xpath, document, typeName, "testCharacter", "charType", required);
        checkAttribute(xpath, document, typeName, "testToken", "tokenType", required);
    }

    private void checkAttribute(XPath xpath, Document document, String typeName, String name, String attributeType, boolean required) throws XPathExpressionException {
        Node node = getAttribute(xpath, document, typeName, name);
        assertNotNull(name, node);
        Node typeAttribute = node.getAttributes().getNamedItem("type");
        assertNotNull("type attribute", typeAttribute);
        assertEquals(attributeType, typeAttribute.getNodeValue());
        Node useAttribute = node.getAttributes().getNamedItem("use");
        assertNotNull("use attribute", useAttribute);
        if (required) {
            assertEquals("required", useAttribute.getNodeValue());
        } else {
            assertEquals("optional", useAttribute.getNodeValue());
        }
    }

    @Test
    public void testElementType() throws Exception {
        String optionalId = "element.type.optional";
        String requiredId = "element.type.required";
        MockObjectClassDefinition ocdOptional = createAttributeMap(optionalId, 10, false);
        ocdOptional.setAlias("ocdOpt");
        MockObjectClassDefinition ocdRequired = createAttributeMap(requiredId, 10, true);
        ocdRequired.setAlias("ocdReq");
        MockMetaTypeInformation metatype = new MockMetaTypeInformation();
        String pidOptional = "pid.optional";
        String pidRequired = "pid.required";
        metatype.add(pidOptional, false, ocdOptional);
        metatype.add(pidRequired, false, ocdRequired);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        checkElements(xpath, document, pidOptional, false);
        checkElements(xpath, document, pidRequired, true);

        checkAttribute(getElement(xpath, document, "serverType", "xsd:choice", pidOptional), "type", pidOptional);
        checkAttribute(getElement(xpath, document, "serverType", "xsd:choice", pidRequired), "type", pidRequired);
    }

    private void checkElements(XPath xpath, Document document, String typeName, boolean required) throws XPathExpressionException {
        checkElement(xpath, document, typeName, "testBoolean", "booleanType", required);
        checkElement(xpath, document, typeName, "testInteger", "intType", required);
        checkElement(xpath, document, typeName, "testLong", "longType", required);
        checkElement(xpath, document, typeName, "testShort", "shortType", required);
        checkElement(xpath, document, typeName, "testString", "xsd:string", required);
        checkElement(xpath, document, typeName, "testByte", "byteType", required);
        checkElement(xpath, document, typeName, "testFloat", "floatType", required);
        checkElement(xpath, document, typeName, "testDouble", "doubleType", required);
        checkElement(xpath, document, typeName, "testCharacter", "charType", required);
        checkElement(xpath, document, typeName, "testDuration", "duration", required);
        checkElement(xpath, document, typeName, "testDurationH", "hourDuration", required);
        checkElement(xpath, document, typeName, "testDurationM", "minuteDuration", required);
        checkElement(xpath, document, typeName, "testDurationS", "secondDuration", required);
        checkElement(xpath, document, typeName, "testToken", "tokenType", required);
    }

    private void checkElement(XPath xpath, Document document, String typeName, String name, String elementType, boolean required) throws XPathExpressionException {
        Node node = getElement(xpath, document, typeName, name);
        assertNotNull(name, node);
        Node typeAttribute = node.getAttributes().getNamedItem("type");
        assertNotNull("type attribute", typeAttribute);
        assertEquals(elementType, typeAttribute.getNodeValue());
    }

    @Test
    public void testFilter() throws Exception {

        MockObjectClassDefinition parent = new MockObjectClassDefinition("parent");
        parent.setAlias("parent");
        MockObjectClassDefinition filterMatchOne = new MockObjectClassDefinition("filterOne");
        filterMatchOne.setAlias("filterOne");
        MockObjectClassDefinition filterMatchTwo = new MockObjectClassDefinition("filterTwo");
        filterMatchTwo.setAlias("filterTwo");
        MockObjectClassDefinition filterNonMatch = new MockObjectClassDefinition("nonMatch");
        filterNonMatch.setAlias("filterNonMatch");

        MockAttributeDefinition filter = new MockAttributeDefinition("filterRef", AttributeDefinition.STRING);
        filter.setService("service1");
        parent.addAttributeDefinition(filter);

        filterMatchOne.setObjectClass("service1");

        filterMatchTwo.setObjectClass("service1");

        // Only attr1 causes a failure to match the filter
        filterNonMatch.setObjectClass("service2");

        MockMetaTypeInformation metatype = new MockMetaTypeInformation();
        metatype.add("test.parent", true, parent);
        metatype.add("test.filterOne", true, filterMatchOne);
        metatype.add("test.filterTwo", true, filterMatchTwo);
        metatype.add("test.nonMatch", true, filterNonMatch);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        assertNotNull(getElement(xpath, document, "serverType", "xsd:choice", "test.parent"));
        assertNotNull(getElement(xpath, document, "serverType", "xsd:choice", "test.filterOne"));
        assertNotNull(getElement(xpath, document, "serverType", "xsd:choice", "test.filterTwo"));
        assertNotNull(getElement(xpath, document, "serverType", "xsd:choice", "test.nonMatch"));

        Node refElement = getAttribute(xpath, document, "test.parent", "filterRef");
        checkAttribute(refElement, "type", "pidType");

        Node filterOne = getDocumentationReference(xpath, refElement);
        checkElement(filterOne, "filterOne");

        Node filterTwo = filterOne.getNextSibling();
        checkElement(filterTwo, "filterTwo");

        assertNull(filterTwo.getNextSibling());

    }

    @Test
    public void testDefaultValue() throws Exception {
        String id = "defaultValue";
        String pid = "pid.defaultValue";
        MockObjectClassDefinition objectClass = new MockObjectClassDefinition(id);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("state", AttributeDefinition.STRING, 0, new String[] { "IL" }));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("states", AttributeDefinition.STRING, 10, new String[] { "NC" }));

        MockMetaTypeInformation metatype = new MockMetaTypeInformation();
        metatype.add(pid, false, objectClass);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        checkAttribute(getAttribute(xpath, document, pid, "state"), "default", "IL");
        checkAttribute(getElement(xpath, document, pid, "states"), "default", "NC");
    }

    @Test
    public void testOptionValues() throws Exception {
        String id = "optionValues";
        String pid = "pid." + id;
        MockObjectClassDefinition objectClass = new MockObjectClassDefinition(id);
        MockAttributeDefinition stateAD = new MockAttributeDefinition("state", AttributeDefinition.STRING, 0);
        stateAD.setOptions(new String[] { "IL", "NC" }, new String[] { "Illinois", "North Carolina" });
        objectClass.addAttributeDefinition(stateAD);
        MockAttributeDefinition statesAD = new MockAttributeDefinition("states", AttributeDefinition.STRING, 5);
        statesAD.setOptions(new String[] { "NY", "NC", "SC" }, new String[] { "New York", "North Carolina", "South Carolina" });
        objectClass.addAttributeDefinition(statesAD);

        MockMetaTypeInformation metatype = new MockMetaTypeInformation();
        metatype.add(pid, false, objectClass);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        checkEnumeration(new String[] { "IL", "NC" }, xpath, getAttribute(xpath, document, pid, "state"));
        checkEnumeration(new String[] { "NY", "NC", "SC" }, xpath, getElement(xpath, document, pid, "states"));
    }

    private void checkEnumeration(String[] values, XPath xpath, Node node) throws XPathExpressionException {
        assertNotNull(node);
        for (String value : values) {
            Node enumerationElement = getEnumeration(xpath, node, value);
            assertNotNull("enumeration element for " + value, enumerationElement);
        }
    }

    /*
     * OCD with an alias shared between two pids.
     */
    @Test
    public void testAliasSharedOCD() throws Exception {
        String id = "alias";
        String aliasName = "myAlias";
        MockObjectClassDefinition objectClass = new MockObjectClassDefinition(id);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("minThreads", AttributeDefinition.INTEGER, 0));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("maxThreads", AttributeDefinition.INTEGER, 0));
        objectClass.setAlias(aliasName);

        MockMetaTypeInformation metatype = new MockMetaTypeInformation();
        String singletonPid = "pid.alias.singleton";
        metatype.add(singletonPid, false, objectClass);
        String factoryPid = "pid.alias.factory";
        metatype.add(factoryPid, true, objectClass);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        checkAttribute(getElement(xpath, document, "serverType", "xsd:choice", singletonPid), "type", singletonPid);
        checkAttribute(getElement(xpath, document, "serverType", "xsd:choice", factoryPid), "type", factoryPid);
        checkAttribute(getElement(xpath, document, "serverType", "xsd:choice", aliasName), "type", "xsd:anyType");
    }

    /*
     * Two OCDs with the same alias.
     */
    @Test
    public void testAliasDifferentOCD() throws Exception {
        String aliasName = "myAlias";

        String threadsId = "threads";
        MockObjectClassDefinition objectClass1 = new MockObjectClassDefinition(threadsId);
        objectClass1.addAttributeDefinition(new MockAttributeDefinition("minThreads", AttributeDefinition.INTEGER, 0));
        objectClass1.addAttributeDefinition(new MockAttributeDefinition("maxThreads", AttributeDefinition.INTEGER, 0));
        objectClass1.setAlias(aliasName);

        String statesId = "states";
        MockObjectClassDefinition objectClass2 = new MockObjectClassDefinition(statesId);
        objectClass2.addAttributeDefinition(new MockAttributeDefinition("state", AttributeDefinition.STRING, 0, new String[] { "NC" }));
        objectClass2.setAlias(aliasName);

        MockMetaTypeInformation metatype = new MockMetaTypeInformation();
        String threadsPid = "pid.alias.threads";
        metatype.add(threadsPid, false, objectClass1);
        String statesPid = "pid.alias.states";
        metatype.add(statesPid, false, objectClass2);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        checkAttribute(getElement(xpath, document, "serverType", "xsd:choice", threadsPid), "type", threadsPid);
        checkAttribute(getElement(xpath, document, "serverType", "xsd:choice", statesPid), "type", statesPid);
        // since the alias is shared between two different OCDs we expect an element with xsd:anyType
        checkAttribute(getElement(xpath, document, "serverType", "xsd:choice", aliasName), "type", "xsd:anyType");
    }

    /*
     * Two OCDs with the same alias.
     */
    @Test
    public void testAliasDifferentOCDSameId() throws Exception {
        String aliasName = "myAlias";
        String testId = "test";

        MockObjectClassDefinition objectClass1 = new MockObjectClassDefinition(testId);
        objectClass1.addAttributeDefinition(new MockAttributeDefinition("minThreads", AttributeDefinition.INTEGER, 0));
        objectClass1.addAttributeDefinition(new MockAttributeDefinition("maxThreads", AttributeDefinition.INTEGER, 0));
        objectClass1.setAlias(aliasName);

        MockObjectClassDefinition objectClass2 = new MockObjectClassDefinition(testId);
        objectClass2.addAttributeDefinition(new MockAttributeDefinition("state", AttributeDefinition.STRING, 0, new String[] { "NC" }));
        objectClass2.setAlias(aliasName);

        MockMetaTypeInformation metatype1 = new MockMetaTypeInformation();
        String testPid1 = "pid.alias.test.1";
        metatype1.add(testPid1, false, objectClass1);

        MockMetaTypeInformation metatype2 = new MockMetaTypeInformation();
        String testPid2 = "pid.alias.test.2";
        metatype2.add(testPid2, false, objectClass2);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out);
        schemaWriter.add(metatype1);
        schemaWriter.add(metatype2);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        checkAttribute(getElement(xpath, document, "serverType", "xsd:choice", testPid1), "type", testPid1);
        // since the OCD id is the same the type name generated should be unique
        checkAttribute(getElement(xpath, document, "serverType", "xsd:choice", testPid2), "type", testPid2);
        // since the alias is shared between two different OCDs we expect an element with xsd:anyType
        checkAttribute(getElement(xpath, document, "serverType", "xsd:choice", aliasName), "type", "xsd:anyType");
    }

    @Test
    public void testDocumentation() throws Exception {
        String id = "documentation";
        String pid = "pid." + id;
        String ocdDescription = "test documentation";
        MockObjectClassDefinition objectClass = new MockObjectClassDefinition(id);
        objectClass.setDescription(ocdDescription);
        String ocdName = "test.OCD.Name";
        objectClass.setName(ocdName);

        MockAttributeDefinition stateAD = new MockAttributeDefinition("state", AttributeDefinition.STRING, 0);
        stateAD.setOptions(new String[] { "IL", "NC" }, new String[] { "Illinois", "North Carolina" });
        String stateDescription = "Current state";
        stateAD.setDescription(stateDescription);
        String adName = "test.AD.Name";
        stateAD.setName(adName);
        objectClass.addAttributeDefinition(stateAD);

        MockAttributeDefinition statesAD = new MockAttributeDefinition("states", AttributeDefinition.STRING, 5);
        statesAD.setOptions(new String[] { "NY", "NC", "SC" }, new String[] { "New York", "North Carolina", "South Carolina" });
        String statesDescription = "List of states";
        statesAD.setDescription(statesDescription);
        objectClass.addAttributeDefinition(statesAD);

        MockAttributeDefinition maxThreadsAD = new MockAttributeDefinition("maxThreads", AttributeDefinition.INTEGER, 0);
        String maxThreadsDescription = "Maximum number of threads";
        maxThreadsAD.setDescription(maxThreadsDescription);
        objectClass.addAttributeDefinition(maxThreadsAD);

        MockMetaTypeInformation metatype = new MockMetaTypeInformation();
        metatype.add(pid, false, objectClass);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        Node node = getComplexType(xpath, document, pid);
        assertNotNull(node);

        checkElement(getDocumentation(xpath, node), ocdDescription);
        checkElement(getDocumentationLabel(xpath, node), ocdName);

        checkElement(getDocumentation(xpath, getAttribute(xpath, node, pid, "maxThreads")), maxThreadsDescription);

        Node stateNode = getAttribute(xpath, node, pid, "state");
        checkElement(getDocumentation(xpath, stateNode), stateDescription);
        checkElement(getDocumentationLabel(xpath, stateNode), adName);
        checkElement(getDocumentation(xpath, getEnumeration(xpath, stateNode, "IL")), "Illinois");
        checkElement(getDocumentation(xpath, getEnumeration(xpath, stateNode, "NC")), "North Carolina");

        Node statesNode = getElement(xpath, node, pid, "states");
        checkElement(getDocumentation(xpath, statesNode), statesDescription);
        checkElement(getDocumentation(xpath, getEnumeration(xpath, statesNode, "NY")), "New York");
        checkElement(getDocumentation(xpath, getEnumeration(xpath, statesNode, "NC")), "North Carolina");
        checkElement(getDocumentation(xpath, getEnumeration(xpath, statesNode, "SC")), "South Carolina");
    }

    @Test
    public void testAttributeWildcards() throws Exception {
        String id = "ocd";
        String pid = "pid.documentation";

        String ocdDescription = "test wildcards";
        MockObjectClassDefinition objectClass = new MockObjectClassDefinition(id);
        objectClass.setDescription(ocdDescription);
        String ocdName = "test.OCD.Name";
        objectClass.setName(ocdName);

        MockMetaTypeInformation metatype = new MockMetaTypeInformation();
        metatype.add("pid.documentation", false, objectClass);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out);
        schemaWriter.add(metatype);
        schemaWriter.setGenerateWildcards(true);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        checkSkipWildcard(document, pid);
        checkSkipWildcard(document, "serverType");
    }

    @Test
    public void testPassword() throws Exception {
        String pid = "pid.usernamePassword";

        MockObjectClassDefinition objectClass = new MockObjectClassDefinition("usernamePassword");

        MockAttributeDefinition keyStorePasswordAD = new MockAttributeDefinition("keyStorePassword", AttributeDefinition.PASSWORD);
        objectClass.addAttributeDefinition(keyStorePasswordAD);

        MockAttributeDefinition trustStorePasswordAD = new MockAttributeDefinition("trustStorePassword", AttributeDefinition.STRING);
        trustStorePasswordAD.setExtensionType("password");
        objectClass.addAttributeDefinition(trustStorePasswordAD);

        MockAttributeDefinition usernameAD = new MockAttributeDefinition("username", AttributeDefinition.STRING);
        objectClass.addAttributeDefinition(usernameAD);

        MockMetaTypeInformation metatype = new MockMetaTypeInformation();
        metatype.add(pid, false, objectClass);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        Node node = getComplexType(xpath, document, pid);
        assertNotNull("should have found an xsd:complexType for pid " + pid, node);

        checkAttribute(xpath, document, pid, "username", "xsd:string", true);
        checkAttribute(xpath, document, pid, "keyStorePassword", "password", true);
        checkAttribute(xpath, document, pid, "trustStorePassword", "password", true);

        Node passwordTypeNode = getSimpleType(xpath, document, "password");
        assertNotNull("password type is missing", passwordTypeNode);
    }

    private MockMetaTypeInformation createNestedMockMetaTypeInformation() {
        MockObjectClassDefinition oneObjectClass = new MockObjectClassDefinition("one");
        oneObjectClass.addAttributeDefinition(new MockAttributeDefinition("oneValue", AttributeDefinition.STRING, 0, new String[] { "one" }));
        oneObjectClass.setAlias("oneAlias");
        oneObjectClass.setSupportsExtensions(true);

        MockObjectClassDefinition twoObjectClass = new MockObjectClassDefinition("two");
        twoObjectClass.addAttributeDefinition(new MockAttributeDefinition("twoValue", AttributeDefinition.STRING, 0, new String[] { "two" }));
        twoObjectClass.setAlias("twoAlias");
        twoObjectClass.setSupportsExtensions(false);

        MockObjectClassDefinition threeObjectClass = new MockObjectClassDefinition("three");
        threeObjectClass.addAttributeDefinition(new MockAttributeDefinition("threeValue", AttributeDefinition.STRING, 0, new String[] { "three" }));
        threeObjectClass.setAlias("threeAlias");
        threeObjectClass.setSupportsExtensions(true);

        MockMetaTypeInformation metatype = new MockMetaTypeInformation();
        metatype.add("one.pid", false, oneObjectClass);
        metatype.add("two.pid", false, twoObjectClass);
        metatype.add("three.pid", false, threeObjectClass);

        return metatype;
    }

    @Test
    public void testNestingAllParents() throws Exception {
        MockMetaTypeInformation metatype = createNestedMockMetaTypeInformation();

        String childAlias = "childAlias";
        String pid = "child.pid";

        MockObjectClassDefinition childObjectClass = new MockObjectClassDefinition("child");
        childObjectClass.addAttributeDefinition(new MockAttributeDefinition("state", AttributeDefinition.STRING, 0, new String[] { "NC" }));
        childObjectClass.setAlias(childAlias);
        childObjectClass.setParentPid("");

        metatype.add(pid, true, childObjectClass);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        Node node;

        node = getComplexType(xpath, document, pid);
        assertNotNull(node);
        checkFactoryType(xpath, document, pid);
        //checkNestedType(xpath, document, childObjectClass.getID());

        node = getElement(xpath, document, "one.pid", childAlias);
        assertNotNull(node);
        checkAttribute(xpath, document, "one.pid", "oneValue", "xsd:string", false);
        checkElement(xpath, document, "one.pid", childAlias, pid, false);

        node = getElement(xpath, document, "two.pid", childAlias);
        assertNotNull(node);
        checkAttribute(xpath, document, "two.pid", "twoValue", "xsd:string", false);
        checkElement(xpath, document, "two.pid", childAlias, pid, false);

        node = getElement(xpath, document, "three.pid", childAlias);
        assertNotNull(node);
        checkAttribute(xpath, document, "three.pid", "threeValue", "xsd:string", false);
        checkElement(xpath, document, "three.pid", childAlias, pid, false);
    }

    @Test
    public void testNestingOneParent() throws Exception {
        MockMetaTypeInformation metatype = createNestedMockMetaTypeInformation();

        String childAlias = "childAlias";
        String pid = "child.pid";

        MockObjectClassDefinition childObjectClass = new MockObjectClassDefinition("child");
        childObjectClass.addAttributeDefinition(new MockAttributeDefinition("state", AttributeDefinition.STRING, 0, new String[] { "NC" }));
        childObjectClass.setAlias(childAlias);
        childObjectClass.setParentPid("one.pid");

        metatype.add(pid, true, childObjectClass);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        Node node;

        node = getComplexType(xpath, document, pid);
        assertNotNull(node);
        checkFactoryType(xpath, document, pid);
        //checkNestedType(xpath, document, childObjectClass.getID());

        node = getElement(xpath, document, "one.pid", childAlias);
        assertNotNull(node);
        checkAttribute(xpath, document, "one.pid", "oneValue", "xsd:string", false);
        checkElement(xpath, document, "one.pid", childAlias, pid, false);

        node = getElement(xpath, document, "two.pid", childAlias);
        assertNull(node);
        checkAttribute(xpath, document, "two.pid", "twoValue", "xsd:string", false);
        assertNull(getElement(xpath, document, "two.pid", childAlias));

        node = getElement(xpath, document, "three.pid", childAlias);
        assertNull(node);
        checkAttribute(xpath, document, "three.pid", "threeValue", "xsd:string", false);
        assertNull(getElement(xpath, document, "three.pid", childAlias));
    }

    @Test
    public void testChildAliasNestingOneParent() throws Exception {
        MockMetaTypeInformation metatype = createNestedMockMetaTypeInformation();

        String childAlias = "childAlias";
        String pid = "child.pid";

        MockObjectClassDefinition childObjectClass = new MockObjectClassDefinition("child");
        childObjectClass.addAttributeDefinition(new MockAttributeDefinition("state", AttributeDefinition.STRING, 0, new String[] { "NC" }));
        childObjectClass.setChildAlias(childAlias);
        childObjectClass.setParentPid("one.pid");

        metatype.add(pid, true, childObjectClass);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        Node node;

        node = getComplexType(xpath, document, pid);
        assertNotNull(node);
        checkFactoryType(xpath, document, pid);
        //checkNestedType(xpath, document, pid);

        node = getElement(xpath, document, "one.pid", childAlias);
        assertNotNull(node);
        checkAttribute(xpath, document, "one.pid", "oneValue", "xsd:string", false);
        checkElement(xpath, document, "one.pid", childAlias, pid, false);

        node = getElement(xpath, document, "serverType", "xsd:choice", childAlias);
        assertNull("The nested element should not exist at the top level", node);

        node = getElement(xpath, document, "two.pid", childAlias);
        assertNull(node);
        checkAttribute(xpath, document, "two.pid", "twoValue", "xsd:string", false);
        assertNull(getElement(xpath, document, "two.pid", childAlias));

        node = getElement(xpath, document, "three.pid", childAlias);
        assertNull(node);
        checkAttribute(xpath, document, "three.pid", "threeValue", "xsd:string", false);
        assertNull(getElement(xpath, document, "three.pid", childAlias));
    }

    @Test
    public void testNestingMultipleParents() throws Exception {
        MockMetaTypeInformation metatype = createNestedMockMetaTypeInformation();

        String childAlias = "childAlias";
        String pid = "child.pid";

        MockObjectClassDefinition childObjectClass = new MockObjectClassDefinition("child");
        childObjectClass.addAttributeDefinition(new MockAttributeDefinition("state", AttributeDefinition.STRING, 0, new String[] { "NC" }));
        childObjectClass.setAlias(childAlias);
        childObjectClass.setParentPid("one.pid, three.pid");

        metatype.add(pid, true, childObjectClass);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        Node node;

        node = getComplexType(xpath, document, pid);
        assertNotNull(node);
        checkFactoryType(xpath, document, pid);
        //checkNestedType(xpath, document, pid);

        node = getElement(xpath, document, "one.pid", childAlias);
        assertNotNull(node);
        checkAttribute(xpath, document, "one.pid", "oneValue", "xsd:string", false);
        checkElement(xpath, document, "one.pid", childAlias, pid, false);

        node = getElement(xpath, document, "two.pid", childAlias);
        assertNull(node);
        checkAttribute(xpath, document, "two.pid", "twoValue", "xsd:string", false);
        assertNull(getElement(xpath, document, "two.pid", childAlias));

        node = getElement(xpath, document, "three.pid", childAlias);
        assertNotNull(node);
        checkAttribute(xpath, document, "three.pid", "threeValue", "xsd:string", false);
        checkElement(xpath, document, "three.pid", childAlias, pid, false);
    }

    @Test
    public void testShortNames() throws Exception {
        String threadsId = "threads";
        String threadsAlias = "threadsAlias";
        MockObjectClassDefinition objectClass1 = new MockObjectClassDefinition(threadsId);
        objectClass1.addAttributeDefinition(new MockAttributeDefinition("minThreads", AttributeDefinition.INTEGER, 0));
        objectClass1.addAttributeDefinition(new MockAttributeDefinition("maxThreads", AttributeDefinition.INTEGER, 0));
        objectClass1.setAlias(threadsAlias);

        String statesId = "states";
        String statesAlias = "statesAlias";
        MockObjectClassDefinition objectClass2 = new MockObjectClassDefinition(statesId);
        objectClass2.addAttributeDefinition(new MockAttributeDefinition("state", AttributeDefinition.STRING, 0, new String[] { "NC" }));
        objectClass2.setAlias(statesAlias);

        String idleId = "idle";
        String idleAlias = "idleAlias";
        MockObjectClassDefinition objectClass3 = new MockObjectClassDefinition(idleId);
        objectClass3.addAttributeDefinition(new MockAttributeDefinition("idle", AttributeDefinition.INTEGER, 0, new String[] { "1000" }));
        objectClass3.setAlias(idleAlias);

        MockMetaTypeInformation metatype = new MockMetaTypeInformation();
        String threadsPid = "pid.alias.threads";
        metatype.add(threadsPid, false, objectClass1);
        String statesPid = "pid.alias.states";
        metatype.add(statesPid, false, objectClass2);
        String idlePid = "pid.alias.idle";
        metatype.add(idlePid, false, objectClass3);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out, true);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        checkAttribute(getElement(xpath, document, "serverType", "xsd:choice", threadsAlias), "type", threadsPid);
        checkAttribute(getElement(xpath, document, "serverType", "xsd:choice", statesAlias), "type", statesPid);
        checkAttribute(getElement(xpath, document, "serverType", "xsd:choice", idleAlias), "type", idlePid);

        assertNull(getElement(xpath, document, "serverType", "xsd:choice", threadsPid));
        assertNull(getElement(xpath, document, "serverType", "xsd:choice", statesPid));
        assertNull(getElement(xpath, document, "serverType", "xsd:choice", idlePid));
    }

    @Test
    public void testShortNamesSharedOCD() throws Exception {
        String id = "alias";
        String aliasName = "myAlias";
        MockObjectClassDefinition objectClass = new MockObjectClassDefinition(id);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("minThreads", AttributeDefinition.INTEGER, 0));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("maxThreads", AttributeDefinition.INTEGER, 0));
        objectClass.setAlias(aliasName);

        MockMetaTypeInformation metatype = new MockMetaTypeInformation();
        String singletonPid = "pid.alias.singleton";
        metatype.add(singletonPid, false, objectClass);
        String factoryPid = "pid.alias.factory";
        metatype.add(factoryPid, true, objectClass);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out, true);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        assertNull(getElement(xpath, document, "serverType", "xsd:choice", singletonPid));
        assertNull(getElement(xpath, document, "serverType", "xsd:choice", factoryPid));

        checkAttribute(getElement(xpath, document, "serverType", "xsd:choice", aliasName), "type", "xsd:anyType");
    }

    @Test
    public void testReferencePidsNoAlias() throws Exception {
        testReferencePids(null, 0);
        testReferencePids(null, 10);
    }

    @Test
    public void testReferencePidsAlias() throws Exception {
        testReferencePids("hostPort", 0);
        testReferencePids("hostPort", 10);
    }

    private void testReferencePids(String hostPortAlias, int cardinality) throws Exception {

        String portPid = "com.ibm.ws.port";
        MockObjectClassDefinition portOCD = new MockObjectClassDefinition("port");
        portOCD.addAttributeDefinition(new MockAttributeDefinition("range", AttributeDefinition.INTEGER, 0, null));
        portOCD.setAlias("port");

        String hostPortPid = "com.ibm.ws.host.port";
        MockObjectClassDefinition hostPortOCD = new MockObjectClassDefinition("hostPort");
        hostPortOCD.addAttributeDefinition(new MockAttributeDefinition("number", AttributeDefinition.STRING, 0, null));
        if (hostPortAlias != null) {
            hostPortOCD.setAlias(hostPortAlias);
        }

        String hostPid = "com.ibm.ws.host";
        String hostPortName = "host port name";
        MockObjectClassDefinition hostOCD = new MockObjectClassDefinition("host");
        hostOCD.addAttributeDefinition(new MockAttributeDefinition("ip", AttributeDefinition.STRING, 0, null));
        MockAttributeDefinition portRef = new MockAttributeDefinition("port", AttributeDefinition.STRING, cardinality, null);
        portRef.setName(hostPortName);
        portRef.setReferencePid(hostPortPid);
        hostOCD.addAttributeDefinition(portRef);
        hostOCD.setAlias("host");

        MockBundle bundle = new MockBundle();
        MockMetaTypeInformation metatype = new MockMetaTypeInformation(bundle);
        metatype.add(portPid, true, portOCD);
        metatype.add(hostPid, true, hostOCD);
        metatype.add(hostPortPid, true, hostPortOCD);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out);
        schemaWriter.add(metatype);
        schemaWriter.setPreferShortNames(true);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        assertNotNull(getElement(xpath, document, "serverType", "xsd:choice", portOCD.getAlias()));
        assertNotNull(getElement(xpath, document, "serverType", "xsd:choice", hostOCD.getAlias()));

        Node refElement = getElement(xpath, document, hostPid, "port");
        checkAttribute(refElement, "type", hostPortPid + (cardinality == 0 ? "" : "-factory"));
        // check label documentation entry
        checkElement(getDocumentationLabel(xpath, refElement), hostPortName);

        Node refAttribute = getAttribute(xpath, document, hostPid, "portRef");
        if (hostPortOCD.getAlias() == null) {
            // no alias present - no portRef xml attribute
            assertNull(refAttribute);
        } else {
            // alias present - portRef xml attribute should be there
            assertNotNull(getElement(xpath, document, "serverType", "xsd:choice", hostPortOCD.getAlias()));
            if (cardinality == 0 || cardinality == 1 || cardinality == -1) {
                checkAttribute(refAttribute, "type", "pidType");
            } else {
                checkAttribute(refAttribute, "type", "pidListType");
            }
            // check label documentation entry
            checkElement(getDocumentationLabel(xpath, refAttribute), hostPortName);
            // check reference documentation entry
            checkElement(getDocumentationReference(xpath, refAttribute), hostPortOCD.getAlias());
        }
    }

    @Test
    public void testRequires() throws Exception {
        String id = "alias";
        String aliasName = "myAlias";
        MockObjectClassDefinition objectClass = new MockObjectClassDefinition(id);

        objectClass.addAttributeDefinition(new MockAttributeDefinition("enabled", AttributeDefinition.BOOLEAN, 0));

        MockAttributeDefinition attr1 = new MockAttributeDefinition("dirScanning", AttributeDefinition.BOOLEAN, 0);
        attr1.setRequires("enabled", true);
        objectClass.addAttributeDefinition(attr1);

        MockAttributeDefinition attr2 = new MockAttributeDefinition("fileScanning", AttributeDefinition.BOOLEAN, 0);
        attr2.setRequires("dirScanning", false);
        objectClass.addAttributeDefinition(attr2);

        MockMetaTypeInformation metatype = new MockMetaTypeInformation();
        String singletonPid = "pid.alias.singleton";
        metatype.add(singletonPid, false, objectClass);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out, true);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        Node node;

        node = getAttribute(xpath, document, singletonPid, attr1.getID());
        assertNotNull(node);
        checkAttribute(getDocumentationRequires(xpath, node, "enabled"), "value", "true");

        node = getAttribute(xpath, document, singletonPid, attr2.getID());
        assertNotNull(node);
        checkAttribute(getDocumentationRequires(xpath, node, "dirScanning"), "value", "false");
    }

    @Test
    public void testGroup() throws Exception {
        String id = "alias";
        String aliasName = "myAlias";
        MockObjectClassDefinition objectClass = new MockObjectClassDefinition(id);

        String groupA = "groupA";
        String groupB = "groupB";

        MockAttributeDefinition attr1 = new MockAttributeDefinition("enabled", AttributeDefinition.BOOLEAN, 0);
        attr1.setGroup(groupA);
        objectClass.addAttributeDefinition(attr1);

        MockAttributeDefinition attr2 = new MockAttributeDefinition("dirScanning", AttributeDefinition.BOOLEAN, 0);
        attr2.setGroup(groupB);
        objectClass.addAttributeDefinition(attr2);

        MockAttributeDefinition attr3 = new MockAttributeDefinition("fileScanning", AttributeDefinition.BOOLEAN, 30);
        attr3.setGroup(groupB);
        objectClass.addAttributeDefinition(attr3);

        MockAttributeDefinition attr4 = new MockAttributeDefinition("internal", AttributeDefinition.INTEGER, 0);
        objectClass.addAttributeDefinition(attr4);

        MockMetaTypeInformation metatype = new MockMetaTypeInformation();
        String singletonPid = "pid.alias.singleton";
        metatype.add(singletonPid, false, objectClass);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out, true);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        Node node;

        // attr1 is xml attribute and belongs to group a
        node = getAttribute(xpath, document, singletonPid, attr1.getID());
        assertNotNull(node);
        assertNotNull(getDocumentationGroup(xpath, node, groupA));

        // attr3 is xml attribute and belongs to group b
        node = getAttribute(xpath, document, singletonPid, attr2.getID());
        assertNotNull(node);
        assertNotNull(getDocumentationGroup(xpath, node, groupB));

        // attr3 is xml element and belongs to group b
        node = getElement(xpath, document, singletonPid, attr3.getID());
        assertNotNull(node);
        assertNotNull(getDocumentationGroup(xpath, node, groupB));

        // attr4 has no group
        node = getAttribute(xpath, document, singletonPid, attr4.getID());
        assertNotNull(node);
        assertNull(getDocumentationGroup(xpath, node));

        // check group declaration
        node = getComplexType(xpath, document, singletonPid);
        assertNotNull(node);
        assertNotNull(getDocumentationGroupDeclaration(xpath, node, groupA));
        assertNotNull(getDocumentationGroupDeclaration(xpath, node, groupB));
    }

    @Test
    public void testExtraProperties() throws Exception {
        String id = "alias";
        String aliasName = "myAlias";
        MockObjectClassDefinition objectClass = new MockObjectClassDefinition(id);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("minThreads", AttributeDefinition.INTEGER, 0));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("maxThreads", AttributeDefinition.INTEGER, 0));
        objectClass.setAlias(aliasName);
        objectClass.setExtraProperties(true);

        MockMetaTypeInformation metatype = new MockMetaTypeInformation();
        String singletonPid = "pid.alias.singleton";
        metatype.add(singletonPid, false, objectClass);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out, true);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        Node node;

        node = getComplexType(xpath, document, singletonPid);
        assertNotNull(node);
        assertNotNull(getDocumentationExtraProperties(xpath, node));

        assertNotNull(getAttribute(xpath, node, singletonPid, "internal.properties"));
    }

    @Test
    public void testIncludeType() throws Exception {
        // The metatype isn't important here, just throwing something in..

        String id = "alias";
        String aliasName = "myAlias";
        MockObjectClassDefinition objectClass = new MockObjectClassDefinition(id);
        objectClass.addAttributeDefinition(new MockAttributeDefinition("minThreads", AttributeDefinition.INTEGER, 0));
        objectClass.addAttributeDefinition(new MockAttributeDefinition("maxThreads", AttributeDefinition.INTEGER, 0));
        objectClass.setAlias(aliasName);
        objectClass.setExtraProperties(true);

        MockMetaTypeInformation metatype = new MockMetaTypeInformation();
        String singletonPid = "pid.alias.singleton";
        metatype.add(singletonPid, false, objectClass);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SchemaWriter schemaWriter = createSchemaWriter(out, true);
        schemaWriter.add(metatype);
        schemaWriter.generate(true);

        Document document = parseDocument(out.toByteArray());

        XPath xpath = createXPath();

        Node node;

        node = getComplexType(xpath, document, "includeType");
        assertNotNull(node);
        checkAttribute(getAttribute(xpath, document, "includeType", "optional"), "default", "false");

    }

    private String getAttribute(Node node, String attributeName) {
        if (node != null) {
            Node attribute = node.getAttributes().getNamedItem(attributeName);
            if (attribute != null) {
                return attribute.getNodeValue();
            }
        }
        return null;
    }

    private void checkAttribute(Node node, String attributeName, String attributeValue) throws XPathExpressionException {
        assertNotNull(node);
        Node attribute = node.getAttributes().getNamedItem(attributeName);
        assertNotNull(attributeName, attribute);
        assertEquals(attributeValue, attribute.getNodeValue());
    }

    private void checkElement(Node node, String elementValue) throws XPathExpressionException {
        assertNotNull(node);
        assertEquals(elementValue, node.getTextContent());
    }

    private void checkSkipWildcard(Node node, String typeName) throws XPathExpressionException {
        XPath xpath = createXPath();
        Node wc = (Node) xpath.evaluate("//xsd:complexType[@name=\"" + typeName + "\"]/xsd:anyAttribute", node, XPathConstants.NODE);
        assertNotNull(wc);
        Node attribute = wc.getAttributes().getNamedItem("processContents");
        assertEquals("skip", attribute.getNodeValue());
    }

    private void checkFactoryType(XPath xpath, Node node, String typeName) throws XPathExpressionException {
        node = getComplexType(xpath, node, typeName);
        assertNotNull(node);
        node = getAttribute(xpath, node, typeName, XMLConfigConstants.CFG_INSTANCE_ID);
        //assertNotNull(node);
        //checkAttribute(node, "type", "factoryIdType");
    }

    private void checkNestedType(XPath xpath, Node node, String typeName) throws XPathExpressionException {
        node = getComplexType(xpath, node, typeName + "-nested");
        assertNotNull(node);
        node = getExtensionAttribute(xpath, node, XMLConfigConstants.CFG_CONFIG_REF);
        assertNotNull(node);
        checkAttribute(node, "type", "factoryRefType");
    }

    private Node getElement(XPath xpath, Node node, String typeName, String name) throws XPathExpressionException {
        Node n = getElement(xpath, node, typeName, "xsd:choice", name);
        if (n == null) {
            n = getElement(xpath, node, typeName, "xsd:sequence", name);
        }
        return n;
    }

    private Node getElement(XPath xpath, Node node, String typeName, String groupType, String name) throws XPathExpressionException {
        return (Node) xpath.evaluate("//xsd:complexType[@name=\"" + typeName + "\"]/" + groupType + "/xsd:element[@name=\"" + name + "\"]", node, XPathConstants.NODE);
    }

    private Node getAttribute(XPath xpath, Node node, String typeName, String name) throws XPathExpressionException {
        return (Node) xpath.evaluate("//xsd:complexType[@name=\"" + typeName + "\"]/xsd:attribute[@name=\"" + name + "\"]", node, XPathConstants.NODE);
    }

    private Node getComplexType(XPath xpath, Node node, String typeName) throws XPathExpressionException {
        return (Node) xpath.evaluate("//xsd:complexType[@name=\"" + typeName + "\"]", node, XPathConstants.NODE);
    }

    private Node getSimpleType(XPath xpath, Node node, String typeName) throws XPathExpressionException {
        return (Node) xpath.evaluate("//xsd:simpleType[@name=\"" + typeName + "\"]", node, XPathConstants.NODE);
    }

    private Node getEnumeration(XPath xpath, Node node, String value) throws XPathExpressionException {
        return (Node) xpath.evaluate(".//xsd:simpleType/xsd:restriction/xsd:enumeration[@value=\"" + value + "\"]", node, XPathConstants.NODE);
    }

    private Node getDocumentation(XPath xpath, Node node) throws XPathExpressionException {
        return (Node) xpath.evaluate("./xsd:annotation/xsd:documentation", node, XPathConstants.NODE);
    }

    private Node getDocumentationLabel(XPath xpath, Node node) throws XPathExpressionException {
        return (Node) xpath.evaluate("./xsd:annotation/xsd:appinfo/ext:label", node, XPathConstants.NODE);
    }

    private Node getDocumentationReference(XPath xpath, Node node) throws XPathExpressionException {
        return (Node) xpath.evaluate("./xsd:annotation/xsd:appinfo/ext:reference", node, XPathConstants.NODE);
    }

    private Node getDocumentationRequires(XPath xpath, Node node, String name) throws XPathExpressionException {
        return (Node) xpath.evaluate("./xsd:annotation/xsd:appinfo/ext:requires[@id=\"" + name + "\"]", node, XPathConstants.NODE);
    }

    private Node getDocumentationGroup(XPath xpath, Node node) throws XPathExpressionException {
        return (Node) xpath.evaluate("./xsd:annotation/xsd:appinfo/ext:group", node, XPathConstants.NODE);
    }

    private Node getDocumentationGroup(XPath xpath, Node node, String name) throws XPathExpressionException {
        return (Node) xpath.evaluate("./xsd:annotation/xsd:appinfo/ext:group[@id=\"" + name + "\"]", node, XPathConstants.NODE);
    }

    private Node getDocumentationGroupDeclaration(XPath xpath, Node node, String name) throws XPathExpressionException {
        return (Node) xpath.evaluate("./xsd:annotation/xsd:appinfo/ext:groupDecl[@id=\"" + name + "\"]", node, XPathConstants.NODE);
    }

    private Node getDocumentationExtraProperties(XPath xpath, Node node) throws XPathExpressionException {
        return (Node) xpath.evaluate("./xsd:annotation/xsd:appinfo/ext:extraProperties", node, XPathConstants.NODE);
    }

    private Node getExtensionAttribute(XPath xpath, Node node, String name) throws XPathExpressionException {
        return (Node) xpath.evaluate("./xsd:complexContent/xsd:extension/xsd:attribute[@name=\"" + name + "\"]", node, XPathConstants.NODE);
    }

    private XPath createXPath() {
        XPath xpath = xPathFactory.newXPath();
        xpath.setNamespaceContext(xsdNamespaceContext);
        return xpath;
    }

    private SchemaWriter createSchemaWriter(OutputStream out) throws XMLStreamException {
        return createSchemaWriter(out, false);
    }

    private SchemaWriter createSchemaWriter(OutputStream out, boolean shortNames) throws XMLStreamException {
        String encoding = "UTF-8";
        XMLStreamWriter xmlWriter = xmlOutputFactory.createXMLStreamWriter(out, encoding);
        SchemaWriter schemaWriter = new SchemaWriter(xmlWriter);
        schemaWriter.setEncoding(encoding);
        schemaWriter.setGenerateDocumentation(true);
        schemaWriter.setPreferShortNames(shortNames);
        return schemaWriter;
    }

    private Document parseDocument(byte[] in) throws Exception {
        try {
            DocumentBuilder domBuilder = domFactory.newDocumentBuilder();

            Document document = domBuilder.parse(new ByteArrayInputStream(in));

            xsdSchema.newValidator().validate(new DOMSource(document));

            format(new DOMSource(document), new StreamResult(System.out));

            return document;
        } catch (SAXException e) {
            System.out.println(new String(in));
            throw e;
        }
    }

    private void format(Source source, Result result) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        //transformer.setOutputProperty (OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(source, result);
    }

    private static class XSDNamespaceContext implements NamespaceContext {

        @Override
        public String getNamespaceURI(String prefix) {
            if ("xsd".equals(prefix)) {
                return SchemaWriter.XSD;
            } else if ("ext".equals(prefix)) {
                return SchemaWriter.IBM_EXT_NS;
            }
            return XMLConstants.NULL_NS_URI;
        }

        @Override
        public String getPrefix(String arg0) {
            return null;
        }

        @Override
        public Iterator getPrefixes(String arg0) {
            return null;
        }

    }
}
