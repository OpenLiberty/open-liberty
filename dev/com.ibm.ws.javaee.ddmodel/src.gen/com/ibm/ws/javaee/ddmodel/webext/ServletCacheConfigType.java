/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// NOTE: This is a generated file. Do not edit it directly.
package com.ibm.ws.javaee.ddmodel.webext;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class ServletCacheConfigType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.webext.ServletCacheConfig {
    public ServletCacheConfigType() {
        this(false);
    }

    public ServletCacheConfigType(boolean xmi) {
        this.xmi = xmi;
    }

    protected final boolean xmi;
    com.ibm.ws.javaee.ddmodel.StringType properties_group_name;
    com.ibm.ws.javaee.ddmodel.webext.IdGenerationPropertiesType id_generation_properties;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.StringType, java.lang.String> servlet_name;
    com.ibm.ws.javaee.ddmodel.IntegerType timeout_value;
    com.ibm.ws.javaee.ddmodel.IntegerType priority_value;
    com.ibm.ws.javaee.ddmodel.BooleanType invalidate_only_value;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.StringType, java.lang.String> external_cache_group_name;
    com.ibm.ws.javaee.ddmodel.StringType id_generator_class;
    com.ibm.ws.javaee.ddmodel.StringType metadata_generator_class;

    @Override
    public java.lang.String getPropertiesGroupName() {
        return properties_group_name != null ? properties_group_name.getValue() : null;
    }

    @Override
    public java.util.List<java.lang.String> getServletNames() {
        if (servlet_name != null) {
            return servlet_name.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean isSetTimeout() {
        return timeout_value != null;
    }

    @Override
    public int getTimeout() {
        return timeout_value != null ? timeout_value.getIntValue() : 0;
    }

    @Override
    public boolean isSetPriority() {
        return priority_value != null;
    }

    @Override
    public int getPriority() {
        return priority_value != null ? priority_value.getIntValue() : 0;
    }

    @Override
    public boolean isSetInvalidateOnly() {
        return invalidate_only_value != null;
    }

    @Override
    public boolean isInvalidateOnly() {
        return invalidate_only_value != null ? invalidate_only_value.getBooleanValue() : false;
    }

    @Override
    public java.util.List<java.lang.String> getExternalCacheGroupNames() {
        if (external_cache_group_name != null) {
            return external_cache_group_name.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.lang.String getIdGenerator() {
        return id_generator_class != null ? id_generator_class.getValue() : null;
    }

    @Override
    public java.lang.String getMetadataGenerator() {
        return metadata_generator_class != null ? metadata_generator_class.getValue() : null;
    }

    @Override
    public com.ibm.ws.javaee.dd.webext.IdGenerationProperties getIdGenerationProperties() {
        return id_generation_properties;
    }

    @Override
    public boolean isIdAllowed() {
        return xmi;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if ((xmi ? "propertiesGroupName" : "properties-group-name").equals(localName)) {
                this.properties_group_name = parser.parseStringAttributeValue(index);
                return true;
            }
            if (xmi && "timeout".equals(localName)) {
                this.timeout_value = parser.parseIntegerAttributeValue(index);
                return true;
            }
            if (xmi && "priority".equals(localName)) {
                this.priority_value = parser.parseIntegerAttributeValue(index);
                return true;
            }
            if (xmi && "invalidateOnly".equals(localName)) {
                this.invalidate_only_value = parser.parseBooleanAttributeValue(index);
                return true;
            }
            if (xmi && "externalCacheGroups".equals(localName)) {
                this.external_cache_group_name = parser.parseStringListAttributeValue(index);
                return true;
            }
            if (xmi && "idGenerator".equals(localName)) {
                this.id_generator_class = parser.parseStringAttributeValue(index);
                return true;
            }
            if (xmi && "metadataGenerator".equals(localName)) {
                this.metadata_generator_class = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if (!xmi && "id-generation-properties".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.webext.IdGenerationPropertiesType id_generation_properties = new com.ibm.ws.javaee.ddmodel.webext.IdGenerationPropertiesType();
            parser.parse(id_generation_properties);
            this.id_generation_properties = id_generation_properties;
            return true;
        }
        if (!xmi && "servlet".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.StringType servlet_name = new com.ibm.ws.javaee.ddmodel.StringType();
            servlet_name.obtainValueFromAttribute("name");
            parser.parse(servlet_name);
            this.addServletName(servlet_name);
            return true;
        }
        if (!xmi && "timeout".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.IntegerType timeout_value = new com.ibm.ws.javaee.ddmodel.IntegerType();
            timeout_value.obtainValueFromAttribute("value");
            parser.parse(timeout_value);
            this.timeout_value = timeout_value;
            return true;
        }
        if (!xmi && "priority".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.IntegerType priority_value = new com.ibm.ws.javaee.ddmodel.IntegerType();
            priority_value.obtainValueFromAttribute("value");
            parser.parse(priority_value);
            this.priority_value = priority_value;
            return true;
        }
        if (!xmi && "invalidate-only".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.BooleanType invalidate_only_value = new com.ibm.ws.javaee.ddmodel.BooleanType();
            invalidate_only_value.obtainValueFromAttribute("value");
            parser.parse(invalidate_only_value);
            this.invalidate_only_value = invalidate_only_value;
            return true;
        }
        if (!xmi && "external-cache-group".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.StringType external_cache_group_name = new com.ibm.ws.javaee.ddmodel.StringType();
            external_cache_group_name.obtainValueFromAttribute("name");
            parser.parse(external_cache_group_name);
            this.addExternalCacheGroupName(external_cache_group_name);
            return true;
        }
        if (!xmi && "id-generator".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.StringType id_generator_class = new com.ibm.ws.javaee.ddmodel.StringType();
            id_generator_class.obtainValueFromAttribute("class");
            parser.parse(id_generator_class);
            this.id_generator_class = id_generator_class;
            return true;
        }
        if (!xmi && "metadata-generator".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.StringType metadata_generator_class = new com.ibm.ws.javaee.ddmodel.StringType();
            metadata_generator_class.obtainValueFromAttribute("class");
            parser.parse(metadata_generator_class);
            this.metadata_generator_class = metadata_generator_class;
            return true;
        }
        return false;
    }

    void addServletName(com.ibm.ws.javaee.ddmodel.StringType servlet_name) {
        if (this.servlet_name == null) {
            this.servlet_name = new com.ibm.ws.javaee.ddmodel.StringType.ListType();
        }
        this.servlet_name.add(servlet_name);
    }

    void addExternalCacheGroupName(com.ibm.ws.javaee.ddmodel.StringType external_cache_group_name) {
        if (this.external_cache_group_name == null) {
            this.external_cache_group_name = new com.ibm.ws.javaee.ddmodel.StringType.ListType();
        }
        this.external_cache_group_name.add(external_cache_group_name);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet(xmi ? "propertiesGroupName" : "properties-group-name", properties_group_name);
        diag.describeIfSet("id-generation-properties", id_generation_properties);
        diag.describeIfSet("servlet[@name]", servlet_name);
        diag.describeIfSet(xmi ? "timeout" : "timeout[@value]", timeout_value);
        diag.describeIfSet(xmi ? "priority" : "priority[@value]", priority_value);
        diag.describeIfSet(xmi ? "invalidateOnly" : "invalidate-only[@value]", invalidate_only_value);
        diag.describeIfSet(xmi ? "externalCacheGroups" : "external-cache-group[@name]", external_cache_group_name);
        diag.describeIfSet(xmi ? "idGenerator" : "id-generator[@class]", id_generator_class);
        diag.describeIfSet(xmi ? "metadataGenerator" : "metadata-generator[@class]", metadata_generator_class);
    }
}
