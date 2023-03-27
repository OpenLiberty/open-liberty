/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.build.update;

import java.io.File;

import io.openliberty.build.update.util.Logger;

/**
 * Common update implementation.
 *
 * Common update properties:
 *
 * <ul>
 * <li>A target file, which may be a simple file or a directory.</li>
 * <li>A directory for temporary files.</li>
 * <li>An optional parent logger.</li>
 * <li>An fail-on-error setting, which defaults to true.</li>
 * </ul>
 *
 * The main {@link #run()} API must be implemented by classes.
 */
public abstract class UpdateImpl implements Update {
    public UpdateImpl(File targetPath, File tmpDir) {
        this(targetPath, tmpDir, null, FAIL_ON_ERROR);
    }

    public UpdateImpl(File targetPath, File tmpDir, boolean failOnError) {
        this(targetPath, tmpDir, null, failOnError);
    }

    public UpdateImpl(File targetPath, File tmpDir, Logger logger) {
        this(targetPath, tmpDir, logger, FAIL_ON_ERROR);
    }

    public static final boolean FAIL_ON_ERROR = true;

    public UpdateImpl(File targetPath, File tmpDir, Logger logger, boolean failOnError) {
        this.targetFile = targetPath;
        this.tmpDir = tmpDir;

        this.logger = new Logger(logger, getClass().getSimpleName());
        this.failOnError = failOnError;
    }

    //

    private final File targetFile;

    public File getTargetFile() {
        return targetFile;
    }

    private final File tmpDir;

    public File getTmpDir() {
        return tmpDir;
    }

    private final Logger logger;

    public Logger getLogger() {
        return logger;
    }

    public void logMark() {
        logger.mark();
    }

    public void log(String m, String text) {
        logger.log(m, text);
    }

    public long logMark(String m, String text) {
        return logger.logMark(m, text);
    }

    public long logTime(String m, String text) {
        return logger.logTime(m, text);
    }

    private final boolean failOnError;

    public boolean getFailOnError() {
        return failOnError;
    }

    //

    @Override
    abstract public int run() throws Exception;
}
