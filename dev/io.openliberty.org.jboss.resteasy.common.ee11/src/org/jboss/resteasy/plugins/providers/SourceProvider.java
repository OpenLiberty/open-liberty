/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

// https://github.com/resteasy/resteasy/blob/5d60f62c67eec6dbdf9ecd10db7473614750c35d/resteasy-core/src/main/java/org/jboss/resteasy/plugins/providers/SourceProvider.java

package org.jboss.resteasy.plugins.providers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.core.messagebody.AsyncBufferedMessageBodyWriter;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.util.NoContent;
import org.jboss.resteasy.util.XmlSupport; // Liberty Change (RESTEASY-3785)
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:bill@burkecentral.com">BillBurke</a>
 * @version $Revision: 1 $
 */
@Provider
@Produces({ MediaType.TEXT_XML, "application/xml", "application/*+xml" })
@Consumes({ MediaType.TEXT_XML, "application/xml", "application/*+xml" })
public class SourceProvider implements MessageBodyReader<Source>, AsyncBufferedMessageBodyWriter<Source> {
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type.equals(Source.class);
    }

    public Source readFrom(Class<Source> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
        LogMessages.LOGGER.debugf("Provider : %s,  Method : readFrom", getClass().getName());
        if (NoContent.isContentLengthZero(httpHeaders))
            return new StreamSource(new ByteArrayInputStream(new byte[0]));
        return new StreamSource(entityStream);
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Source.class.isAssignableFrom(type);
    }

    public long getSize(Source inputStream, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    /** // Liberty Change start (RESTEASY-3785)
     * Writes a {@link Source} to the output stream. For {@link StreamSource} instances, the source is wrapped with a
     * configured {@link org.xml.sax.XMLReader} via {@link XmlSupport#newXmlReader()}. For other
     * {@code Source} types (e.g. {@link SAXSource}), the caller is responsible for providing a configured
     * {@code XMLReader}. The {@link TransformerFactory} used for serialization is always configured with
     * processing enabled.
     */ // Liberty Change end (RESTEASY-3785)
    public void writeTo(Source source, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
        LogMessages.LOGGER.debugf("Provider : %s,  Method : writeTo", getClass().getName());
        try {
            if (source instanceof StreamSource) {
                StreamSource stream = (StreamSource) source;
                InputSource inputStream;

                if (stream.getInputStream() == null && stream.getReader() != null) {
                    inputStream = new InputSource(stream.getReader());
                } else {
                    inputStream = new InputSource(stream.getInputStream());
                }

                inputStream.setPublicId(stream.getPublicId()); // Liberty Change (RESTEASY-3785)
                inputStream.setSystemId(source.getSystemId());
                source = new SAXSource(XmlSupport.newXmlReader(), inputStream); // Liberty Change (RESTEASY-3785)
            }

            StreamResult sr = new StreamResult(entityStream);
            final TransformerFactory tf = XmlSupport.newTransformerFactory(); // Liberty Change (RESTEASY-3785)
            tf.newTransformer().transform(source, sr); // Liberty Change (RESTEASY-3785)

        } catch (SAXException ex) {
            throw new InternalServerErrorException(ex);
        } catch (ParserConfigurationException ex) {
            throw new InternalServerErrorException(ex);
        } catch (TransformerException ex) {
            throw new InternalServerErrorException(ex);
        }
    }
}