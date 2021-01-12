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

package org.apache.cxf.message;

import java.util.Collection;
import java.util.Set;

import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.transport.Destination;

/**
 * The base interface for all all message implementations.
 * All message objects passed to interceptors use this interface.
 */
public interface Message extends StringMap {

    String TRANSPORT = "org.apache.cxf.transport";

    /*
     * Boolean property which can be used to check that the current request
     * is part of the SOAP (JAX-WS) or non-SOAP/REST (JAX-RS) execution context.
     */
    String REST_MESSAGE = "org.apache.cxf.rest.message";

    /**
     * Boolean property specifying if the message is a request message.
     */
    String REQUESTOR_ROLE = "org.apache.cxf.client";

    /**
     * Boolean property specifying if the message is inbound.
     */
    String INBOUND_MESSAGE = "org.apache.cxf.message.inbound";

    /**
     * A Map keyed by a string that stores optional context information
     * associated with the invocation that spawned the message.
     */
    String INVOCATION_CONTEXT = "org.apache.cxf.invocation.context";

    /**
     *  Current Service Object
     */
    String SERVICE_OBJECT = "org.apache.cxf.service.object";

    /**
     * A Map containing the MIME headers for a SOAP message.
     */
    String MIME_HEADERS = "org.apache.cxf.mime.headers";

    /**
     * Boolean property specifying if the server should send the response
     * asynchronously.
     */
    String ASYNC_POST_RESPONSE_DISPATCH =
        "org.apache.cxf.async.post.response.dispatch";

    /**
     * Boolean property specifying if this message arrived via a
     * decoupled endpoint.
     */
    String DECOUPLED_CHANNEL_MESSAGE = "decoupled.channel.message";
    String PARTIAL_RESPONSE_MESSAGE = "org.apache.cxf.partial.response";
    String EMPTY_PARTIAL_RESPONSE_MESSAGE = "org.apache.cxf.partial.response.empty";
    String ONE_WAY_REQUEST = "OnewayRequest";

    /**
     * Boolean property specifying if oneWay response must be processed.
     */
    String PROCESS_ONEWAY_RESPONSE = "org.apache.cxf.transport.processOneWayResponse";

    
    /**
     * Boolean property specifying if 202 response is partial/oneway response.
     * Default value is true
     */
    String PROCESS_202_RESPONSE_ONEWAY_OR_PARTIAL = "org.apache.cxf.transport.process202Response";

    /**
     * Boolean property specifying if the thread which runs a request is
     * different to the thread which created this Message.
     */
    String THREAD_CONTEXT_SWITCHED = "thread.context.switched";


    String ROBUST_ONEWAY = "org.apache.cxf.oneway.robust";

    String HTTP_REQUEST_METHOD = "org.apache.cxf.request.method";
    String REQUEST_URI = "org.apache.cxf.request.uri";
    String REQUEST_URL = "org.apache.cxf.request.url";

    String PROTOCOL_HEADERS = Message.class.getName() + ".PROTOCOL_HEADERS";
    String RESPONSE_CODE = Message.class.getName() + ".RESPONSE_CODE";
    String ERROR_MESSAGE = Message.class.getName() + ".ERROR_MESSAGE";
    String ENDPOINT_ADDRESS = Message.class.getName() + ".ENDPOINT_ADDRESS";
    String PATH_INFO = Message.class.getName() + ".PATH_INFO";
    String QUERY_STRING = Message.class.getName() + ".QUERY_STRING";

    String PROPOGATE_EXCEPTION = Message.class.getName() + ".PROPOGATE_EXCEPTION";
    /**
     * Boolean property specifying in the runtime is configured to process
     * MTOM attachments.
     */
    String MTOM_ENABLED = "mtom-enabled";
    String MTOM_THRESHOLD = "mtom-threshold";

    /**
     * Runtime schema validation property
     */
    String SCHEMA_VALIDATION_ENABLED = "schema-validation-enabled";

    /**
     * The default values for schema validation will be set in the service model using this property
     */
    String SCHEMA_VALIDATION_TYPE = "schema-validation-type";

    /**
     * Boolean property specifying if the Java stack trace is returned as a
     * SOAP fault message.
     */
    String FAULT_STACKTRACE_ENABLED = "faultStackTraceEnabled";
    /**
     * Boolean property specifying if the name of the exception that caused
     * the Java stack trace is returned.
     */
    String EXCEPTION_MESSAGE_CAUSE_ENABLED = "exceptionMessageCauseEnabled";

    /**
     * A very unique delimiter used for exception with FAULT_STACKTRACE_ENABLED enable,
     * which is easy for client to differentiate the cause and stacktrace when unmarsall
     * a fault message
     */
    String EXCEPTION_CAUSE_SUFFIX = "#*#";

