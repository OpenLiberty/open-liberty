/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.merge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.Reference;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

import io.openliberty.microprofile.openapi20.merge.NameProcessor.DocumentNameProcessor;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelVisitor;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;

/**
 * Visitor which updates references within an OpenAPI model after things have been renamed
 * <p>
 * Updates:
 * <ul>
 * <li>references to objects in {@link OpenAPI#getComponents()}
 * <li>references from links to operationIds
 * <li>references to tags from operations
 * </ul>
 */
public class RenameReferenceVisitor implements OpenAPIModelVisitor {

    private final DocumentNameProcessor documentNameProcessor;

    /**
     * Creates a RenameReferenceVisitor
     * 
     * @param documentNameProcessor the {@link DocumentNameProcessor} holding the renames for the model
     */
    public RenameReferenceVisitor(DocumentNameProcessor documentNameProcessor) {
        this.documentNameProcessor = documentNameProcessor;
    }

    private void updateReference(Reference<?> referrer, NameType nameType) {
        String reference = referrer.getRef();
        String prefix = nameType.getReferencePrefix();
        if (reference != null && reference.startsWith(prefix)) {
            String oldName = reference.split("/")[3];
            String newName = documentNameProcessor.lookupName(nameType, oldName);
            if (!oldName.equals(newName)) {
                referrer.setRef(prefix + newName);
            }
        }
    }

    @Override
    public Example visitExample(Context context, Example example) {
        updateReference(example, NameType.EXAMPLES);
        return example;
    }

    @Override
    public Example visitExample(Context context, String key, Example example) {
        return visitExample(context, example);
    }

    @Override
    public Header visitHeader(Context context, String key, Header header) {
        updateReference(header, NameType.HEADERS);
        return header;
    }

    @Override
    public Link visitLink(Context context, String key, Link link) {
        if (link.getRef() != null) {
            updateReference(link, NameType.LINKS);
        } else {
            link.setOperationId(documentNameProcessor.lookupName(NameType.OPERATION_ID, link.getOperationId()));
            link.setOperationRef(updateOperationRef(link.getOperationRef()));
        }
        return link;
    }

    private static final Pattern PATH_REF_PATTERN = Pattern.compile("#/paths/([^/]+)/.*");

    /**
     * References to operations include a path as part of the reference which may have been renamed as part of the merge process.
     * <p>
     * This method updates the operation reference to use the new path if necessary
     * 
     * @param operationRef the operation reference, may be {@code null}
     * @return the updated operation reference
     */
    private String updateOperationRef(String operationRef) {
        if (operationRef == null) {
            return null;
        }

        Matcher m = PATH_REF_PATTERN.matcher(operationRef);
        if (!m.matches()) {
            return operationRef;
        }

        String oldPath = decodePathReference(m.group(1));
        String newPath = documentNameProcessor.lookupName(NameType.PATHS, oldPath);

        if (!oldPath.equals(newPath)) {
            String newRef = operationRef.substring(0, m.start(1))
                            + encodePathReference(newPath)
                            + operationRef.substring(m.end(1));
            return newRef;
        } else {
            return operationRef;
        }

    }

    private static String decodePathReference(String pathReference) {
        return pathReference.replace("~1", "/").replace("~0", "~");
    }

    private static String encodePathReference(String path) {
        return path.replace("~", "~0").replace("/", "~1");
    }

    @Override
    public Parameter visitParameter(Context context, Parameter p) {
        updateReference(p, NameType.PARAMETERS);
        return p;
    }

    @Override
    public Parameter visitParameter(Context context, String key, Parameter p) {
        return visitParameter(context, p);
    }

    @Override
    public RequestBody visitRequestBody(Context context, RequestBody rb) {
        updateReference(rb, NameType.REQUEST_BODIES);
        return rb;
    }

    @Override
    public RequestBody visitRequestBody(Context context, String key, RequestBody rb) {
        return visitRequestBody(context, rb);
    }

    @Override
    public APIResponse visitResponse(Context context, String key, APIResponse response) {
        updateReference(response, NameType.RESPONSES);
        return response;
    }

    @Override
    public Schema visitSchema(Context context, Schema schema) {
        updateReference(schema, NameType.SCHEMAS);
        return schema;
    }

    @Override
    public Schema visitSchema(Context context, String key, Schema schema) {
        return visitSchema(context, schema);
    }

    @Override
    public SecurityScheme visitSecurityScheme(Context context, String key, SecurityScheme scheme) {
        updateReference(scheme, NameType.SECURITY_SCHEMES);
        return scheme;
    }

    @Override
    public SecurityRequirement visitSecurityRequirement(Context context, SecurityRequirement secReq) {
        Map<String, List<String>> schemes = secReq.getSchemes();
        if (schemes != null) {
            Map<String, List<String>> newSchemes = new HashMap<>();
            for (Entry<String, List<String>> entry : schemes.entrySet()) {
                String newName = documentNameProcessor.lookupName(NameType.SECURITY_SCHEMES, entry.getKey());
                newSchemes.put(newName, entry.getValue());
            }
            secReq.setSchemes(newSchemes);
        }
        return secReq;
    }

    @Override
    public Callback visitCallback(Context context, String key, Callback callback) {
        updateReference(callback, NameType.CALLBACKS);
        return callback;
    }

    @Override
    public Operation visitOperation(Context context, Operation operation) {
        //rename the tag references
        List<String> tags = operation.getTags();
        if (tags != null && !tags.isEmpty()) {
            List<String> newTags = new ArrayList<>();
            for (String tag : tags) {
                newTags.add(documentNameProcessor.lookupName(NameType.TAG, tag));
            }
            if (!Objects.equals(tags, newTags)) {
                operation.setTags(newTags);
            }
        }
        return operation;
    }

}
