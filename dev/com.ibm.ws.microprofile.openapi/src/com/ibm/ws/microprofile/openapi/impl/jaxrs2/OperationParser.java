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

package com.ibm.ws.microprofile.openapi.impl.jaxrs2;

import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.media.Encoding;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;

import com.ibm.ws.microprofile.openapi.impl.core.util.AnnotationsUtils;
import com.ibm.ws.microprofile.openapi.impl.model.media.ContentImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.MediaTypeImpl;
import com.ibm.ws.microprofile.openapi.impl.model.parameters.RequestBodyImpl;

public class OperationParser {

    public static Optional<RequestBody> getRequestBody(org.eclipse.microprofile.openapi.annotations.parameters.RequestBody requestBody, Consumes classConsumes,
                                                       Consumes methodConsumes, Components components) {
        if (requestBody == null) {
            return Optional.empty();
        }
        RequestBody requestBodyObject = new RequestBodyImpl();
        boolean isEmpty = true;
        if (StringUtils.isNotBlank(requestBody.ref())) {
            requestBodyObject.setRef(requestBody.ref());
            isEmpty = false;
        }
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
        return AnnotationsUtils.getApiResponses(responses, classProduces, methodProduces, components, true);
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
                AnnotationsUtils.getExample(example).ifPresent(exampleObject -> mediaType.addExample(AnnotationsUtils.getNameOfReferenceableItem(example), exampleObject));
            }
            Encoding[] encodings = annotationContent.encoding();
            for (Encoding encoding : encodings) {
                AnnotationsUtils.addEncodingToMediaType(mediaType, encoding);
            }
            if (StringUtils.isNotBlank(annotationContent.mediaType())) {
                content.addMediaType(annotationContent.mediaType(), mediaType);
            } else {
                AnnotationsUtils.applyTypes(classTypes, methodTypes, content, mediaType);
            }
        }
        if (content.size() == 0) {
            return Optional.empty();
        }
        return Optional.of(content);
    }

    public static Optional<? extends Schema> getSchema(org.eclipse.microprofile.openapi.annotations.media.Content annotationContent, Components components) {
        return AnnotationsUtils.getSchema(annotationContent.schema(), components);
    }

}
