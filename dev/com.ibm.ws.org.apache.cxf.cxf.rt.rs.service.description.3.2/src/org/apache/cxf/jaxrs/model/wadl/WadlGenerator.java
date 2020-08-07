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
package org.apache.cxf.jaxrs.model.wadl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.servlet.ServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.jaxb.JAXBBeanInfo;
import org.apache.cxf.common.jaxb.JAXBContextProxy;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.XmlSchemaPrimitiveUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.ext.Oneway;
import org.apache.cxf.jaxrs.ext.ResponseStatus;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.cxf.jaxrs.ext.xml.XMLName;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.ResourceTypes;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.model.doc.DocumentationProvider;
import org.apache.cxf.jaxrs.model.doc.JavaDocProvider;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.jaxrs.utils.schemas.SchemaHandler;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.DelegatingXMLStreamWriter;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.constants.Constants;

public class WadlGenerator implements ContainerRequestFilter {

    public static final String WADL_QUERY = "_wadl";
    public static final MediaType WADL_TYPE = JAXRSUtils.toMediaType("application/vnd.sun.wadl+xml");
    public static final String WADL_NS = "http://wadl.dev.java.net/2009/02";
    public static final String DEFAULT_WADL_SCHEMA_LOC = "http://www.w3.org/Submission/wadl/wadl.xsd";

    private static final Logger LOG = LogUtils.getL7dLogger(WadlGenerator.class);
    private static final String CONVERT_WADL_RESOURCES_TO_DOM = "convert.wadl.resources.to.dom";
    private static final String XLS_NS = "http://www.w3.org/1999/XSL/Transform";
    private static final String JAXB_DEFAULT_NAMESPACE = "##default";
    private static final String JAXB_DEFAULT_NAME = "##default";
    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String DEFAULT_NS_PREFIX = "prefix";
    private static final Map<ParameterType, Class<? extends Annotation>> PARAMETER_TYPE_MAP;
    static {
        PARAMETER_TYPE_MAP = new HashMap<>();
        PARAMETER_TYPE_MAP.put(ParameterType.FORM, FormParam.class);
        PARAMETER_TYPE_MAP.put(ParameterType.QUERY, QueryParam.class);
        PARAMETER_TYPE_MAP.put(ParameterType.HEADER, HeaderParam.class);
        PARAMETER_TYPE_MAP.put(ParameterType.PATH, PathParam.class);
        PARAMETER_TYPE_MAP.put(ParameterType.MATRIX, MatrixParam.class);
    }

    private String wadlNamespace;

    private boolean singleResourceMultipleMethods = true;
    private boolean useSingleSlashResource;
    private boolean ignoreForwardSlash;
    private boolean linkAnyMediaTypeToXmlSchema;
    private boolean useJaxbContextForQnames = true;
    private boolean supportCollections = true;
    private boolean supportJaxbXmlType = true;
    private boolean supportJaxbSubstitutions = true;
    private boolean ignoreOverloadedMethods;
    private boolean checkAbsolutePathSlash;
    private boolean keepRelativeDocLinks;
    private boolean usePathParamsToCompareOperations = true;
    private boolean incrementNamespacePrefix = true;

    private boolean ignoreMessageWriters = true;
    private boolean ignoreRequests;
    private boolean convertResourcesToDOM = true;
    private String wadlSchemaLocation;
    private List<String> externalSchemasCache;
    private List<URI> externalSchemaLinks;
    private Map<String, List<String>> externalQnamesMap;

    private final ConcurrentHashMap<String, String> docLocationMap = new ConcurrentHashMap<>();

    private ElementQNameResolver resolver;
    private List<String> privateAddresses;
    private List<String> allowList; // Liberty change
    private String applicationTitle;
    private String nsPrefix = DEFAULT_NS_PREFIX;
    private MediaType defaultWadlResponseMediaType = MediaType.APPLICATION_XML_TYPE;
    private final MediaType defaultRepMediaType = MediaType.WILDCARD_TYPE;
    private String stylesheetReference;
    private boolean applyStylesheetLocally;
    private Bus bus;
    private final List<DocumentationProvider> docProviders = new LinkedList<>();
    private ResourceIdGenerator idGenerator;
    private Map<String, Object> jaxbContextProperties;

    public WadlGenerator() {
    }

    public WadlGenerator(Bus bus) {
        this.bus = bus;
        this.bus.setProperty("wadl.service.description.available", "true");
    }

    @Override
    public void filter(ContainerRequestContext context) {
        Message m = JAXRSUtils.getCurrentMessage();
        if (m == null) {
            return;
        }
        doFilter(context, m);
    }

