/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.nosql.cdi;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jnosql.mapping.document.spi.DocumentExtension;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.metadata.ApplicationMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import jakarta.enterprise.inject.spi.Extension;

/**
 *
 *
 */
@Component(configurationPid = "io.openliberty.data.internal.persistence.cdi.DocumentExtensionProvider",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           service = { CDIExtensionMetadata.class, DocumentExtensionProvider.class, ApplicationMetaDataListener.class })
public class DocumentExtensionProvider implements CDIExtensionMetadata, ApplicationMetaDataListener {
    private static final TraceComponent tc = Tr.register(DocumentExtensionProvider.class);

    private static final Set<Class<? extends Extension>> extensions = Collections.singleton(DocumentExtension.class);

    @Override
    @Trivial
    public void applicationMetaDataCreated(MetaDataEvent<ApplicationMetaData> event) throws MetaDataException {
        // TODO Auto-generated method stub

    }

    @Override
    public void applicationMetaDataDestroyed(MetaDataEvent<ApplicationMetaData> event) {
        // TODO Auto-generated method stub

    }

    @Override
    @Trivial
    public Set<Class<? extends Extension>> getExtensions() {
        return extensions;
    }

}