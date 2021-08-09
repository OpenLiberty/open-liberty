/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.war.identitystores.db.db1;

import java.util.logging.Logger;

import javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;

@DatabaseIdentityStoreDefinition(
                                 dataSourceLookup = "jdbc/db1",
                                 priority = 200,
                                 callerQuery = "select password from callers where name = ?",
                                 groupsQuery = "select group_name from caller_groups where caller_name = ?")

public class DatabaseAnnotate200 {
    private static Logger log = Logger.getLogger(DatabaseAnnotate200.class.getName());

    public DatabaseAnnotate200() {
        log.info("<ctor>");
    }
}
