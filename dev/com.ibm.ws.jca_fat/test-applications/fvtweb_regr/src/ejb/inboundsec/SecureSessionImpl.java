/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package ejb.inboundsec;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

@Stateless
@DeclareRoles({ "Administrator", "Owner", "administrator" })
public class SecureSessionImpl implements SecureSessionLocal {

    @Resource
    private transient SessionContext ctx;

    @Override
    @RolesAllowed({ "Administrator", "administrator" })
    public void execute() {
        System.out
                        .println("EJBDEMOEXECUTE:In the session bean SecureSessionImpl method execute as user: "
                                 + ctx.getCallerPrincipal());
        System.out
                        .println("EJBDEMOEXECUTE:The caller of SecureSessionImpl.execute() is in role Administrator:"
                                 + ctx.isCallerInRole("Administrator"));
    }

    @Override
    @RolesAllowed("Owner")
    public void executeSpecial() {
        System.out
                        .println("EJBDEMOEXECUTESPECIAL:In the session bean SecureSessionImpl method executeSpecial as user: "
                                 + ctx.getCallerPrincipal());
        System.out
                        .println("EJBDEMOEXECUTESPECIAL:The caller of SecureSessionImpl.executeSpecial() is in role Owner:"
                                 + ctx.isCallerInRole("Administrator"));
    }
}