    protected void doFilter(ContainerRequestContext context, Message m) {
        //Liberty code change start
        if (!"GET".equals(((MessageImpl) m).getHttpRequestMethod())) {
            //Liberty code change end
            return;
        }

        UriInfo ui = context.getUriInfo();
        if (!ui.getQueryParameters().containsKey(WADL_QUERY)) {
            if (stylesheetReference != null || !docLocationMap.isEmpty()) {
                String path = ui.getPath(false);
                if (path.startsWith("/") && path.length() > 0) {
                    path = path.substring(1);
                }
                if (stylesheetReference != null && path.endsWith(".xsl")
                    || docLocationMap.containsKey(path)) {
                    context.abortWith(getExistingResource(m, ui, path));
                }
            }
            return;
        }

        if (ignoreRequests) {
            context.abortWith(Response.status(404).build());
            return;
        }

        if (allowList != null && !allowList.isEmpty()) { //Liberty change
            ServletRequest servletRequest = (ServletRequest)m.getContextualProperty(
                "HTTP.REQUEST");
            String remoteAddress = null;
            if (servletRequest != null) {
                remoteAddress = servletRequest.getRemoteAddr();
            } else {
                remoteAddress = "";
            }
            boolean foundMatch = false;
            for (String addr : allowList) { //Liberty change
                if (addr.equals(remoteAddress)) {
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) {
                context.abortWith(Response.status(404).build());
                return;
            }
        }

        HttpHeaders headers = new HttpHeadersImpl(m);
        List<MediaType> accepts = headers.getAcceptableMediaTypes();
        MediaType type = accepts.contains(WADL_TYPE) ? WADL_TYPE : accepts
            .contains(MediaType.APPLICATION_JSON_TYPE) ? MediaType.APPLICATION_JSON_TYPE
                : defaultWadlResponseMediaType;

        Response response = getExistingWadl(m, ui, type);
        if (response != null) {
            context.abortWith(response);
            return;
        }

        boolean isJson = isJson(type);

        StringBuilder sbMain = generateWADL(getBaseURI(m, ui), getResourcesList(m, ui), isJson, m, ui);

        m.getExchange().put(JAXRSUtils.IGNORE_MESSAGE_WRITERS, !isJson && ignoreMessageWriters);
        Response r = Response.ok().type(type).entity(createResponseEntity(m, ui, sbMain.toString(), isJson)).build();
        context.abortWith(r);
    }
    private boolean isJson(MediaType mt) {
        return mt == MediaType.APPLICATION_JSON_TYPE;
    }
    private String getStylesheetInstructionData(String baseURI) {
        String theStylesheetReference = stylesheetReference;
        if (!keepRelativeDocLinks) {
            theStylesheetReference = UriBuilder.fromUri(baseURI)
                .path(theStylesheetReference).build().toString();
        }
        return "type=\"text/xsl\" href=\"" + theStylesheetReference + "\"";
    }
    public StringBuilder generateWADL(String baseURI,
                                       List<ClassResourceInfo> cris,
                                       boolean isJson,
                                       Message m,
                                       UriInfo ui) {
        StringBuilder sbMain = new StringBuilder(64);
        if (!isJson && stylesheetReference != null && !applyStylesheetLocally) {
            sbMain.append("<?xml-stylesheet ").append(getStylesheetInstructionData(baseURI)).append("?>");
        }
        sbMain.append("<application");
        if (!isJson) {
            sbMain.append(" xmlns=\"").append(getNamespace()).append("\" xmlns:xs=\"")
                .append(Constants.URI_2001_SCHEMA_XSD).append('"');
        }
        StringBuilder sbGrammars = new StringBuilder(32);
        sbGrammars.append("<grammars>");

        StringBuilder sbResources = new StringBuilder(64);
        sbResources.append("<resources base=\"").append(baseURI).append("\">");


        MessageBodyWriter<?> jaxbWriter = (m != null && useJaxbContextForQnames)
            ? ServerProviderFactory.getInstance(m).getDefaultJaxbWriter() : null;
        ResourceTypes resourceTypes = ResourceUtils.getAllRequestResponseTypes(cris,
                                                                               useJaxbContextForQnames,
                                                                               jaxbWriter);
        checkXmlSeeAlso(resourceTypes);
        Set<Class<?>> allTypes = resourceTypes.getAllTypes().keySet();


        JAXBContext jaxbContext = null;
        if (useJaxbContextForQnames && !allTypes.isEmpty()) {
            jaxbContext = org.apache.cxf.jaxrs.utils.JAXBUtils
                    .createJaxbContext(new HashSet<>(allTypes), null, jaxbContextProperties);
            if (jaxbContext == null) {
                LOG.warning("JAXB Context is null: possibly due to one of input classes being not accepted");
            }
        }

        SchemaWriter schemaWriter = createSchemaWriter(resourceTypes, jaxbContext, ui);
        ElementQNameResolver qnameResolver = schemaWriter == null
            ? null : createElementQNameResolver(jaxbContext);

        Map<Class<?>, QName> clsMap = new IdentityHashMap<>();
        Set<ClassResourceInfo> visitedResources = new LinkedHashSet<>();
        for (ClassResourceInfo cri : cris) {
            startResourceTag(sbResources, cri, cri.getURITemplate().getValue());

            Annotation description = AnnotationUtils.getClassAnnotation(cri.getServiceClass(), Description.class);
            if (description == null) {
                description = AnnotationUtils.getClassAnnotation(cri.getServiceClass(), Descriptions.class);
            }
            if (description != null) {
                handleDocs(new Annotation[] {description}, sbResources, DocTarget.RESOURCE, true, isJson);
            } else {
                handleClassJavaDocs(cri, sbResources);
            }
            handleResource(sbResources, allTypes, qnameResolver, clsMap, cri, visitedResources, isJson);
            sbResources.append("</resource>");
        }
        sbResources.append("</resources>");

        handleGrammars(sbMain, sbGrammars, schemaWriter, clsMap);

        sbGrammars.append("</grammars>");
        sbMain.append('>');
        handleApplicationDocs(sbMain);
        sbMain.append(sbGrammars.toString());
        sbMain.append(sbResources.toString());
        sbMain.append("</application>");
        return sbMain;
    }

    private Object createResponseEntity(Message m, UriInfo ui, String entity, boolean isJson) {
        try {
            if (!isJson) {
                if (stylesheetReference != null && applyStylesheetLocally) {
                    return transformLocally(m, ui, new StreamSource(new StringReader(entity)));
                }
                return entity;
            }
            return StaxUtils.read(new StringReader(entity));
        } catch (Exception ex) {
            throw ExceptionUtils.toInternalServerErrorException(ex, null);
        }
    }

    protected String getBaseURI(Message m, UriInfo ui) {
        EndpointInfo ei = m.getExchange().getEndpoint().getEndpointInfo();
        String publishedEndpointUrl = (String)ei.getProperty("publishedEndpointUrl");
        if (publishedEndpointUrl == null) {
            return ui.getBaseUri().toString();
        }
        return publishedEndpointUrl;
    }

    protected void handleGrammars(StringBuilder sbApp, StringBuilder sbGrammars, SchemaWriter writer,
                                  Map<Class<?>, QName> clsMap) {
        if (writer == null) {
            return;
        }

        Map<String, String> map = new HashMap<>();
        for (QName qname : clsMap.values()) {
            map.put(qname.getPrefix(), qname.getNamespaceURI());
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sbApp.append(" xmlns:").append(entry.getKey()).append("=\"").append(entry.getValue())
                .append('"');
        }

        if (wadlSchemaLocation != null) {
            sbApp.append(" xmlns:xsi=\"").append(Constants.URI_2001_SCHEMA_XSI).append('"');
            sbApp.append(" xsi:schemaLocation=\"")
                 .append(getNamespace()).append(' ').append(wadlSchemaLocation)
                 .append('"');
        }

        writer.write(sbGrammars);
    }

    protected void handleResource(StringBuilder sb, Set<Class<?>> jaxbTypes,
                                  ElementQNameResolver qnameResolver, Map<Class<?>, QName> clsMap,
                                  ClassResourceInfo cri, Set<ClassResourceInfo> visitedResources,
                                  boolean isJson) {
        visitedResources.add(cri);
        Map<Parameter, Object> classParams = getClassParameters(cri);

        List<OperationResourceInfo> sortedOps = sortOperationsByPath(cri.getMethodDispatcher()
            .getOperationResourceInfos());

        boolean resourceTagOpened = false;
        for (int i = 0; i < sortedOps.size(); i++) {
            OperationResourceInfo ori = sortedOps.get(i);
            if (i > 0 && ignoreOverloadedMethods
                && ori.getMethodToInvoke().getName().equals(sortedOps.get(i - 1).getMethodToInvoke().getName())) {
                continue;
            }
            if (ori.getHttpMethod() == null) {
                Class<?> cls = getMethod(ori).getReturnType();
                ClassResourceInfo subcri = cri.findResource(cls, cls);
                if (subcri != null && !visitedResources.contains(subcri)) {
                    startResourceTag(sb, subcri, ori.getURITemplate().getValue());
                    handleDocs(subcri.getServiceClass().getAnnotations(), sb, DocTarget.RESOURCE, true,
                               isJson);
                    handlePathAndMatrixParams(sb, ori, isJson);
                    handleResource(sb, jaxbTypes, qnameResolver, clsMap, subcri, visitedResources, isJson);
                    sb.append("</resource>");
                } else {
                    handleDynamicSubresource(sb, jaxbTypes, qnameResolver, clsMap, ori, subcri, isJson);
                }
                continue;
            }
            OperationResourceInfo nextOp = i + 1 < sortedOps.size() ? sortedOps.get(i + 1) : null;
            resourceTagOpened = handleOperation(sb, jaxbTypes, qnameResolver, clsMap, ori, classParams,
                                                nextOp, resourceTagOpened, isJson, i);
        }
    }

    private Map<Parameter, Object> getClassParameters(ClassResourceInfo cri) {
        Map<Parameter, Object> classParams = new LinkedHashMap<>();
        List<Method> paramMethods = cri.getParameterMethods();
        for (Method m : paramMethods) {
            classParams.put(ResourceUtils.getParameter(0, m.getAnnotations(), m.getParameterTypes()[0]), m);
        }
        List<Field> fieldParams = cri.getParameterFields();
        for (Field f : fieldParams) {
            classParams.put(ResourceUtils.getParameter(0, f.getAnnotations(), f.getType()), f);
        }
        return classParams;
    }

    protected void startResourceTag(StringBuilder sb, ClassResourceInfo cri, String path) {
        sb.append("<resource path=\"").append(getPath(path)).append('"');
        if (idGenerator != null) {
            String id = idGenerator.getClassResourceId(cri);
            sb.append(" id=\"").append(id).append('"');
        }
        sb.append('>');
    }

    protected String getPath(String path) {
        String thePath;
        if (ignoreForwardSlash && path.startsWith("/") && path.length() > 0) {
            thePath = path.substring(1);
        } else {
            thePath = path;
        }

        return xmlEncodeIfNeeded(thePath);
    }

    private void checkXmlSeeAlso(ResourceTypes resourceTypes) {
        if (!this.useJaxbContextForQnames) {
            return;
        }
        List<Class<?>> extraClasses = new LinkedList<>();
        for (Class<?> cls : resourceTypes.getAllTypes().keySet()) {
            if (!isXmlRoot(cls) || Modifier.isAbstract(cls.getModifiers())) {
                XmlSeeAlso seeAlsoAnn = cls.getAnnotation(XmlSeeAlso.class);
                if (seeAlsoAnn != null) {
                    List<Class<?>> seeAlsoList = CastUtils.cast(Arrays.asList(seeAlsoAnn.value()));
                    if (this.supportJaxbSubstitutions) {
                        for (Class<?> seeAlsoCls : seeAlsoList) {
                            resourceTypes.getSubstitutions().put(seeAlsoCls, cls);
                        }
                    }
                    extraClasses.addAll(seeAlsoList);
                }
            }
        }
        for (Class<?> cls : extraClasses) {
            resourceTypes.getAllTypes().put(cls, cls);
        }
    }

    private String xmlEncodeIfNeeded(String value) {

        StringBuilder builder = new StringBuilder(value.length());
        boolean change = false;
        for (int x = 0; x < value.length(); x++) {
            char ch = value.charAt(x);
            String ap = null;
            switch (ch) {
            case '\"':
                ap = "&quot;";
                break;
            case '\'':
                ap = "&apos;";
                break;
            case '<':
                ap = "&lt;";
                break;
            case '>':
                ap = "&gt;";
                break;
            case '&':
                ap = "&amp;";
                break;
            default:
                ap = null;
            }
            if (ap != null) {
                change = true;
                builder.append(ap);
            } else {
                builder.append(ch);
            }
        }
        return change ? builder.toString() : value;
    }

    protected void startMethodTag(StringBuilder sb, OperationResourceInfo ori) {
        sb.append("<method name=\"").append(ori.getHttpMethod()).append('"');
        if (idGenerator != null) {
            String id = idGenerator.getMethodResourceId(ori);
            sb.append(" id=\"").append(id).append('"');
        }
        sb.append('>');
    }
    protected void endMethodTag(StringBuilder sb, OperationResourceInfo ori) {
        sb.append("</method>");
    }
    protected void startMethodRequestTag(StringBuilder sb, OperationResourceInfo ori) {
        sb.append("<request>");
    }
    protected void startMethodResponseTag(StringBuilder sb, OperationResourceInfo ori) {
        sb.append("<response");
    }
    protected void endMethodRequestTag(StringBuilder sb, OperationResourceInfo ori) {
        sb.append("</request>");
    }
    protected void endMethodResponseTag(StringBuilder sb, OperationResourceInfo ori) {
        sb.append("</response>");
    }
    protected void startResourceTag(StringBuilder sb, OperationResourceInfo ori, String path) {
        sb.append("<resource path=\"").append(path).append("\">");
    }
    protected void endResourceTag(StringBuilder sb, OperationResourceInfo ori) {
        sb.append("</resource>");
    }

    // CHECKSTYLE:OFF
    protected boolean handleOperation(StringBuilder sb, Set<Class<?>> jaxbTypes, //NOPMD
                                      ElementQNameResolver qnameResolver, Map<Class<?>, QName> clsMap,
                                      OperationResourceInfo ori, Map<Parameter, Object> classParams,
                                      OperationResourceInfo nextOp, boolean resourceTagOpened,
                                      boolean isJson, int index) {
        Annotation[] anns = getMethod(ori).getAnnotations();
        // CHECKSTYLE:ON
        boolean samePathOperationFollows = singleResourceMultipleMethods && compareOperations(ori, nextOp);

        String path = ori.getURITemplate().getValue();
        if (!resourceTagOpened && openResource(path)) {
            resourceTagOpened = true;
            URITemplate template = ori.getClassResourceInfo().getURITemplate();
            if (template != null) {
                String parentPath = template.getValue();
                if (parentPath.endsWith("/") && path.startsWith("/") && path.length() > 1) {
                    path = path.substring(1);
                }
            }
            startResourceTag(sb, ori, getPath(path));
            handleDocs(anns, sb, DocTarget.RESOURCE, false, isJson);
            handlePathAndMatrixClassParams(ori, sb, classParams, isJson);
            handlePathAndMatrixParams(sb, ori, isJson);
        } else if (index == 0) {
            handlePathAndMatrixClassParams(ori, sb, classParams, isJson);
            handlePathAndMatrixParams(sb, ori, isJson);
        }

        startMethodTag(sb, ori);
        if (!handleDocs(anns, sb, DocTarget.METHOD, true, isJson)) {
            handleOperJavaDocs(ori, sb);
        }
        int numOfParams = getMethod(ori).getParameterTypes().length;
        if ((numOfParams > 1 || numOfParams == 1 && !ori.isAsync()) || !classParams.isEmpty()) {

            startMethodRequestTag(sb, ori);
            handleDocs(anns, sb, DocTarget.REQUEST, false, isJson);

            boolean isForm = isFormRequest(ori);

            doHandleClassParams(ori, sb, classParams, isJson, ParameterType.QUERY, ParameterType.HEADER);
            doHandleJaxrsBeanParamClassParams(ori, sb, classParams, isJson,
                                              ParameterType.QUERY, ParameterType.HEADER);
            for (Parameter p : ori.getParameters()) {
                if (isForm && p.getType() == ParameterType.REQUEST_BODY) {
                    continue;
                }
                handleParameter(sb, jaxbTypes, qnameResolver, clsMap, ori, p, isJson);
            }
            if (isForm) {
                handleFormRepresentation(sb, jaxbTypes, qnameResolver, clsMap, ori, getFormClass(ori), isJson);
            }
            endMethodRequestTag(sb, ori);
        }
        startMethodResponseTag(sb, ori);
        Class<?> returnType = getMethod(ori).getReturnType();
        boolean isVoid = void.class == returnType && !ori.isAsync();
        ResponseStatus responseStatus = getMethod(ori).getAnnotation(ResponseStatus.class);
        if (responseStatus != null) {
            setResponseStatus(sb, responseStatus.value());
        } else if (isVoid) {
            boolean oneway = getMethod(ori).getAnnotation(Oneway.class) != null;
            setResponseStatus(sb, oneway ? Response.Status.ACCEPTED : Response.Status.NO_CONTENT);
        }
        sb.append('>');
        handleDocs(anns, sb, DocTarget.RESPONSE, false, isJson);
        if (!isVoid) {
            handleRepresentation(sb, jaxbTypes, qnameResolver, clsMap, ori, returnType, isJson, false);
        }
        endMethodResponseTag(sb, ori);

        endMethodTag(sb, ori);

        if (resourceTagOpened && !samePathOperationFollows) {
            endResourceTag(sb, ori);
            resourceTagOpened = false;
        }
        return resourceTagOpened;
    }

    private void setResponseStatus(StringBuilder sb, Response.Status... statuses) {
        sb.append(" status=\"");
        for (int i = 0; i < statuses.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(statuses[i].getStatusCode());
        }
        sb.append('"');

    }

    protected boolean compareOperations(OperationResourceInfo ori1, OperationResourceInfo ori2) {
        if (ori1 == null || ori2 == null
            || !ori1.getURITemplate().getValue().equals(ori2.getURITemplate().getValue())
            || ori1.getHttpMethod() != null && ori2.getHttpMethod() == null || ori2.getHttpMethod() != null
            && ori1.getHttpMethod() == null) {
            return false;
        }
        if (usePathParamsToCompareOperations) {
            int ori1PathParams = 0;
            int ori1MatrixParams = 0;
            for (Parameter p : ori1.getParameters()) {
                if (p.getType() == ParameterType.PATH) {
                    ori1PathParams++;
                } else if (p.getType() == ParameterType.MATRIX) {
                    ori1MatrixParams++;
                }
            }

            int ori2PathParams = 0;
            int ori2MatrixParams = 0;
            for (Parameter p : ori2.getParameters()) {
                if (p.getType() == ParameterType.PATH) {
                    ori2PathParams++;
                } else if (p.getType() == ParameterType.MATRIX) {
                    ori2MatrixParams++;
                }
            }

            return ori1PathParams == ori2PathParams && ori1MatrixParams == ori2MatrixParams;
        }
        return true;
    }

    private boolean openResource(String path) {
        if ("/".equals(path)) {
            return useSingleSlashResource;
        }
        return true;
    }

    protected void handleDynamicSubresource(StringBuilder sb, Set<Class<?>> jaxbTypes,
                                            ElementQNameResolver qnameResolver, Map<Class<?>, QName> clsMap,
                                            OperationResourceInfo ori, ClassResourceInfo subcri,
                                            boolean isJson) {
        if (!isJson) {
            if (subcri != null) {
                sb.append("<!-- Recursive subresource -->");
            } else {
                sb.append("<!-- Dynamic subresource -->");
            }
        }
        startResourceTag(sb, subcri, ori.getURITemplate().getValue());
        handlePathAndMatrixParams(sb, ori, isJson);
        sb.append("</resource>");
    }

    protected void handlePathAndMatrixClassParams(OperationResourceInfo ori,
                                                  StringBuilder sb,
                                                  Map<Parameter, Object> params,
                                                  boolean isJson) {
        doHandleClassParams(ori, sb, params, isJson, ParameterType.PATH);
        doHandleClassParams(ori, sb, params, isJson, ParameterType.MATRIX);
        doHandleJaxrsBeanParamClassParams(ori, sb, params, isJson, ParameterType.PATH, ParameterType.MATRIX);
    }

    protected void doHandleClassParams(OperationResourceInfo ori,
                                       StringBuilder sb,
                                       Map<Parameter, Object> params,
                                       boolean isJson,
                                       ParameterType... pType) {
        Set<ParameterType> pTypes = new LinkedHashSet<>(Arrays.asList(pType));
        for (Map.Entry<Parameter, Object> entry : params.entrySet()) {
            Parameter pm = entry.getKey();
            Object obj = entry.getValue();
            if (pTypes.contains(pm.getType())) {
                Class<?> cls = obj instanceof Method ? ((Method)obj).getParameterTypes()[0] : ((Field)obj)
                    .getType();
                Type type = obj instanceof Method
                    ? ((Method)obj).getGenericParameterTypes()[0] : ((Field)obj).getGenericType();
                Annotation[] ann = obj instanceof Method
                    ? ((Method)obj).getParameterAnnotations()[0] : ((Field)obj).getAnnotations();
                doWriteParam(ori, sb, pm, cls, type, pm.getName(), ann, isJson);
            }
        }
    }
    protected void doHandleJaxrsBeanParamClassParams(OperationResourceInfo ori,
                                       StringBuilder sb,
                                       Map<Parameter, Object> params,
                                       boolean isJson,
                                       ParameterType... pType) {
        for (Map.Entry<Parameter, Object> entry : params.entrySet()) {
            Parameter pm = entry.getKey();
            Object obj = entry.getValue();
            if (pm.getType() == ParameterType.BEAN) {
                Class<?> cls = obj instanceof Method ? ((Method)obj).getParameterTypes()[0] : ((Field)obj)
                    .getType();
                doWriteJaxrsBeanParam(sb, ori, cls, isJson, pType);
            }
        }
    }

    protected void handlePathAndMatrixParams(StringBuilder sb, OperationResourceInfo ori, boolean isJson) {
        handleParams(sb, ori, ParameterType.PATH, isJson);
        handleParams(sb, ori, ParameterType.MATRIX, isJson);
        doWriteJaxrsBeanParams(sb, ori, isJson, ParameterType.PATH, ParameterType.MATRIX);
    }

    protected void handleParameter(StringBuilder sb, Set<Class<?>> jaxbTypes,
                                   ElementQNameResolver qnameResolver, Map<Class<?>, QName> clsMap,
                                   OperationResourceInfo ori, Parameter pm, boolean isJson) {
        Class<?> cls = getMethod(ori).getParameterTypes()[pm.getIndex()];
        if (pm.getType() == ParameterType.REQUEST_BODY && cls != AsyncResponse.class) {
            handleRepresentation(sb, jaxbTypes, qnameResolver, clsMap, ori, cls, isJson, true);
            return;
        }
        if (pm.getType() == ParameterType.PATH || pm.getType() == ParameterType.MATRIX) {
            return;
        }
        if (pm.getType() == ParameterType.HEADER || pm.getType() == ParameterType.QUERY) {
            writeParam(sb, pm, ori, isJson);
            return;

        }
        if (pm.getType() == ParameterType.BEAN) {
            doWriteJaxrsBeanParams(sb, ori, isJson, ParameterType.HEADER, ParameterType.QUERY);
        }

    }

    protected void handleParams(StringBuilder sb, OperationResourceInfo ori, ParameterType type,
                                boolean isJson) {
        for (Parameter pm : ori.getParameters()) {
            if (pm.getType() == type) {
                writeParam(sb, pm, ori, isJson);
            }
        }
    }

    private Annotation[] getBodyAnnotations(OperationResourceInfo ori, boolean inbound) {
        Method opMethod = getMethod(ori);
        if (inbound) {
            for (Parameter pm : ori.getParameters()) {
                if (pm.getType() == ParameterType.REQUEST_BODY) {
                    return opMethod.getParameterAnnotations()[pm.getIndex()];
                }
            }
            return new Annotation[] {};
        }
        return opMethod.getDeclaredAnnotations();
    }

    private void writeParam(StringBuilder sb, Parameter pm, OperationResourceInfo ori, boolean isJson) {
        Method method = getMethod(ori);
        Class<?> type = method.getParameterTypes()[pm.getIndex()];
        if (!"".equals(pm.getName())) {
            doWriteParam(ori,
                         sb,
                         pm,
                         type,
                         method.getGenericParameterTypes()[pm.getIndex()],
                         pm.getName(),
                         method.getParameterAnnotations()[pm.getIndex()],
                         isJson);
        } else {
            List<Class<?>> parentBeanClasses = new LinkedList<>();
            parentBeanClasses.add(type);
            doWriteBeanParam(ori, sb, type, pm, null, parentBeanClasses, isJson);
            parentBeanClasses.remove(type);
        }
    }
    private void doWriteJaxrsBeanParams(StringBuilder sb,
                                       OperationResourceInfo ori,
                                       boolean isJson,
                                       ParameterType ...parameterTypes) {
        for (Parameter p : ori.getParameters()) {
            if (p.getType() == ParameterType.BEAN) {
                Method method = getMethod(ori);
                Class<?> type = method.getParameterTypes()[p.getIndex()];
                doWriteJaxrsBeanParam(sb, ori, type, isJson, parameterTypes);
            }
        }
    }

    private void doWriteJaxrsBeanParam(StringBuilder sb,
                                       OperationResourceInfo ori,
                                       Class<?> beanType,
                                       boolean isJson,
                                       ParameterType ...parameterTypes) {
        for (Method m : beanType.getMethods()) {
            if (m.getName().startsWith("set")) {
                String propertyName = StringUtils.uncapitalize(m.getName().substring(3));
                Field f = InjectionUtils.getDeclaredField(beanType, propertyName);

                for (ParameterType parameterType : parameterTypes) {
                    Class<? extends Annotation> annClass = getAnnotationFromParamType(parameterType);
                    Annotation annotation = m.getAnnotation(annClass);
                    if (annotation != null) {
                        Parameter pm = new Parameter(parameterType, propertyName);
                        pm.setEncoded(m.getAnnotation(Encoded.class) != null);
                        DefaultValue dv = m.getAnnotation(DefaultValue.class);
                        if (dv != null) {
                            pm.setDefaultValue(dv.value());
                        }
                        doWriteParam(ori,
                                     sb,
                                     pm,
                                     m.getParameterTypes()[0],
                                     m.getGenericParameterTypes()[0],
                                     propertyName,
                                     new Annotation[]{},
                                     isJson);
                    } else if (f != null) {
                        annotation = f.getAnnotation(annClass);
                        if (annotation != null) {
                            Parameter pm = new Parameter(parameterType, propertyName);
                            pm.setEncoded(f.getAnnotation(Encoded.class) != null);
                            DefaultValue dv = f.getAnnotation(DefaultValue.class);
                            if (dv != null) {
                                pm.setDefaultValue(dv.value());
                            }
                            doWriteParam(ori,
                                         sb,
                                         pm,
                                         f.getType(),
                                         f.getGenericType(),
                                         propertyName,
                                         new Annotation[]{},
                                         isJson);
                        }

                    }
                }
                if (m.getAnnotation(BeanParam.class) != null) {
                    doWriteJaxrsBeanParam(sb, ori, m.getParameterTypes()[0], isJson, parameterTypes);
                } else if (f != null && f.getAnnotation(BeanParam.class) != null) {
                    doWriteJaxrsBeanParam(sb, ori, f.getType(), isJson, parameterTypes);
                }
            }
        }
    }

    private Class<? extends Annotation> getAnnotationFromParamType(ParameterType pt) {
        return PARAMETER_TYPE_MAP.get(pt);
    }

    private void doWriteBeanParam(OperationResourceInfo ori,
                                  StringBuilder sb,
                                  Class<?> type,
                                  Parameter pm,
                                  String parentName,
                                  List<Class<?>> parentBeanClasses,
                                  boolean isJson) {
        Map<Parameter, Class<?>> pms = InjectionUtils.getParametersFromBeanClass(type, pm.getType(), true);
        for (Map.Entry<Parameter, Class<?>> entry : pms.entrySet()) {
            String name = entry.getKey().getName();
            if (parentName != null) {
                name = parentName + "." + name;
            }
            Class<?> paramCls = entry.getValue();
            boolean isPrimitive = InjectionUtils.isPrimitive(paramCls) || paramCls.isEnum();
            if (isPrimitive
                || Date.class.isAssignableFrom(paramCls)
                || XMLGregorianCalendar.class.isAssignableFrom(paramCls)
                || InjectionUtils.isSupportedCollectionOrArray(paramCls)) {
                doWriteParam(ori, sb, entry.getKey(), paramCls, paramCls, name, new Annotation[] {}, isJson);
            } else if (!parentBeanClasses.contains(paramCls)) {
                parentBeanClasses.add(paramCls);
                doWriteBeanParam(ori, sb, paramCls, entry.getKey(), name, parentBeanClasses, isJson);
                parentBeanClasses.remove(paramCls);
            }
        }
    }
    //CHECKSTYLE:OFF
    protected void doWriteParam(OperationResourceInfo ori,
                                StringBuilder sb,
                                Parameter pm,
                                Class<?> type,
                                Type genericType,
                                String paramName,
                                Annotation[] anns,
                                boolean isJson) {
      //CHECKSTYLE:ON
        ParameterType pType = pm.getType();
        boolean isForm = isFormParameter(pm, type, anns);
        if (paramName == null && isForm) {
            Multipart m = AnnotationUtils.getAnnotation(anns, Multipart.class);
            if (m != null) {
                paramName = m.value();
            }
        }
        sb.append("<param name=\"").append(paramName).append("\" ");
        String style = ParameterType.PATH == pType ? "template" : isForm
            ? "query" : ParameterType.REQUEST_BODY == pType ? "plain" : pType.toString().toLowerCase();
        sb.append("style=\"").append(style).append('"');
        if (pm.getDefaultValue() != null) {
            sb.append(" default=\"").append(xmlEncodeIfNeeded(pm.getDefaultValue()))
                .append('"');
        }
        if (InjectionUtils.isSupportedCollectionOrArray(type)) {
            type = InjectionUtils.getActualType(genericType);
            sb.append(" repeating=\"true\"");
        }

        String value = XmlSchemaPrimitiveUtils.getSchemaRepresentation(type);
        if (value == null) {
            if (type.isEnum()) {
                value = "xs:string";
            } else if (type == InputStream.class) {
                value = "xs:anyType";
            }
        }
        if (value != null) {
            if (isJson) {
                value = value.substring(3);
            }
            sb.append(" type=\"").append(value).append('"');
        }
        if (type.isEnum()) {
            sb.append('>');
            handleDocs(anns, sb, DocTarget.PARAM, true, isJson);
            setEnumOptions(sb, type);
            sb.append("</param>");
        } else {
            addDocsAndCloseElement(ori, pm.getIndex(), sb, anns, "param", DocTarget.PARAM, true, isJson);
        }
    }

    private void setEnumOptions(StringBuilder sb, Class<?> enumClass) {
        try {
            Method m = enumClass.getMethod("values", new Class<?>[] {});
            Object[] values = (Object[])m.invoke(null, new Object[] {});
            m = enumClass.getMethod("toString", new Class<?>[] {});
            for (Object o : values) {
                String str = (String)m.invoke(o, new Object[] {});
                sb.append("<option value=\"").append(str).append("\"/>");
            }

        } catch (Throwable ex) {
            // ignore
        }
    }
    //CHECKSTYLE:OFF
    private void addDocsAndCloseElement(OperationResourceInfo ori,
                                        int paramIndex,
                                        StringBuilder sb,
                                        Annotation[] anns,
                                        String elementName,
                                        String category,
                                        boolean allowDefault,
                                        boolean isJson) {
    //CHECKSTYLE:ON
        boolean docAnnAvailable = isDocAvailable(anns);
        if (docAnnAvailable || (ori != null && !docProviders.isEmpty())) {
            sb.append('>');
            if (docAnnAvailable) {
                handleDocs(anns, sb, category, allowDefault, isJson);
            } else if (DocTarget.RETURN.equals(category)) {
                handleOperResponseJavaDocs(ori, sb);
            } else if (DocTarget.PARAM.equals(category)) {
                handleOperParamJavaDocs(ori, paramIndex, sb);
            }
            sb.append("</").append(elementName).append('>');
        } else {
            sb.append("/>");
        }
    }

    private boolean isDocAvailable(Annotation[] anns) {
        return AnnotationUtils.getAnnotation(anns, Description.class) != null
               || AnnotationUtils.getAnnotation(anns, Descriptions.class) != null;
    }

    // TODO: Collapse multiple parameters into a holder
    // CHECKSTYLE:OFF
    protected void handleRepresentation(StringBuilder sb, Set<Class<?>> jaxbTypes,
                                        ElementQNameResolver qnameResolver, Map<Class<?>, QName> clsMap,
                                        OperationResourceInfo ori, Class<?> type, boolean isJson,
                                        boolean inbound) {
        // CHECKSTYLE:ON
        List<MediaType> types = inbound ? ori.getConsumeTypes() : ori.getProduceTypes();
        if (MultivaluedMap.class.isAssignableFrom(type)) {
            types = Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        } else if (isWildcard(types)) {
            types = Collections.singletonList(defaultRepMediaType);
        }

        Method opMethod = getMethod(ori);
        boolean isPrimitive = InjectionUtils.isPrimitive(type) && !ori.isAsync();
        for (MediaType mt : types) {

            sb.append("<representation");
            sb.append(" mediaType=\"").append(JAXRSUtils.mediaTypeToString(mt)).append('"');
            if (isJson && !mt.getSubtype().contains("json")) {
                sb.append("/>");
                continue;
            }

            boolean allowDefault = true;
            String docCategory;
            Annotation[] anns;
            int inParamIndex = -1;
            Type genericType;
            if (inbound) {
                inParamIndex = getRequestBodyParam(ori).getIndex();
                anns = opMethod.getParameterAnnotations()[inParamIndex];
                if (!isDocAvailable(anns)) {
                    anns = opMethod.getAnnotations();
                }
                docCategory = DocTarget.PARAM;
                genericType = opMethod.getGenericParameterTypes()[inParamIndex];
            } else {
                anns = opMethod.getAnnotations();
                docCategory = DocTarget.RETURN;
                allowDefault = false;
                genericType = opMethod.getGenericReturnType();
            }
            if (isPrimitive) {
                sb.append('>');
                Parameter p = inbound ? getRequestBodyParam(ori) : new Parameter(ParameterType.REQUEST_BODY,
                                                                                 0, "result");
                doWriteParam(ori, sb, p, type, type, p.getName() == null ? "request" : p.getName(), anns, isJson);
                sb.append("</representation>");
            } else {
                boolean isCollection = InjectionUtils.isSupportedCollectionOrArray(type);
                Class<?> theActualType;
                if (isCollection) {
                    theActualType = InjectionUtils.getActualType(genericType);
                } else {
                    theActualType = ResourceUtils.getActualJaxbType(type, opMethod, inbound);
                }
                if (theActualType == Object.class && !(genericType instanceof Class)
                    || genericType instanceof TypeVariable) {
                    Type theType = InjectionUtils.processGenericTypeIfNeeded(
                        ori.getClassResourceInfo().getServiceClass(), Object.class, genericType);
                    theActualType = InjectionUtils.getActualType(theType);
                }
                if (isJson) {
                    sb.append(" element=\"").append(theActualType.getSimpleName()).append('"');
                } else if (qnameResolver != null
                           && (linkAnyMediaTypeToXmlSchema || mt.getSubtype().contains("xml"))
                           && jaxbTypes.contains(theActualType)) {
                    generateQName(sb, qnameResolver, clsMap, theActualType, isCollection,
                                  getBodyAnnotations(ori, inbound));
                }
                addDocsAndCloseElement(ori, inParamIndex, sb, anns, "representation",
                                       docCategory, allowDefault, isJson);
            }
        }

    }

    private Parameter getRequestBodyParam(OperationResourceInfo ori) {
        for (Parameter p : ori.getParameters()) {
            if (p.getType() == ParameterType.REQUEST_BODY) {
                return p;
            }
        }
        throw new IllegalStateException();
    }

    private boolean isWildcard(List<MediaType> types) {
        return types.size() == 1 && types.get(0).equals(MediaType.WILDCARD_TYPE);
    }

    private void handleFormRepresentation(StringBuilder sb, Set<Class<?>> jaxbTypes,
                                          ElementQNameResolver qnameResolver, Map<Class<?>, QName> clsMap,
                                          OperationResourceInfo ori, Class<?> type, boolean isJson) {
        if (type != null) {
            handleRepresentation(sb, jaxbTypes, qnameResolver, clsMap, ori, type, false, true);
        } else {
            List<MediaType> types = ori.getConsumeTypes();
            MediaType formType = isWildcard(types) ? MediaType.APPLICATION_FORM_URLENCODED_TYPE : types
                .get(0);
            sb.append("<representation");
            sb.append(" mediaType=\"").append(formType).append('"');
            if (isJson) {
                sb.append("/>");
            } else {
                sb.append('>');
                List<Parameter> params = ori.getParameters();
                for (int i = 0; i < params.size(); i++) {
                    if (isFormParameter(params.get(i), getMethod(ori).getParameterTypes()[i], getMethod(ori)
                        .getParameterAnnotations()[i])) {
                        writeParam(sb, params.get(i), ori, isJson);
                    }
                }
                sb.append("</representation>");
            }
        }
    }

    protected List<OperationResourceInfo> sortOperationsByPath(Set<OperationResourceInfo> ops) {
        List<OperationResourceInfo> opsWithSamePath = new LinkedList<>(ops);
        Collections.sort(opsWithSamePath, new Comparator<OperationResourceInfo>() {

            @Override
            public int compare(OperationResourceInfo op1, OperationResourceInfo op2) {
                boolean sub1 = op1.getHttpMethod() == null;
                boolean sub2 = op2.getHttpMethod() == null;
                if (sub1 && !sub2) {
                    return 1;
                } else if (!sub1 && sub2) {
                    return -1;
                }
                URITemplate ut1 = op1.getURITemplate();
                URITemplate ut2 = op2.getURITemplate();
                int result = ut1.getValue().compareTo(ut2.getValue());
                if (result == 0 && !(sub1 && sub2)) {
                    result = op1.getHttpMethod().compareTo(op2.getHttpMethod());
                }
                if (result == 0 && ignoreOverloadedMethods
                    && op1.getMethodToInvoke().getName().equals(op2.getMethodToInvoke().getName())) {
                    Integer paramLen1 = op1.getMethodToInvoke().getParameterTypes().length;
                    Integer paramLen2 = op2.getMethodToInvoke().getParameterTypes().length;
                    result = paramLen1.compareTo(paramLen2) * -1;
                }
                return result;
            }

        });
        return opsWithSamePath;
    }

    public List<ClassResourceInfo> getResourcesList(Message m, UriInfo ui) {
        final String slash = "/";
        String path = ui.getPath();
        if (!path.startsWith(slash)) {
            path = slash + path;
        }
        List<ClassResourceInfo> all = ((JAXRSServiceImpl)m.getExchange().getService())
            .getClassResourceInfos();
        boolean absolutePathSlashOn = checkAbsolutePathSlash && ui.getAbsolutePath().getPath().endsWith(slash);
        if (slash.equals(path) && !absolutePathSlashOn) {
            return all;
        }
        List<ClassResourceInfo> cris = new LinkedList<>();
        for (ClassResourceInfo cri : all) {
            MultivaluedMap<String, String> map = new MetadataMap<>();
            if (cri.getURITemplate().match(path, map)
                && slash.equals(map.getFirst(URITemplate.FINAL_MATCH_GROUP))) {
                cris.add(cri);
            }
        }
        return cris;
    }

    // TODO: deal with caching later on
    public Response getExistingWadl(Message m, UriInfo ui, MediaType mt) {
        Endpoint ep = m.getExchange().getEndpoint();
        if (ep != null) {
            String loc = (String)ep.get(JAXRSUtils.DOC_LOCATION);
            if (loc != null) {
                try {
                    InputStream is = ResourceUtils.getResourceStream(loc, (Bus)ep.get(Bus.class.getName()));
                    if (is != null) {
                        Object contextProp = m.getContextualProperty(CONVERT_WADL_RESOURCES_TO_DOM);
                        boolean doConvertResourcesToDOM = contextProp == null
                            ? convertResourcesToDOM : PropertyUtils.isTrue(contextProp);
                        if (!doConvertResourcesToDOM || isJson(mt)) {
                            return Response.ok(is, mt).build();
                        }
                        Document wadlDoc = StaxUtils.read(is);
                        Element appEl = wadlDoc.getDocumentElement();

                        List<Element> grammarEls = DOMUtils.getChildrenWithName(appEl, WadlGenerator.WADL_NS,
                                                                                "grammars");
                        if (grammarEls.size() == 1) {
                            handleExistingDocRefs(DOMUtils.getChildrenWithName(grammarEls.get(0),
                                                                               WadlGenerator.WADL_NS,
                                                                               "include"), "href", loc, "",
                                                  m, ui);
                        }

                        List<Element> resourcesEls = DOMUtils.getChildrenWithName(appEl,
                                                                                  WadlGenerator.WADL_NS,
                                                                                  "resources");
                        if (resourcesEls.size() == 1) {
                            DOMUtils.setAttribute(resourcesEls.get(0), "base", getBaseURI(m, ui));

                            List<Element> resourceEls = DOMUtils.getChildrenWithName(resourcesEls.get(0),
                                                                                     WadlGenerator.WADL_NS,
                                                                                     "resource");
                            handleExistingDocRefs(resourceEls, "type", loc, "", m, ui);
                            return finalizeExistingWadlResponse(wadlDoc, m, ui, mt);
                        }

                    }
                } catch (Exception ex) {
                    throw ExceptionUtils.toInternalServerErrorException(ex, null);
                }
            }
        }
        return null;
    }
    private Response finalizeExistingWadlResponse(Document wadlDoc, Message m, UriInfo ui, MediaType mt)
        throws Exception {
        Object entity;
        if (stylesheetReference != null) {
            if (!applyStylesheetLocally) {
                ProcessingInstruction pi = wadlDoc.createProcessingInstruction("xml-stylesheet",
                                              getStylesheetInstructionData(getBaseURI(m, ui)));
                wadlDoc.insertBefore(pi, wadlDoc.getDocumentElement());
                entity = copyDOMToString(wadlDoc);
            } else {
                entity = transformLocally(m, ui, new DOMSource(wadlDoc));
            }
        } else {
            entity = new DOMSource(wadlDoc);
        }
        return Response.ok(entity, mt).build();

    }
    private String copyDOMToString(Document wadlDoc) throws Exception {
        DOMSource domSource = new DOMSource(wadlDoc);
        // temporary workaround
        StringWriter stringWriter = new StringWriter();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer transformer = transformerFactory.newTransformer();
        transformer.transform(domSource, new StreamResult(stringWriter));
        return stringWriter.toString();
    }
    private String transformLocally(Message m, UriInfo ui, Source source) throws Exception {
        InputStream is = ResourceUtils.getResourceStream(stylesheetReference, m.getExchange().getBus());
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer t = transformerFactory.newTemplates(new StreamSource(is)).newTransformer();
        t.setParameter("base.path", m.get("http.base.path"));
        StringWriter stringWriter = new StringWriter();
        t.transform(source, new StreamResult(stringWriter));
        return stringWriter.toString();
    }


    // TODO: deal with caching later on
    public Response getExistingResource(Message m, UriInfo ui, String href) {
        try {
            String loc = docLocationMap.get(href);
            if (loc != null) {
                int fragmentIndex = loc.lastIndexOf('#');
                if (fragmentIndex != -1) {
                    loc = loc.substring(0, fragmentIndex);
                }
                InputStream is = ResourceUtils.getResourceStream(loc, m.getExchange().getBus());
                if (is != null) {
                    Element docEl = StaxUtils.read(is).getDocumentElement();
                    if (href.endsWith(".xsl")) {
                        List<Element> xslImports = DOMUtils.getChildrenWithName(docEl, XLS_NS, "import");
                        handleExistingDocRefs(xslImports, "href", loc, href, m, ui);
                        List<Element> xslIncludes = DOMUtils.getChildrenWithName(docEl, XLS_NS, "include");
                        handleExistingDocRefs(xslIncludes, "href", loc, href, m, ui);
                    } else {
                        if (fragmentIndex != -1) {
                            List<Element> grammarEls = DOMUtils.getChildrenWithName(docEl, WADL_NS, "grammars");
                            if (grammarEls.size() == 1) {
                                handleExistingDocRefs(DOMUtils.getChildrenWithName(grammarEls.get(0), WADL_NS,
                                                                                   "include"), "href", loc, href,
                                                      m, ui);
                            }
                        } else {
                            handleExistingDocRefs(DOMUtils.getChildrenWithName(docEl,
                                                                               Constants.URI_2001_SCHEMA_XSD,
                                                                               "import"), "schemaLocation", loc,
                                                  href, m, ui);
                            handleExistingDocRefs(DOMUtils.getChildrenWithName(docEl,
                                                                               Constants.URI_2001_SCHEMA_XSD,
                                                                               "include"), "schemaLocation", loc,
                                                  href, m, ui);
                        }
                    }

                    return Response.ok().type(MediaType.APPLICATION_XML_TYPE).entity(new DOMSource(docEl))
                        .build();
                }
            } else if (stylesheetReference != null && href.endsWith(".xsl")) {
                InputStream is = ResourceUtils.getResourceStream(href, m.getExchange().getBus());
                return Response.ok().type(MediaType.APPLICATION_XML_TYPE).entity(is).build();
            }

        } catch (Exception ex) {
            throw ExceptionUtils.toBadRequestException(null, null);
        }
        return null;
    }

    private void handleExistingDocRefs(List<Element> elements, String attrName, String parentDocLoc,
                                       String parentRef, Message m, UriInfo ui) {
        if (keepRelativeDocLinks) {
            return;
        }
        int index = parentDocLoc.lastIndexOf('/');
        parentDocLoc = index == -1 ? parentDocLoc : parentDocLoc.substring(0, index + 1);

        index = parentRef.lastIndexOf('/');
        parentRef = index == -1 ? "" : parentRef.substring(0, index + 1);

        for (Element element : elements) {
            String href = element.getAttribute(attrName);
            String originalRef = href;
            if (!StringUtils.isEmpty(href) && !href.startsWith("#")) {
                int fragmentIndex = href.lastIndexOf('#');
                String fragment = null;
                if (fragmentIndex != -1) {
                    fragment = href.substring(fragmentIndex + 1);
                    href = href.substring(0, fragmentIndex);
                }

                String actualRef = parentRef + href;
                docLocationMap.put(actualRef, parentDocLoc + originalRef);
                UriBuilder ub = UriBuilder.fromUri(getBaseURI(m, ui)).path(actualRef).fragment(fragment);
                URI schemaURI = ub.build();
                DOMUtils.setAttribute(element, attrName, schemaURI.toString());
            }
        }
    }

    private void generateQName(StringBuilder sb, ElementQNameResolver qnameResolver,
                               Map<Class<?>, QName> clsMap, Class<?> type, boolean isCollection,
                               Annotation[] annotations) {
        if (!isCollection) {
            QName typeQName = clsMap.get(type);
            if (typeQName != null) {
                writeQName(sb, typeQName);
                return;
            }
        }

        QName qname = qnameResolver.resolve(type, annotations, Collections.unmodifiableMap(clsMap));

        if (qname != null) {
            if (!isCollection) {
                writeQName(sb, qname);
                clsMap.put(type, qname);
            } else {
                XMLName name = AnnotationUtils.getAnnotation(annotations, XMLName.class);
                String localPart;
                if (name != null) {
                    localPart = JAXRSUtils.convertStringToQName(name.value()).getLocalPart();
                } else {
                    localPart = qname.getLocalPart() + "s";
                }
                QName collectionName = new QName(qname.getNamespaceURI(), localPart, qname.getPrefix());
                writeQName(sb, collectionName);
            }
        }
    }

    private void writeQName(StringBuilder sb, QName qname) {
        sb.append(" element=\"").append(qname.getPrefix()).append(':').append(qname.getLocalPart()).append('"');
    }

    private boolean isXmlRoot(Class<?> cls) {
        return cls.getAnnotation(XmlRootElement.class) != null;
    }

    private SchemaCollection getSchemaCollection(ResourceTypes resourceTypes, JAXBContext context) {
        if (context == null) {
            return null;
        }
        SchemaCollection xmlSchemaCollection = new SchemaCollection();
        Collection<DOMSource> schemas = new HashSet<>();
        List<String> targetNamespaces = new ArrayList<>();
        try {
            for (DOMResult r : JAXBUtils.generateJaxbSchemas(context, CastUtils.cast(Collections.emptyMap(),
                                                                                     String.class,
                                                                                     DOMResult.class))) {
                Document doc = (Document)r.getNode();
                ElementQNameResolver theResolver = createElementQNameResolver(context);
                String tns = doc.getDocumentElement().getAttribute("targetNamespace");

                String tnsPrefix = doc.getDocumentElement().lookupPrefix(tns);
                if (tnsPrefix == null) {
                    String tnsDecl =
                        doc.getDocumentElement().getAttribute("xmlns:tns");
                    tnsPrefix = tnsDecl != null && tnsDecl.equals(tns) ? "tns:" : "";
                } else {
                    tnsPrefix += ":";
                }

                if (supportJaxbXmlType) {
                    for (Class<?> cls : resourceTypes.getAllTypes().keySet()) {
                        if (isXmlRoot(cls)) {
                            continue;
                        }
                        XmlType root = cls.getAnnotation(XmlType.class);
                        if (root != null) {
                            QName typeName = theResolver.resolve(cls, new Annotation[] {},
                                                           Collections.<Class<?>, QName> emptyMap());
                            if (typeName != null && tns.equals(typeName.getNamespaceURI())) {
                                QName elementName = resourceTypes.getXmlNameMap().get(cls);
                                if (elementName == null) {
                                    elementName = typeName;
                                }
                                Element newElement = doc
                                    .createElementNS(Constants.URI_2001_SCHEMA_XSD, "xs:element");
                                newElement.setAttribute("name", elementName.getLocalPart());
                                newElement.setAttribute("type", tnsPrefix + typeName.getLocalPart());

                                if (Modifier.isAbstract(cls.getModifiers())
                                    && resourceTypes.getSubstitutions().values().contains(cls)) {
                                    newElement.setAttribute("abstract", "true");
                                }

                                doc.getDocumentElement().appendChild(newElement);
                            }
                        }
                    }
                    if (supportJaxbSubstitutions) {
                        for (Map.Entry<Class<?>, Class<?>> entry : resourceTypes.getSubstitutions().entrySet()) {
                            QName typeName = theResolver.resolve(entry.getKey(), new Annotation[] {},
                                                                 Collections.<Class<?>, QName> emptyMap());
                            for (Element element : DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(),
                                                                                       Constants.URI_2001_SCHEMA_XSD,
                                                                                       "element")) {
                                if (element.getAttribute("name").equals(typeName.getLocalPart())) {
                                    QName groupName = theResolver.resolve(entry.getValue(), new Annotation[] {},
                                                                         Collections.<Class<?>, QName> emptyMap());
                                    if (groupName != null) {
                                        element.setAttribute("substitutionGroup", tnsPrefix + groupName.getLocalPart());
                                    }
                                }
                            }
                        }
                    }
                }
                if (supportCollections && !resourceTypes.getCollectionMap().isEmpty()) {
                    for (Map.Entry<Class<?>, QName> entry : resourceTypes.getCollectionMap().entrySet()) {
                        QName colQName = entry.getValue();
                        if (colQName == null) {
                            colQName = theResolver.resolve(entry.getKey(), new Annotation[] {},
                                                Collections.<Class<?>, QName> emptyMap());
                            if (colQName != null) {
                                colQName = new QName(colQName.getNamespaceURI(),
                                                     colQName.getLocalPart() + "s",
                                                     colQName.getPrefix());
                            }
                        }
                        if (colQName == null) {
                            continue;
                        }
                        if (tns.equals(colQName.getNamespaceURI())) {
                            QName typeName = theResolver.resolve(entry.getKey(), new Annotation[] {},
                                                                 Collections.<Class<?>, QName> emptyMap());
                            if (typeName != null) {
                                Element newElement = doc
                                    .createElementNS(Constants.URI_2001_SCHEMA_XSD, "xs:element");
                                newElement.setAttribute("name", colQName.getLocalPart());
                                Element ctElement = doc.createElementNS(Constants.URI_2001_SCHEMA_XSD,
                                                                        "xs:complexType");
                                newElement.appendChild(ctElement);
                                Element seqElement = doc
                                    .createElementNS(Constants.URI_2001_SCHEMA_XSD, "xs:sequence");
                                ctElement.appendChild(seqElement);
                                Element xsElement = doc.createElementNS(Constants.URI_2001_SCHEMA_XSD,
                                                                        "xs:element");
                                seqElement.appendChild(xsElement);
                                xsElement.setAttribute("ref", tnsPrefix + typeName.getLocalPart());
                                xsElement.setAttribute("minOccurs", "0");
                                xsElement.setAttribute("maxOccurs", "unbounded");

                                doc.getDocumentElement().appendChild(newElement);
                            }
                        }
                    }
                }
                DOMSource source = new DOMSource(doc, r.getSystemId());
                schemas.add(source);
                if (!StringUtils.isEmpty(tns)) {
                    targetNamespaces.add(tns);
                }
            }
        } catch (IOException e) {
            LOG.fine("No schema can be generated");
            return null;
        }

        boolean hackAroundEmptyNamespaceIssue = false;
        for (DOMSource r : schemas) {
            hackAroundEmptyNamespaceIssue = addSchemaDocument(xmlSchemaCollection, targetNamespaces,
                                                              (Document)r.getNode(), r.getSystemId(),
                                                              hackAroundEmptyNamespaceIssue);
        }
        return xmlSchemaCollection;
    }

