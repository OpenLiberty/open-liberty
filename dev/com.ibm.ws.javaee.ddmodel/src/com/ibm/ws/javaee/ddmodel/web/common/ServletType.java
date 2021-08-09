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
package com.ibm.ws.javaee.ddmodel.web.common;

import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.ParamValue;
import com.ibm.ws.javaee.dd.common.RunAs;
import com.ibm.ws.javaee.dd.common.SecurityRoleRef;
import com.ibm.ws.javaee.dd.web.common.MultipartConfig;
import com.ibm.ws.javaee.dd.web.common.Servlet;
import com.ibm.ws.javaee.ddmodel.AnySimpleType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.IntegerType;
import com.ibm.ws.javaee.ddmodel.LongType;
import com.ibm.ws.javaee.ddmodel.StringType;
import com.ibm.ws.javaee.ddmodel.common.DescriptionGroup;
import com.ibm.ws.javaee.ddmodel.common.ParamValueType;
import com.ibm.ws.javaee.ddmodel.common.RunAsType;
import com.ibm.ws.javaee.ddmodel.common.SecurityRoleRefType;
import com.ibm.ws.javaee.ddmodel.common.XSDBooleanType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:complexType name="servletType">
 <xsd:sequence>
 <xsd:group ref="javaee:descriptionGroup"/>
 <xsd:element name="servlet-name"
 type="javaee:servlet-nameType"/>
 <xsd:choice minOccurs="0"
 maxOccurs="1">
 <xsd:element name="servlet-class"
 type="javaee:fully-qualified-classType">
 </xsd:element>
 <xsd:element name="jsp-file"
 type="javaee:jsp-fileType"/>
 </xsd:choice>
 <xsd:element name="init-param"
 type="javaee:param-valueType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="load-on-startup"
 type="javaee:load-on-startupType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="enabled"
 type="javaee:true-falseType"
 minOccurs="0"/>
 <xsd:element name="async-supported"
 type="javaee:true-falseType"
 minOccurs="0"/>
 <xsd:element name="run-as"
 type="javaee:run-asType"
 minOccurs="0"/>
 <xsd:element name="security-role-ref"
 type="javaee:security-role-refType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="multipart-config"
 type="javaee:multipart-configType"
 minOccurs="0"
 maxOccurs="1"/>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class ServletType extends DescriptionGroup implements Servlet {

    public static class ListType extends ParsableListImplements<ServletType, Servlet> {
        @Override
        public ServletType newInstance(DDParser parser) {
            return new ServletType();
        }
    }

    @Override
    public String getServletName() {
        return servlet_name.getValue();
    }

    @Override
    public String getServletClass() {
        return servlet_class != null ? servlet_class.getValue() : null;
    }

    @Override
    public String getJSPFile() {
        return jsp_file != null ? jsp_file.getValue() : null;
    }

    @Override
    public List<ParamValue> getInitParams() {
        if (init_param != null) {
            return init_param.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isSetLoadOnStartup() {
        return AnySimpleType.isSet(load_on_startup);
    }

    @Override
    public boolean isNullLoadOnStartup() {
        return load_on_startup != null && load_on_startup.value == null;
    }

    @Override
    public int getLoadOnStartup() {
        return load_on_startup != null && load_on_startup.value != null ? load_on_startup.value.getIntValue() : 0;
    }

    @Override
    public boolean isSetEnabled() {
        return AnySimpleType.isSet(enabled);
    }

    @Override
    public boolean isEnabled() {
        return enabled != null && enabled.getBooleanValue();
    }

    @Override
    public boolean isSetAsyncSupported() {
        return AnySimpleType.isSet(async_supported);
    }

    @Override
    public boolean isAsyncSupported() {
        return async_supported != null && async_supported.getBooleanValue();
    }

    @Override
    public RunAs getRunAs() {
        return run_as;
    }

    @Override
    public List<SecurityRoleRef> getSecurityRoleRefs() {
        if (security_role_ref != null) {
            return security_role_ref.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public MultipartConfig getMultipartConfig() {
        return multipart_config;
    }

    // elements
    // DescriptionGroup fields appear here in sequence
    XSDTokenType servlet_name = new XSDTokenType();
    // choice [0,1] {
    XSDTokenType servlet_class;
    XSDTokenType jsp_file;
    // } choice
    ParamValueType.ListType init_param;
    LoadOnStartupType load_on_startup;
    XSDBooleanType enabled;
    XSDBooleanType async_supported;
    RunAsType run_as;
    SecurityRoleRefType.ListType security_role_ref;
    MultipartConfigType multipart_config;

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }
        if ("servlet-name".equals(localName)) {
            parser.parse(servlet_name);
            return true;
        }
        if ("servlet-class".equals(localName)) {
            XSDTokenType servlet_class = new XSDTokenType();
            parser.parse(servlet_class);
            this.servlet_class = servlet_class;
            return true;
        }
        if ("jsp-file".equals(localName)) {
            XSDTokenType jsp_file = new XSDTokenType();
            parser.parse(jsp_file);
            this.jsp_file = jsp_file;
            return true;
        }
        if ("init-param".equals(localName)) {
            ParamValueType init_param = new ParamValueType();
            parser.parse(init_param);
            addInitParam(init_param);
            return true;
        }
        if ("load-on-startup".equals(localName)) {
            LoadOnStartupType load_on_startup = new LoadOnStartupType();
            parser.parse(load_on_startup);
            this.load_on_startup = load_on_startup;
            return true;
        }
        if ("enabled".equals(localName)) {
            XSDBooleanType enabled = new XSDBooleanType();
            parser.parse(enabled);
            this.enabled = enabled;
            return true;
        }
        if ("async-supported".equals(localName)) {
            XSDBooleanType async_supported = new XSDBooleanType();
            parser.parse(async_supported);
            this.async_supported = async_supported;
            return true;
        }
        if ("run-as".equals(localName)) {
            RunAsType run_as = new RunAsType();
            parser.parse(run_as);
            this.run_as = run_as;
            return true;
        }
        if ("security-role-ref".equals(localName)) {
            SecurityRoleRefType security_role_ref = new SecurityRoleRefType();
            parser.parse(security_role_ref);
            addSecurityRoleRef(security_role_ref);
            return true;
        }
        if ("multipart-config".equals(localName)) {
            MultipartConfigType multipart_config = new MultipartConfigType();
            parser.parse(multipart_config);
            this.multipart_config = multipart_config;
            return true;
        }
        return false;
    }

    private void addInitParam(ParamValueType init_param) {
        if (this.init_param == null) {
            this.init_param = new ParamValueType.ListType();
        }
        this.init_param.add(init_param);
    }

    private void addSecurityRoleRef(SecurityRoleRefType security_role_ref) {
        if (this.security_role_ref == null) {
            this.security_role_ref = new SecurityRoleRefType.ListType();
        }
        this.security_role_ref.add(security_role_ref);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        super.describe(diag);
        diag.describe("servlet-name", servlet_name);
        diag.describeIfSet("servlet-class", servlet_class);
        diag.describeIfSet("jsp-file", jsp_file);
        diag.describeIfSet("init-param", init_param);
        diag.describeIfSet("load-on-startup", load_on_startup);
        diag.describeIfSet("enabled", enabled);
        diag.describeIfSet("async-supported", async_supported);
        diag.describeIfSet("run-as", run_as);
        diag.describeIfSet("security-role-ref", security_role_ref);
        diag.describeIfSet("multipart-config", multipart_config);
    }

    /*
     * <xsd:simpleType name="load-on-startupType">
     * <xsd:union memberTypes="javaee:null-charType xsd:integer"/>
     * </xsd:simpleType>
     */
    static class LoadOnStartupType extends StringType {
        IntegerType value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (isNil() || getValue().length() == 0) {
                value = null;
                return;
            }
            value = parser.parseInteger(getValue());
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            if (value != null) {
                value.describe(diag);
            } else {
                diag.append("\"\"");
            }
        }
    }

    /*
     * <xsd:complexType name="multipart-configType">
     * <xsd:sequence>
     * <xsd:element name="location"
     * type="javaee:xsdTokenType"
     * minOccurs="0"
     * maxOccurs="1">
     * </xsd:element>
     * <xsd:element name="max-file-size"
     * type="xsd:long"
     * minOccurs="0"
     * maxOccurs="1">
     * </xsd:element>
     * <xsd:element name="max-request-size"
     * type="xsd:long"
     * minOccurs="0"
     * maxOccurs="1">
     * </xsd:element>
     * <xsd:element name="file-size-threshold"
     * type="xsd:integer"
     * minOccurs="0"
     * maxOccurs="1">
     * </xsd:element>
     * </xsd:sequence>
     * </xsd:complexType>
     */
    static class MultipartConfigType extends DDParser.ElementContentParsable implements MultipartConfig {

        @Override
        public String getLocation() {
            return location != null ? location.getValue() : null;
        }

        @Override
        public boolean isSetMaxFileSize() {
            return AnySimpleType.isSet(max_file_size);
        }

        @Override
        public long getMaxFileSize() {
            return max_file_size.getLongValue();
        }

        @Override
        public boolean isSetMaxRequestSize() {
            return AnySimpleType.isSet(max_request_size);
        }

        @Override
        public long getMaxRequestSize() {
            return max_request_size.getLongValue();
        }

        @Override
        public boolean isSetFileSizeThreshold() {
            return AnySimpleType.isSet(file_size_threshold);
        }

        @Override
        public int getFileSizeThreshold() {
            return file_size_threshold.getIntValue();
        }

        XSDTokenType location;
        LongType max_file_size;
        LongType max_request_size;
        IntegerType file_size_threshold;

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("location".equals(localName)) {
                XSDTokenType location = new XSDTokenType();
                parser.parse(location);
                this.location = location;
                return true;
            }
            if ("max-file-size".equals(localName)) {
                LongType max_file_size = new LongType();
                parser.parse(max_file_size);
                this.max_file_size = max_file_size;
                return true;
            }
            if ("max-request-size".equals(localName)) {
                LongType max_request_size = new LongType();
                parser.parse(max_request_size);
                this.max_request_size = max_request_size;
                return true;
            }
            if ("file-size-threshold".equals(localName)) {
                IntegerType file_size_threshold = new IntegerType();
                parser.parse(file_size_threshold);
                this.file_size_threshold = file_size_threshold;
                return true;
            }
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeIfSet("location", location);
            diag.describeIfSet("max-file-size", max_file_size);
            diag.describeIfSet("max-request-size", max_request_size);
            diag.describeIfSet("file-size-threshold", file_size_threshold);
        }
    }
}
