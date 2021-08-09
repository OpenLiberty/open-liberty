/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resources.writeable;

import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryResourceCreationException;
import com.ibm.ws.repository.resources.internal.AdminScriptResourceImpl;
import com.ibm.ws.repository.resources.internal.ConfigSnippetResourceImpl;
import com.ibm.ws.repository.resources.internal.EsaResourceImpl;
import com.ibm.ws.repository.resources.internal.IfixResourceImpl;
import com.ibm.ws.repository.resources.internal.ProductResourceImpl;
import com.ibm.ws.repository.resources.internal.SampleResourceImpl;
import com.ibm.ws.repository.resources.internal.ToolResourceImpl;

/**
 *
 */
public class WritableResourceFactory {

    public static AdminScriptResourceWritable createAdminScript(RepositoryConnection repoConnection) {
        return new AdminScriptResourceImpl(repoConnection);
    }

    public static ConfigSnippetResourceWritable createConfigSnippet(RepositoryConnection repoConnection) {
        return new ConfigSnippetResourceImpl(repoConnection);
    }

    public static EsaResourceWritable createEsa(RepositoryConnection repoConnection) {
        return new EsaResourceImpl(repoConnection);
    }

    public static IfixResourceWritable createIfix(RepositoryConnection repoConnection) {
        return new IfixResourceImpl(repoConnection);
    }

    public static ProductResourceWritable createProduct(RepositoryConnection repoConnection, ResourceType type) {
        ProductResourceWritable product = new ProductResourceImpl(repoConnection);
        product.setType(type);
        return product;
    }

    public static SampleResourceWritable createSample(RepositoryConnection repoConnection, ResourceType type) {
        SampleResourceWritable sample = new SampleResourceImpl(repoConnection);
        sample.setType(type);
        return sample;
    }

    public static ToolResourceWritable createTool(RepositoryConnection repoConnection) {
        return new ToolResourceImpl(repoConnection);
    }

    public static RepositoryResourceWritable createResource(RepositoryConnection repoConnection, ResourceType type) throws RepositoryResourceCreationException {
        switch (type) {
            case ADDON:
            case INSTALL:
                return createProduct(repoConnection, type);
            case ADMINSCRIPT:
                return createAdminScript(repoConnection);
            case CONFIGSNIPPET:
                return createConfigSnippet(repoConnection);
            case FEATURE:
                return createEsa(repoConnection);
            case IFIX:
                return createIfix(repoConnection);
            case OPENSOURCE:
            case PRODUCTSAMPLE:
                return createSample(repoConnection, type);
            case TOOL:
                return createTool(repoConnection);
            default:
                throw new RepositoryResourceCreationException("Can not create an asset of type " + type, null);
        }
    }

}
