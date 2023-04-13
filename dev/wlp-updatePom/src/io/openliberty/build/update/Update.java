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

/**
 * Update main API.
 */
public interface Update {
    /**
     * Run the update. Answer -1 in case of a failure when the
     * fail-on-error setting is false. Otherwise, answer the count
     * of updates which were performed.
     *
     * Single file type updates will answer -1, 0, or 1. Directory
     * type updates are expected to answer -1 or the count of files
     * which were updated.
     *
     * @return The count of files which were updated.
     *
     * @throws Exception Thrown in case of an error when fail-on-error
     *                       is true.
     */
    int run() throws Exception;
}
