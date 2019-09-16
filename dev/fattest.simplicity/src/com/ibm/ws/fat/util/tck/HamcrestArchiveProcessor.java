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
 * We weave in the hamcrest jar that is used by some of the microprofile config tck tests.
 * The build.gradle file pull the hamcrest jar from maven and puts it in the lib directory
 */
public class HamcrestArchiveProcessor extends AbstractArchiveWeaver {

    private final Set<File> files = Collections.singleton(new File("../../../lib/hamcrest-all-1.3.jar"));

    @Override
    protected Set<File> getFilesToWeave() {
        return files;
    }
}