    String CONTENT_TYPE = "Content-Type";
    String ACCEPT_CONTENT_TYPE = "Accept";
    String BASE_PATH = Message.class.getName() + ".BASE_PATH";
    String ENCODING = Message.class.getName() + ".ENCODING";
    String FIXED_PARAMETER_ORDER = Message.class.getName() + ".FIXED_PARAMETER_ORDER";
    String MAINTAIN_SESSION = Message.class.getName() + ".MAINTAIN_SESSION";
    String ATTACHMENTS = Message.class.getName() + ".ATTACHMENTS";

    String WSDL_DESCRIPTION = "javax.xml.ws.wsdl.description";
    String WSDL_SERVICE = "javax.xml.ws.wsdl.service";
    String WSDL_PORT = "javax.xml.ws.wsdl.port";
    String WSDL_INTERFACE = "javax.xml.ws.wsdl.interface";
    String WSDL_OPERATION = "javax.xml.ws.wsdl.operation";

    /**
     * Some properties to allow adding interceptors to the chain
     * on a per-request basis.  All are a Collection<Interceptor>
     * These are NOT contextual properties (ie: not searched outside the message).
     * They must exist on the message itself at time of Chain creation
     */
    String IN_INTERCEPTORS = Message.class.getName() + ".IN_INTERCEPTORS";
    String OUT_INTERCEPTORS = Message.class.getName() + ".OUT_INTERCEPTORS";
    String FAULT_IN_INTERCEPTORS = Message.class.getName() + ".FAULT_IN_INTERCEPTORS";
    String FAULT_OUT_INTERCEPTORS = Message.class.getName() + ".FAULT_OUT_INTERCEPTORS";
    /**
     * As above, but Collection<InterceptorProvider>
     */
    String INTERCEPTOR_PROVIDERS = Message.class.getName() + ".INTERCEPTOR_PROVIDER";

    /**
     * Content-Transfer-Encoding used for MTOM attachment
     * binary, base64, etc
     */
    String CONTENT_TRANSFER_ENCODING = Message.class.getName() + ".CONTENT_TRANSFER_ENCODING";

    /*
     * The properties to allow configure the client timeout
     */
    String CONNECTION_TIMEOUT = "javax.xml.ws.client.connectionTimeout";
    String RECEIVE_TIMEOUT = "javax.xml.ws.client.receiveTimeout";

    /**
     * Boolean property to indicate whether application-defined StAX-factories (stored as contextual property in the
     * message) are thread-safe. If set to {@code true}, CXF doesn't synchronize accesses to the factories.
     */
    String THREAD_SAFE_STAX_FACTORIES = Message.class.getName() + ".THREAD_SAFE_STAX_FACTORIES";

    String getId();
    void setId(String id);

    /**
     * Returns a live copy of the messages interceptor chain. This is
     * useful when an interceptor wants to modify the interceptor chain on the
     * fly.
     *
     * @return the interceptor chain used to process the message
     */
    InterceptorChain getInterceptorChain();
    void setInterceptorChain(InterceptorChain chain);

    /**
     * @return the associated Destination if message is inbound, null otherwise
     */
    Destination getDestination();

    Exchange getExchange();

    void setExchange(Exchange exchange);

    /**
     * Retrieve any binary attachments associated with the message.
     *
     * @return a collection containing the attachments
     */
    Collection<Attachment> getAttachments();

    void setAttachments(Collection<Attachment> attachments);

    /**
     * Retrieve the encapsulated content as a particular type. The content is
     * available as a result type if the message is outbound. The content
     * is available as a source type if message is inbound. If the content is
     * not available as the specified type null is returned.
     *
     * @param format the expected content format
     * @return the encapsulated content
     */
    <T> T getContent(Class<T> format);

    /**
     * Provide the encapsulated content as a particular type (a result type
     * if message is outbound, a source type if message is inbound)
     *
     * @param format the provided content format
     * @param content the content to be encapsulated
     */
    <T> void setContent(Class<T> format, Object content);

    /**
     * @return the set of currently encapsulated content formats
     */
    Set<Class<?>> getContentFormats();

    /**
     * Removes a content from a message.  If some contents are completely consumed,
     * removing them is a good idea
     * @param format the format to remove
     */
    <T> void removeContent(Class<T> format);

    /**
     * Queries the Message object's metadata for a specific property.
     *
     * @param key the Message interface's property strings that
     * correlates to the desired property
     * @return the property's value
     */
    Object getContextualProperty(String key);

    /**
     * Resets the cache of contextual properties that messages may contain.  Subsequent
     * calls to getContextualProperty will likely recalculate the cache.
     */
    void resetContextCache();

    /**
     * @return set of defined contextual property keys
     */
    Set<String> getContextualPropertyKeys();
}
