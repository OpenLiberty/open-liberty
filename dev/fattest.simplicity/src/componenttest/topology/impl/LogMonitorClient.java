/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.impl;

import com.ibm.websphere.simplicity.RemoteFile;

/**
 * This interface may be used to provides services to the LogMonitor class for the purpose of
 * abstracting the server type that is using the LogMonitor services for the purpose of log wait/search.
 */
public interface LogMonitorClient {
    /**
     * This method returns the default log file for the server being monitored.
     * Typically, in a LibertyServer instance, this would be the 'messages.log' file.
     *
     * @return
     * @throws Exception
     */
    public RemoteFile lmcGetDefaultLogFile() throws Exception;

    /**
     * This method clears the log offset for the server instance. Log offset style
     * tracking has been deprecated. This method has been provided to enable backward
     * compatibility with logic that was moved into the LogMonitor class. For new implementations,
     * this method can be a 'no op'.
     */
    public void lmcClearLogOffsets();

    /**
     * Reset the mark and offset values for logs back to the start of this process.
     */
    public void lmcResetLogOffsets();

    /**
     * Set the offsets that we'll go back to when {@link #lmcResetLogOffsets()} is called.
     */
    public void lmcSetOriginLogOffsets();

    /**
     * This method updates the log offset for the server instance. Log offset style
     * tracking has been deprecated. This method has been provided to enable backward
     * compatibility with logic that was moved into the LogMonitor class. For new implementations,
     * this method can be a 'no op'.
     */
    public void lmcUpdateLogOffset(String logFile, Long newLogOffset);
}
