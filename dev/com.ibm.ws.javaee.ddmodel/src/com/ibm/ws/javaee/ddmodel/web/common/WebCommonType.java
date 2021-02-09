/*******************************************************************************
 * Copyright (c) 2011,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.web.common;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.dd.common.Icon;
import com.ibm.ws.javaee.dd.common.Listener;
import com.ibm.ws.javaee.dd.common.MessageDestination;
import com.ibm.ws.javaee.dd.common.ParamValue;
import com.ibm.ws.javaee.dd.common.SecurityRole;
import com.ibm.ws.javaee.dd.jsp.JSPConfig;
import com.ibm.ws.javaee.dd.web.common.AbsoluteOrdering;
import com.ibm.ws.javaee.dd.web.common.DefaultContextPath;
import com.ibm.ws.javaee.dd.web.common.ErrorPage;
import com.ibm.ws.javaee.dd.web.common.Filter;
import com.ibm.ws.javaee.dd.web.common.FilterMapping;
import com.ibm.ws.javaee.dd.web.common.LocaleEncodingMappingList;
import com.ibm.ws.javaee.dd.web.common.LoginConfig;
import com.ibm.ws.javaee.dd.web.common.MimeMapping;
import com.ibm.ws.javaee.dd.web.common.Ordering;
import com.ibm.ws.javaee.dd.web.common.RequestEncoding;
import com.ibm.ws.javaee.dd.web.common.ResponseEncoding;
import com.ibm.ws.javaee.dd.web.common.SecurityConstraint;
import com.ibm.ws.javaee.dd.web.common.Servlet;
import com.ibm.ws.javaee.dd.web.common.ServletMapping;
import com.ibm.ws.javaee.dd.web.common.SessionConfig;
import com.ibm.ws.javaee.dd.web.common.WebCommon;
import com.ibm.ws.javaee.dd.web.common.WelcomeFileList;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.DefaultContextPathType;
import com.ibm.ws.javaee.ddmodel.common.DescriptionType;
import com.ibm.ws.javaee.ddmodel.common.DisplayNameType;
import com.ibm.ws.javaee.ddmodel.common.EJBLocalRefType;
import com.ibm.ws.javaee.ddmodel.common.EJBRefType;
import com.ibm.ws.javaee.ddmodel.common.EnvEntryType;
import com.ibm.ws.javaee.ddmodel.common.IconType;
import com.ibm.ws.javaee.ddmodel.common.JNDIEnvironmentRefsGroup;
import com.ibm.ws.javaee.ddmodel.common.JNDINameType;
import com.ibm.ws.javaee.ddmodel.common.ListenerType;
import com.ibm.ws.javaee.ddmodel.common.MessageDestinationRefType;
import com.ibm.ws.javaee.ddmodel.common.MessageDestinationType;
import com.ibm.ws.javaee.ddmodel.common.ParamValueType;
import com.ibm.ws.javaee.ddmodel.common.RequestEncodingType;
import com.ibm.ws.javaee.ddmodel.common.ResourceEnvRefType;
import com.ibm.ws.javaee.ddmodel.common.ResourceRefType;
import com.ibm.ws.javaee.ddmodel.common.ResponseEncodingType;
import com.ibm.ws.javaee.ddmodel.common.SecurityRoleRefType;
import com.ibm.ws.javaee.ddmodel.common.SecurityRoleType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;
import com.ibm.ws.javaee.ddmodel.common.wsclient.HandlerType;
import com.ibm.ws.javaee.ddmodel.common.wsclient.ServiceRefType;
import com.ibm.ws.javaee.ddmodel.jsp.JSPConfigType;

/*
 <xsd:group name="web-commonType">
 <xsd:choice>
 <xsd:group ref="javaee:descriptionGroup"/>
 <xsd:element name="distributable"
 type="javaee:emptyType"/>
 <xsd:element name="context-param"
 type="javaee:param-valueType">
 </xsd:element>
 <xsd:element name="filter"
 type="javaee:filterType"/>
 <xsd:element name="filter-mapping"
 type="javaee:filter-mappingType"/>
 <xsd:element name="listener"
 type="javaee:listenerType"/>
 <xsd:element name="servlet"
 type="javaee:servletType"/>
 <xsd:element name="servlet-mapping"
 type="javaee:servlet-mappingType"/>
 <xsd:element name="session-config"
 type="javaee:session-configType"/>
 <xsd:element name="mime-mapping"
 type="javaee:mime-mappingType"/>
 <xsd:element name="welcome-file-list"
 type="javaee:welcome-file-listType"/>
 <xsd:element name="error-page"
 type="javaee:error-pageType"/>
 <xsd:element name="jsp-config"
 type="javaee:jsp-configType"/>
 <xsd:element name="deny-uncovered-http-methods"
 type="javaee:emptyType"/>
 <xsd:element name="security-constraint"
 type="javaee:security-constraintType"/>
 <xsd:element name="login-config"
 type="javaee:login-configType"/>
 <xsd:element name="security-role"
 type="javaee:security-roleType"/>
 <xsd:group ref="javaee:jndiEnvironmentRefsGroup"/>
 <xsd:element name="message-destination"
 type="javaee:message-destinationType"/>
 <xsd:element name="locale-encoding-mapping-list"
 type="javaee:locale-encoding-mapping-listType"/>
 </xsd:choice>
 </xsd:group>
 */
