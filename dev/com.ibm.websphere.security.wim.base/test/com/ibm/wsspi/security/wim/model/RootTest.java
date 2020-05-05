/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.wim.model;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;

import com.ibm.wsspi.security.wim.SchemaConstants;

public class RootTest {

    /** WIM XML namespaces. */
    public static final String WIM_XMLNS = "xmlns:wim=\"" + SchemaConstants.WIM_NS_URI
                                           + "\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";

    /** Date for testing time stamps. */
    public static final Date NOW = Calendar.getInstance().getTime();

    /** String representation of NOW as it should appear in XML. */
    public static final String NOW_STRING = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(NOW);

    @Test
    public void testToString() {

        /*
         * Test empty instance.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:Root " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new Root().toString());

        /*
         * Test fully configured instance.
         */
        Context context1 = new Context();
        context1.setKey("key1");
        context1.setValue("value1");
        Context context2 = new Context();
        context2.setKey("key2");
        context2.setValue("value2");
        Container entity1 = new Container();
        entity1.setCn("entity1");
        OrgContainer entity2 = new OrgContainer();
        entity2.setCn("entity2");

        Root root = new Root();
        root.set("contexts", context1);
        root.set("contexts", context2);
        root.set("entities", entity1);
        root.set("entities", entity2);
        root.set("controls", new CacheControl());
        root.set("controls", new ChangeControl());

        sb = new StringBuffer();
        sb.append("<wim:Root " + RootTest.WIM_XMLNS + ">\n");
        sb.append("    <wim:contexts>\n");
        sb.append("        <wim:key>key1</wim:key>\n");
        sb.append("        <wim:value xsi:type=\"xs:string\">value1</wim:value>\n");
        sb.append("    </wim:contexts>\n");
        sb.append("    <wim:contexts>\n");
        sb.append("        <wim:key>key2</wim:key>\n");
        sb.append("        <wim:value xsi:type=\"xs:string\">value2</wim:value>\n");
        sb.append("    </wim:contexts>\n");
        sb.append("    <wim:entities xsi:type=\"wim:Container\">\n");
        sb.append("        <wim:cn>entity1</wim:cn>\n");
        sb.append("    </wim:entities>\n");
        sb.append("    <wim:entities xsi:type=\"wim:OrgContainer\">\n");
        sb.append("        <wim:cn>entity2</wim:cn>\n");
        sb.append("    </wim:entities>\n");
        sb.append("    <wim:controls xsi:type=\"wim:CacheControl\"/>\n");
        sb.append("    <wim:controls xsi:type=\"wim:ChangeControl\"/>\n");
        sb.append("</wim:Root>");

        assertEquals(sb.toString(), root.toString());
    }

    @Test
    public void mashalAndUnmarshal() throws Exception {
        /*
         * Test fully configured instance.
         */
        Context context1 = new Context();
        context1.setKey("key1");
        context1.setValue("value1");
        Context context2 = new Context();
        context2.setKey("key2");
        context2.setValue("value2");
        Container entity1 = new Container();
        entity1.setCn("entity1");
        OrgContainer entity2 = new OrgContainer();
        entity2.setCn("entity2");

        Root originalRoot = new Root();
        originalRoot.set("contexts", context1);
        originalRoot.set("contexts", context2);
        originalRoot.set("entities", entity1);
        originalRoot.set("entities", entity2);
        originalRoot.set("controls", new CacheControl());
        originalRoot.set("controls", new ChangeControl());

        /*
         * Marshal the root object to XML.
         */
        JAXBContext rootContext = JAXBContext.newInstance(Root.class);
        Marshaller marshaller = rootContext.createMarshaller();
        StringWriter sw = new StringWriter();
        marshaller.marshal(originalRoot, sw);
        String xmlString = sw.toString();

        /*
         * Unmarshal the XML String to an Root object.
         */
        StringReader reader = new StringReader(xmlString);
        Unmarshaller unmarshaller = rootContext.createUnmarshaller();
        Root unmarshalledRoot = (Root) unmarshaller.unmarshal(reader);

        /*
         * Compare them.
         *
         * TODO We really need to define equals methods on the JAXB classes, but
         * the toString will do for now.
         */
        assertEquals(originalRoot.toString(), unmarshalledRoot.toString());
    }
}
