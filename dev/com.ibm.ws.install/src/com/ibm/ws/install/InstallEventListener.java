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
package com.ibm.ws.install;

import java.util.EventListener;

/**
 * This interface provides a way for InstallKernel
 * to feedback information back to the caller.
 */
public interface InstallEventListener extends EventListener {

    /**
     * This interface is used by the listener to handle the event
     * which is notified by the InstallKernel.
     *
     * @param event Install progress event
     * @throws CancelException
     */
    public void handleInstallEvent(InstallProgressEvent event) throws Exception;
}
