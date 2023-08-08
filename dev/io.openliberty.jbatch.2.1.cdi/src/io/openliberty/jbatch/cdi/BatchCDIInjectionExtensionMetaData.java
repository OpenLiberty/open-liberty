/**
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 */
package io.openliberty.jbatch.cdi;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.openliberty.cdi.spi.CDIExtensionMetadata;

/**
 * Use the marker interface to register via DS within Open Liberty, but we 
 * provide our logic through the application Extension interface methods.
 */
@Component(service = CDIExtensionMetadata.class, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "api.classes=" +
                                                                                                                 "jakarta.batch.api.BatchProperty;" +
                                                                                                                 "jakarta.batch.operations.JobOperator;" +
                                                                                                                 "jakarta.batch.runtime.context.JobContext;" +
                                                                                                                 "jakarta.batch.runtime.context.StepContext" })
public class BatchCDIInjectionExtensionMetaData implements CDIExtensionMetadata {

    public Set<Class<? extends Extension>> getExtensions() {
        
        final Set<Class<? extends Extension>> t = new HashSet<Class<? extends Extension>>();
        t.add(BatchCDIInjectionExtension.class);
        return Collections.unmodifiableSet(t);
    }

}
