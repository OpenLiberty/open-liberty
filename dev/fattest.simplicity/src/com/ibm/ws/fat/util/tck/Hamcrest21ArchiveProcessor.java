/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.util.tck;

import java.io.File;
import java.util.Collections;
import java.util.Set;

/**
 * We weave in the hamcrest jar that is used by some of the microprofile config tck tests.
 * The tck's build.gradle file pulls the hamcrest jar from maven central as a requiredLibs dependency.
 * The gradle FAT build logic copies requiredLibs archives to the autoFVT/lib directory and from there we
 * acquire it and add it to the test archive.
 */
public class Hamcrest21ArchiveProcessor extends AbstractArchiveWeaver {

    private final Set<File> files = Collections.singleton(new File("../../../lib/hamcrest-2.1.jar"));

    @Override
    protected Set<File> getFilesToWeave() {
        return files;
    }
}
