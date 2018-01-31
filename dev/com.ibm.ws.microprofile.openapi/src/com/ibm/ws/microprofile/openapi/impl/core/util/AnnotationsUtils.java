/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.impl.core.util;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.Produces;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.microprofile.openapi.annotations.links.LinkParameter;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.info.Contact;
import org.eclipse.microprofile.openapi.models.info.License;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.servers.ServerVariables;
import org.eclipse.microprofile.openapi.models.tags.Tag;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.openapi.impl.core.converter.ModelConverters;
import com.ibm.ws.microprofile.openapi.impl.core.converter.ResolvedSchema;
import com.ibm.ws.microprofile.openapi.impl.jaxrs2.OperationParser;
import com.ibm.ws.microprofile.openapi.impl.model.ExternalDocumentationImpl;
import com.ibm.ws.microprofile.openapi.impl.model.examples.ExampleImpl;
import com.ibm.ws.microprofile.openapi.impl.model.headers.HeaderImpl;
import com.ibm.ws.microprofile.openapi.impl.model.info.ContactImpl;
import com.ibm.ws.microprofile.openapi.impl.model.info.InfoImpl;
import com.ibm.ws.microprofile.openapi.impl.model.info.LicenseImpl;
import com.ibm.ws.microprofile.openapi.impl.model.links.LinkImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.ContentImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.EncodingImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.MediaTypeImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.SchemaImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponseImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponsesImpl;
import com.ibm.ws.microprofile.openapi.impl.model.servers.ServerImpl;
import com.ibm.ws.microprofile.openapi.impl.model.servers.ServerVariableImpl;
import com.ibm.ws.microprofile.openapi.impl.model.servers.ServerVariablesImpl;
import com.ibm.ws.microprofile.openapi.impl.model.tags.TagImpl;

public abstract class AnnotationsUtils {

    //private static Logger LOGGER = LoggerFactory.getLogger(AnnotationsUtils.class);

    public static final String COMPONENTS_REF = "#/components/schemas/";

    public static boolean hasSchemaAnnotation(org.eclipse.microprofile.openapi.annotations.media.Schema schema) {
        if (schema == null) {
            return false;
        }
        if (StringUtils.isBlank(schema.type().toString())
            && StringUtils.isBlank(schema.format())
            && StringUtils.isBlank(schema.title())
            && StringUtils.isBlank(schema.description())
            && StringUtils.isBlank(schema.ref())
            && StringUtils.isBlank(schema.name())
            && schema.multipleOf() == 0
            && StringUtils.isBlank(schema.maximum())
            && StringUtils.isBlank(schema.minimum())
            && !schema.exclusiveMinimum()
            && !schema.exclusiveMaximum()
            && schema.maxLength() == Integer.MAX_VALUE
            && schema.minLength() == 0
            && schema.minProperties() == 0
            && schema.maxProperties() == 0
            && schema.requiredProperties().length == 0
            && !schema.required()
            && !schema.nullable()
            && !schema.readOnly()
            && !schema.writeOnly()
            && !schema.deprecated()
            && schema.enumeration().length == 0
            && StringUtils.isBlank(schema.defaultValue())
            && schema.implementation().equals(Void.class)
            && StringUtils.isBlank(schema.example())
            && StringUtils.isBlank(schema.pattern())
            && schema.not().equals(Void.class)
            && schema.oneOf().length == 0
            && schema.anyOf().length == 0) {
            return false;
        }
        return true;
    }

    public static boolean hasArrayAnnotation(org.eclipse.microprofile.openapi.annotations.media.ArraySchema array) {
        if (array == null) {
            return false;
        }
        if (array.uniqueItems() == false
            && array.maxItems() == Integer.MIN_VALUE
            && array.minItems() == Integer.MAX_VALUE
            && !hasSchemaAnnotation(array.schema())) {
            return false;
        }
        return true;
    }

