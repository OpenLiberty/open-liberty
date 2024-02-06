/*
 * Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     Oracle - initial API and implementation
package org.eclipse.persistence.asm;

import org.eclipse.persistence.asm.internal.Util;

import org.eclipse.persistence.config.SystemProperties;
import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLog;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ASMFactory {

    // This block must be first - begin
    public final static String ASM_SERVICE_ECLIPSELINK = "eclipselink";
    public final static String ASM_SERVICE_OW2 = "ow2";

    private final static String ASM_OW2_CLASS_VISITOR = "org.objectweb.asm.ClassVisitor";
    private final static String ASM_ECLIPSELINK_CLASS_VISITOR = "org.eclipse.persistence.internal.libraries.asm.ClassVisitor";

    private final static String ASM_OPCCODES_OW2 = "org.objectweb.asm.Opcodes";
    private final static String ASM_OPCCODES_ECLIPSELINK = "org.eclipse.persistence.internal.libraries.asm.Opcodes";

    private final static Map<String, String> ASM_OPCCODES_MAP = new HashMap<>();

    static {
        ASM_OPCCODES_MAP.put(ASMFactory.ASM_SERVICE_OW2, ASM_OPCCODES_OW2);
        ASM_OPCCODES_MAP.put(ASMFactory.ASM_SERVICE_ECLIPSELINK, ASM_OPCCODES_ECLIPSELINK);
    }

    private final static SessionLog LOG = AbstractSessionLog.getLog();
    // This block must be first - end

    // Do not reference static fields in Opcodes to avoid circular static initialization

    // Should be changed in case of ASM upgrade
    public final static int ASM_API_SELECTED = valueOpcodesInt("ASM9");
    public final static int JAVA_CLASS_VERSION = valueOpcodesInt("V1_8");
    public final static int JAVA_CLASS_LATEST_VERSION = ASMFactory.getLatestOPCodeVersion();

    private static int valueOpcodesInt(String fieldName) {
        return ((int) Util.getFieldValue(ASM_OPCCODES_MAP, fieldName, Integer.TYPE));
    }

    public static AnnotationVisitor createAnnotationVisitor(final int api) {
        String asmService = ASMFactory.getAsmService();
        if (ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.AnnotationVisitorImpl(api);
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.AnnotationVisitorImpl(api);
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static AnnotationVisitor createAnnotationVisitor(final int api, final AnnotationVisitor annotationVisitor) {
        String asmService = ASMFactory.getAsmService();
        if (ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.AnnotationVisitorImpl(api, annotationVisitor);
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.AnnotationVisitorImpl(api, annotationVisitor);
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static FieldVisitor createFieldVisitor(final int api) {
        String asmService = ASMFactory.getAsmService();
        if (ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.FieldVisitorImpl(api);
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.FieldVisitorImpl(api);
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static FieldVisitor createFieldVisitor(final int api, final FieldVisitor fieldVisitor) {
        String asmService = ASMFactory.getAsmService();
        if (ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.FieldVisitorImpl(api, fieldVisitor);
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.FieldVisitorImpl(api, fieldVisitor);
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static MethodVisitor createMethodVisitor(final int api) {
        String asmService = ASMFactory.getAsmService();
        if (ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.MethodVisitorImpl(api);
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.MethodVisitorImpl(api);
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static MethodVisitor createMethodVisitor(final int api, final MethodVisitor methodVisitor) {
        String asmService = ASMFactory.getAsmService();
        if (ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.MethodVisitorImpl(api, methodVisitor);
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.MethodVisitorImpl(api, methodVisitor);
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static ClassReader createClassReader(final InputStream inputStream) throws IOException {
        String asmService = ASMFactory.getAsmService();
        if (ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.ClassReaderImpl(inputStream);
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.ClassReaderImpl(inputStream);
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static ClassReader createClassReader(final byte[] classFileBuffer) throws IOException {
        String asmService = ASMFactory.getAsmService();
        if (ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.ClassReaderImpl(classFileBuffer);
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.ClassReaderImpl(classFileBuffer);
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static ClassReader createClassReader(final byte[] classFileBuffer, final int classFileOffset, final int classFileLength) throws IOException {
        String asmService = ASMFactory.getAsmService();
        if (ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.ClassReaderImpl(classFileBuffer, classFileOffset, classFileLength);
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.ClassReaderImpl(classFileBuffer, classFileOffset, classFileLength);
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static ClassReader createClassReader(final InputStream inputStream, final boolean checkClassVersion) throws IOException {
        String asmService = ASMFactory.getAsmService();
        if (!checkClassVersion || ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.ClassReaderImpl(inputStream, checkClassVersion);
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.ClassReaderImpl(inputStream);
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static ClassWriter createClassWriter() {
        String asmService = ASMFactory.getAsmService();
        if (ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.ClassWriterImpl();
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.ClassWriterImpl();
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static ClassWriter createClassWriter(final int flags) {
        String asmService = ASMFactory.getAsmService();
        if (ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.ClassWriterImpl(flags);
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.ClassWriterImpl(flags);
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static ClassWriter createClassWriter(final ClassReader classReader, final int flags) {
        String asmService = ASMFactory.getAsmService();
        if (ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.ClassWriterImpl(classReader, flags);
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.ClassWriterImpl(classReader, flags);
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static ClassVisitor createClassVisitor(final int api) {
        String asmService = ASMFactory.getAsmService();
        if (ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.ClassVisitorImpl(api);
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.ClassVisitorImpl(api);
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static ClassVisitor createClassVisitor(final int api, final ClassVisitor classVisitor) {
        String asmService = ASMFactory.getAsmService();
        if (ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.ClassVisitorImpl(api, classVisitor);
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.ClassVisitorImpl(api, classVisitor);
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static Type createType(final Class<?> clazz) {
        String asmService = ASMFactory.getAsmService();
        if (ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.TypeImpl(clazz);
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.TypeImpl(clazz);
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static Type createType(final String typeDescriptor) {
        String asmService = ASMFactory.getAsmService();
        if (ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.TypeImpl(typeDescriptor);
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.TypeImpl(typeDescriptor);
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static Type createVoidType() {
        String asmService = ASMFactory.getAsmService();
        if (ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.TypeImpl((Class<?>)null);
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.TypeImpl((Class<?>)null);
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static Label createLabel() {
        String asmService = ASMFactory.getAsmService();
        if (ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.LabelImpl();
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.LabelImpl();
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static SerialVersionUIDAdder createSerialVersionUIDAdder(final ClassVisitor classVisitor) {
        String asmService = ASMFactory.getAsmService();
        if (ASM_SERVICE_ECLIPSELINK.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.eclipselink.SerialVersionUIDAdderImpl(classVisitor);
        } else if (ASM_SERVICE_OW2.equals(asmService)) {
            return new org.eclipse.persistence.asm.internal.platform.ow2.SerialVersionUIDAdderImpl(classVisitor);
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    public static String getAsmService() {
        String asmService = PrivilegedAccessHelper.getSystemProperty(SystemProperties.ASM_SERVICE);
        if (asmService != null) {
            if (ASM_SERVICE_ECLIPSELINK.equals(asmService) && isASMImplementationAvailable(ASM_ECLIPSELINK_CLASS_VISITOR)) {
                LOG.finest("EclipseLink ASM implementation is used.");
                return asmService;
            } else if (ASM_SERVICE_OW2.equals(asmService) && isASMImplementationAvailable(ASM_OW2_CLASS_VISITOR)) {
                LOG.finest("OW2 ASM implementation is used.");
                return asmService;
            } else {
                throw ValidationException.incorrectASMServiceProvided();
            }
        }
        //Fallback to default if ASM service is not specified
        if (isASMImplementationAvailable(ASM_ECLIPSELINK_CLASS_VISITOR)) {
            LOG.finest("EclipseLink ASM implementation is used.");
            return ASM_SERVICE_ECLIPSELINK;
        } else if (isASMImplementationAvailable(ASM_OW2_CLASS_VISITOR)) {
            LOG.finest("OW2 ASM implementation is used.");
            return ASM_SERVICE_OW2;
        } else {
            throw ValidationException.notAvailableASMService();
        }
    }

    private static boolean isASMImplementationAvailable(String className) {
        try {
            PrivilegedAccessHelper.getClassForName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static int getLatestOPCodeVersion() {
        final Map<String, Integer> versionMap = new LinkedHashMap<>();
        Pattern searchPattern = Pattern.compile("^V\\d((_\\d)?|\\d*)");
        try {
            Class opcodesClazz = Opcodes.getOpcodesClass();
            for (Field f : opcodesClazz.getDeclaredFields()) {
                if (searchPattern.matcher(f.getName()).matches()) {
                    versionMap.put(f.getName().replace("V","").replace('_', '.'), f.getInt(opcodesClazz));
                }
            }
        } catch (IllegalAccessException ex) {
            LOG.log(SessionLog.SEVERE, "Error Java versions map from Opcodes.class fields.", ex);
            throw new RuntimeException(ex);
        }
        final List<String> versions = new ArrayList<String>(versionMap.keySet());
        final String oldest = versions.get(0);
        final String latest = versions.get(versions.size() - 1);

        // let's default to oldest supported Java SE version
        String v = oldest;
        if (System.getSecurityManager() == null) {
            v = System.getProperty("java.specification.version");
        } else {
            try {
                v = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        return System.getProperty("java.specification.version");
                    }
                });
            } catch (Throwable t) {
                // ie SecurityException
                LOG.log(SessionLog.WARNING, "Cannot read 'java.specification.version' property.", t);
                if (LOG.shouldLog(SessionLog.FINE)) {
                    LOG.log(SessionLog.FINE, "Generating bytecode for Java SE ''{0}''.", v);
                }
            }
        }
        Integer version = versionMap.get(v);
        if (version == null) {
            // current JDK is either too new
            if (latest.compareTo(v) < 0) {
                LOG.log(SessionLog.WARNING, "Java SE ''{0}'' is not fully supported yet. Report this error to the EclipseLink open source project.", v);
                if (LOG.shouldLog(SessionLog.FINE)) {
                    LOG.log(SessionLog.FINE, "Generating bytecode for Java SE ''{0}''.", latest);
                }
                version = versionMap.get(latest);
            } else {
                // or too old
                String key = oldest;
                LOG.log(SessionLog.WARNING, "Java SE ''{0}'' is too old.", v);
                if (LOG.shouldLog(SessionLog.FINE)) {
                    LOG.log(SessionLog.FINE, "Generating bytecode for Java SE ''{0}''.", key);
                }
                version = versionMap.get(key);
            }
        }
        return version;
    }
}
