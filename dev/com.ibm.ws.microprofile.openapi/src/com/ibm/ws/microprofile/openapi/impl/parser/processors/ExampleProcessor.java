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

package com.ibm.ws.microprofile.openapi.impl.parser.processors;

import static com.ibm.ws.microprofile.openapi.impl.parser.util.RefUtils.computeRefFormat;
import static com.ibm.ws.microprofile.openapi.impl.parser.util.RefUtils.isAnExternalRefFormat;

import java.util.List;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.examples.Example;

import com.ibm.ws.microprofile.openapi.impl.parser.ResolverCache;
import com.ibm.ws.microprofile.openapi.impl.parser.models.RefFormat;

/**
 * Created by gracekarina on 23/06/17.
 */
public class ExampleProcessor {
    private final ResolverCache cache;
    private final OpenAPI openAPI;
    private final ExternalRefProcessor externalRefProcessor;

    public ExampleProcessor(ResolverCache cache, OpenAPI openAPI) {
        this.cache = cache;
        this.openAPI = openAPI;
        this.externalRefProcessor = new ExternalRefProcessor(cache, openAPI);
    }

    public void processExample(Example example) {

        if (example.getRef() != null) {
            processReferenceExample(example);
        }
    }

    public void processExample(List<Example> examples) {
        for (Example example : examples) {
            if (example.getRef() != null) {
                processReferenceExample(example);
            }
        }
    }

    private void processReferenceExample(Example example) {
        RefFormat refFormat = computeRefFormat(example.getRef());
        String $ref = example.getRef();
        if (isAnExternalRefFormat(refFormat)) {
            final String newRef = externalRefProcessor.processRefToExternalExample($ref, refFormat);
            if (newRef != null) {
                example.setRef(newRef);
            }
        }
    }
}
