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
package com.ibm.ws.openapi31.merge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

import com.ibm.ws.microprofile.openapi.utils.DefaultOpenAPIModelVisitor;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class OASRenameVisitor extends DefaultOpenAPIModelVisitor {

    private final Map<String, Map<String, String>> conflicsMap;
    private final OpenAPI openAPI;

    public OASRenameVisitor(OpenAPI openAPI, Map<String, Map<String, String>> confMap) {
        this.conflicsMap = confMap;
        this.openAPI = openAPI;
    }

    public void renameRefs() {
        OpenAPIModelWalker walker = new OpenAPIModelWalker(openAPI);
        walker.accept(this);
    }

    /** {@inheritDoc} */
    @Override
    public void visitExample(Context context, Example example) {
        String reference = example.getRef();
        if (reference != null && reference.startsWith("#/components/examples/")) {
            String oldName = reference.split("/")[3];
            String newName = getNewName(OASMergeService.OA_COMPONENTS_EXAMPLES, oldName);
            if (newName != null)
                example.setRef("#/components/examples/" + newName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void visitExample(Context context, String key, Example example) {
        String reference = example.getRef();
        if (reference != null && reference.startsWith("#/components/examples/")) {
            String oldName = reference.split("/")[3];
            String newName = getNewName(OASMergeService.OA_COMPONENTS_EXAMPLES, oldName);
            if (newName != null)
                example.setRef("#/components/examples/" + newName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void visitHeader(Context context, String key, Header header) {
        String reference = header.getRef();
        if (reference != null && reference.startsWith("#/components/headers/")) {
            String oldName = reference.split("/")[3];
            String newName = getNewName(OASMergeService.OA_COMPONENTS_HEADERS, oldName);
            if (newName != null)
                header.setRef("#/components/headers/" + newName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void visitLink(Context context, String key, Link link) {
        String reference = link.getRef();
        if (reference != null && reference.startsWith("#/components/links/")) {
            String oldName = reference.split("/")[3];
            String newName = getNewName(OASMergeService.OA_COMPONENTS_LINKS, oldName);
            if (newName != null)
                link.setRef("#/components/links/" + newName);
        } else if (reference == null) {
            String operaitonId = link.getOperationId();
            String new_operationId = getConflictsMap(OASMergeService.OA_OPERATION_ID).get(operaitonId);
            if (new_operationId != null) {
                link.setOperationId(new_operationId);;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void visitParameter(Context context, Parameter p) {
        String reference = p.getRef();
        if (reference != null && reference.startsWith("#/components/parameters/")) {
            String oldName = reference.split("/")[3];
            String newName = getNewName(OASMergeService.OA_COMPONENTS_PARAMETERS, oldName);
            if (newName != null)
                p.setRef("#/components/parameters/" + newName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void visitParameter(Context context, String key, Parameter p) {
        String reference = p.getRef();
        if (reference != null && reference.startsWith("#/components/parameters/")) {
            String oldName = reference.split("/")[3];
            String newName = getNewName(OASMergeService.OA_COMPONENTS_PARAMETERS, oldName);
            if (newName != null)
                p.setRef("#/components/parameters/" + newName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void visitRequestBody(Context context, RequestBody rb) {
        String reference = rb.getRef();
        if (reference != null && reference.startsWith("#/components/requestBodies/")) {
            String oldName = reference.split("/")[3];
            String newName = getNewName(OASMergeService.OA_COMPONENTS_REQUEST_BODIES, oldName);
            if (newName != null)
                rb.setRef("#/components/requestBodies/" + newName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void visitRequestBody(Context context, String key, RequestBody rb) {
        String reference = rb.getRef();
        if (reference != null && reference.startsWith("#/components/requestBodies/")) {
            String oldName = reference.split("/")[3];
            String newName = getNewName(OASMergeService.OA_COMPONENTS_REQUEST_BODIES, oldName);
            if (newName != null)
                rb.setRef("#/components/requestBodies/" + newName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void visitResponse(Context context, String key, APIResponse response) {
        String reference = response.getRef();
        if (reference != null && reference.startsWith("#/components/responses/")) {
            String oldName = reference.split("/")[3];
            String newName = getNewName(OASMergeService.OA_COMPONENTS_RESPONSES, oldName);
            if (newName != null)
                response.setRef("#/components/responses/" + newName);
        }

    }

    /** {@inheritDoc} */
    @Override
    public void visitSchema(Context context, Schema schema) {
        String reference = schema.getRef();
        if (reference != null && reference.startsWith("#/components/schemas/")) {
            String oldName = reference.split("/")[3];
            String newName = getNewName(OASMergeService.OA_COMPONENTS_SCHEMAS, oldName);
            if (newName != null)
                schema.setRef("#/components/schemas/" + newName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void visitSchema(Context context, String key, Schema schema) {
        String reference = schema.getRef();
        if (reference != null && reference.startsWith("#/components/schemas/")) {
            String oldName = reference.split("/")[3];
            String newName = getNewName(OASMergeService.OA_COMPONENTS_SCHEMAS, oldName);
            if (newName != null)
                schema.setRef("#/components/schemas/" + newName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void visitSecurityScheme(Context context, String key, SecurityScheme scheme) {
        String reference = scheme.getRef();
        if (reference != null && reference.startsWith("#/components/securitySchemes/")) {
            String oldName = reference.split("/")[3];
            String newName = getNewName(OASMergeService.OA_COMPONENTS_SECURITY_SHEMES, oldName);
            if (newName != null)
                scheme.setRef("#/components/securitySchemes/" + newName);
        }

    }

    /** {@inheritDoc} */
    @Override
    public void visitOperation(Context context, Operation operation) {
        //rename the tag references
        List<String> tags = operation.getTags();
        if (tags != null) {
            Iterator<String> tagsIt = tags.iterator();
            Set<String> new_tags = new HashSet<>();
            while (tagsIt.hasNext()) {
                String tagName = tagsIt.next();
                String new_tagName = getNewName(OASMergeService.OA_TAGS, tagName);
                if (new_tagName != null) {
                    tagsIt.remove();
                    new_tags.add(new_tagName);
                }
            }
            tags.addAll(new_tags);
        }
    }

    private String getNewName(String type, String original) {
        Map<String, String> conflictPairs = getConflictsMap(type);
        return conflictPairs.get(original);
    }

    private Map<String, String> getConflictsMap(String type) {
        Map<String, String> conflicts = conflicsMap.get(type);
        if (conflicts == null) {
            conflicts = new HashMap<>();
            conflicsMap.put(type, conflicts);
        }
        return conflicts;
    }
}
