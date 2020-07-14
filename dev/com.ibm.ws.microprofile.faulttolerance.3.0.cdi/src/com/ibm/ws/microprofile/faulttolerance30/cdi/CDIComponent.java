/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance30.cdi;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.Collections;
import java.util.Set;

import org.osgi.service.component.annotations.Component;

import io.openliberty.cdi.spi.CDIExtensionMetadata;

@Component(configurationPolicy = IGNORE)
public class CDIComponent implements CDIExtensionMetadata {

    @Override
    public Set<Class<?>> getBeanClasses() {
        return Collections.singleton(PolicyStoreImpl30.class);
    }

}
