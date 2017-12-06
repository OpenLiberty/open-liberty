package com.ibm.ws.microprofile.openapi.impl.jaxrs2;

import java.util.Map;
import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.media.Encoding;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;

import com.ibm.ws.microprofile.openapi.impl.core.converter.ModelConverters;
import com.ibm.ws.microprofile.openapi.impl.core.converter.ResolvedSchema;
import com.ibm.ws.microprofile.openapi.impl.core.util.AnnotationsUtils;
import com.ibm.ws.microprofile.openapi.impl.model.media.ContentImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.MediaTypeImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.SchemaImpl;
import com.ibm.ws.microprofile.openapi.impl.model.parameters.RequestBodyImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponseImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponsesImpl;

public class OperationParser {

    public static final String COMPONENTS_REF = "#/components/schemas/";

    public static Optional<RequestBody> getRequestBody(org.eclipse.microprofile.openapi.annotations.parameters.RequestBody requestBody, Consumes classConsumes,
                                                       Consumes methodConsumes, Components components) {
        if (requestBody == null) {
            return Optional.empty();
        }
        RequestBody requestBodyObject = new RequestBodyImpl();
        boolean isEmpty = true;
        if (StringUtils.isNotBlank(requestBody.description())) {
            requestBodyObject.setDescription(requestBody.description());
            isEmpty = false;
        }
        if (requestBody.required()) {
            requestBodyObject.setRequired(requestBody.required());
            isEmpty = false;
        }
        if (isEmpty) {
            return Optional.empty();
        }
        getContent(requestBody.content(), classConsumes == null ? new String[0] : classConsumes.value(),
                   methodConsumes == null ? new String[0] : methodConsumes.value(), components).ifPresent(requestBodyObject::setContent);
        return Optional.of(requestBodyObject);
    }

    public static Optional<APIResponses> getApiResponses(final org.eclipse.microprofile.openapi.annotations.responses.APIResponse[] responses, Produces classProduces,
                                                         Produces methodProduces, Components components) {
        if (responses == null) {
            return Optional.empty();
        }
        APIResponses apiResponsesObject = new APIResponsesImpl();
        for (org.eclipse.microprofile.openapi.annotations.responses.APIResponse response : responses) {
            APIResponse apiResponseObject = new APIResponseImpl();
            if (StringUtils.isNotBlank(response.description())) {
                apiResponseObject.setDescription(response.description());
            }
            getContent(response.content(), classProduces == null ? new String[0] : classProduces.value(),
                       methodProduces == null ? new String[0] : methodProduces.value(), components).ifPresent(apiResponseObject::content);
            AnnotationsUtils.getHeaders(response.headers()).ifPresent(apiResponseObject::headers);
            if (StringUtils.isNotBlank(apiResponseObject.getDescription()) || apiResponseObject.getContent() != null || apiResponseObject.getHeaders() != null) {

                Map<String, Link> links = AnnotationsUtils.getLinks(response.links());
                if (links.size() > 0) {
                    apiResponseObject.setLinks(links);
                }
                if (StringUtils.isNotBlank(response.responseCode())) {
                    apiResponsesObject.addApiResponse(response.responseCode(), apiResponseObject);
                } else {
                    apiResponsesObject.defaultValue(apiResponseObject);
                }
            }
        }
        if (apiResponsesObject.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(apiResponsesObject);
    }

    public static Optional<Content> getContent(org.eclipse.microprofile.openapi.annotations.media.Content[] annotationContents, String[] classTypes, String[] methodTypes,
                                               Components components) {
        if (annotationContents == null) {
            return Optional.empty();
        }

        //Encapsulating Content model
        Content content = new ContentImpl();

        for (org.eclipse.microprofile.openapi.annotations.media.Content annotationContent : annotationContents) {
            MediaType mediaType = new MediaTypeImpl();
            getSchema(annotationContent, components).ifPresent(mediaType::setSchema);

            ExampleObject[] examples = annotationContent.examples();
            for (ExampleObject example : examples) {
                AnnotationsUtils.getExample(example).ifPresent(exampleObject -> mediaType.addExample(example.name(), exampleObject));
            }
            Encoding[] encodings = annotationContent.encoding();
            for (Encoding encoding : encodings) {
                AnnotationsUtils.addEncodingToMediaType(mediaType, encoding);
            }
            if (StringUtils.isNotBlank(annotationContent.mediaType())) {
                content.addMediaType(annotationContent.mediaType(), mediaType);
            } else {
                if (mediaType.getSchema() != null) {
                    AnnotationsUtils.applyTypes(classTypes, methodTypes, content, mediaType);
                }
            }
        }
        if (content.size() == 0) {
            return Optional.empty();
        }
        return Optional.of(content);
    }

    public static Optional<? extends Schema> getSchema(org.eclipse.microprofile.openapi.annotations.media.Content annotationContent, Components components) {
        Class<?> schemaImplementation = annotationContent.schema().implementation();
        boolean isArray = false;
        if (annotationContent.schema().type() == org.eclipse.microprofile.openapi.annotations.enums.SchemaType.ARRAY) {
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
            Optional<Schema> schemaFromAnnotation = AnnotationsUtils.getSchemaFromAnnotation(annotationContent.schema());
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

}