    private QName getJaxbQName(String name, String namespace, Class<?> type, Map<Class<?>, QName> clsMap) {
        QName qname = getQNameFromParts(name, namespace, type, clsMap);
        if (qname != null) {
            return qname;
        }
        String ns = JAXBUtils.getPackageNamespace(type);
        if (ns != null) {
            return getQNameFromParts(name, ns, type, clsMap);
        }
        return null;

    }

    private QName getJaxbQName(JAXBContextProxy jaxbProxy, Class<?> type, Map<Class<?>, QName> clsMap) {
        XmlRootElement root = type.getAnnotation(XmlRootElement.class);
        if (root != null) {
            return getJaxbQName(root.name(), root.namespace(), type, clsMap);
        }

        try {
            JAXBBeanInfo jaxbInfo = jaxbProxy == null ? null : JAXBUtils.getBeanInfo(jaxbProxy, type);
            if (jaxbInfo == null) {
                return null;
            }
            Object instance = type.newInstance();
            return getQNameFromParts(jaxbInfo.getElementLocalName(instance),
                                     jaxbInfo.getElementNamespaceURI(instance), type, clsMap);
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }

    private String getPrefix(String ns, Map<Class<?>, QName> clsMap) {
        String prefix = null;
        int index = 0;
        for (QName name : clsMap.values()) {
            String currentPrefix = name.getPrefix();
            if (currentPrefix.startsWith(nsPrefix)) {
                int currentIndex = currentPrefix.equals(nsPrefix) ? 0
                    : Integer.parseInt(currentPrefix.substring(nsPrefix.length()));
                if (currentIndex > index) {
                    index = currentIndex;
                }
            }
            if (name.getNamespaceURI().equals(ns)) {
                prefix = currentPrefix;
                break;
            }
        }
        if (StringUtils.isEmpty(prefix)) {
            prefix = index == 0 && !incrementNamespacePrefix ? nsPrefix : nsPrefix + (index + 1);
        }
        return prefix;
    }

    private boolean isFormRequest(OperationResourceInfo ori) {
        for (Parameter p : ori.getParameters()) {
            if (p.getType() == ParameterType.FORM
                || p.getType() == ParameterType.REQUEST_BODY
                && (getMethod(ori).getParameterTypes()[p.getIndex()] == MultivaluedMap.class || AnnotationUtils
                    .getAnnotation(getMethod(ori).getParameterAnnotations()[p.getIndex()], Multipart.class) != null)) {
                return true;
            }
        }
        return false;
    }

    private Class<?> getFormClass(OperationResourceInfo ori) {
        List<Parameter> params = ori.getParameters();
        for (int i = 0; i < params.size(); i++) {
            if (isFormParameter(params.get(i), getMethod(ori).getParameterTypes()[i], getMethod(ori)
                .getParameterAnnotations()[i])) {
                return null;
            }
        }
        return MultivaluedMap.class;
    }

    private boolean isFormParameter(Parameter pm, Class<?> type, Annotation[] anns) {
        return ParameterType.FORM == pm.getType() || ParameterType.REQUEST_BODY == pm.getType()
               && AnnotationUtils.getAnnotation(anns, Multipart.class) != null
               && (InjectionUtils.isPrimitive(type) || type == InputStream.class);
    }

    // TODO : can we reuse this block with JAXBBinding somehow ?
    public boolean addSchemaDocument(SchemaCollection col, List<String> tnsList, Document d, String systemId,
                                     boolean hackAroundEmptyNamespaceIssue) {
        String ns = d.getDocumentElement().getAttribute("targetNamespace");

        if (StringUtils.isEmpty(ns)) {
            if (DOMUtils.getFirstElement(d.getDocumentElement()) == null) {
                hackAroundEmptyNamespaceIssue = true;
                return hackAroundEmptyNamespaceIssue;
            }
            // create a copy of the dom so we
            // can modify it.
            d = copy(d);
            ns = tnsList.isEmpty() ? "" : tnsList.get(0);
            d.getDocumentElement().setAttribute("targetNamespace", ns);
        }

        if (hackAroundEmptyNamespaceIssue) {
            d = doEmptyNamespaceHack(d);
        }

        Node n = d.getDocumentElement().getFirstChild();
        while (n != null) {
            if (n instanceof Element) {
                Element e = (Element)n;
                if ("import".equals(e.getLocalName())) {
                    e.removeAttribute("schemaLocation");
                }
            }
            n = n.getNextSibling();
        }

        synchronized (d) {
            col.read(d, systemId);
        }
        return hackAroundEmptyNamespaceIssue;
    }

    private Document doEmptyNamespaceHack(Document d) {
        boolean hasStuffToRemove = false;
        Element el = DOMUtils.getFirstElement(d.getDocumentElement());
        while (el != null) {
            if ("import".equals(el.getLocalName()) && StringUtils.isEmpty(el.getAttribute("targetNamespace"))) {
                hasStuffToRemove = true;
                break;
            }
            el = DOMUtils.getNextElement(el);
        }
        if (hasStuffToRemove) {
            // create a copy of the dom so we
            // can modify it.
            d = copy(d);
            el = DOMUtils.getFirstElement(d.getDocumentElement());
            while (el != null) {
                if ("import".equals(el.getLocalName())
                    && StringUtils.isEmpty(el.getAttribute("targetNamespace"))) {
                    d.getDocumentElement().removeChild(el);
                    el = DOMUtils.getFirstElement(d.getDocumentElement());
                } else {
                    el = DOMUtils.getNextElement(el);
                }
            }
        }

        return d;
    }

    private Document copy(Document doc) {
        try {
            return StaxUtils.copy(doc);
        } catch (XMLStreamException | ParserConfigurationException e) {
            // ignore
        }
        return doc;
    }

    private QName getQNameFromParts(String name, String namespace, Class<?> type, Map<Class<?>, QName> clsMap) {
        if (namespace == null || JAXB_DEFAULT_NAMESPACE.equals(namespace) || namespace.length() == 0) {
            return null;
        }
        if (name == null || name.length() == 0) {
            return null;
        }
        if (JAXB_DEFAULT_NAME.equals(name)) {
            name = StringUtils.uncapitalize(type.getSimpleName());
        }
        String prefix = getPrefix(namespace, clsMap);
        return new QName(namespace, name, prefix);
    }

    public void setIgnoreMessageWriters(boolean ignoreMessageWriters) {
        this.ignoreMessageWriters = ignoreMessageWriters;
    }

    private void handleApplicationDocs(StringBuilder sbApp) {
        if (applicationTitle != null) {
            sbApp.append("<doc title=\"").append(xmlEncodeIfNeeded(applicationTitle)).append("\"/>");
        }
    }

    protected void handleClassJavaDocs(ClassResourceInfo cri, StringBuilder sb) {
        for (DocumentationProvider docProvider : docProviders) {
            addProvidedDocs(sb, docProvider.getClassDoc(cri));
        }
    }

    protected void handleOperJavaDocs(OperationResourceInfo ori, StringBuilder sb) {
        for (DocumentationProvider docProvider : docProviders) {
            addProvidedDocs(sb, docProvider.getMethodDoc(ori));
        }
    }

    protected void handleOperResponseJavaDocs(OperationResourceInfo ori, StringBuilder sb) {
        for (DocumentationProvider docProvider : docProviders) {
            addProvidedDocs(sb, docProvider.getMethodResponseDoc(ori));
        }
    }

    protected void handleOperParamJavaDocs(OperationResourceInfo ori,
                                           int paramIndex,
                                           StringBuilder sb) {
        for (DocumentationProvider docProvider : docProviders) {
            addProvidedDocs(sb, docProvider.getMethodParameterDoc(ori, paramIndex));
        }
    }

    private void addProvidedDocs(StringBuilder sb, String text) {
        if (!StringUtils.isEmpty(text)) {
            sb.append("<doc>");
            sb.append(xmlEncodeIfNeeded(text));
            sb.append("</doc>");
        }
    }

    protected boolean handleDocs(Annotation[] anns,
                              StringBuilder sb,
                              String category,
                              boolean allowDefault,
                              boolean isJson) {
        boolean found = false;
        for (Annotation a : anns) {
            if (a.annotationType() == Descriptions.class) {
                Descriptions ds = (Descriptions)a;
                return handleDocs(ds.value(), sb, category, allowDefault, isJson);
            }
            if (a.annotationType() == Description.class) {
                Description d = (Description)a;
                if (d.target().length() == 0 && !allowDefault || d.target().length() > 0
                    && !d.target().equals(category)) {
                    continue;
                }

                sb.append("<doc");
                if (!isJson && d.lang().length() > 0) {
                    sb.append(" xml:lang=\"").append(d.lang()).append('"');
                }
                if (d.title().length() > 0) {
                    sb.append(" title=\"").append(xmlEncodeIfNeeded(d.title())).append('"');
                }
                sb.append('>');
                if (d.value().length() > 0) {
                    sb.append(xmlEncodeIfNeeded(d.value()));
                } else if (d.docuri().length() > 0) {
                    InputStream is;
                    if (d.docuri().startsWith(CLASSPATH_PREFIX)) {
                        String path = d.docuri().substring(CLASSPATH_PREFIX.length());
                        is = ResourceUtils.getClasspathResourceStream(path, SchemaHandler.class,
                            bus == null ? BusFactory.getDefaultBus() : bus);
                        if (is != null) {
                            try {
                                sb.append(IOUtils.toString(is));
                            } catch (IOException ex) {
                                // ignore
                            }
                        }
                    }
                }
                sb.append("</doc>");
                found = true;
            }
        }
        return found;
    }

    private String getNamespace() {
        return wadlNamespace != null ? wadlNamespace : WADL_NS;
    }

    public void setWadlNamespace(String namespace) {
        this.wadlNamespace = namespace;
    }

    public void setSingleResourceMultipleMethods(boolean singleResourceMultipleMethods) {
        this.singleResourceMultipleMethods = singleResourceMultipleMethods;
    }

    public void setUseSingleSlashResource(boolean useSingleSlashResource) {
        this.useSingleSlashResource = useSingleSlashResource;
    }

    @Deprecated
    public void setLinkJsonToXmlSchema(boolean link) {
        setLinkAnyMediaTypeToXmlSchema(link);
    }
    public void setLinkAnyMediaTypeToXmlSchema(boolean link) {
        linkAnyMediaTypeToXmlSchema = link;
    }

    public void setSchemaLocations(List<String> locations) {
        externalQnamesMap = new HashMap<>();
        externalSchemasCache = new ArrayList<>(locations.size());
        for (int i = 0; i < locations.size(); i++) {
            String loc = locations.get(i);
            try {
                loadSchemasIntoCache(loc);
            } catch (Exception ex) {
                LOG.warning("No schema resource " + loc + " can be loaded : " + ex.getMessage());
                externalSchemasCache = null;
                externalQnamesMap = null;
                return;
            }
        }
    }

    private void loadSchemasIntoCache(String loc) throws Exception {
        InputStream is = ResourceUtils.getResourceStream(loc,
            bus == null ? BusFactory.getDefaultBus() : bus);
        if (is == null) {
            return;
        }
        try (ByteArrayInputStream bis = IOUtils.loadIntoBAIS(is)) {
            XMLSource source = new XMLSource(bis);
            source.setBuffering();
            String targetNs = source.getValue("/*/@targetNamespace");

            Map<String, String> nsMap = Collections.singletonMap("xs", Constants.URI_2001_SCHEMA_XSD);
            String[] elementNames = source.getValues("/*/xs:element/@name", nsMap);
            externalQnamesMap.put(targetNs, Arrays.asList(elementNames));
            String schemaValue = source.getNode("/xs:schema", nsMap, String.class);
            externalSchemasCache.add(schemaValue);
        }
    }

    public void setUseJaxbContextForQnames(boolean checkJaxbOnly) {
        this.useJaxbContextForQnames = checkJaxbOnly;
    }

    protected ElementQNameResolver createElementQNameResolver(JAXBContext context) {
        if (resolver != null) {
            return resolver;
        }
        if (useJaxbContextForQnames) {
            if (context != null) {
                JAXBContextProxy proxy = JAXBUtils.createJAXBContextProxy(context);
                return new JaxbContextQNameResolver(proxy);
            }
            return null;
        } else if (externalQnamesMap != null) {
            return new SchemaQNameResolver(externalQnamesMap);
        } else {
            return new XMLNameQNameResolver();
        }
    }

    protected SchemaWriter createSchemaWriter(ResourceTypes resourceTypes, JAXBContext context, UriInfo ui) {
        // if neither externalSchemaLinks nor externalSchemasCache is set
        // then JAXBContext will be used to generate the schema
        if (externalSchemaLinks != null && externalSchemasCache == null) {
            return new ExternalSchemaWriter(externalSchemaLinks, ui);
        } else if (externalSchemasCache != null) {
            return new StringSchemaWriter(externalSchemasCache, externalSchemaLinks, ui);
        } else {
            SchemaCollection coll = getSchemaCollection(resourceTypes, context);
            if (coll != null) {
                return new SchemaCollectionWriter(coll);
            }
        }
        return null;
    }

    public void setExternalLinks(List<String> externalLinks) {
        externalSchemaLinks = new LinkedList<>();
        for (String s : externalLinks) {
            try {
                String href = s;
                if (href.startsWith("classpath:")) {
                    int index = href.lastIndexOf('/');
                    href = index == -1 ? href.substring(9) : href.substring(index + 1);
                    docLocationMap.put(href, s);
                }
                externalSchemaLinks.add(URI.create(href));
            } catch (Exception ex) {
                LOG.warning("Not a valid URI : " + s);
                externalSchemaLinks = null;
                break;
            }
        }
    }

    protected interface SchemaWriter {
        void write(StringBuilder sb);
    }

    private class StringSchemaWriter implements SchemaWriter {

        private final List<String> theSchemas;

        StringSchemaWriter(List<String> schemas, List<URI> links, UriInfo ui) {
            this.theSchemas = new LinkedList<>();
            // we'll need to do the proper schema caching eventually
            for (String s : schemas) {
                XMLSource source = new XMLSource(new ByteArrayInputStream(s.getBytes()));
                source.setBuffering();
                Map<String, String> locs = getLocationsMap(source, "import", links, ui);
                locs.putAll(getLocationsMap(source, "include", links, ui));
                String actualSchema = !locs.isEmpty() ? transformSchema(s, locs) : s;
                theSchemas.add(actualSchema);
            }
        }

        private Map<String, String> getLocationsMap(XMLSource source, String elementName, List<URI> links,
                                                    UriInfo ui) {
            Map<String, String> nsMap = Collections.singletonMap("xs", Constants.URI_2001_SCHEMA_XSD);
            String[] locations = source.getValues("/*/xs:" + elementName + "/@schemaLocation", nsMap);

            Map<String, String> locs = new HashMap<>();
            if (locations == null) {
                return locs;
            }

            for (String loc : locations) {
                try {
                    URI uri = URI.create(loc);
                    if (!uri.isAbsolute()) {
                        if (links != null) {
                            for (URI overwriteURI : links) {
                                if (overwriteURI.toString().endsWith(loc)) {
                                    if (overwriteURI.isAbsolute()) {
                                        locs.put(loc, overwriteURI.toString());
                                    } else {
                                        locs.put(loc, ui.getBaseUriBuilder().path(overwriteURI.toString())
                                            .build().toString());
                                    }
                                    break;
                                }
                            }
                        }
                        if (!locs.containsKey(loc)) {
                            locs.put(loc, ui.getBaseUriBuilder().path(loc).build().toString());
                        }
                    }
                } catch (Exception ex) {
                    // continue
                }
            }
            return locs;
        }

        private String transformSchema(String schema, Map<String, String> locs) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            SchemaConverter sc = new SchemaConverter(StaxUtils.createXMLStreamWriter(bos), locs);
            try {
                StaxUtils.copy(new StreamSource(new StringReader(schema)), sc);
                sc.flush();
                sc.close();
                return bos.toString();
            } catch (Exception ex) {
                return schema;
            }

        }

        @Override
        public void write(StringBuilder sb) {
            for (String s : theSchemas) {
                sb.append(s);
            }
        }
    }

