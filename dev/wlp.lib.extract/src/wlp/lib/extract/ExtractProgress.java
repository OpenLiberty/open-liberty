/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
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
package wlp.lib.extract;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 *
 */
public interface ExtractProgress {
    public void setFilesToExtract(int count);

    public void skippedFile();

    public void extractedFile(String f);

    public void commandsToRun(int count);

    public void commandRun(List args);

    public boolean isCanceled();

    public void downloadingFile(URL sourceUrl, File targetFile);

    public void dataDownloaded(int numBytes);
}
