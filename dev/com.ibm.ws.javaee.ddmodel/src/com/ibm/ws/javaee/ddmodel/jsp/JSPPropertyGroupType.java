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
package com.ibm.ws.javaee.ddmodel.jsp;

import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.jsp.JSPPropertyGroup;
import com.ibm.ws.javaee.ddmodel.AnySimpleType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.StringType;
import com.ibm.ws.javaee.ddmodel.common.DescriptionGroup;
import com.ibm.ws.javaee.ddmodel.common.XSDBooleanType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:complexType name="jsp-property-groupType">
 <xsd:sequence>
 <xsd:group ref="javaee:descriptionGroup"/>
 <xsd:element name="url-pattern"
 type="javaee:url-patternType"
 maxOccurs="unbounded"/>
 <xsd:element name="el-ignored"
 type="javaee:true-falseType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="page-encoding"
 type="javaee:xsdTokenType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="scripting-invalid"
 type="javaee:true-falseType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="is-xml"
 type="javaee:true-falseType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="include-prelude"
 type="javaee:pathType"
 minOccurs="0"
 maxOccurs="unbounded">
 </xsd:element>
 <xsd:element name="include-coda"
 type="javaee:pathType"
 minOccurs="0"
 maxOccurs="unbounded">
 </xsd:element>
 <xsd:element name="deferred-syntax-allowed-as-literal"
 type="javaee:true-falseType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="trim-directive-whitespaces"
 type="javaee:true-falseType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="default-content-type"
 type="javaee:xsdTokenType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="buffer"
 type="javaee:xsdTokenType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="error-on-undeclared-namespace"
 type="javaee:true-falseType"
 minOccurs="0">
 </xsd:element>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class JSPPropertyGroupType extends DescriptionGroup implements JSPPropertyGroup {

    public static class ListType extends ParsableListImplements<JSPPropertyGroupType, JSPPropertyGroup> {
        @Override
        public JSPPropertyGroupType newInstance(DDParser parser) {
            return new JSPPropertyGroupType();
        }
    }

    @Override
    public List<String> getURLPatterns() {
        return url_pattern.getList();
    }

    @Override
    public boolean isSetElIgnored() {
        return AnySimpleType.isSet(el_ignored);
    }

    @Override
    public boolean isElIgnored() {
        return el_ignored != null && el_ignored.getBooleanValue();
    }

    @Override
    public String getPageEncoding() {
        return page_encoding != null ? page_encoding.getValue() : null;
    }

    @Override
    public boolean isSetScriptingInvalid() {
        return AnySimpleType.isSet(scripting_invalid);
    }

    @Override
    public boolean isScriptingInvalid() {
        return scripting_invalid != null && scripting_invalid.getBooleanValue();
    }

    @Override
    public boolean isSetIsXml() {
        return AnySimpleType.isSet(is_xml);
    }

    @Override
    public boolean isIsXml() {
        return is_xml != null && is_xml.getBooleanValue();
    }

    @Override
    public List<String> getIncludePreludes() {
        if (include_prelude != null) {
            return include_prelude.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getIncludeCodas() {
        if (include_coda != null) {
            return include_coda.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isSetDeferredSyntaxAllowedAsLiteral() {
        return AnySimpleType.isSet(deferred_syntax_allowed_as_literal);
    }

    @Override
    public boolean isDeferredSyntaxAllowedAsLiteral() {
        return deferred_syntax_allowed_as_literal != null && deferred_syntax_allowed_as_literal.getBooleanValue();
    }

    @Override
    public boolean isSetTrimDirectiveWhitespaces() {
        return AnySimpleType.isSet(trim_directive_whitespaces);
    }

    @Override
    public boolean isTrimDirectiveWhitespaces() {
        return trim_directive_whitespaces != null && trim_directive_whitespaces.getBooleanValue();
    }

    @Override
    public String getDefaultContentType() {
        return default_content_type != null ? default_content_type.getValue() : null;
    }

    @Override
    public String getBuffer() {
        return buffer != null ? buffer.getValue() : null;
    }

    @Override
    public boolean isSetErrorOnUndeclaredNamespace() {
        return AnySimpleType.isSet(error_on_undeclared_namespace);
    }

    @Override
    public boolean isErrorOnUndeclaredNamespace() {
        return error_on_undeclared_namespace != null && error_on_undeclared_namespace.getBooleanValue();
    }

    // elements
    // DescriptionGroup fields appear here in sequence
    StringType.ListType url_pattern = new StringType.ListType();
    XSDBooleanType el_ignored;
    XSDTokenType page_encoding;
    XSDBooleanType scripting_invalid;
    XSDBooleanType is_xml;
    XSDTokenType.ListType include_prelude;
    XSDTokenType.ListType include_coda;
    XSDBooleanType deferred_syntax_allowed_as_literal;
    XSDBooleanType trim_directive_whitespaces;
    XSDTokenType default_content_type;
    XSDTokenType buffer;
    XSDBooleanType error_on_undeclared_namespace;

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }
        if ("url-pattern".equals(localName)) {
            StringType url_pattern = new StringType();
            parser.parse(url_pattern);
            this.url_pattern.add(url_pattern);
            return true;
        }
        if ("el-ignored".equals(localName)) {
            XSDBooleanType el_ignored = new XSDBooleanType();
            parser.parse(el_ignored);
            this.el_ignored = el_ignored;
            return true;
        }
        if ("page-encoding".equals(localName)) {
            XSDTokenType page_encoding = new XSDTokenType();
            parser.parse(page_encoding);
            this.page_encoding = page_encoding;
            return true;
        }
        if ("scripting-invalid".equals(localName)) {
            XSDBooleanType scripting_invalid = new XSDBooleanType();
            parser.parse(scripting_invalid);
            this.scripting_invalid = scripting_invalid;
            return true;
        }
        if ("is-xml".equals(localName)) {
            XSDBooleanType is_xml = new XSDBooleanType();
            parser.parse(is_xml);
            this.is_xml = is_xml;
            return true;
        }
        if ("include-prelude".equals(localName)) {
            XSDTokenType include_prelude = new XSDTokenType();
            parser.parse(include_prelude);
            addIncludePrelude(include_prelude);
            return true;
        }
        if ("include-coda".equals(localName)) {
            XSDTokenType include_coda = new XSDTokenType();
            parser.parse(include_coda);
            addIncludeCoda(include_coda);
            return true;
        }
        if ("deferred-syntax-allowed-as-literal".equals(localName)) {
            XSDBooleanType deferred_syntax_allowed_as_literal = new XSDBooleanType();
            parser.parse(deferred_syntax_allowed_as_literal);
            this.deferred_syntax_allowed_as_literal = deferred_syntax_allowed_as_literal;
            return true;
        }
        if ("trim-directive-whitespaces".equals(localName)) {
            XSDBooleanType trim_directive_whitespaces = new XSDBooleanType();
            parser.parse(trim_directive_whitespaces);
            this.trim_directive_whitespaces = trim_directive_whitespaces;
            return true;
        }
        if ("default-content-type".equals(localName)) {
            XSDTokenType default_content_type = new XSDTokenType();
            parser.parse(default_content_type);
            this.default_content_type = default_content_type;
            return true;
        }
        if ("buffer".equals(localName)) {
            XSDTokenType buffer = new XSDTokenType();
            parser.parse(buffer);
            this.buffer = buffer;
            return true;
        }
        if ("error-on-undeclared-namespace".equals(localName)) {
            XSDBooleanType error_on_undeclared_namespace = new XSDBooleanType();
            parser.parse(error_on_undeclared_namespace);
            this.error_on_undeclared_namespace = error_on_undeclared_namespace;
            return true;
        }
        return false;
    }

    private void addIncludePrelude(XSDTokenType include_prelude) {
        if (this.include_prelude == null) {
            this.include_prelude = new XSDTokenType.ListType();
        }
        this.include_prelude.add(include_prelude);
    }

    private void addIncludeCoda(XSDTokenType include_coda) {
        if (this.include_coda == null) {
            this.include_coda = new XSDTokenType.ListType();
        }
        this.include_coda.add(include_coda);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        super.describe(diag);
        diag.describe("url-pattern", url_pattern);
        diag.describeIfSet("el-ignored", el_ignored);
        diag.describeIfSet("page-encoding", page_encoding);
        diag.describeIfSet("scripting-invalid", scripting_invalid);
        diag.describeIfSet("is-xml", is_xml);
        diag.describeIfSet("include-prelude", include_prelude);
        diag.describeIfSet("include-coda", include_coda);
        diag.describeIfSet("deferred-syntax-allowed-as-literal", deferred_syntax_allowed_as_literal);
        diag.describeIfSet("trim-directive-whitespaces", trim_directive_whitespaces);
        diag.describeIfSet("default-content-type", default_content_type);
        diag.describeIfSet("buffer", buffer);
        diag.describeIfSet("error-on-undeclared-namespace", error_on_undeclared_namespace);
    }
}
