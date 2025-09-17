/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.errpaths.web;

import jakarta.data.repository.Delete;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;

/**
 * Repository for an invalid entity type that has Jakarta Persistence
 * annotations on members but lacks an Entity annotation.
 */
@Repository(dataStore = "java:comp/jdbc/env/DSForInvalidEntityClassWithoutAnnoRef")
public interface Invitations {
    @Insert
    public void invite(Invitation newInvitation);

    @Delete
    public void uninvite(Invitation invitationToRemove);
}