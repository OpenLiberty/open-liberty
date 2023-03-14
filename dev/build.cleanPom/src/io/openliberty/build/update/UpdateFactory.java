/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.build.update;

import java.io.File;

import io.openliberty.build.update.util.Logger;

public interface UpdateFactory {
    Class<? extends Update> getUpdateClass();

    default String getUsage() {
        return ("Usage: " + getUpdateClass().getName() + " <target> <tmp> [ <failOnError> ]");
    }

    Update createUpdate(File targetFile, File tmpDir, Logger logger, boolean failOnError) throws Exception;
}
