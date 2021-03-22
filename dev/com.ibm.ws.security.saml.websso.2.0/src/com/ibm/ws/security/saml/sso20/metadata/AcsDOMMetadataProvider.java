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

package com.ibm.ws.security.saml.sso20.metadata;

import java.io.File;

import org.opensaml.saml.metadata.resolver.impl.DOMMetadataResolver;
import org.w3c.dom.Element;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.internal.utils.FileInfo;



/**
 * A <code>MetadataProvider</code> implementation that retrieves metadata from a DOM <code>Element</code> as
 * supplied by the user.
 *
 * It is the responsibility of the caller to re-initialize, via {@link #initialize()}, if any properties of this
 * provider are changed.
 */
public class AcsDOMMetadataProvider extends DOMMetadataResolver {
    @SuppressWarnings("unused")
    private static TraceComponent tc = Tr.register(AcsDOMMetadataProvider.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    private final FileInfo fileInfo;
    private final Element mdElement;

    /**
     * Constructor.
     *
     * @param mdElement the metadata element
     * @throws SamlException
     */
    public AcsDOMMetadataProvider(Element mdElement, final File file) throws SamlException {
        super(mdElement);
        this.mdElement = mdElement;
        fileInfo = FileInfo.getFileInfo(file);
    }

    public String getMetadataFilename() {
        return fileInfo.getPath();
    }

    public boolean sameIdpFile(File file) throws SamlException {
        FileInfo newFile = FileInfo.getFileInfo(file);
        return this.fileInfo.equals(newFile);
    }

    public String getEntityId() {
        if (this.mdElement != null) {
            return this.mdElement.getAttribute("entityID");
        }
        return null;
    }

}