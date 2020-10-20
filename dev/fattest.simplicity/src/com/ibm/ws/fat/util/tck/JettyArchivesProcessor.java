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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * We may need to weave in the TCK jar for some tests that do not package the correct interface classes
 */
public class JettyArchivesProcessor extends AbstractArchiveWeaver {

    @Override
    protected Set<File> getFilesToWeave() {
        File[] jettyFiles = new File(System.getProperty("wlp"), "/usr/servers/FATServer").listFiles((dir, name) -> name.startsWith("jetty-"));
        return new HashSet<>(Arrays.asList(jettyFiles));
    }
}
