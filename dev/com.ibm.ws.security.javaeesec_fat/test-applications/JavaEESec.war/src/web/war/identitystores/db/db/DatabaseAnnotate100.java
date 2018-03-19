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
package web.war.identitystores.db.db;

import java.util.logging.Logger;

import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;

/**
 * Base servlet for RunAs test where servlet with RunAs (Manager) annotation invokes an EJB. The
 * EJB also has a RunAs (Employee) annotation and it invokes a second EJB which should be run as the
 * user in Employee role.
 */
@BasicAuthenticationMechanismDefinition(realmName = "ejbRealmRunAs")
@DatabaseIdentityStoreDefinition(
                                 callerQuery = "select password from callertable where name = ?",
                                 groupsQuery = "select group_name from callertable_groups where caller_name = ?",
                                 dataSourceLookup = "jdbc/derby1fat")

public class DatabaseAnnotate100 {
    private static Logger log = Logger.getLogger(DatabaseAnnotate100.class.getName());
    public DatabaseAnnotate100() {
        log.info("<ctor>");
    }
}
