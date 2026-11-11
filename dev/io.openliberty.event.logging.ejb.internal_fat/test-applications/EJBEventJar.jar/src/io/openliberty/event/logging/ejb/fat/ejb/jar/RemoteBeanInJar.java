/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.event.logging.ejb.fat.ejb.jar;

import java.util.logging.Logger;

import javax.ejb.Stateless;

import io.openliberty.event.logging.ejb.fat.ejb.jar.api.RemoteBeanInJarRemote;

/**
 * Remote bean in the standalone EJB JAR.
 * Context info (standalone JAR): EJBEventJar | RemoteBeanInJar | doRemote
 */
@Stateless
public class RemoteBeanInJar implements RemoteBeanInJarRemote {

    private static final Logger log = Logger.getLogger(RemoteBeanInJar.class.getName());

    @Override
    public String doRemote() {
        log.info("RemoteBeanInJar.doRemote() invoked");
        return "remote";
    }
}