    @FFDCIgnore(IOException.class)
    public static Optional<Example> getExample(ExampleObject example) {
        if (example == null) {
            return Optional.empty();
        }

        Example exampleObject = new ExampleImpl();
        boolean isEmpty = true;

        if (StringUtils.isNotBlank(example.description())) {
            exampleObject.setDescription(example.description());
            isEmpty = false;
        }
        if (StringUtils.isNotBlank(example.summary())) {
            exampleObject.setSummary(example.summary());
            isEmpty = false;
        }
        if (StringUtils.isNotBlank(example.externalValue())) {
            exampleObject.setExternalValue(example.externalValue());
            isEmpty = false;
        }
        if (StringUtils.isNotBlank(example.value())) {
            try {
                exampleObject.setValue(Json.mapper().readTree(example.value()));
            } catch (IOException e) {
                exampleObject.setValue(example.value());
            }
            isEmpty = false;
        }

        if (isEmpty) {
            return Optional.empty();
        }

        return Optional.of(exampleObject);
    }

    public static Optional<Schema> getArraySchema(org.eclipse.microprofile.openapi.annotations.media.ArraySchema arraySchema) {
        if (arraySchema == null || !hasArrayAnnotation(arraySchema)) {
            return Optional.empty();
        }
        Schema arraySchemaObject = new SchemaImpl().type(SchemaType.ARRAY);
        if (arraySchema.uniqueItems()) {
            arraySchemaObject.setUniqueItems(arraySchema.uniqueItems());
        }
        if (arraySchema.maxItems() > 0) {
            arraySchemaObject.setMaxItems(arraySchema.maxItems());
        }

        if (arraySchema.minItems() < Integer.MAX_VALUE) {
            arraySchemaObject.setMinItems(arraySchema.minItems());
        }

        if (arraySchema.schema() != null) {
            if (arraySchema.schema().implementation().equals(Void.class)) {
                getSchemaFromAnnotation(arraySchema.schema()).ifPresent(schema -> {
                    if (schema.getType() != null) {
                        arraySchemaObject.setItems(schema);
                    }
                });
            } // if present, schema implementation handled upstream
        }

        return Optional.of(arraySchemaObject);
    }

    @FFDCIgnore(IOException.class)
    public static Optional<Schema> getSchemaFromAnnotation(org.eclipse.microprofile.openapi.annotations.media.Schema schema) {
        if (schema == null || !hasSchemaAnnotation(schema)) {
            return Optional.empty();
        }
        Schema schemaObject = new SchemaImpl();
        if (StringUtils.isNotBlank(schema.description())) {
            schemaObject.setDescription(schema.description());
        }
        if (StringUtils.isNotBlank(schema.ref())) {
            schemaObject.setRef(schema.ref());
        }
        if (StringUtils.isNotBlank(schema.type().toString())) {
            schemaObject.setType(SchemaType.valueOf(schema.type().toString().toUpperCase()));
        }
        if (StringUtils.isNotBlank(schema.defaultValue())) {
            schemaObject.setDefaultValue(schema.defaultValue());
        }
        if (StringUtils.isNotBlank(schema.example())) {
            try {
                schemaObject.setExample(Json.mapper().readTree(schema.example()));
            } catch (IOException e) {
                schemaObject.setExample(schema.example());
            }
        }
        if (StringUtils.isNotBlank(schema.format())) {
            schemaObject.setFormat(schema.format());
        }
        if (StringUtils.isNotBlank(schema.pattern())) {
            schemaObject.setPattern(schema.pattern());
        }
        if (schema.readOnly()) {
            schemaObject.setReadOnly(schema.readOnly());
        }
        if (schema.deprecated()) {
            schemaObject.setDeprecated(schema.deprecated());
        }
        if (schema.exclusiveMaximum()) {
            schemaObject.setExclusiveMaximum(schema.exclusiveMaximum());
        }
        if (schema.exclusiveMinimum()) {
            schemaObject.setExclusiveMinimum(schema.exclusiveMinimum());
        }
        if (schema.maxProperties() > 0) {
            schemaObject.setMaxProperties(schema.maxProperties());
        }
        if (schema.maxLength() != Integer.MAX_VALUE && schema.maxLength() > 0) {
            schemaObject.setMaxLength(schema.maxLength());
        }
        if (schema.minProperties() > 0) {
            schemaObject.setMinProperties(schema.minProperties());
        }
        if (schema.minLength() > 0) {
            schemaObject.setMinLength(schema.minLength());
        }
        if (schema.multipleOf() != 0) {
            schemaObject.setMultipleOf(new BigDecimal(schema.multipleOf()));
        }
        if (NumberUtils.isNumber(schema.maximum())) {
            String filteredMaximum = schema.maximum().replaceAll(Constants.COMMA, StringUtils.EMPTY);
            schemaObject.setMaximum(new BigDecimal(filteredMaximum));
        }
        if (NumberUtils.isNumber(schema.minimum())) {
            String filteredMinimum = schema.minimum().replaceAll(Constants.COMMA, StringUtils.EMPTY);
            schemaObject.setMinimum(new BigDecimal(filteredMinimum));
        }
        if (schema.nullable()) {
            schemaObject.setNullable(schema.nullable());
        }
        if (StringUtils.isNotBlank(schema.title())) {
            schemaObject.setTitle(schema.title());
        }
        if (schema.writeOnly()) {
            schemaObject.setWriteOnly(schema.writeOnly());
        }
        if (schema.requiredProperties().length > 0) {
            schemaObject.setRequired(Arrays.asList(schema.requiredProperties()));
        }
        if (schema.enumeration().length > 0) {
            schemaObject.setEnumeration(Arrays.asList(schema.enumeration()));
        }

        getExternalDocumentation(schema.externalDocs()).ifPresent(schemaObject::setExternalDocs);

        return Optional.of(schemaObject);
    }

