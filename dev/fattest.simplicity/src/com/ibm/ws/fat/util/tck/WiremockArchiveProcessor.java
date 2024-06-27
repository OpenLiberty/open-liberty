/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
public class WiremockArchiveProcessor extends AbstractArchiveWeaver {

    @Override
    protected Set<File> getFilesToWeave() {
        File[] wiremockFiles = new File(System.getProperty("wlp"), "/usr/servers/FATServer").listFiles((dir, name) -> name.startsWith("wiremock-standalone-"));
        return new HashSet<>(Arrays.asList(wiremockFiles));
    }
}