    private class SchemaCollectionWriter implements SchemaWriter {

        private final SchemaCollection coll;

        SchemaCollectionWriter(SchemaCollection coll) {
            this.coll = coll;
        }

        @Override
        public void write(StringBuilder sb) {
            for (XmlSchema xs : coll.getXmlSchemas()) {
                if (xs.getItems().isEmpty() || Constants.URI_2001_SCHEMA_XSD.equals(xs.getTargetNamespace())) {
                    continue;
                }
                StringWriter writer = new StringWriter();
                xs.write(writer);
                sb.append(writer.toString());
            }
        }
    }

    private class ExternalSchemaWriter implements SchemaWriter {

        private final List<URI> links;
        private final UriInfo uriInfo;

        ExternalSchemaWriter(List<URI> links, UriInfo ui) {
            this.links = links;
            this.uriInfo = ui;
        }

        @Override
        public void write(StringBuilder sb) {
            for (URI link : links) {
                try {
                    URI value = link.isAbsolute() ? link : uriInfo.getBaseUriBuilder().path(link.toString()).build();
                    sb.append("<include href=\"").append(value.toString()).append("\"/>");
                } catch (Exception ex) {
                    LOG.warning("WADL grammar section will be incomplete, this link is not a valid URI : "
                                + link.toString());
                }
            }
        }
    }

