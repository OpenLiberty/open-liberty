/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.event.logging.ejb.fat.ejb.war;

import java.util.logging.Logger;

import javax.ejb.Stateless;

/**
 * Remote stateless bean embedded in the standalone WAR.
 * Context info (standalone WAR): EJBEventWar | RemoteBeanInWar | doRemote
 */
@Stateless
public class RemoteBeanInWar implements RemoteBeanInWarRemote {

    private static final Logger log = Logger.getLogger(RemoteBeanInWar.class.getName());

    @Override
    public String doRemote() {
        log.info("RemoteBeanInWar.doRemote() invoked");
        return "remote";
    }
}
