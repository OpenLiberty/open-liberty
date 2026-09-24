/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.restfulWS40.fat.rest40examples;

/**
 * Response object for file upload operations
 */
public class UploadResponse {
    private String fileName;
    private long fileSize;
    private String message;

    public UploadResponse() {}

    public UploadResponse(String fileName, long fileSize, String message) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.message = message;
    }

    // Getters and setters
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

// Made with Bob