    private class JaxbContextQNameResolver implements ElementQNameResolver {

        private final JAXBContextProxy proxy;

        JaxbContextQNameResolver(JAXBContextProxy proxy) {
            this.proxy = proxy;
        }

        @Override
        public QName resolve(Class<?> type, Annotation[] annotations, Map<Class<?>, QName> clsMap) {
            QName qname = WadlGenerator.this.getJaxbQName(proxy, type, clsMap);
            if (qname == null && supportJaxbXmlType) {
                XmlType root = type.getAnnotation(XmlType.class);
                if (root != null) {
                    XMLName name = AnnotationUtils.getAnnotation(annotations, XMLName.class);
                    if (name == null) {
                        qname = getJaxbQName(root.name(), root.namespace(), type, clsMap);
                    } else {
                        QName tempQName = JAXRSUtils.convertStringToQName(name.value());
                        qname = new QName(tempQName.getNamespaceURI(),
                                          tempQName.getLocalPart(),
                                          getPrefix(tempQName.getNamespaceURI(), clsMap));
                    }
                }
            }
            return qname;
        }

    }

    private class XMLNameQNameResolver implements ElementQNameResolver {

        @Override
        public QName resolve(Class<?> type, Annotation[] annotations, Map<Class<?>, QName> clsMap) {
            XMLName name = AnnotationUtils.getAnnotation(annotations, XMLName.class);
            if (name == null) {
                name = type.getAnnotation(XMLName.class);
            }
            if (name != null) {
                QName qname = DOMUtils.convertStringToQName(name.value(), name.prefix());
                if (qname.getPrefix().length() > 0) {
                    return qname;
                }
                return getQNameFromParts(qname.getLocalPart(), qname.getNamespaceURI(), type, clsMap);
            }
            return null;
        }

    }

