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

/**
 * This class provides APIs for Installation Progress Event Messages.
 */
public class InstallProgressEvent extends InstallEvent {

    private static final long serialVersionUID = -3706303282146724776L;

    /**
     * Beginning of the installation
     */
    public static final int BEGIN = 0;

    /**
     * Resolve assets
     */
    public static final int RESOLVE = 8;

    /**
     * Check following:
     * <ul>
     * <li>features and ifixes are installed,</li>
     * <li>features and ifixes are available in massive repository</li>
     * <li>licenses are accepted</li>
     * </ul>
     */
    public static final int CHECK = 16;

    /**
     * Download assets
     */
    public static final int DOWNLOAD = 32;

    /**
     * Install assets
     */
    public static final int INSTALL = 64;

    /**
     * Deploy assets
     */
    public static final int DEPLOY = 96;

    /**
     * Uninstall assets
     */
    public static final int UNINSTALL = 128;

    /**
     * Post install
     */
    public static final int POST_INSTALL = 176;

    /**
     * Post uninstall
     */
    public static final int POST_UNINSTALL = 200;

    /**
     * Clean up such as the temporary files, etc
     */
    public static final int CLEAN_UP = 224;

    /**
     * Installation/Uninstallation completed
     */
    public static final int COMPLETE = 255;

    /**
     * State of the progress:
     * <ul>
     * <li>Installation: BEGIN, RESOLVE, CHECK, DOWNLOAD, INSTALL, CLEAN_UP, COMPLETE</li>
     * <li>Uninstallation: BEGIN, CHECK, UNINSTALL, CLEAN_UP, COMPLETE</li>
     * </ul>
     * <p/>
     * In the case of exception, event CLEAN_UP and COMPLETE will also be fired.
     */
    public int state;

    /**
     * The percentage of the operation completed.
     */
    public int progress;

    /**
     * The message associated with the progress.
     */
    public String message;

    /*
     * BEGIN, CHECK, DOWNLOAD, INSTALL, CLEAN_UP, COMPLETE
     * 0, 1, 10~49, 50~89, 90, 100
     * BEGIN, CHECK, UNINSTALL, CLEAN_UP, COMPLETE
     * 0, 1, 10~89, 90, 100
     */

    /**
     * Creates Install Progress Event with state and progress.
     *
     * @param state State of install
     * @param progress Progress of install
     */
    public InstallProgressEvent(int state, int progress) {
        super(InstallConstants.EVENT_TYPE_PROGRESS);
        this.state = state;
        this.progress = progress;
    }

    /**
     * Creates Install Progress Event with state, progress, and message.
     *
     * @param state State of install
     * @param progress Progress of install
     * @param message Message for progress event
     */
    public InstallProgressEvent(int state, int progress, String message) {
        super(InstallConstants.EVENT_TYPE_PROGRESS);
        this.state = state;
        this.progress = progress;
        this.message = message;
    }

}
