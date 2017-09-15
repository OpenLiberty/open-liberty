/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.config;

import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Implementers of this interface can be notified when a &ltfileset/&gt has changed.
 * The {@link #filesetNotification(String, Fileset)} method is called when the fileset
 * configuration has changed and in the case of monitored filesets also when the
 * contents of the fileset have changed.
 * <P>
 * To get called the implementation must be registered in the service registry with
 * this interface and with a property named fileset.
 * The value of the fileset property must be the fileset configuration id.
 * For example fileset=configID for the fileset with configuration
 * &ltfileset id="configID"/&gt.
 */
public interface FilesetChangeListener {

    /**
     * Called when the fileset configuration has changed.
     * Additionally called when the contents of a {@link Fileset} have changed when
     * it has been configured to monitor for changes.
     * 
     * @param pid the {@link ConfigurationAdmin} persistent ID
     * @param fileset the modified {@link Fileset}
     */
    public void filesetNotification(String pid, Fileset fileset);

}
