/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package web.war.identitystores.db.derby2;

import java.util.logging.Logger;

import javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;

@DatabaseIdentityStoreDefinition(
                                 dataSourceLookup = "jdbc/derby2fat",
                                 priority = 200,
                                 callerQuery = "select password from callers where name = ?",
                                 groupsQuery = "select group_name from caller_groups where caller_name = ?")

public class DatabaseAnnotateDerby2 {
    private static Logger log = Logger.getLogger(DatabaseAnnotateDerby2.class.getName());

    public DatabaseAnnotateDerby2() {
        log.info("<ctor>");
    }
}
