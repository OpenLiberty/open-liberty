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
package io.openliberty.build.update.pom.task;

import java.io.File;

import org.apache.tools.ant.Task;

import io.openliberty.build.update.pom.UpdatePomJars;

/**
 *
 */
public class UpdatePomJarsTask extends Task {

    /**
     * @return the inputDir
     */
    public File getInputDir() {
        return inputDir;
    }

    /**
     * @param inputDir the inputDir to set
     */
    public void setInputDir(File inputDir) {
        this.inputDir = inputDir;
    }

    /**
     * @return the tmpDir
     */
    public File getTmpDir() {
        return tmpDir;
    }

    /**
     * @param tmpDir the tmpDir to set
     */
    public void setTmpDir(File tmpDir) {
        this.tmpDir = tmpDir;
    }

    /**
     * @return the failOnError
     */
    public boolean isFailOnError() {
        return failOnError;
    }

    /**
     * @param failOnError the failOnError to set
     */
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    protected File inputDir;
    protected File tmpDir;
    protected boolean failOnError;

    /**
     * Execute the build task.
     */
    @Override
    public void execute() {

        String[] args = { inputDir.getPath(), tmpDir.getPath(), Boolean.toString(failOnError) };

        UpdatePomJars.main(args);
    }

}
