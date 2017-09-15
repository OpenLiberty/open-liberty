/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;

import com.ibm.ws.kernel.boot.delegated.zos.NativeMethodHelper;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;

/**
 * Factory class for tagging text files with the current file encoding. This
 * is a package protected class returned by the factory.
 */
class TaggedFileOutputStream extends FileOutputStream {

    /**
     * Flag to indicate whether or not the tagging method was successfully
     * registered.
     */
    private static final boolean initialized = registerNatives();

    /**
     * Flag that indicates whether or not tagging failed message was issued.
     */
    private static boolean taggingFailedIssued = false;

    /**
     * The character code set identifier that represents {@c file.encoding} or
     * 0 if the codeset id could not be resolved.
     */
    private static int fileEncodingCcsid = acquireFileEncodingCcsid();

    /**
     * Constructor for a Tagged file stream. Package-private, this can
     * be constructed only via the TaggedFileOutputStreamFactory
     * 
     * @param file
     * @param append
     * @throws IOException
     */
    TaggedFileOutputStream(File file, boolean append) throws IOException {
        super(file, append);
        setFileTag(file);
    }

    /**
     * Tag the specified file as ISO8859-1 text.
     * 
     * @param file the file to tag as ISO8859-1 text
     */
    void setFileTag(File file) throws IOException {
        if (initialized && fileEncodingCcsid != 0) {
            int returnCode = setFileTag(file.getAbsolutePath(), fileEncodingCcsid);
            if (returnCode != 0) {
                issueTaggingFailedMessage(returnCode);
            }
        }
    }

    /**
     * Tag the file with the specified character code set ID.
     * 
     * @param file the file to tag
     * @param ccsid the character codeset ID
     * 
     * @return 0 on success; error number on failure
     */
    int setFileTag(String file, int ccsid) {
        return ntv_setFileTag(file, ccsid);
    }

    /**
     * Issue the tagging failed message.
     */
    private synchronized void issueTaggingFailedMessage(int returnCode) {
        if (taggingFailedIssued) {
            return;
        }

        System.err.println(MessageFormat.format(BootstrapConstants.messages.getString("warn.unableTagFile"), returnCode));
        taggingFailedIssued = true;
    }

    /**
     * Register the native method needed for file tagging.
     * 
     * @return true if the methods were successfully registered
     */
    private static boolean registerNatives() {
        try {
            final String methodDescriptor = "zJNIBOOT_" + TaggedFileOutputStream.class.getCanonicalName().replaceAll("\\.", "_");
            long dllHandle = NativeMethodHelper.registerNatives(TaggedFileOutputStream.class, methodDescriptor, null);
            if (dllHandle > 0) {
                return true;
            }
            // if the native DLL doesnot exist, we fail quietly
            if (dllHandle == 0) {
                return false;
            }
        } catch (Throwable t) {
            // Eat exception from registerNatives if DLL can't be loaded
        }

        // if native DLL exists but failed to link the native method descriptor
        System.err.println(MessageFormat.format(BootstrapConstants.messages.getString("warn.registerNative"), TaggedFileOutputStream.class.getCanonicalName()));
        return false;
    }

    /**
     * Get the character code set identifier that represents the JVM's file
     * encoding. This should only be called by the class static initializer.
     * 
     * @return the character code set id or 0 if the code set ID could not be
     *         determined
     */
    private static int acquireFileEncodingCcsid() {
        // Get the charset represented by file.encoding
        Charset charset = null;
        String fileEncoding = System.getProperty("file.encoding");
        try {
            charset = Charset.forName(fileEncoding);
        } catch (Throwable t) {
            // Problem with the JVM's file.encoding property
        }

        int ccsid = getCcsid(charset);
        if (ccsid == 0) {
            System.err.println(MessageFormat.format(BootstrapConstants.messages.getString("warn.fileEncodingNotFound"), fileEncoding));
        }
        return ccsid;
    }

    /**
     * Get the character code set identifier for the specified {@link Charset}.
     * 
     * @param charset the charset
     * 
     * @return the code set for the specified charset or 0 if the code set could
     *         not be determined
     */
    private static int getCcsid(Charset charset) {
        int ccsid = 0;

        // Bail out if the charset is null
        if (charset == null) {
            return 0;
        }

        // Try the name first
        if (ccsid == 0) {
            ccsid = getCcsid(charset.name());
        }

        // Try the aliases
        if (ccsid == 0) {
            for (String encoding : charset.aliases()) {
                ccsid = getCcsid(encoding);
                if (ccsid > 0) {
                    break;
                }
            }
        }

        return ccsid;
    }

    /**
     * Get the IBM character code set ID for the specified Java
     * character encoding.
     * 
     * @param encoding the encoding name or alias
     * 
     * @return the ccsid or 0 if unknown
     */
    private static int getCcsid(String encoding) {
        if (!initialized) {
            return -1;
        }
        return ntv_getCcsid(encoding);
    }

    //-----------------------------------------------------------------
    // Native methods (server_tagging_jni.c).
    //-----------------------------------------------------------------
    private native int ntv_setFileTag(String file, int ccsid);

    private native static int ntv_getCcsid(String encoding);
}
