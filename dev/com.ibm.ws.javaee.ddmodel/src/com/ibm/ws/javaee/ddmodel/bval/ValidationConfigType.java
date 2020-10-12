/*******************************************************************************
 * Copyright (c) 2014,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.bval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.bval.DefaultValidatedExecutableTypes;
import com.ibm.ws.javaee.dd.bval.ExecutableValidation;
import com.ibm.ws.javaee.dd.bval.Property;
import com.ibm.ws.javaee.dd.bval.ValidationConfig;
import com.ibm.ws.javaee.ddmodel.BooleanType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableList;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.StringType;
import com.ibm.ws.javaee.ddmodel.TokenType;
import com.ibm.ws.javaee.ddmodel.common.DescriptionGroup;

public class ValidationConfigType extends DescriptionGroup implements ValidationConfig, DDParser.RootParsable {
    public ValidationConfigType(String path) {
        this.path = path;
    }

    @Override
    public int getVersionID() {
        return versionId;
    }

    @Override
    public String getDefaultProvider() {
        return default_provider != null ? default_provider.getValue() : null;
    }

    @Override
    public String getMessageInterpolator() {
        return message_interpolator != null ? message_interpolator.getValue() : null;
    }

    @Override
    public String getTraversableResolver() {
        return traversable_resolver != null ? traversable_resolver.getValue() : null;
    }

    @Override
    public String getConstraintValidatorFactory() {
        return constraint_validator_factory != null ? constraint_validator_factory.getValue() : null;
    }

    @Override
    public String getParameterNameProvider() {
        return parameter_name_provider != null ? parameter_name_provider.getValue() : null;
    }

    @Override
    public ExecutableValidation getExecutableValidation() {
        return executable_validation;
    }

    @Override
    public List<String> getConstraintMappings() {
        if (constraint_mapping != null) {
            return constraint_mapping.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Property> getProperties() {
        if (property != null) {
            return property.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public String getDeploymentDescriptorPath() {
        return path;
    }

    @Override
    public Object getComponentForId(String id) {
        return idMap.getComponentForId(id);
    }

    @Override
    public String getIdForComponent(Object ddComponent) {
        return idMap.getIdForComponent(ddComponent);
    }

    final String path;
    DDParser.ComponentIDMap idMap;

    int versionId;
    TokenType version;

    private StringType default_provider;
    private StringType message_interpolator;
    private StringType traversable_resolver;
    private StringType constraint_validator_factory;
    private StringType parameter_name_provider;
    private ExecutableValidationType executable_validation;
    private StringType.ListType constraint_mapping;
    private PropertyType.ListType property;

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
        if (nsURI == null) {
            if ("version".equals(localName)) {
                version = parser.parseTokenAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public void finish(DDParser parser) throws ParseException {
        super.finish(parser);

        this.versionId = parser.version;
        this.idMap = parser.idMap;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }
        if ("default-provider".equals(localName)) {
            StringType default_provider = new StringType();
            parser.parse(default_provider);
            this.default_provider = default_provider;
            return true;
        }
        if ("message-interpolator".equals(localName)) {
            StringType message_interpolator = new StringType();
            parser.parse(message_interpolator);
            this.message_interpolator = message_interpolator;
            return true;
        }
        if ("traversable-resolver".equals(localName)) {
            StringType traversable_resolver = new StringType();
            parser.parse(traversable_resolver);
            this.traversable_resolver = traversable_resolver;
            return true;
        }
        if ("constraint-validator-factory".equals(localName)) {
            StringType constraint_validator_factory = new StringType();
            parser.parse(constraint_validator_factory);
            this.constraint_validator_factory = constraint_validator_factory;
            return true;
        }
        if ("parameter-name-provider".equals(localName)) {
            StringType parameter_name_provider = new StringType();
            parser.parse(parameter_name_provider);
            this.parameter_name_provider = parameter_name_provider;
            return true;
        }
        if ("executable-validation".equals(localName)) {
            ExecutableValidationType executable_validation = new ExecutableValidationType();
            parser.parse(executable_validation);
            this.executable_validation = executable_validation;
            return true;
        }
        if ("constraint-mapping".equals(localName)) {
            StringType constraint_mapping = new StringType();
            parser.parse(constraint_mapping);
            addConstraintMapping(constraint_mapping);
            return true;
        }
        if ("property".equals(localName)) {
            PropertyType property = new PropertyType();
            parser.parse(property);
            addProperty(property);
            return true;
        }
        return false;
    }

    private void addConstraintMapping(StringType constraint) {
        if (this.constraint_mapping == null) {
            this.constraint_mapping = new StringType.ListType();
        }
        this.constraint_mapping.add(constraint);
    }

    private void addProperty(PropertyType property) {
        if (this.property == null) {
            this.property = new PropertyType.ListType();
        }
        this.property.add(property);
    }

    static class PropertyType extends DDParser.ElementContentParsable implements Property {

        public static class ListType extends ParsableListImplements<PropertyType, Property> {
            @Override
            public PropertyType newInstance(DDParser parser) {
                return new PropertyType();
            }
        }

        @Override
        public String getName() {
            return name != null ? name.getValue() : null;
        }

        @Override
        public String getValue() {
            return value.getValue();
        }

        // name attribute for the property
        StringType name;

        // property value
        StringType value;

        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
            if ("name".equals(localName)) {
                name = parser.parseStringAttributeValue(index);
                return true;
            }

            return false;
        }

        @Override
        public boolean handleContent(DDParser parser) throws ParseException {
            parser.appendTextToContent();
            return true;
        }

        @Override
        public void finish(DDParser parser) throws ParseException {
            if (name == null) {
                throw new ParseException(parser.requiredAttributeMissing("name"));
            }

            if (!isNil()) {
                value = parser.parseString(parser.getContentString());
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describe("name", name);
            diag.describe("value", value);
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            return false;
        }
    }

    static class ExecutableValidationType extends DDParser.ElementContentParsable implements ExecutableValidation {

        @Override
        public DefaultValidatedExecutableTypes getDefaultValidatedExecutableTypes() {
            return default_validated_executable_types;
        }

        @Override
        public boolean getEnabled() {
            return enabled != null ? enabled.getBooleanValue() : true;
        }

        BooleanType enabled;
        DefaultValidatedExecutableTypesType default_validated_executable_types;

        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
            if ("enabled".equals(localName)) {
                enabled = parser.parseBooleanAttributeValue(index);
                return true;
            }

            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            // EMPTY
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("default-validated-executable-types".equals(localName)) {
                DefaultValidatedExecutableTypesType default_validated_executable_types = new DefaultValidatedExecutableTypesType();
                parser.parse(default_validated_executable_types);
                this.default_validated_executable_types = default_validated_executable_types;
                return true;
            }
            return false;
        }

    }

    static class DefaultValidatedExecutableTypesType extends DDParser.ElementContentParsable implements DefaultValidatedExecutableTypes {

        @Override
        public List<ExecutableTypeEnum> getExecutableTypes() {
            if (executable_type != null) {
                return executable_type.getList();
            } else {
                return Collections.emptyList();
            }
        }

        ExecutableTypeType.ListType executable_type;

        @Override
        public boolean handleContent(DDParser parser) throws ParseException {
            parser.appendTextToContent();
            return true;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            // EMPTY
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("executable-type".equals(localName)) {
                ExecutableTypeType executable_type = new ExecutableTypeType();
                parser.parse(executable_type);
                addExecutableType(executable_type);
                return true;
            }
            return false;
        }

        private void addExecutableType(ExecutableTypeType type) {
            if (this.executable_type == null) {
                this.executable_type = new ExecutableTypeType.ListType();
            }
            this.executable_type.add(type);
        }

    }

    static class ExecutableTypeType extends TokenType {
        public static class ListType extends ParsableList<ExecutableTypeType> {
            @Override
            public ExecutableTypeType newInstance(DDParser parser) {
                return new ExecutableTypeType();
            }

            public List<DefaultValidatedExecutableTypes.ExecutableTypeEnum> getList() {
                List<DefaultValidatedExecutableTypes.ExecutableTypeEnum> values =
                                new ArrayList<DefaultValidatedExecutableTypes.ExecutableTypeEnum>();
                for (ExecutableTypeType type : list) {
                    values.add(type.value);
                }
                return values;
            }
        }

        // content
        DefaultValidatedExecutableTypes.ExecutableTypeEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                value = parseEnumValue(parser, DefaultValidatedExecutableTypes.ExecutableTypeEnum.class);
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeEnum(value);
        }
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describe("version", version);
        super.describe(diag);
        diag.describeIfSet("default-provider", default_provider);
        diag.describeIfSet("message-interpolator", message_interpolator);
        diag.describeIfSet("traversable-resolver", traversable_resolver);
        diag.describeIfSet("constraint-validator-factory", constraint_validator_factory);
        diag.describeIfSet("constraint-mapping", constraint_mapping);
        diag.describeIfSet("property", property);

    }

    @Override
    protected String toTracingSafeString() {
        return "validation-config";
    }

    @Override
    public void describe(StringBuilder sb) {
        DDParser.Diagnostics diag = new DDParser.Diagnostics(idMap, sb);
        diag.describe(toTracingSafeString(), this);
    }
}
