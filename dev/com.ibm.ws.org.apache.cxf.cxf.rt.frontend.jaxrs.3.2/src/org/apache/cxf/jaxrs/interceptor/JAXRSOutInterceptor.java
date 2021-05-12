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

package org.apache.cxf.jaxrs.interceptor;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.sse.SseEventSink;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.apache.cxf.jaxrs.impl.WriterInterceptorMBW;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.provider.AbstractConfigurableProvider;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.CachingXmlEventWriter;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.JaxRsConstants;

public class JAXRSOutInterceptor extends AbstractOutDatabindingInterceptor {
    private static final TraceComponent tc = Tr.register(JAXRSOutInterceptor.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JAXRSOutInterceptor.class);

    public JAXRSOutInterceptor() {
        super(Phase.MARSHAL);
    }

    @Override
    public void handleMessage(Message message) {
        ServerProviderFactory providerFactory = ServerProviderFactory.getInstance(message);
        try {
            processResponse(providerFactory, message);
        } finally {
            if (message.get(SseEventSink.class) == null) {
                ServerProviderFactory.releaseRequestState(providerFactory, message);
            }
        }

    }

    @SuppressWarnings("resource") // Response shouldn't be closed here
    private void processResponse(ServerProviderFactory providerFactory, Message message) {

        if (isResponseAlreadyHandled(message)) {
            return;
        }
        MessageContentsList objs = MessageContentsList.getContentsList(message);
        if (objs == null || objs.isEmpty()) {
            return;
        }

        Object responseObj = objs.get(0);

        Response response = null;
        if (responseObj instanceof Response) {
            response = (Response)responseObj;
            if (response.getStatus() == 500
                && message.getExchange().get(JAXRSUtils.EXCEPTION_FROM_MAPPER) != null) {
                message.put(Message.RESPONSE_CODE, 500);
                return;
            }
        } else {
            int status = getStatus(message, responseObj != null ? 200 : 204);
            response = JAXRSUtils.toResponseBuilder(status).entity(responseObj).build();
        }

        Exchange exchange = message.getExchange();
        OperationResourceInfo ori = (OperationResourceInfo)exchange.get(OperationResourceInfo.class
            .getName());

        serializeMessage(providerFactory, message, response, ori, true);
    }



    private int getStatus(Message message, int defaultValue) {
        Object customStatus = message.getExchange().get(Message.RESPONSE_CODE);
        return customStatus == null ? defaultValue : (Integer)customStatus;
    }

