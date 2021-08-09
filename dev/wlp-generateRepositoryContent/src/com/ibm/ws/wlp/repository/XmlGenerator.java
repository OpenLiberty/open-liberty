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
package com.ibm.ws.wlp.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * This is a generic helper class to use JAXB to read and create XML documents from an ANT task. Any errors will be logged by the ant task's logger.
 * 
 * @param <T> The type of the root of the XML document
 */
public class XmlGenerator<T> {

    /** The JAXB context for working with the XML file */
    private final JAXBContext xmlContext;
    /** The ANT task that was called to trigger the generation, used for logging error messages */
    private final Task task;

    private final Class<T> clazz;

    /**
     * @param clazz The class of the root of the XML document being worked with
     * @param task The ANT task that was called to trigger the generation, used for logging error messages
     */
    public XmlGenerator(Class<T> clazz, Task task) {
        this.task = task;
        try {
            this.xmlContext = JAXBContext.newInstance(clazz);
            this.clazz = clazz;
        } catch (JAXBException e) {
            this.task.log("Failed to create JAXB context for download XML class, error was: " + e.getMessage(), e, Project.MSG_ERR);
            throw new BuildException(e);
        }
    }

    /**
     * This method will parse the supplied file and return the root document for it. If the file does not exist then a new instance of the root object will be created however if it
     * is <code>null</code> then <code>null</code> is returned.
     * 
     * @param file The file to parse
     * @return The parsed document or <code>null</code> if the file was <code>null</code>. If the file was not <code>null</code> did not exist then a new instance is returned
     * @throws BuildException if something goes wrong
     */
    public T parseXml(File file) throws BuildException {
        T xml = null;
        if (file != null) {
            try {
                if (file.exists()) {
                    Unmarshaller unmarshaller = xmlContext.createUnmarshaller();
                    xml = (T) unmarshaller.unmarshal(file);
                } else {
                    xml = clazz.newInstance();
                }
            } catch (JAXBException e) {
                this.task.log("Failed to read existing XML from :\t" + file.getAbsolutePath() + ". Exception message: " + e.getMessage(), e,
                              Project.MSG_ERR);
                throw new BuildException(e);
            } catch (IllegalAccessException e) {
                this.task.log("Failed to create new instance of " + this.clazz + " due to an IllegalAccessException. Exception message: " + e.getMessage(), e,
                              Project.MSG_ERR);
                throw new BuildException(e);
            } catch (InstantiationException e) {
                this.task.log("Failed to create new instance of " + this.clazz + " due to an InstantiationException. Exception message: " + e.getMessage(), e,
                              Project.MSG_ERR);
                throw new BuildException(e);
            }
        }
        return xml;
    }

    /**
     * This method will write the XML document to the supplied file, neither of which should be <code>null</code>.
     * 
     * @param xml The root of the XML document to write
     * @param file The file to write the XML to
     * @throws BuildException if something goes wrong
     */
    public void writeXml(T xml, File file) {
        writeXml(xml, file, null);
    }

    /**
     * This method will write the XML document to the supplied file, neither of which should be <code>null</code>.
     * 
     * @param xml The root of the XML document to write
     * @param file The file to write the XML to
     * @param cdataElements Space-separated list of names of elements to handle as CDATA
     * @throws BuildException if something goes wrong
     */
    public void writeXml(T xml, File file, String cdataElements) {
        Marshaller xmlMarshaller = null;
        try {
            xmlMarshaller = this.xmlContext.createMarshaller();
        } catch (JAXBException e) {
            // Without a marshaller we can't write the file
            this.task.log("Unable to create the marshaller so can't write the XML file, exception message: " + e.getMessage(), Project.MSG_ERR);
            throw new BuildException(e);
        }

        // Add some nice to have bits to the marshaller
        try {
            xmlMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            Schema schema = createSchema(this.xmlContext);
            xmlMarshaller.setSchema(schema);
        } catch (PropertyException e) {
            // This means we couldn't pretty print the XML, not the end of the world but log it
            this.task.log("Unable to produce schema for XML due to PropertyException: " + e.getMessage(), Project.MSG_WARN);
        } catch (SAXException e) {
            // This means we couldn't generate the schema so can't validate the XML we produce which is not a fatal error but log this happened
            this.task.log("Unable to produce schema for XML due to SAXException: " + e.getMessage(), Project.MSG_WARN);
        } catch (IOException e) {
            // This means we couldn't generate the schema so can't validate the XML we produce which is not a fatal error but log this happened
            this.task.log("Unable to produce schema for XML due to IOException: " + e.getMessage(), Project.MSG_WARN);
        }

        // Now save
        try {
            if (cdataElements != null) {
                //Wondering what I'm up to here?
                //JAXB doesn't support CDATA natively, and any simple workarounds are Sun / implementation specific :-(
                //If we have CDATA elements, me are marshalling the data out via a DOM transform instead....
                DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
                Document document = docBuilderFactory.newDocumentBuilder().newDocument();

                // Marshall into the document, then we'll put it to file later
                xmlMarshaller.marshal(xml, document);

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer cdataSpecificNullTransform = transformerFactory.newTransformer();
                cdataSpecificNullTransform.setOutputProperty(
                                                             OutputKeys.CDATA_SECTION_ELEMENTS, cdataElements);
                cdataSpecificNullTransform.transform(new DOMSource(document), new StreamResult(file));
            } else {
                xmlMarshaller.marshal(xml, file);
            }
        } catch (JAXBException e) {
            this.task.log("Unable to create XML due to a JAXBException: " + e.getMessage(), Project.MSG_ERR);
            throw new BuildException(e);
        } catch (ParserConfigurationException e) {
            this.task.log("Unable to transform CDATA fields in XML due to a ParserConfigurationException: " + e.getMessage(), Project.MSG_ERR);
            throw new BuildException(e);
        } catch (TransformerConfigurationException e) {
            this.task.log("Unable to transform CDATA fields in XML due to a TransformerConfigurationException: " + e.getMessage(), Project.MSG_ERR);
            throw new BuildException(e);
        } catch (TransformerException e) {
            this.task.log("Unable to transform CDATA fields in XML due to a TransformerException: " + e.getMessage(), Project.MSG_ERR);
            throw new BuildException(e);
        }
    }

    /**
     * This method will create a {@link Schema} from a {@link JAXBContext}.
     * 
     * @param context The context to create the schema for
     * @return The {@link Schema} for this context
     * @throws IOException
     * @throws SAXException
     */
    private Schema createSchema(JAXBContext context) throws IOException, SAXException {
        // This is surprisingly faffy for something that has a generateSchema method! This will only produce
        // a Result object, of which a schema result is not possible but you can produce dom results that
        // can be converted into dom sources to be read by the schema factory. As you can define multiple
        // namespaces you need to have a list of these.
        final List<DOMResult> schemaDomResults = new ArrayList<DOMResult>();

        // First use the context to create the schema dom objects
        context.generateSchema(new SchemaOutputResolver() {

            @Override
            public Result createOutput(String namespaceUri, String suggestedFileName)
                            throws IOException {
                DOMResult domResult = new DOMResult();
                domResult.setSystemId(suggestedFileName);
                schemaDomResults.add(domResult);
                return domResult;
            }
        });

        // convert to a form that can be used by the schema factory
        DOMSource[] schemaDomSources = new DOMSource[schemaDomResults.size()];
        for (int i = 0; i < schemaDomResults.size(); i++) {
            DOMSource domSource = new DOMSource(schemaDomResults.get(i).getNode());
            schemaDomSources[i] = domSource;
        }

        // Finally create the schema
        Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schemaDomSources);
        return schema;
    }

}
