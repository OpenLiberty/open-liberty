/*******************************************************************************
 * Copyright (c) 2018-2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util.tck;

import java.io.File;
import java.util.Collections;
import java.util.Set;

/**
 * We may need to weave in the TCK jar for some tests that do not package the correct interface classes
 */
public class WiremockArchiveProcessor extends AbstractArchiveWeaver {

    private final Set<File> files = Collections.singleton(new File(System.getProperty("wlp"),
                                                                   "/usr/servers/FATServer/wiremock-standalone-2.14.0.jar"));

    @Override
    protected Set<File> getFilesToWeave() {
        return files;
    }
}