    private void serializeMessage(ServerProviderFactory providerFactory,
                                  Message message,
                                  Response theResponse,
                                  OperationResourceInfo ori,
                                  boolean firstTry) {

        ResponseImpl response = (ResponseImpl)JAXRSUtils.copyResponseIfNeeded(theResponse);

        final Exchange exchange = message.getExchange();

        boolean headResponse = response.getStatus() == 200 && firstTry
            && ori != null && HttpMethod.HEAD.equals(ori.getHttpMethod());
        Object entity = response.getActualEntity();
        if (headResponse && entity != null) {
            Tr.info(tc, new org.apache.cxf.common.i18n.Message("HEAD_WITHOUT_ENTITY", BUNDLE).toString());
            entity = null;
        }

        Method invoked = ori == null ? null : ori.getAnnotatedMethod() != null
            ? ori.getAnnotatedMethod() : ori.getMethodToInvoke();

        Annotation[] annotations = null;
        Annotation[] staticAnns = ori != null ? ori.getOutAnnotations() : new Annotation[]{};
        Annotation[] responseAnns = response.getEntityAnnotations();
        if (responseAnns != null) {
            annotations = new Annotation[staticAnns.length + responseAnns.length];
            System.arraycopy(staticAnns, 0, annotations, 0, staticAnns.length);
            System.arraycopy(responseAnns, 0, annotations, staticAnns.length, responseAnns.length);
        } else {
            annotations = staticAnns;
        }

        response.setStatus(getActualStatus(response.getStatus(), entity));
        response.setEntity(entity, annotations);

        // Prepare the headers
        MultivaluedMap<String, Object> responseHeaders =
            prepareResponseHeaders(message, response, entity, firstTry);

        // Run the filters
        if (JaxRsConstants.JAXRS_CONTAINER_FILTER_DISABLED == false) {
            try {
                JAXRSUtils.runContainerResponseFilters(providerFactory, response, message, ori, invoked);
            } catch (Throwable ex) {
                handleWriteException(providerFactory, message, ex, firstTry);
                return;
            }
        }

        // Write the entity
        entity = InjectionUtils.getEntity(response.getActualEntity());
        setResponseStatus(message, getActualStatus(response.getStatus(), entity));
        if (entity == null) {
            if (!headResponse) {
                responseHeaders.putSingle(HttpHeaders.CONTENT_LENGTH, "0");
                if (MessageUtils.getContextualBoolean(message, "remove.content.type.for.empty.response", false)) {
                    responseHeaders.remove(HttpHeaders.CONTENT_TYPE);
                    message.remove(Message.CONTENT_TYPE);
                }
            }
            HttpUtils.convertHeaderValuesToString(responseHeaders, true);
            return;
        }

        Object ignoreWritersProp = exchange.get(JAXRSUtils.IGNORE_MESSAGE_WRITERS);
        boolean ignoreWriters =
            ignoreWritersProp != null && Boolean.valueOf(ignoreWritersProp.toString());
        if (ignoreWriters) {
            writeResponseToStream(message.getContent(OutputStream.class), entity);
            return;
        }

        MediaType responseMediaType =
            getResponseMediaType(responseHeaders.getFirst(HttpHeaders.CONTENT_TYPE));

        Class<?> serviceCls = invoked != null ? ori.getClassResourceInfo().getServiceClass() : null;
        Class<?> targetType = InjectionUtils.getRawResponseClass(entity);
        Type genericType = InjectionUtils.getGenericResponseType(invoked, serviceCls,
                                                                 response.getActualEntity(), targetType, exchange);
        targetType = InjectionUtils.updateParamClassToTypeIfNeeded(targetType, genericType);
        annotations = response.getEntityAnnotations();

        List<WriterInterceptor> writers = providerFactory
            .createMessageBodyWriterInterceptor(targetType, genericType, annotations, responseMediaType, message,
                                                ori == null ? null : ori.getNameBindings());

        OutputStream outOriginal = message.getContent(OutputStream.class);
        if (writers == null || writers.isEmpty()) {
            writeResponseErrorMessage(message, outOriginal, "NO_MSG_WRITER", targetType, responseMediaType);
            return;
        }
        try {
            boolean checkWriters = false;
            if (responseMediaType.isWildcardSubtype()) {
                Produces pM = AnnotationUtils.getMethodAnnotation(ori == null ? null : ori.getAnnotatedMethod(),
                                                                              Produces.class);
                Produces pC = AnnotationUtils.getClassAnnotation(serviceCls, Produces.class);
                checkWriters = pM == null && pC == null;
            }
            responseMediaType = checkFinalContentType(responseMediaType, writers, checkWriters);
        } catch (Throwable ex) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, ex.getMessage() + ", " + ex);
            }
	    handleWriteException(providerFactory, message, ex, firstTry);
            return;
        }
        String finalResponseContentType = JAXRSUtils.mediaTypeToString(responseMediaType);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Response content type is: " + finalResponseContentType);
        }
        responseHeaders.putSingle(HttpHeaders.CONTENT_TYPE, finalResponseContentType);
        message.put(Message.CONTENT_TYPE, finalResponseContentType);

        boolean enabled = checkBufferingMode(message, writers, firstTry);
        try {

            try {       // NOPMD
                JAXRSUtils.writeMessageBody(writers,
                        entity,
                        targetType,
                        genericType,
                        annotations,
                        responseMediaType,
                        responseHeaders,
                        message);

                if (isResponseRedirected(message)) {
                    return;
                }
                checkCachedStream(message, outOriginal, enabled);
            } finally {
                if (enabled) {
                    OutputStream os = message.getContent(OutputStream.class);
                    if (os != outOriginal && os instanceof CachedOutputStream) {
                        os.close();
                    }
                    message.setContent(OutputStream.class, outOriginal);
                    message.put(XMLStreamWriter.class.getName(), null);
                }
            }

        } catch (Throwable ex) {
            logWriteError(firstTry, targetType, responseMediaType);
            handleWriteException(providerFactory, message, ex, firstTry);
        }
    }

    private MultivaluedMap<String, Object> prepareResponseHeaders(Message message,
                                                                  ResponseImpl response,
                                                                  Object entity,
                                                                  boolean firstTry) {
        MultivaluedMap<String, Object> responseHeaders = response.getMetadata();
        @SuppressWarnings("unchecked")
        Map<String, List<Object>> userHeaders = (Map<String, List<Object>>)message.get(Message.PROTOCOL_HEADERS);
        if (firstTry && userHeaders != null) {
            responseHeaders.putAll(userHeaders);
        }
        if (entity != null) {
            Object customContentType = responseHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
            if (customContentType == null) {
                String initialResponseContentType = (String)message.get(Message.CONTENT_TYPE);
                if (initialResponseContentType != null) {
                    responseHeaders.putSingle(HttpHeaders.CONTENT_TYPE, initialResponseContentType);
                }
            } else {
                message.put(Message.CONTENT_TYPE, customContentType.toString());
            }
        }
        message.put(Message.PROTOCOL_HEADERS, responseHeaders);
        setResponseDate(responseHeaders, firstTry);
        return responseHeaders;
    }

    private MediaType getResponseMediaType(Object mediaTypeHeader) {
        MediaType responseMediaType;
        if (mediaTypeHeader instanceof MediaType) {
            responseMediaType = (MediaType)mediaTypeHeader;
        } else {
            responseMediaType = mediaTypeHeader == null ? MediaType.WILDCARD_TYPE
                : JAXRSUtils.toMediaType(mediaTypeHeader.toString());
        }
        return responseMediaType;
    }

    private int getActualStatus(int status, Object responseObj) {
        if (status == -1) {
            return responseObj == null ? 204 : 200;
        }
        return status;
    }

    private boolean checkBufferingMode(Message m, List<WriterInterceptor> writers, boolean firstTry) {
        if (!firstTry) {
            return false;
        }
        WriterInterceptor last = writers.get(writers.size() - 1);
        MessageBodyWriter<Object> w = ((WriterInterceptorMBW)last).getMBW();
        Object outBuf = m.getContextualProperty(OUT_BUFFERING);
        boolean enabled = PropertyUtils.isTrue(outBuf);
        boolean configurableProvider = w instanceof AbstractConfigurableProvider;
        if (!enabled && outBuf == null && configurableProvider) {
            enabled = ((AbstractConfigurableProvider)w).getEnableBuffering();
        }
        if (enabled) {
            boolean streamingOn = configurableProvider
                && ((AbstractConfigurableProvider)w).getEnableStreaming();
            if (streamingOn) {
                m.setContent(XMLStreamWriter.class, new CachingXmlEventWriter());
            } else {
                m.setContent(OutputStream.class, new CachedOutputStream());
            }
        }
        return enabled;
    }

    private void checkCachedStream(Message m, OutputStream osOriginal, boolean enabled) throws Exception {
        XMLStreamWriter writer = null;
        if (enabled) {
            writer = m.getContent(XMLStreamWriter.class);
        } else {
            writer = (XMLStreamWriter)m.get(XMLStreamWriter.class.getName());
        }
        if (writer instanceof CachingXmlEventWriter) {
            CachingXmlEventWriter cache = (CachingXmlEventWriter)writer;
            if (cache.getEvents().size() != 0) {
                XMLStreamWriter origWriter = null;
                try {
                    origWriter = StaxUtils.createXMLStreamWriter(osOriginal);
                    for (XMLEvent event : cache.getEvents()) {
                        StaxUtils.writeEvent(event, origWriter);
                    }
                } finally {
                    StaxUtils.close(origWriter);
                }
            }
            m.setContent(XMLStreamWriter.class, null);
            return;
        }
        if (enabled) {
            OutputStream os = m.getContent(OutputStream.class);
            if (os != osOriginal && os instanceof CachedOutputStream) {
                CachedOutputStream cos = (CachedOutputStream)os;
                if (cos.size() != 0) {
                    cos.writeCacheTo(osOriginal);
                }
            }
        }
    }

    private void logWriteError(boolean firstTry, Class<?> cls, MediaType ct) {
        if (firstTry) {
            JAXRSUtils.logMessageHandlerProblem("MSG_WRITER_PROBLEM", cls, ct);
        }
    }

    private void handleWriteException(ServerProviderFactory pf,
                                      Message message,
                                      Throwable ex,
                                      boolean firstTry) {
        Response excResponse = null;
        if (firstTry) {
            excResponse = JAXRSUtils.convertFaultToResponse(ex, message);
        } else {
            message.getExchange().put(JAXRSUtils.SECOND_JAXRS_EXCEPTION, Boolean.TRUE);
        }
        if (excResponse == null) {
            setResponseStatus(message, 500);
            throw new Fault(ex);
        }
        serializeMessage(pf, message, excResponse, null, false);

    }


    private void writeResponseErrorMessage(Message message, OutputStream out,
                                           String name, Class<?> cls, MediaType ct) {
        message.put(Message.CONTENT_TYPE, "text/plain");
        message.put(Message.RESPONSE_CODE, 500);
        try {
            String errorMessage = JAXRSUtils.logMessageHandlerProblem(name, cls, ct);
            if (out != null) {
                out.write(errorMessage.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException another) {
            // ignore
        }
    }


    private MediaType checkFinalContentType(MediaType mt, List<WriterInterceptor> writers, boolean checkWriters) {
        if (checkWriters) {
            int mbwIndex = writers.size() == 1 ? 0 : writers.size() - 1;
            MessageBodyWriter<Object> writer = ((WriterInterceptorMBW)writers.get(mbwIndex)).getMBW();
            Produces pm = writer.getClass().getAnnotation(Produces.class);
            if (pm != null) {
                List<MediaType> sorted =
                    JAXRSUtils.sortMediaTypes(JAXRSUtils.getMediaTypes(pm.value()), JAXRSUtils.MEDIA_TYPE_QS_PARAM);
                mt = JAXRSUtils.intersectMimeTypes(sorted, mt).get(0);
            }
        }
        if (mt.isWildcardType() || mt.isWildcardSubtype()) {
            if ("application".equals(mt.getType()) || mt.isWildcardType()) {
                mt = MediaType.APPLICATION_OCTET_STREAM_TYPE;
            } else {
                throw ExceptionUtils.toNotAcceptableException(null,  null);
            }
        }
        return mt;
    }

    private void setResponseDate(MultivaluedMap<String, Object> headers, boolean firstTry) {
        if (!firstTry || headers.containsKey(HttpHeaders.DATE)) {
            return;
        }
// Liberty Change for CXF Begin
//        SimpleDateFormat format = HttpUtils.getHttpDateFormat();
        headers.putSingle(HttpHeaders.DATE, CachedTime.getCachedTime().getTimeAsString(-1));
//Liberty Change for CXF End
    }

    private boolean isResponseAlreadyHandled(Message m) {
        return isResponseAlreadyCommited(m) || isResponseRedirected(m);
    }

    private boolean isResponseAlreadyCommited(Message m) {
        return Boolean.TRUE.equals(m.getExchange().get(AbstractHTTPDestination.RESPONSE_COMMITED));
    }

    private boolean isResponseRedirected(Message m) {
        return Boolean.TRUE.equals(m.getExchange().get(AbstractHTTPDestination.REQUEST_REDIRECTED));
    }

    private void writeResponseToStream(OutputStream os, Object responseObj) {
        try {
            byte[] bytes = responseObj.toString().getBytes(StandardCharsets.UTF_8);
            os.write(bytes, 0, bytes.length);
        } catch (Exception ex) {
            Tr.error(tc, "Problem with writing the data to the output stream");
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    private void setResponseStatus(Message message, int status) {
        message.put(Message.RESPONSE_CODE, status);
        boolean responseHeadersCopied = isResponseHeadersCopied(message);
        if (responseHeadersCopied) {
            HttpServletResponse response =
                (HttpServletResponse)message.get(AbstractHTTPDestination.HTTP_RESPONSE);
            response.setStatus(status);
        }
    }

    // Some CXF interceptors such as FIStaxOutInterceptor will indirectly initiate
    // an early copying of response code and headers into the HttpServletResponse
    // TODO : Pushing the filter processing and copying response headers into say
    // PRE-LOGICAl and PREPARE_SEND interceptors will most likely be a good thing
    // however JAX-RS MessageBodyWriters are also allowed to add response headers
    // which is reason why a MultipartMap parameter in MessageBodyWriter.writeTo
    // method is modifiable. Thus we do need to know if the initial copy has already
    // occurred: for now we will just use to ensure the correct status is set
    private boolean isResponseHeadersCopied(Message message) {
        return PropertyUtils.isTrue(message.get(AbstractHTTPDestination.RESPONSE_HEADERS_COPIED));
    }

    @Override
    public void handleFault(Message message) {
        // complete
    }
}