/*
 The sub elements under web-app can be in an arbitrary order in this version of the
 specification. Because of the restriction of XML Schema, The multiplicity of the
 elements distributable, session-config, welcome-file-list, jsp-config,
 login-config, and locale-encoding-mapping-list was changed from
 "optional" to "0 or more". The containers must inform the developer with a
 descriptive error message when the deployment descriptor contains more than
 one element of session-config, jsp-config, and login-config. The container
 must concatenate the items in welcome-file-list and locale-encodingmapping-
 list when there are multiple occurrences. The multiple occurrence of
 distributable must be treated exactly in the same way as the single occurrence
 of distributable.
 */
public class WebCommonType extends JNDIEnvironmentRefsGroup implements WebCommon {
    public WebCommonType(String path) {
        this.path = path;
    }

    public String getDeploymentDescriptorPath() {
        return path;
    }

    public Object getComponentForId(String id) {
        return idMap.getComponentForId(id);
    }

    public String getIdForComponent(Object ddComponent) {
        return idMap.getIdForComponent(ddComponent);
    }

    @Override
    public List<Description> getDescriptions() {
        if (description != null) {
            return description.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<DisplayName> getDisplayNames() {
        if (display_name != null) {
            return display_name.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Icon> getIcons() {
        if (icon != null) {
            return icon.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isSetDistributable() {
        return distributable != null;
    }

    @Override
    public List<ParamValue> getContextParams() {
        if (context_param != null) {
            return context_param.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Filter> getFilters() {
        if (filter != null) {
            return filter.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<FilterMapping> getFilterMappings() {
        if (filter_mapping != null) {
            return filter_mapping.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Listener> getListeners() {
        if (listener != null) {
            return listener.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Servlet> getServlets() {
        if (servlet != null) {
            return servlet.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<ServletMapping> getServletMappings() {
        if (servlet_mapping != null) {
            return servlet_mapping.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public SessionConfig getSessionConfig() {
        return session_config;
    }

    @Override
    public List<MimeMapping> getMimeMappings() {
        if (mime_mapping != null) {
            return mime_mapping.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public WelcomeFileList getWelcomeFileList() {
        return welcome_file_list;
    }

    @Override
    public List<ErrorPage> getErrorPages() {
        if (error_page != null) {
            return error_page.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public JSPConfig getJSPConfig() {
        return jsp_config;
    }

    @Override
    public List<SecurityConstraint> getSecurityConstraints() {
        if (security_constraint != null) {
            return security_constraint.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public LoginConfig getLoginConfig() {
        return login_config;
    }

    @Override
    //public DenyUncoveredHttpMethods getDenyUncoveredHttpMethods() {
    //    return deny_uncovered_http_methods;
    //}
    public boolean isSetDenyUncoveredHttpMethods() {
        return deny_uncovered_http_methods != null;
    }

    @Override
    public List<SecurityRole> getSecurityRoles() {
        if (security_role != null) {
            return security_role.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<MessageDestination> getMessageDestinations() {
        if (message_destination != null) {
            return message_destination.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public LocaleEncodingMappingList getLocaleEncodingMappingList() {
        return locale_encoding_mapping_list;
    }

    @Override
    public DefaultContextPath getDefaultContextPath() {
        return default_context_path;
    }

    @Override
    public RequestEncoding getRequestEncoding() {
        return request_encoding;
    }

    @Override
    public ResponseEncoding getResponseEncoding() {
        return response_encoding;
    }

    // choice {
    DescriptionType.ListType description;
    DisplayNameType.ListType display_name;
    IconType.ListType icon;
    EmptyType distributable;
    ParamValueType.ListType context_param;
    FilterType.ListType filter;
    FilterMappingType.ListType filter_mapping;
    ListenerType.ListType listener;
    ServletType.ListType servlet;
    ServletMappingType.ListType servlet_mapping;
    SessionConfigType session_config;
    MimeMappingType.ListType mime_mapping;
    WelcomeFileListType welcome_file_list;
    ErrorPageType.ListType error_page;
    JSPConfigType jsp_config;
    SecurityConstraintType.ListType security_constraint;
    //DenyUncoveredHttpMethodsType deny_uncovered_http_methods;
    EmptyType deny_uncovered_http_methods;
    LoginConfigType login_config;
    SecurityRoleType.ListType security_role;
    // JNDIEnvironmentRefsGroup fields appear here in sequence
    MessageDestinationType.ListType message_destination;
    LocaleEncodingMappingListType locale_encoding_mapping_list;
    DefaultContextPathType default_context_path;
    RequestEncodingType request_encoding;
    ResponseEncodingType response_encoding;
    // } choice

    /*
     * In the javaee6 schemas this appears as identical copies in web-app and web-fragment,
     * placed here for convenience.
     */

    // unique web-common-servlet-name-uniqueness
    Map<XSDTokenType, ServletType> servletNameToServletMap;
    // unique web-common-filter-name-uniqueness
    Map<XSDTokenType, FilterType> filterNameToFilterMap;
    // unique web-common-ejb-local-ref-name-uniqueness
    Map<JNDINameType, EJBLocalRefType> ejbRefNameToEJBLocalRefMap;
    // unique web-common-ejb-ref-name-uniqueness
    Map<JNDINameType, EJBRefType> ejbRefNameToEJBRefMap;
    // unique web-common-resource-env-ref-uniqueness
    Map<JNDINameType, ResourceEnvRefType> resourceEnvRefNameToResourceEnvRefMap;
    // unique web-common-message-destination-ref-uniqueness
    Map<JNDINameType, MessageDestinationRefType> messageDestinationRefNameToMessageDestinationRefMap;
    // unique web-common-res-ref-name-uniqueness
    Map<JNDINameType, ResourceRefType> resRefNameToResourceRefMap;
    // unique web-common-env-entry-name-uniqueness
    Map<JNDINameType, EnvEntryType> envEntryNameToEnvEntryMap;
    // key web-common-role-name-key
    Map<XSDTokenType, SecurityRoleType> roleNameToSecurityRoleMap;
    // keyRef web-common-role-name-references
    Map<XSDTokenType, SecurityRoleRefType> roleLinkToSecurityRoleRefMap;
    // key service-ref_handler-name-key
    Map<ServiceRefType, Map<XSDTokenType, HandlerType>> handlerNameToHandlerMap;

    String path;
    // Component ID map
    DDParser.ComponentIDMap idMap;

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }

        if ("description".equals(localName)) {
            DescriptionType description = new DescriptionType();
            parser.parse(description);
            addDescription(description);
            return true;
        }
        if ("display-name".equals(localName)) {
            DisplayNameType display_name = new DisplayNameType();
            parser.parse(display_name);
            addDisplayName(display_name);
            return true;
        }
        if ("icon".equals(localName)) {
            IconType icon = new IconType();
            parser.parse(icon);
            addIcon(icon);
            return true;
        }
        if ("distributable".equals(localName)) {
            EmptyType distributable = new EmptyType();
            parser.parse(distributable);
            this.distributable = distributable;
            return true;
        }
        if ("context-param".equals(localName)) {
            ParamValueType context_param = new ParamValueType();
            parser.parse(context_param);
            addContextParam(context_param);
            return true;
        }
        if ("filter".equals(localName)) {
            FilterType filter = new FilterType();
            parser.parse(filter);
            addFilter(filter);
            return true;
        }
        if ("filter-mapping".equals(localName)) {
            FilterMappingType filter_mapping = new FilterMappingType();
            parser.parse(filter_mapping);
            addFilterMapping(filter_mapping);
            return true;
        }
        if ("listener".equals(localName)) {
            ListenerType listener = new ListenerType();
            parser.parse(listener);
            addListener(listener);
            return true;
        }
        if ("servlet".equals(localName)) {
            ServletType servlet = new ServletType();
            parser.parse(servlet);
            addServlet(servlet);
            return true;
        }
        if ("servlet-mapping".equals(localName)) {
            ServletMappingType servlet_mapping = new ServletMappingType();
            parser.parse(servlet_mapping);
            addServletMapping(servlet_mapping);
            return true;
        }
        if ("session-config".equals(localName)) {
            SessionConfigType session_config = new SessionConfigType();
            parser.parse(session_config);
            if (this.session_config == null) {
                this.session_config = session_config;
            } else {
                throw new ParseException(parser.tooManyElements("session-config"));
            }
            return true;
        }
        if ("mime-mapping".equals(localName)) {
            MimeMappingType mime_mapping = new MimeMappingType();
            parser.parse(mime_mapping);
            addMimeMapping(mime_mapping);
            return true;
        }
        if ("welcome-file-list".equals(localName)) {
            WelcomeFileListType welcome_file_list;
            if (this.welcome_file_list == null) {
                welcome_file_list = new WelcomeFileListType();
            } else {
                welcome_file_list = this.welcome_file_list;
            }
            parser.parse(welcome_file_list);
            this.welcome_file_list = welcome_file_list;
            return true;
        }
        if ("error-page".equals(localName)) {
            ErrorPageType error_page = new ErrorPageType();
            parser.parse(error_page);
            addErrorPage(error_page);
            return true;
        }
        if ("jsp-config".equals(localName)) {
            JSPConfigType jsp_config = new JSPConfigType();
            parser.parse(jsp_config);
            if (this.jsp_config == null) {
                this.jsp_config = jsp_config;
            } else {
                throw new ParseException(parser.tooManyElements("jsp-config"));
            }
            return true;
        }

        if (parser.version >= 31 && "deny-uncovered-http-methods".equals(localName)) {
            EmptyType deny_uncovered_http_methods = new EmptyType();
            parser.parse(deny_uncovered_http_methods);
            this.deny_uncovered_http_methods = deny_uncovered_http_methods;
            return true;
        }
        if (parser.version < 24 && "taglib".equals(localName)) {
            if (jsp_config == null) {
                jsp_config = new JSPConfigType();
            }
            jsp_config.parseTaglib(parser);
            return true;
        }
        if ("security-constraint".equals(localName)) {
            SecurityConstraintType security_constraint = new SecurityConstraintType();
            parser.parse(security_constraint);
            addSecurityConstraint(security_constraint);
            return true;
        }
        if ("login-config".equals(localName)) {
            LoginConfigType login_config = new LoginConfigType();
            parser.parse(login_config);
            if (this.login_config == null) {
                this.login_config = login_config;
            } else {
                throw new ParseException(parser.tooManyElements("login-config"));
            }
            return true;
        }
        if ("security-role".equals(localName)) {
            SecurityRoleType security_role = new SecurityRoleType();
            parser.parse(security_role);
            addSecurityRole(security_role);
            return true;
        }
        if ("message-destination".equals(localName)) {
            MessageDestinationType message_destination = new MessageDestinationType();
            parser.parse(message_destination);
            addMessageDestination(message_destination);
            return true;
        }
        if ("locale-encoding-mapping-list".equals(localName)) {
            LocaleEncodingMappingListType locale_encoding_mapping_list;
            if (this.locale_encoding_mapping_list == null) {
                locale_encoding_mapping_list = new LocaleEncodingMappingListType();
            } else {
                locale_encoding_mapping_list = this.locale_encoding_mapping_list;
            }
            parser.parse(locale_encoding_mapping_list);
            this.locale_encoding_mapping_list = locale_encoding_mapping_list;
            return true;
        }
        if ("default-context-path".equals(localName)) {
            if (this.default_context_path != null) {
                throw new ParseException(parser.tooManyElements("default-context-path"));
            }

            DefaultContextPathType default_context_path = new DefaultContextPathType();
            parser.parse(default_context_path);

            String value = default_context_path.getValue();
            if ((value.length() >= 1 && value.charAt(0) != '/') || (value.length() > 1 && value.charAt(value.length() - 1) == '/')) {
                throw new ParseException(parser.unexpectedContent());
            }
            this.default_context_path = default_context_path;
            return true;
        }
        if ("request-character-encoding".equals(localName)) {
            RequestEncodingType request_encoding = new RequestEncodingType();
            parser.parse(request_encoding);
            if (this.request_encoding == null) {
                this.request_encoding = request_encoding;
            } else {
                throw new ParseException(parser.tooManyElements("request-character-encoding"));
            }
            return true;
        }
        if ("response-character-encoding".equals(localName)) {
            ResponseEncodingType response_encoding = new ResponseEncodingType();
            parser.parse(response_encoding);
            if (this.response_encoding == null) {
                this.response_encoding = response_encoding;
            } else {
                throw new ParseException(parser.tooManyElements("response-character-encoding"));
            }
            return true;
        }

        return false;
    }

    private void addDescription(DescriptionType description) {
        if (this.description == null) {
            this.description = new DescriptionType.ListType();
        }
        this.description.add(description);
    }

    private void addDisplayName(DisplayNameType display_name) {
        if (this.display_name == null) {
            this.display_name = new DisplayNameType.ListType();
        }
        this.display_name.add(display_name);
    }

    private void addIcon(IconType icon) {
        if (this.icon == null) {
            this.icon = new IconType.ListType();
        }
        this.icon.add(icon);
    }

    private void addContextParam(ParamValueType context_param) {
        if (this.context_param == null) {
            this.context_param = new ParamValueType.ListType();
        }
        this.context_param.add(context_param);
    }

    private void addFilter(FilterType filter) {
        if (this.filter == null) {
            this.filter = new FilterType.ListType();
        }
        this.filter.add(filter);
    }

    private void addFilterMapping(FilterMappingType filter_mapping) {
        if (this.filter_mapping == null) {
            this.filter_mapping = new FilterMappingType.ListType();
        }
        this.filter_mapping.add(filter_mapping);
    }

    private void addListener(ListenerType listener) {
        if (this.listener == null) {
            this.listener = new ListenerType.ListType();
        }
        this.listener.add(listener);
    }

    private void addServlet(ServletType servlet) {
        if (this.servlet == null) {
            this.servlet = new ServletType.ListType();
        }
        this.servlet.add(servlet);
    }

    private void addServletMapping(ServletMappingType servlet_mapping) {
        if (this.servlet_mapping == null) {
            this.servlet_mapping = new ServletMappingType.ListType();
        }
        this.servlet_mapping.add(servlet_mapping);
    }

    private void addMimeMapping(MimeMappingType mime_mapping) {
        if (this.mime_mapping == null) {
            this.mime_mapping = new MimeMappingType.ListType();
        }
        this.mime_mapping.add(mime_mapping);
    }

    private void addErrorPage(ErrorPageType error_page) {
        if (this.error_page == null) {
            this.error_page = new ErrorPageType.ListType();
        }
        this.error_page.add(error_page);
    }

    private void addSecurityConstraint(SecurityConstraintType security_constraint) {
        if (this.security_constraint == null) {
            this.security_constraint = new SecurityConstraintType.ListType();
        }
        this.security_constraint.add(security_constraint);
    }

    private void addSecurityRole(SecurityRoleType security_role) {
        if (this.security_role == null) {
            this.security_role = new SecurityRoleType.ListType();
        }
        this.security_role.add(security_role);
    }

    private void addMessageDestination(MessageDestinationType message_destination) {
        if (this.message_destination == null) {
            this.message_destination = new MessageDestinationType.ListType();
        }
        this.message_destination.add(message_destination);
    }

    @Override
    public void finish(DDParser parser) throws ParseException {
        this.idMap = parser.idMap;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("description", description);
        diag.describeIfSet("display-name", display_name);
        diag.describeIfSet("icon", icon);
        diag.describeIfSet("distributable", distributable);
        diag.describeIfSet("context-param", context_param);
        diag.describeIfSet("filter", filter);
        diag.describeIfSet("filter-mapping", filter_mapping);
        diag.describeIfSet("listener", listener);
        diag.describeIfSet("servlet", servlet);
        diag.describeIfSet("servlet-mapping", servlet_mapping);
        diag.describeIfSet("session-config", session_config);
        diag.describeIfSet("mime-mapping", mime_mapping);
        diag.describeIfSet("welcome-file-list", welcome_file_list);
        diag.describeIfSet("error-page", error_page);
        diag.describeIfSet("jsp-config", jsp_config);
        diag.describeIfSet("deny-uncovered-http-methods", deny_uncovered_http_methods);
        diag.describeIfSet("security-constraint", security_constraint);
        diag.describeIfSet("login-config", login_config);
        diag.describeIfSet("security-role", security_role);
        super.describe(diag);
        diag.describeIfSet("message-destination", message_destination);
        diag.describeIfSet("locale-encoding-mapping-list", locale_encoding_mapping_list);
        diag.describeIfSet("default-context-path", default_context_path);
        diag.describeIfSet("request-character-encoding", request_encoding);
        diag.describeIfSet("response-character-encoding", response_encoding);
    }

    /*
     * <xsd:complexType name="emptyType">
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */

    static class EmptyType extends DDParser.ElementContentParsable {

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            // EMPTY
        }
    }

    /*
     * <xsd:complexType name="ordering-othersType">
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */

    static class OrderingOthersType extends DDParser.ElementContentParsable {

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            // EMPTY
        }
    }

    /*
     * <xsd:complexType name="absoluteOrderingType">
     * <xsd:choice minOccurs="0"
     * maxOccurs="unbounded">
     * <xsd:element name="name"
     * type="javaee:java-identifierType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="others"
     * type="javaee:ordering-othersType"
     * minOccurs="0"
     * maxOccurs="1"/>
     * </xsd:choice>
     * </xsd:complexType>
     */
    static class AbsoluteOrderingType extends DDParser.ElementContentParsable implements AbsoluteOrdering {

        static class ListType extends ParsableListImplements<AbsoluteOrderingType, AbsoluteOrdering> {
            @Override
            public AbsoluteOrderingType newInstance(DDParser parser) {
                return new AbsoluteOrderingType();
            }
        }

        @Override
        public boolean isSetOthers() {
            return others != null;
        }

        @Override
        public List<String> getNamesBeforeOthers() {
            if (name != null) {
                return name.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<String> getNamesAfterOthers() {
            if (name2 != null) {
                return name2.getList();
            } else {
                return Collections.emptyList();
            }
        }

        XSDTokenType.ListType name;
        OrderingOthersType others;
        XSDTokenType.ListType name2;

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("name".equals(localName)) {
                XSDTokenType name = new XSDTokenType();
                parser.parse(name);
                addName(name);
                return true;
            }
            if ("others".equals(localName)) {
                OrderingOthersType others = new OrderingOthersType();
                parser.parse(others);
                this.others = others;
                return true;
            }
            return false;
        }

        private void addName(XSDTokenType name) {
            if (others == null) {
                if (this.name == null) {
                    this.name = new XSDTokenType.ListType();
                }
                this.name.add(name);
            } else {
                if (this.name2 == null) {
                    this.name2 = new XSDTokenType.ListType();
                }
                this.name2.add(name);
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeIfSet("name", name);
            diag.describeIfSet("others", others);
            diag.describeIfSet("name", name2);
        }
    }

    /*
     * <xsd:complexType name="orderingType">
     * <xsd:sequence>
     * <xsd:element name="after"
     * type="javaee:ordering-orderingType"
     * minOccurs="0"
     * maxOccurs="1"/>
     * <xsd:element name="before"
     * type="javaee:ordering-orderingType"
     * minOccurs="0"
     * maxOccurs="1"/>
     * </xsd:sequence>
     * </xsd:complexType>
     */
    static class OrderingType extends DDParser.ElementContentParsable implements Ordering {

        static class ListType extends ParsableListImplements<OrderingType, Ordering> {
            @Override
            public OrderingType newInstance(DDParser parser) {
                return new OrderingType();
            }
        }

        @Override
        public boolean isSetAfter() {
            return after != null;
        }

        @Override
        public List<String> getAfterNames() {
            return after.getNames();
        }

        @Override
        public boolean isSetAfterOthers() {
            return after.isSetOthers();
        }

        @Override
        public boolean isSetBefore() {
            return before != null;
        }

        @Override
        public List<String> getBeforeNames() {
            return before.getNames();
        }

        @Override
        public boolean isSetBeforeOthers() {
            return before.isSetOthers();
        }

        OrderingOrderingType after;
        OrderingOrderingType before;

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("after".equals(localName)) {
                OrderingOrderingType after = new OrderingOrderingType();
                parser.parse(after);
                this.after = after;
                return true;
            }
            if ("before".equals(localName)) {
                OrderingOrderingType before = new OrderingOrderingType();
                parser.parse(before);
                this.before = before;
                return true;
            }
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeIfSet("after", after);
            diag.describeIfSet("before", before);
        }
    }

    /*
     * <xsd:complexType name="ordering-orderingType">
     * <xsd:sequence>
     * <xsd:element name="name"
     * type="javaee:java-identifierType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="others"
     * type="javaee:ordering-othersType"
     * minOccurs="0"
     * maxOccurs="1"/>
     * </xsd:sequence>
     * </xsd:complexType>
     */
    static class OrderingOrderingType extends DDParser.ElementContentParsable {

        public List<String> getNames() {
            if (name != null) {
                return name.getList();
            } else {
                return Collections.emptyList();
            }
        }

        public boolean isSetOthers() {
            return others != null;
        }

        XSDTokenType.ListType name;
        OrderingOthersType others;

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("name".equals(localName)) {
                XSDTokenType name = new XSDTokenType();
                parser.parse(name);
                addName(name);
                return true;
            }
            if ("others".equals(localName)) {
                OrderingOthersType others = new OrderingOthersType();
                parser.parse(others);
                this.others = others;
                return true;
            }
            return false;
        }

        private void addName(XSDTokenType name) {
            if (this.name == null) {
                this.name = new XSDTokenType.ListType();
            }
            this.name.add(name);
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeIfSet("name", name);
            diag.describeIfSet("others", others);
        }
    }
}
