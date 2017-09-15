/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.management;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

import javax.persistence.spi.ClassTransformer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * The CapturingClassTransformer is intended to be a debugging tool for scenarios when it is suspected
 * there is an issue with the bytecode enhancement produced by the JPA Provider implementation.
 * 
 * Its usage is easy, it simply wraps the ClassTransformer provided by the JPA Provider, and passes
 * the transform() invocation to the JPA Provider's ClassTransformer. When enhanced bytecode, if
 * any, is returned from the JPA Provider's ClassTransformer, the CapturingClassTransformer checks
 * against the original bytecode for a size difference. If there is a difference, then the
 * byte array produced by the JPA Provider's ClassTransformer is persisted to disk before it is
 * returned to the calling ClassLoader that requested the transformation.
 * 
 * Even though the performance impacts are largely front-loaded and should only impact the
 * application when it is servicing its first requests, it is clearly not intended to be
 * left enabled in a production capacity.
 */
public class CapturingClassTransformer implements ClassTransformer {
    private static final TraceComponent tc = Tr.register
                    (CapturingClassTransformer.class,
                     JPA_TRACE_GROUP,
                     JPA_RESOURCE_BUNDLE_NAME);

    private final ClassTransformer providerClassTransformer;

    private File captureRootDir = null;
    private boolean captureEnabled = false;

    CapturingClassTransformer(ClassTransformer providerClassTransformer, String aaplName, File logDirectory) {
        if (providerClassTransformer == null) {
            throw new NullPointerException("A ClassTransformer instance must be provided.");
        }

        this.providerClassTransformer = providerClassTransformer;
        this.initialize(logDirectory, aaplName);

        if (captureEnabled && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "CapturingClassTransformer creation successful for ClassTransformer " + providerClassTransformer);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.spi.ClassTransformer#transform(java.lang.ClassLoader, java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
     */
    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] transformedBytes = providerClassTransformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);

        try {
            // Capture bytecode to disk if capturing is enabled and if a transformation has occurred.
            // The transform() method contract states a return value of null indicates no transformation.
            // We check the returned byte array just in case the JPA Implementation violates that contract.
            if (captureEnabled && transformedBytes != null) {
                File saveFile = new File(captureRootDir, className.replace('.', '/') + ".class");
                File targetDir = saveFile.getParentFile();

                if (mkdirs(targetDir)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Capturing enhanced Entity Class bytecode to " + saveFile.getAbsolutePath());
                    }

                    writeCapturedEnhancedBytecode(saveFile, transformedBytes);
                } else {
                    // Couldn't create the target directory
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Failed to create capture directory " + targetDir.getAbsolutePath());
                    }
                }
            }
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught unexpected Exception while capturing " + className + ".", t);
            }
        }

        return transformedBytes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "CapturingClassTransformer [providerClassTransformer=" + providerClassTransformer + "]";
    }

    /**
     * Determines the existence of the server log directory, and attempts to create a capture
     * directory within the log directory. If this can be accomplished, the field captureEnabled
     * is set to true. Otherwise, it is left to its default value false if the capture directory
     * cannot be used.
     * 
     * @param logDirectory
     */
    private void initialize(final File logDirectory, final String aaplName) {
        if (logDirectory == null) {
            captureEnabled = false;
            return;
        }

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                // Create a new or reuse an existing base capture directory
                String captureDirStr = "JPATransform" + "/" + ((aaplName != null) ? aaplName : "unknownapp");
                captureRootDir = new File(logDirectory, captureDirStr);
                captureEnabled = captureRootDir.mkdirs() || captureRootDir.isDirectory();

                if (!captureEnabled) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Cannot create server instance capture directory, so enhanced entity bytecode will not be captured.");
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Capturing enhanced bytecode for JPA entities to " + captureRootDir.getAbsolutePath());
                    }
                }

                return null;
            }
        });
    }

    private final static boolean mkdirs(final File file) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return file.mkdirs() || file.isDirectory();
            }
        });
    }

    private final static void writeCapturedEnhancedBytecode(final File file, final byte[] data) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(file);
                    fos.write(data);
                } catch (Throwable t) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Failed to save captured enhanced Entity Class bytecode.  Reason: " + t);
                    }
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (Throwable t) {
                        }
                    }
                }

                return null;
            }
        });
    }
}