    private class SchemaQNameResolver implements ElementQNameResolver {
        private final Map<String, List<String>> map;

        SchemaQNameResolver(Map<String, List<String>> map) {
            this.map = map;
        }

        @Override
        public QName resolve(Class<?> type, Annotation[] annotations, Map<Class<?>, QName> clsMap) {
            String name = type.getSimpleName();
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                String elementName = null;
                if (entry.getValue().contains(name)) {
                    elementName = name;
                } else if (entry.getValue().contains(name.toLowerCase())) {
                    elementName = name.toLowerCase();
                }
                if (elementName != null) {
                    return getQNameFromParts(elementName, entry.getKey(), type, clsMap);
                }
            }
            return null;
        }

    }

    public void setResolver(ElementQNameResolver resolver) {
        this.resolver = resolver;
    }

    public void setPrivateAddresses(List<String> privateAddresses) {
        this.privateAddresses = privateAddresses;
    }

    public List<String> getPrivateAddresses() {
        return privateAddresses;
    }


    public void setAddResourceAndMethodIds(boolean addResourceAndMethodIds) {
        ResourceIdGenerator idGen = addResourceAndMethodIds ? new ResourceIdGeneratorImpl() : null;
        setResourceIdGenerator(idGen);
    }
    public void setResourceIdGenerator(ResourceIdGenerator idGen) {
        this.idGenerator = idGen;
    }

    private Method getMethod(OperationResourceInfo ori) {
        Method annMethod = ori.getAnnotatedMethod();
        return annMethod != null ? annMethod : ori.getMethodToInvoke();
    }

    public void setApplicationTitle(String applicationTitle) {
        this.applicationTitle = applicationTitle;
    }

    public void setNamespacePrefix(String prefix) {
        this.nsPrefix = prefix;
    }

    public void setIgnoreForwardSlash(boolean ignoreForwardSlash) {
        this.ignoreForwardSlash = ignoreForwardSlash;
    }

    public void setIgnoreRequests(boolean ignoreRequests) {
        this.ignoreRequests = ignoreRequests;
    }

    public void setSupportCollections(boolean support) {
        this.supportCollections = support;
    }

    /**
     * Set the default WADL response media type.
     * For example, a browser may display WADL better if Content-Type
     * is set to application/xml which is a default response content type.
     * Users may set it to application/vnd.sun.wadl+xml or other type.
     * @param mt WADL response media type
     */
    public void setDefaultMediaType(String mt) {
        this.defaultWadlResponseMediaType = JAXRSUtils.toMediaType(mt);
    }

    /**
     * Set the default representation media type to be used
     * if JAX-RS Produces or Consumes annotation is missing.
     * Wild-card media type is used by default in such cases.
     * @param mt the default representation media type
     */
    public void setDefaultRepresentationMediaType(String mt) {
        this.defaultWadlResponseMediaType = JAXRSUtils.toMediaType(mt);
    }

    public void setSupportJaxbXmlType(boolean supportJaxbXmlType) {
        this.supportJaxbXmlType = supportJaxbXmlType;
    }

    public void setSupportJaxbSubstitutions(boolean supportJaxbSubstitutions) {
        this.supportJaxbSubstitutions = supportJaxbSubstitutions;
    }

    public void setCheckAbsolutePathSlash(boolean checkAbsolutePathSlash) {
        this.checkAbsolutePathSlash = checkAbsolutePathSlash;
    }

    public void setJavaDocPath(String path) throws Exception {
        setDocumentationProvider(new JavaDocProvider(bus == null ? BusFactory.getDefaultBus() : bus, path));
    }

    public void setJavaDocPaths(String... paths) throws Exception {
        setDocumentationProvider(new JavaDocProvider(bus == null ? BusFactory.getDefaultBus() : bus, paths));
    }

    public void setJavaDocURLs(final URL[] javaDocURLs) {
        setDocumentationProvider(new JavaDocProvider(javaDocURLs));
    }

    public void setDocumentationProvider(DocumentationProvider p) {
        docProviders.add(p);
    }
    public void setDocumentationProvider(List<DocumentationProvider> ps) {
        docProviders.addAll(ps);
    }
    public void setStylesheetReference(String stylesheetReference) {
        this.stylesheetReference = stylesheetReference;
    }
    public void setWadlSchemaLocation(String loc) {
        this.wadlSchemaLocation = loc;
    }
    public void setIncludeDefaultWadlSchemaLocation(boolean inc) {
        if (inc) {
            setWadlSchemaLocation(DEFAULT_WADL_SCHEMA_LOC);
        }
    }

    public void setIgnoreOverloadedMethods(boolean ignore) {
        this.ignoreOverloadedMethods = ignore;
    }

    public void setKeepRelativeDocLinks(boolean keepRelativeDocLinks) {
        this.keepRelativeDocLinks = keepRelativeDocLinks;
    }

    public void setApplyStylesheetLocally(boolean applyStylesheetLocally) {
        this.applyStylesheetLocally = applyStylesheetLocally;
    }

    public void setUsePathParamsToCompareOperations(boolean usePathParamsToCompareOperations) {
        this.usePathParamsToCompareOperations = usePathParamsToCompareOperations;
    }

    public void setConvertResourcesToDOM(boolean convertResourcesToDOM) {
        this.convertResourcesToDOM = convertResourcesToDOM;
    }

    public void setIncrementNamespacePrefix(boolean incrementNamespacePrefix) {
        this.incrementNamespacePrefix = incrementNamespacePrefix;
    }
    public void setJaxbContextProperties(Map<String, Object> jaxbContextProperties) {
        this.jaxbContextProperties = jaxbContextProperties;
    }


    public List<String> getAllowList() {  //Liberty change
        return allowList;
    }

    public void setAllowList(List<String> allowList) { //Liberty change
        this.allowList = allowList;
    }

    private static class SchemaConverter extends DelegatingXMLStreamWriter {
        private static final String SCHEMA_LOCATION = "schemaLocation";
        private final Map<String, String> locsMap;

        SchemaConverter(XMLStreamWriter writer, Map<String, String> locsMap) {
            super(writer);
            this.locsMap = locsMap;
        }

        @Override
        public void writeAttribute(String local, String value) throws XMLStreamException {
            if (SCHEMA_LOCATION.equals(local) && locsMap.containsKey(value)) {
                value = locsMap.get(value);
            }
            super.writeAttribute(local, value);
        }
    }


    private class ResourceIdGeneratorImpl implements ResourceIdGenerator {

        @Override
        public String getClassResourceId(ClassResourceInfo cri) {
            Class<?> serviceClass = cri != null ? cri.getServiceClass() : Object.class;
            QName jaxbQname = null;
            if (useJaxbContextForQnames) {
                jaxbQname = getJaxbQName(null, serviceClass, new HashMap<Class<?>, QName>(0));
            }
            String pName = jaxbQname == null ? PackageUtils.getPackageName(serviceClass) : null;
            String localName = jaxbQname == null ? serviceClass.getSimpleName() : jaxbQname.getLocalPart();
            String nsName = jaxbQname == null ? pName + "." : "";
            return nsName + localName;
        }

        @Override
        public String getMethodResourceId(OperationResourceInfo ori) {
            return getMethod(ori).getName();
        }

    }

}
