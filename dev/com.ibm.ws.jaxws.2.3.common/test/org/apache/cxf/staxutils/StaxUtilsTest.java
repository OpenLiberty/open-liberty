/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.staxutils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.InputSource;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.IOUtils;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StaxUtilsTest {

    @Test
    public void testFactoryCreation() {
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(getTestStream("./resources/amazon.xml"));
        assertNotNull(reader);
    }

    private InputStream getTestStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }

    @Test
    public void testCommentNode() throws Exception {
        //CXF-3034
        Document document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().newDocument();
        Element root = document.createElementNS("urn:test", "root");
        root.appendChild(document.createComment("test comment"));
        StaxUtils.copy(StaxUtils.createXMLStreamReader(root), StaxUtils.createXMLStreamWriter(System.out));
    }

    @Test
    public void testToNextElement() {
        String soapMessage = "./resources/sayHiRpcLiteralReq.xml";
        XMLStreamReader r = StaxUtils.createXMLStreamReader(getTestStream(soapMessage));
        DepthXMLStreamReader reader = new DepthXMLStreamReader(r);
        assertTrue(StaxUtils.toNextElement(reader));
        assertEquals("Envelope", reader.getLocalName());

        StaxUtils.nextEvent(reader);

        assertTrue(StaxUtils.toNextElement(reader));
        assertEquals("Body", reader.getLocalName());
    }

    @Test
    public void testToNextTag() throws Exception {
        String soapMessage = "./resources/headerSoapReq.xml";
        XMLStreamReader r = StaxUtils.createXMLStreamReader(getTestStream(soapMessage));
        DepthXMLStreamReader reader = new DepthXMLStreamReader(r);
        reader.nextTag();
        StaxUtils.toNextTag(reader, new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body"));
        assertEquals("Body", reader.getLocalName());
    }

    @Test
    public void testCopy() throws Exception {

        // do the stream copying
        String soapMessage = "./resources/headerSoapReq.xml";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(getTestStream(soapMessage));
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(baos);
        StaxUtils.copy(reader, writer);
        writer.flush();
        baos.flush();

        // write output to a string
        String output = baos.toString();
        baos.close();

        // re-read the input xml doc to a string
        String input = IOUtils.toString(getTestStream(soapMessage));

        // seach for the first begin of "<soap:Envelope" to escape the apache licenses header
        int beginIndex = input.indexOf("<soap:Envelope");
        input = input.substring(beginIndex);
        beginIndex = output.indexOf("<soap:Envelope");
        output = output.substring(beginIndex);

        output = output.replace("\r\n", "\n");
        input = input.replace("\r\n", "\n");

        // compare the input and output string
        assertEquals(input, output);
    }

    @Test
    public void testCXF2468() throws Exception {
        Document doc = DOMUtils.newDocument();
        doc.appendChild(doc.createElementNS("http://blah.org/", "blah"));
        Element foo = doc.createElementNS("http://blah.org/", "foo");
        Attr attr = doc.createAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:nil");
        attr.setValue("true");
        foo.setAttributeNodeNS(attr);
        doc.getDocumentElement().appendChild(foo);
        XMLStreamReader sreader = StaxUtils.createXMLStreamReader(doc);
        StringWriter sw = new StringWriter();
        XMLStreamWriter swriter = StaxUtils.createXMLStreamWriter(sw);
        StaxUtils.copy(sreader, swriter, true);
        swriter.flush();
        assertTrue("No xsi namespace: " + sw.toString(), sw.toString().contains("XMLSchema-instance"));
    }

    @Test
    public void testNonNamespaceAwareParser() throws Exception {
        String xml = "<blah xmlns=\"http://blah.org/\" xmlns:snarf=\"http://snarf.org\">"
            + "<foo snarf:blop=\"blop\">foo</foo></blah>";


        StringReader reader = new StringReader(xml);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        Document doc = dbf.newDocumentBuilder().parse(new InputSource(reader));
        Source source = new DOMSource(doc);

        dbf.setNamespaceAware(true);
        reader = new StringReader(xml);
        Document docNs = dbf.newDocumentBuilder().parse(new InputSource(reader));
        Source sourceNs = new DOMSource(docNs);


        XMLStreamReader sreader = StaxUtils.createXMLStreamReader(source);

        StringWriter sw = new StringWriter();
        XMLStreamWriter swriter = StaxUtils.createXMLStreamWriter(sw);

        //should not throw an exception
        StaxUtils.copy(sreader, swriter);
        swriter.flush();
        swriter.close();

        String output = sw.toString();
        assertTrue(output.contains("blah"));
        assertTrue(output.contains("foo"));
        assertTrue(output.contains("snarf"));
        assertTrue(output.contains("blop"));


        sreader = StaxUtils.createXMLStreamReader(sourceNs);
        sw = new StringWriter();
        swriter = StaxUtils.createXMLStreamWriter(sw);
        //should not throw an exception
        StaxUtils.copy(sreader, swriter);
        swriter.flush();
        swriter.close();

        output = sw.toString();
        assertTrue(output.contains("blah"));
        assertTrue(output.contains("foo"));
        assertTrue(output.contains("snarf"));
        assertTrue(output.contains("blop"));


        sreader = StaxUtils.createXMLStreamReader(source);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        swriter = StaxUtils.createXMLStreamWriter(bout);
        StaxUtils.copy(sreader, swriter);
        swriter.flush();
        swriter.close();

        output = bout.toString();
        assertTrue(output.contains("blah"));
        assertTrue(output.contains("foo"));
        assertTrue(output.contains("snarf"));
        assertTrue(output.contains("blop"));
    }

    @Test
    public void testEmptyNamespace() throws Exception {
        String testString = "<ns1:a xmlns:ns1=\"http://www.apache.org/\"><s1 xmlns=\"\">"
            + "abc</s1><s2 xmlns=\"\">def</s2></ns1:a>";

        cycleString(testString);

        testString = "<a xmlns=\"http://www.apache.org/\"><s1 xmlns=\"\">"
            + "abc</s1><s2 xmlns=\"\">def</s2></a>";
        cycleString(testString);

        testString = "<a xmlns=\"http://www.apache.org/\"><s1 xmlns=\"\">"
            + "abc</s1><s2>def</s2></a>";
        cycleString(testString);

        testString = "<ns1:a xmlns:ns1=\"http://www.apache.org/\"><s1>"
            + "abc</s1><s2 xmlns=\"\">def</s2></ns1:a>";

        cycleString(testString);
    }

    private void cycleString(String s) throws Exception {
        StringReader reader = new StringReader(s);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(new InputSource(reader));
        String orig = StaxUtils.toString(doc.getDocumentElement());

        StringWriter sw = new StringWriter();
        XMLStreamWriter swriter = StaxUtils.createXMLStreamWriter(sw);
        //should not throw an exception
        StaxUtils.writeDocument(doc, swriter, false, true);
        swriter.flush();
        swriter.close();

        String output = sw.toString();
        assertEquals(s, output);

        W3CDOMStreamWriter domwriter = new W3CDOMStreamWriter();
        StaxUtils.writeDocument(doc, domwriter, false, true);
        output = StaxUtils.toString(domwriter.getDocument().getDocumentElement());
        assertEquals(orig, output);
    }

    @Test
    public void testRootPI() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(getTestStream("./resources/rootMaterialTest.xml"));
        StringWriter sw = new StringWriter();
        XMLStreamWriter swriter = StaxUtils.createXMLStreamWriter(sw);
        StaxUtils.writeDocument(doc, swriter, true, false);
        swriter.flush();
        swriter.close();
        String output = sw.toString();
        assertTrue(output.contains("<?pi in='the sky'?>"));
        assertTrue(output.contains("<?e excl='gads'?>"));
    }

    @Test
    public void testRootPInoProlog() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().parse(getTestStream("./resources/rootMaterialTest.xml"));
        StringWriter sw = new StringWriter();
        XMLStreamWriter swriter = StaxUtils.createXMLStreamWriter(sw);
        StaxUtils.writeDocument(doc, swriter, false, false);
        swriter.flush();
        swriter.close();
        String output = sw.toString();
        assertFalse(output.contains("<?pi in='the sky'?>"));
        assertFalse(output.contains("<?e excl='gads'?>"));
    }

    @Test
    public void testDefaultPrefix() throws Exception {
        String soapMessage = "./resources/AddRequest.xml";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(getTestStream(soapMessage));
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(baos);
        StaxSource staxSource = new StaxSource(reader);
        StaxUtils.copy(staxSource, writer);
        writer.flush();
        baos.flush();
    }

    @Test
    public void testDefaultPrefixInRootElementWithIdentityTransformer() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String xml = "<root xmlns=\"urn:org.apache.cxf:test\">Text</root>";
        StringReader stringReader = new StringReader(xml);
        StreamSource source = new StreamSource(stringReader);
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(source);
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(baos);
        StaxSource staxSource = new StaxSource(reader);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(staxSource, new StreamResult(baos));
        writer.flush();
        baos.flush();
        assertThat(new String(baos.toByteArray()), equalTo(xml));
    }

    @Test
    public void testDefaultPrefixInRootElementWithXalanCopyTransformer() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String xml = "<root xmlns=\"urn:org.apache.cxf:test\">Text</root>";
        StringReader stringReader = new StringReader(xml);
        //StreamSource source = new StreamSource(stringReader);
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(stringReader);
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(baos);
        StaxSource staxSource = new StaxSource(reader);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Document doc = StaxUtils.read(getTestStream("./resources/copy.xsl"));
        Transformer transformer = transformerFactory.newTransformer(new DOMSource(doc));
        //System.out.println("Used transformer: " + transformer.getClass().getName());
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(staxSource, new StreamResult(baos));
        writer.flush();
        baos.flush();
        assertThat(new String(baos.toByteArray()), equalTo(xml));
    }

    @Test
    public void testDefaultPrefixInRootElementWithJDKInternalCopyTransformer() throws Exception {
        TransformerFactory trf = null;
        try {
            trf = TransformerFactory
                .newInstance("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl", null);
            trf.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String xml = "<root xmlns=\"urn:org.apache.cxf:test\">Text</root>";
            StringReader stringReader = new StringReader(xml);
            StreamSource source = new StreamSource(stringReader);
            XMLStreamReader reader = StaxUtils.createXMLStreamReader(source);
            XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(baos);
            StaxSource staxSource = new StaxSource(reader);
            Document doc = StaxUtils.read(getTestStream("./resources/copy.xsl"));
            Transformer transformer = trf.newTransformer(new DOMSource(doc));
            //System.out.println("Used transformer: " + transformer.getClass().getName());
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(staxSource, new StreamResult(baos));
            writer.flush();
            baos.flush();
            assertThat(new String(baos.toByteArray()), equalTo(xml));
        } catch (Throwable throwable) {
            // ignore on non Sun/Oracle JDK
            return;
        }
    }

    @Test
    public void testCXF3193() throws Exception {
        String testString = "<a:elem1 xmlns:a=\"test\" xmlns:b=\"test\" a:attr1=\"value\"/>";
        CachingXmlEventWriter writer = new CachingXmlEventWriter();
        StaxUtils.copy(StaxUtils.createXMLStreamReader(new StringReader(testString)),
                       writer);
        StringWriter swriter = new StringWriter();
        XMLStreamWriter xwriter = StaxUtils.createXMLStreamWriter(swriter);
        for (XMLEvent event : writer.getEvents()) {
            StaxUtils.writeEvent(event, xwriter);
        }
        xwriter.flush();

        String s = swriter.toString();
        int idx = s.indexOf("xmlns:a");
        idx = s.indexOf("xmlns:a", idx + 1);
        assertEquals(-1, idx);
    }

    @Test
    public void testCopyWithEmptyNamespace() throws Exception {
        StringBuilder in = new StringBuilder(128);
        in.append("<foo xmlns=\"http://example.com/\">");
        in.append("<bar xmlns=\"\"/>");
        in.append("</foo>");

        XMLStreamReader reader = StaxUtils.createXMLStreamReader(
             new ByteArrayInputStream(in.toString().getBytes()));

        Writer out = new StringWriter();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(out);
        StaxUtils.copy(reader, writer);
        writer.close();

        assertEquals(in.toString(), out.toString());
    }

    @Test
    public void testQName() throws Exception {
        StringBuilder in = new StringBuilder(128);
        in.append("<f:foo xmlns:f=\"http://example.com/\">");
        in.append("<bar>f:Bar</bar>");
        in.append("<bar> f:Bar </bar>");
        in.append("<bar>x:Bar</bar>");
        in.append("</f:foo>");

        XMLStreamReader reader = StaxUtils.createXMLStreamReader(
             new ByteArrayInputStream(in.toString().getBytes()));

        QName qname = new QName("http://example.com/", "Bar");
        assertEquals(XMLStreamConstants.START_ELEMENT, reader.next());
        assertEquals(XMLStreamConstants.START_ELEMENT, reader.next());
        // first bar
        assertEquals(qname, StaxUtils.readQName(reader));
        assertEquals(XMLStreamConstants.START_ELEMENT, reader.next());
        // second bar
        assertEquals(qname, StaxUtils.readQName(reader));
        assertEquals(XMLStreamConstants.START_ELEMENT, reader.next());
        // third bar
        try {
            StaxUtils.readQName(reader);
            fail("invalid qname in mapping");
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    public void testCopyFromTheMiddle() throws Exception {
        String innerXml =
                "<inner>\n"
                + "<body>body text here</body>\n"
                + "</inner>\n";
        String xml =
                "<outer>\n"
                + innerXml
                + "</outer>";

        StringReader reader = new StringReader(xml);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);

        Document doc = dbf.newDocumentBuilder().parse(new InputSource(reader));
        Source source = new DOMSource(doc);

        // Skip <outer>
        XMLStreamReader sreader = StaxUtils.createXMLStreamReader(source);
        while (!"inner".equals(sreader.getLocalName())) {
            sreader.next();
        }

        StringWriter sw = new StringWriter();
        XMLStreamWriter swriter = StaxUtils.createXMLStreamWriter(sw);

        StaxUtils.copy(sreader, swriter, true, true);
        swriter.flush();
        swriter.close();

        //System.out.println(innerXml);
        //System.out.println(sw.toString());
        assertEquals(innerXml, sw.toString());
    }

    @Test
    public void testIsSecureReader() {
        Document doc = DOMUtils.newDocument();
        Element documentElement = doc.createElementNS(null, "root");
        doc.appendChild(documentElement);

        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new StringReader(StaxUtils.toString(doc)));
        assertTrue(StaxUtils.isSecureReader(reader, null));
    }

    @Test
    public void testDefaultMaxAttributeCount() throws XMLStreamException {
        Document doc = DOMUtils.newDocument();
        Element documentElement = doc.createElementNS(null, "root");
        doc.appendChild(documentElement);

        for (int i = 0; i < 300; i++) {
            documentElement.setAttributeNS(null, "attr-" + i, Integer.toString(i));
        }

        // Should be OK
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new StringReader(StaxUtils.toString(doc)));
        assertNotNull(StaxUtils.read(reader));

        for (int i = 300; i < 800; i++) {
            documentElement.setAttributeNS(null, "attr-" + i, Integer.toString(i));
        }

        assertTrue(documentElement.getAttributes().getLength() > 500);

        // Should fail as we are over the max attribute count
        reader = StaxUtils.createXMLStreamReader(new StringReader(StaxUtils.toString(doc)));
        try {
            StaxUtils.read(reader);
            fail("Failure expected on exceeding the limit");
        } catch (XMLStreamException ex) {
            assertTrue(ex.getMessage().contains("Attribute limit"));
        }
    }

    @Test
    public void testDefaultMaxAttributeLength() throws XMLStreamException {
        Document doc = DOMUtils.newDocument();
        Element documentElement = doc.createElementNS(null, "root");
        doc.appendChild(documentElement);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1024; i++) {
            sb.append(i);
        }

        documentElement.setAttributeNS(null, "attr", sb.toString());

        // Should be OK
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new StringReader(StaxUtils.toString(doc)));
        assertNotNull(StaxUtils.read(reader));

        for (int i = 0; i < 1024 * 64; i++) {
            sb.append(i);
        }

        documentElement.setAttributeNS(null, "attr", sb.toString());
        assertTrue(documentElement.getAttributeNS(null, "attr").length() > (1024 * 64));

        // Should fail as we are over the max attribute length
        reader = StaxUtils.createXMLStreamReader(new StringReader(StaxUtils.toString(doc)));
        try {
            StaxUtils.read(reader);
            fail("Failure expected on exceeding the limit");
        } catch (XMLStreamException ex) {
            assertTrue(ex.getMessage().contains("Maximum attribute size limit"));
        }

    }

    @Test
    public void testDefaultMaxElementDepth() throws XMLStreamException {
        Document doc = DOMUtils.newDocument();
        Element documentElement = doc.createElementNS(null, "root");
        doc.appendChild(documentElement);

        Element currentNode = documentElement;
        for (int i = 0; i < 50; i++) {
            Element childElement = doc.createElementNS("null", "root" + i);
            currentNode.appendChild(childElement);
            currentNode = childElement;
        }

        // Should be OK
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new StringReader(StaxUtils.toString(doc)));
        assertNotNull(StaxUtils.read(reader));

        for (int i = 50; i < 102; i++) {
            Element childElement = doc.createElementNS("null", "root" + i);
            currentNode.appendChild(childElement);
            currentNode = childElement;
        }

        // Should fail as we are over the max element depth value
        reader = StaxUtils.createXMLStreamReader(new StringReader(StaxUtils.toString(doc)));
        try {
            StaxUtils.read(reader);
            fail("Failure expected on exceeding the limit");
        } catch (XMLStreamException ex) {
            assertTrue(ex.getMessage().contains("Maximum Element Depth limit"));
        }
    }

    @Test
    public void testDefaultMaxChildElements() throws XMLStreamException {
        Document doc = DOMUtils.newDocument();
        Element documentElement = doc.createElementNS(null, "root");
        doc.appendChild(documentElement);

        for (int i = 0; i < 1000; i++) {
            Element childElement = doc.createElementNS("null", "root" + i);
            documentElement.appendChild(childElement);
        }

        // Should be OK
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new StringReader(StaxUtils.toString(doc)));
        assertNotNull(StaxUtils.read(reader));

        for (int i = 0; i < 49001; i++) {
            Element childElement = doc.createElementNS("null", "root" + i);
            documentElement.appendChild(childElement);
        }

        // Should fail as we are over the max element count value
        reader = StaxUtils.createXMLStreamReader(new StringReader(StaxUtils.toString(doc)));
        try {
            StaxUtils.read(reader);
            fail("Failure expected on exceeding the limit");
        } catch (XMLStreamException ex) {
            assertTrue(ex.getMessage().contains("Maximum Number of Child Elements limit"));
        }
    }

}