    public static Optional<? extends Schema> getSchema(org.eclipse.microprofile.openapi.annotations.media.Schema annotationSchema, Components components) {
        Class<?> schemaImplementation = annotationSchema.implementation();
        boolean isArray = false;
        if (annotationSchema.type() == org.eclipse.microprofile.openapi.annotations.enums.SchemaType.ARRAY) {
            isArray = true;
        }
        Map<String, Schema> schemaMap;
        if (schemaImplementation != Void.class) {
            Schema schemaObject = new SchemaImpl();
            if (schemaImplementation.getName().startsWith("java.lang")) {
                //schemaObject.setType(schemaImplementation.getSimpleName().toLowerCase());
                //TODO: support simple type to schema mapping
            } else {
                ResolvedSchema resolvedSchema = ModelConverters.getInstance().readAllAsResolvedSchema(schemaImplementation);
                if (resolvedSchema != null) {
                    schemaMap = resolvedSchema.referencedSchemas;
                    schemaMap.forEach((key, schema) -> {
                        components.addSchema(key, schema);
                    });
                    schemaObject.setRef(COMPONENTS_REF + ((SchemaImpl) resolvedSchema.schema).getName());
                }
            }
            if (StringUtils.isBlank(schemaObject.getRef()) && schemaObject.getType() == null) {
                // default to string
                schemaObject.setType(SchemaType.STRING);
            }
            if (isArray) {
                Schema arraySchema = new SchemaImpl().type(SchemaType.ARRAY);
                arraySchema.setItems(schemaObject);
                return Optional.of(arraySchema);
            } else {
                return Optional.of(schemaObject);
            }

        } else {
            Optional<Schema> schemaFromAnnotation = AnnotationsUtils.getSchemaFromAnnotation(annotationSchema);
            if (schemaFromAnnotation.isPresent()) {
                if (StringUtils.isBlank(schemaFromAnnotation.get().getRef()) && schemaFromAnnotation.get().getType() == null) {
                    // default to string
                    schemaFromAnnotation.get().setType(SchemaType.STRING);
                }
                return Optional.of(schemaFromAnnotation.get());
            }
//                else {
//                Optional<Schema> arraySchemaFromAnnotation = AnnotationsUtils.getArraySchema(annotationContent.array());
//                if (arraySchemaFromAnnotation.isPresent()) {
//                    if (StringUtils.isBlank(arraySchemaFromAnnotation.get().getItems().getRef()) && arraySchemaFromAnnotation.get().getItems().getType() == null) {
//                        // default to string
//                        arraySchemaFromAnnotation.get().getItems().setType(SchemaType.STRING);
//                    }
//                    return Optional.of(arraySchemaFromAnnotation.get());
//                }
//            }
        }
        return Optional.empty();
    }

