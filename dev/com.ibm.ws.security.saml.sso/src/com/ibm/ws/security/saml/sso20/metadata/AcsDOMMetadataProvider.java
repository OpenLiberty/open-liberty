/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.ws.security.saml.sso20.metadata;

import java.io.File;

import org.opensaml.saml2.metadata.provider.DOMMetadataProvider;
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
public class AcsDOMMetadataProvider extends DOMMetadataProvider {
    @SuppressWarnings("unused")
    private static TraceComponent tc = Tr.register(AcsDOMMetadataProvider.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    private final FileInfo fileInfo;

    /**
     * Constructor.
     *
     * @param mdElement the metadata element
     * @throws SamlException
     */
    public AcsDOMMetadataProvider(Element mdElement, final File file) throws SamlException {
        super(mdElement);
        fileInfo = FileInfo.getFileInfo(file);
    }

    public String getMetadataFilename() {
        return fileInfo.getPath();
    }

    public boolean sameIdpFile(File file) throws SamlException {
        FileInfo newFile = FileInfo.getFileInfo(file);
        return this.fileInfo.equals(newFile);
    }
}