    public static Optional<Set<Tag>> getTags(org.eclipse.microprofile.openapi.annotations.tags.Tag[] tags, boolean skipOnlyName) {
        if (tags == null) {
            return Optional.empty();
        }
        Set<Tag> tagsList = new LinkedHashSet<>();
        for (org.eclipse.microprofile.openapi.annotations.tags.Tag tag : tags) {
            if (StringUtils.isBlank(tag.name()) && StringUtils.isBlank(tag.ref())) {
                continue;
            }
            if (skipOnlyName &&
                StringUtils.isBlank(tag.description()) &&
                StringUtils.isBlank(tag.externalDocs().description()) &&
                StringUtils.isBlank(tag.externalDocs().url())) {
                continue;
            }
            Tag tagObject = new TagImpl();
            if (StringUtils.isNotBlank(tag.description())) {
                tagObject.setDescription(tag.description());
            }
            if (StringUtils.isNotBlank(tag.ref())) {
                tagObject.setName(tag.ref());
            } else {
                tagObject.setName(tag.name());
            }
            getExternalDocumentation(tag.externalDocs()).ifPresent(tagObject::setExternalDocs);
            tagsList.add(tagObject);
        }
        if (tagsList.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(tagsList);
    }

    public static Optional<List<Server>> getServers(org.eclipse.microprofile.openapi.annotations.servers.Server[] servers) {
        if (servers == null) {
            return Optional.empty();
        }
        List<Server> serverObjects = new ArrayList<>();
        for (org.eclipse.microprofile.openapi.annotations.servers.Server server : servers) {
            getServer(server).ifPresent(serverObjects::add);
        }
        if (serverObjects.size() == 0) {
            return Optional.empty();
        }
        return Optional.of(serverObjects);
    }

    public static Optional<Server> getServer(org.eclipse.microprofile.openapi.annotations.servers.Server server) {
        if (server == null) {
            return Optional.empty();
        }

        Server serverObject = new ServerImpl();
        boolean isEmpty = true;
        if (StringUtils.isNotBlank(server.url())) {
            serverObject.setUrl(server.url());
            isEmpty = false;
        }
        if (StringUtils.isNotBlank(server.description())) {
            serverObject.setDescription(server.description());
            isEmpty = false;
        }
        if (isEmpty) {
            return Optional.empty();
        }
        org.eclipse.microprofile.openapi.annotations.servers.ServerVariable[] serverVariables = server.variables();
        ServerVariables serverVariablesObject = new ServerVariablesImpl();
        for (org.eclipse.microprofile.openapi.annotations.servers.ServerVariable serverVariable : serverVariables) {
            ServerVariable serverVariableObject = new ServerVariableImpl();
            if (StringUtils.isNotBlank(serverVariable.description())) {
                serverVariableObject.setDescription(serverVariable.description());
            }
            if (StringUtils.isNotBlank(serverVariable.defaultValue())) {
                serverVariableObject.setDefaultValue(serverVariable.defaultValue());
            }
            if (serverVariable.enumeration() != null && serverVariable.enumeration().length > 0) {
                serverVariableObject.setEnumeration(Arrays.asList(serverVariable.enumeration()));
            }
            // TODO extensions
            serverVariablesObject.addServerVariable(serverVariable.name(), serverVariableObject);
        }
        if (serverVariablesObject.size() > 0) {
            serverObject.setVariables(serverVariablesObject);
        }

        return Optional.of(serverObject);
    }

    public static Optional<ExternalDocumentation> getExternalDocumentation(org.eclipse.microprofile.openapi.annotations.ExternalDocumentation externalDocumentation) {
        if (externalDocumentation == null) {
            return Optional.empty();
        }
        boolean isEmpty = true;
        ExternalDocumentation external = new ExternalDocumentationImpl();
        if (StringUtils.isNotBlank(externalDocumentation.description())) {
            isEmpty = false;
            external.setDescription(externalDocumentation.description());
        }
        if (StringUtils.isNotBlank(externalDocumentation.url())) {
            isEmpty = false;
            external.setUrl(externalDocumentation.url());
        }
        if (isEmpty) {
            return Optional.empty();
        }
        return Optional.of(external);
    }

    public static Optional<Info> getInfo(org.eclipse.microprofile.openapi.annotations.info.Info info) {
        if (info == null) {
            return Optional.empty();
        }

        boolean isEmpty = true;
        Info infoObject = new InfoImpl();
        if (StringUtils.isNotBlank(info.description())) {
            infoObject.setDescription(info.description());
            isEmpty = false;
        }
        if (StringUtils.isNotBlank(info.termsOfService())) {
            infoObject.setTermsOfService(info.termsOfService());
            isEmpty = false;
        }
        if (StringUtils.isNotBlank(info.title())) {
            infoObject.setTitle(info.title());
            isEmpty = false;
        }
        if (StringUtils.isNotBlank(info.version())) {
            infoObject.setVersion(info.version());
            isEmpty = false;
        }

        getContact(info.contact()).ifPresent(infoObject::setContact);
        isEmpty &= infoObject.getContact() == null;

        getLicense(info.license()).ifPresent(infoObject::setLicense);
        isEmpty &= infoObject.getLicense() == null;

        if (isEmpty) {
            return Optional.empty();
        }

        return Optional.of(infoObject);
    }

    public static Optional<Contact> getContact(org.eclipse.microprofile.openapi.annotations.info.Contact contact) {
        if (contact == null) {
            return Optional.empty();
        }
        boolean isEmpty = true;
        Contact contactObject = new ContactImpl();
        if (StringUtils.isNotBlank(contact.email())) {
            contactObject.setEmail(contact.email());
            isEmpty = false;
        }
        if (StringUtils.isNotBlank(contact.name())) {
            contactObject.setName(contact.name());
            isEmpty = false;
        }
        if (StringUtils.isNotBlank(contact.url())) {
            contactObject.setUrl(contact.url());
            isEmpty = false;
        }
        if (isEmpty) {
            return Optional.empty();
        }
        return Optional.of(contactObject);
    }

    public static Optional<License> getLicense(org.eclipse.microprofile.openapi.annotations.info.License license) {
        if (license == null) {
            return Optional.empty();
        }
        License licenseObject = new LicenseImpl();
        boolean isEmpty = true;
        if (StringUtils.isNotBlank(license.name())) {
            licenseObject.setName(license.name());
            isEmpty = false;
        }
        if (StringUtils.isNotBlank(license.url())) {
            licenseObject.setUrl(license.url());
            isEmpty = false;
        }
        if (isEmpty) {
            return Optional.empty();
        }
        return Optional.of(licenseObject);
    }

    public static Map<String, Link> getLinks(org.eclipse.microprofile.openapi.annotations.links.Link[] links) {
        Map<String, Link> linkMap = new HashMap<>();
        if (links == null) {
            return linkMap;
        }
        for (org.eclipse.microprofile.openapi.annotations.links.Link link : links) {
            getLink(link).ifPresent(linkResult -> linkMap.put(link.name(), linkResult));
        }
        return linkMap;
    }
    
    @FFDCIgnore(IOException.class)    
    public static Optional<Link> getLink(org.eclipse.microprofile.openapi.annotations.links.Link link) {
        if (link == null) {
            return Optional.empty();
        }
        boolean isEmpty = true;
        Link linkObject = new LinkImpl();
        if (StringUtils.isNotBlank(link.description())) {
            linkObject.setDescription(link.description());
            isEmpty = false;
        }
        if (StringUtils.isNotBlank(link.operationId())) {
            linkObject.setOperationId(link.operationId());
            isEmpty = false;
            if (StringUtils.isNotBlank(link.operationRef())) {
                //LOGGER.debug("OperationId and OperatonRef are mutually exclusive, there must be only one setted");
            }
        } else {
            if (StringUtils.isNotBlank(link.operationRef())) {
                linkObject.setOperationRef(link.operationRef());
                isEmpty = false;
            }
        }

        if (StringUtils.isNotBlank(link.requestBody())) {
            try {
                linkObject.setRequestBody(Json.mapper().readTree(link.requestBody()));
            } catch (IOException e) {
                linkObject.setRequestBody(link.requestBody());
            }
            isEmpty = false;
        }

        Optional<Server> server = getServer(link.server());
        if (server.isPresent()) {
            linkObject.setServer(server.get());
            isEmpty = false;
        }
        
        if (isEmpty) {
            return Optional.empty();
        }
        Map<String, Object> linkParameters = getLinkParameters(link.parameters());
        if (linkParameters.size() > 0) {
            linkObject.setParameters(linkParameters);
        }
        return Optional.of(linkObject);
    }

    public static Map<String, Object> getLinkParameters(LinkParameter[] linkParameter) {
        Map<String, Object> linkParametersMap = new HashMap<>();
        if (linkParameter == null) {
            return linkParametersMap;
        }
        for (LinkParameter parameter : linkParameter) {
            linkParametersMap.put(parameter.name(), parameter.expression());
        }

        return linkParametersMap;
    }

    public static Optional<Map<String, Header>> getHeaders(org.eclipse.microprofile.openapi.annotations.headers.Header[] annotationHeaders) {
        if (annotationHeaders == null) {
            return Optional.empty();
        }

        Map<String, Header> headers = new HashMap<>();
        for (org.eclipse.microprofile.openapi.annotations.headers.Header header : annotationHeaders) {
            getHeader(header).ifPresent(headerResult -> headers.put(header.name(), headerResult));
        }

        if (headers.size() == 0) {
            return Optional.empty();
        }
        return Optional.of(headers);
    }

    public static Optional<Header> getHeader(org.eclipse.microprofile.openapi.annotations.headers.Header header) {

        if (header == null) {
            return Optional.empty();
        }

        Header headerObject = new HeaderImpl();
        boolean isEmpty = true;
        if (StringUtils.isNotBlank(header.description())) {
            headerObject.setDescription(header.description());
            isEmpty = false;
        }
        if (header.deprecated()) {
            headerObject.setDeprecated(header.deprecated());
        }
        if (header.required()) {
            headerObject.setRequired(header.required());
            isEmpty = false;
        }
        if (header.allowEmptyValue()) {
            headerObject.setAllowEmptyValue(header.allowEmptyValue());
            isEmpty = false;
        }

        headerObject.setStyle(Header.Style.SIMPLE);

        if (header.schema() != null) {
            if (header.schema().implementation().equals(Void.class)) {
                AnnotationsUtils.getSchemaFromAnnotation(header.schema()).ifPresent(schema -> {
                    if (schema.getType() != null) {
                        headerObject.setSchema(schema);
                        //schema inline no need to add to components
                        //components.addSchemas(schema.getType(), schema);
                    }
                });
            }
        }

        if (isEmpty) {
            return Optional.empty();
        }

        return Optional.of(headerObject);
    }

    public static void addEncodingToMediaType(MediaType mediaType, org.eclipse.microprofile.openapi.annotations.media.Encoding encoding) {
        if (encoding == null) {
            return;
        }

        Encoding encodingObject = new EncodingImpl();
        boolean isEmpty = true;

        if (StringUtils.isNotBlank(encoding.contentType())) {
            encodingObject.setContentType(encoding.contentType());
            isEmpty = false;
        }
        if (StringUtils.isNotBlank(encoding.style())) {
            //TODO handle exception due to incorrect enum value
            encodingObject.setStyle(Encoding.Style.valueOf(encoding.style().toUpperCase()));
            isEmpty = false;
        }
        if (encoding.explode()) {
            encodingObject.setExplode(encoding.explode());
            isEmpty = false;
        }
        if (encoding.allowReserved()) {
            encodingObject.setAllowReserved(encoding.allowReserved());
            isEmpty = false;
        }

        if (encoding.headers() != null) {
            Optional<Map<String, Header>> optHeaders = getHeaders(encoding.headers());
            if (optHeaders.isPresent()) {
                encodingObject.headers(optHeaders.get());
                isEmpty = false;
            }
        }

        if (!isEmpty) {
            mediaType.addEncoding(encoding.name(), encodingObject);
        }
    }

    public static Type getSchemaType(org.eclipse.microprofile.openapi.annotations.media.Schema schema) {
        if (schema == null) {
            return String.class;
        }
        String schemaType = schema.type().toString();
        Class schemaImplementation = schema.implementation();

        if (!schemaImplementation.equals(Void.class)) {
            return schemaImplementation;
        } else if (StringUtils.isBlank(schemaType)) {
            return String.class;
        }
        switch (schemaType) {
            case "number":
                return BigDecimal.class;
            case "integer":
                return Long.class;
            case "boolean":
                return Boolean.class;
            default:
                return String.class;
        }
    }

    public static Optional<Content> getContent(org.eclipse.microprofile.openapi.annotations.media.Content[] annotationContents, String[] classTypes, String[] methodTypes,
                                               Schema schema) {
        if (annotationContents == null || annotationContents.length == 0) {
            return Optional.empty();
        }

        //Encapsulating Content model
        Content content = new ContentImpl();

        org.eclipse.microprofile.openapi.annotations.media.Content annotationContent = annotationContents[0];
        MediaType mediaType = new MediaTypeImpl();
        mediaType.setSchema(schema);

        ExampleObject[] examples = annotationContent.examples();
        for (ExampleObject example : examples) {
            getExample(example).ifPresent(exampleObject -> mediaType.addExample(example.name(), exampleObject));
        }
        org.eclipse.microprofile.openapi.annotations.media.Encoding[] encodings = annotationContent.encoding();
        for (org.eclipse.microprofile.openapi.annotations.media.Encoding encoding : encodings) {
            addEncodingToMediaType(mediaType, encoding);
        }
        if (StringUtils.isNotBlank(annotationContent.mediaType())) {
            content.addMediaType(annotationContent.mediaType(), mediaType);
        } else {
            if (mediaType.getSchema() != null) {
                applyTypes(classTypes, methodTypes, content, mediaType);
            }
        }
        if (content.size() == 0) {
            return Optional.empty();
        }
        return Optional.of(content);
    }

    public static Optional<APIResponses> getApiResponses(final org.eclipse.microprofile.openapi.annotations.responses.APIResponse[] responses, Produces classProduces,
                                                         Produces methodProduces, Components components, boolean useResponseCodeAsKey) {
        if (responses == null) {
            return Optional.empty();
        }
        APIResponses apiResponsesObject = new APIResponsesImpl();
        for (org.eclipse.microprofile.openapi.annotations.responses.APIResponse response : responses) {
            APIResponse apiResponseObject = new APIResponseImpl();
            if (StringUtils.isNotBlank(response.description())) {
                apiResponseObject.setDescription(response.description());
            }
            OperationParser.getContent(response.content(), classProduces == null ? new String[0] : classProduces.value(),
                                       methodProduces == null ? new String[0] : methodProduces.value(), components).ifPresent(apiResponseObject::content);
            AnnotationsUtils.getHeaders(response.headers()).ifPresent(apiResponseObject::headers);

            Map<String, Link> links = AnnotationsUtils.getLinks(response.links());
            if (links.size() > 0) {
                apiResponseObject.setLinks(links);
            }

            if (useResponseCodeAsKey) {
                //Add the response object using the response code (for operations)
                if (StringUtils.isNotBlank(response.responseCode())) {
                    apiResponsesObject.addApiResponse(response.responseCode(), apiResponseObject);
                } else {
                    apiResponsesObject.defaultValue(apiResponseObject);
                }
            } else {
                // Add the response object using the name of response (for components)
                apiResponsesObject.addApiResponse(response.name(), apiResponseObject);
            }

        }
        if (apiResponsesObject.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(apiResponsesObject);
    }

    public static void applyTypes(String[] classTypes, String[] methodTypes, Content content, MediaType mediaType) {
        if (methodTypes != null && methodTypes.length > 0) {
            for (String value : methodTypes) {
                content.addMediaType(value, mediaType);
            }
        } else if (classTypes != null && classTypes.length > 0) {
            for (String value : classTypes) {
                content.addMediaType(value, mediaType);
            }
        } else {
            content.addMediaType(ParameterProcessor.MEDIA_TYPE, mediaType);
        }

    }

    public static org.eclipse.microprofile.openapi.annotations.media.Schema getSchemaAnnotation(Annotated a) {
        if (a == null) {
            return null;
        }
        org.eclipse.microprofile.openapi.annotations.media.ArraySchema arraySchema = a.getAnnotation(org.eclipse.microprofile.openapi.annotations.media.ArraySchema.class);
        if (arraySchema != null) {
            return arraySchema.schema();
        } else {
            return a.getAnnotation(org.eclipse.microprofile.openapi.annotations.media.Schema.class);
        }
    }

    public static org.eclipse.microprofile.openapi.annotations.media.Schema getSchemaAnnotation(Class<?> cls) {
        if (cls == null) {
            return null;
        }
        org.eclipse.microprofile.openapi.annotations.media.Schema mp = null;
        org.eclipse.microprofile.openapi.annotations.media.ArraySchema as = cls.getAnnotation(org.eclipse.microprofile.openapi.annotations.media.ArraySchema.class);
        if (as != null) {
            mp = as.schema();
        } else {
            mp = cls.getAnnotation(org.eclipse.microprofile.openapi.annotations.media.Schema.class);
        }
        return mp;
    }
}
