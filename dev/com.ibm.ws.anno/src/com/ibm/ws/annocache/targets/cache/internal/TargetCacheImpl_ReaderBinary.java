/*******************************************************************************
  * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.cache.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.targets.internal.TargetsTableAnnotationsImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableClassesImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableClassesMultiImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableContainersImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableTimeStampImpl;
import com.ibm.ws.annocache.util.internal.UtilImpl_InternMap;
import com.ibm.ws.annocache.util.internal.UtilImpl_ReadBuffer;
import com.ibm.ws.annocache.util.internal.UtilImpl_ReadBufferFull;
import com.ibm.ws.annocache.util.internal.UtilImpl_ReadBufferPartial;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_BinaryConstants;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_ExternalConstants;

public class TargetCacheImpl_ReaderBinary implements TargetCache_BinaryConstants {
    private static final String CLASS_NAME = TargetCacheImpl_ReaderBinary.class.getSimpleName();

    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;

    //

    /** Control parameter: Indicates that a cache file has a compact strings table. */
    public static final boolean DO_READ_STRINGS = true;

    /** Control parameter: Tells if the the entire file be read into a buffer. */
    public static final boolean DO_READ_FULL = true;

    //

    public TargetCacheImpl_ReaderBinary(
        TargetCacheImpl_Factory factory,
        String path, String encoding,
        boolean readStrings, boolean readFull) throws IOException {

        this.factory = factory;

        if ( readFull ) {
            this.bufInput = new UtilImpl_ReadBufferFull(path, encoding);
            if ( readStrings ) {
                this.strings = readStrings();
                // 'readStrings' finishes by seeking to offset 0.
            } else {
                this.strings = null;
            }
        } else {
            this.bufInput = new UtilImpl_ReadBufferPartial(path, TargetCacheImpl_WriterBinary.STAMP_SIZE, encoding);
            this.strings = null; // Do not read strings unless doing a fully buffered read.
        }

        // Make sure the file is a cache storage file.
        requireBegin();
    }

    //

    protected final TargetCacheImpl_Factory factory;

    @Trivial
    public TargetCacheImpl_Factory getFactory() {
        return factory;
    }

    //

    protected final UtilImpl_ReadBuffer bufInput;

    @Trivial
    public UtilImpl_ReadBuffer getInput() {
        return bufInput;
    }

    @Trivial
    public String getPath() {
        return bufInput.getPath();
    }

    @Trivial
    public String getEncoding() {
        return bufInput.getEncoding();
    }

    public void close() {
        String methodName = "close";
        try {
            bufInput.close(); // throws IOException
        } catch ( IOException e ) {
            // FFDC
            // CWWKC0101W: Annotation processing cache error: {0}
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION", e.getMessage());
        }
    }

    //

    private final byte[] magicBytes = new byte[4];

    /**
     * Read and verify that the next four bytes read from the cache
     * file are specified bytes.
     * 
     * @param requiredBytes The bytes which must be read from the cache.
     *
     * @throws IOException Thrown if the required bytes were not read.
     */
    public void requireMagic(byte[] requiredBytes) throws IOException {
        bufInput.read(magicBytes, 0, 4);

        if ( !Arrays.equals(requiredBytes, magicBytes) ) {
            throw new IOException(
                "Failed read of [ " + getPath() + " ]:" +
                " Expected magic bytes [ " + requiredBytes + " ]" +
                " actual magic bytes [ " + magicBytes + " ]");
        }
    }

    public void requireBegin() throws IOException {
        requireMagic(MAGIC_BEGIN_BYTES);
    }

    public void requireEnd() throws IOException {
        requireMagic(MAGIC_END_BYTES);
    }

    //

    private final String[] strings;

    /**
     * Read the compact strings table from a cache file.
     *
     * The compact strings table, which present in a cache file,
     * is present as the last region of the file.
     *
     * The standard binary format which includes a strings table,
     * reading backwards from the end of the cache file, has end
     * bytes, a trailer record, the table of compact files, and
     * a single field byte:
     * 
     * <pre>
     *     STRINGS_BYTE    : 1 byte
     *     numberOfStrings : 4 bytes
     *     string          : 2 bytes + string.length bytes
     *     TRAILER_BYTE    : 1 byte
     *     stringsOffset   : 4 bytes
     *     MAGIC_END_BYTES : 4 bytes
     * </pre>
     *
     * @return The array of strings which were read.
     *
     * @throws IOException Thrown if the read failed.
     */
    protected String[] readStrings() throws IOException {
        // 4 for the end magic bytes
        // 1 + 4 for the trailer record
        // the trailer record has a field byte and 4 bytes for a large int
        bufInput.seekEnd( -4 + ((-4) + (-1)) );

        bufInput.requireByte(TRAILER_BYTE);
        int stringsOffset = bufInput.readLargeInt();
        requireEnd();

        bufInput.seek(stringsOffset);

        bufInput.requireByte(STRINGS_BYTE);

        int numStrings = bufInput.readLargeInt();

        String[] useStrings = new String[numStrings];

        for ( int strNo = 0; strNo < numStrings; strNo++ ) {
            String nextString = bufInput.readString();
            useStrings[strNo] = nextString;
        }

        bufInput.seek(0);

        return useStrings;
    }

    public String requireCompact(byte fieldByte) throws IOException {
        bufInput.requireByte(fieldByte);
        return requireCompact();
    }
    
    public String requireCompact() throws IOException {
        int offset = bufInput.readLargeInt(); 
        if ( (offset < 0) || (offset >= strings.length) ) {
            throw new IOException(
                "Compact string offset [ " + offset + " ]" +
                " must be at least 0 and less than [ " + strings.length + " ]");
        }
        return strings[offset];
    }    

    //

    public void requireHeader(String tableName, String tableVersion) throws IOException {
        bufInput.requireByte(HEADER_BYTE);

        String actualEncoding = bufInput.requireField(ENCODING_BYTE, HEADER_WIDTH);
        String actualTableName = bufInput.requireField(TABLE_BYTE, HEADER_WIDTH);
        String actualVersion = bufInput.requireField(VERSION_BYTE, HEADER_WIDTH);

        @SuppressWarnings("unused")
        String writeStamp = bufInput.requireField(TIMESTAMP_BYTE, HEADER_WIDTH);

        if ( !getEncoding().equals(actualEncoding) ) {
            throw new IOException(
                "Incompatible encoding [ " + actualEncoding + " ]:" +
                " Encoding should be [ " + getEncoding() + " ]" +
                " for [ " + getPath() + "]");
        }

        if ( !tableName.equals(actualTableName) ) {
            throw new IOException(
                    "Incorrect table name [ " + actualTableName + " ]:" +
                    " Expecting table name [ " + tableName + " ]" +
                    " for [ " + getPath() + "]");
        }

        if ( !tableVersion.equals(actualVersion) ) {
            throw new IOException(
                "Unexpected table version [ " + actualVersion + " ]:" +
                " Expecting table version [ " + tableVersion + " ]" +
                " for [ " + getPath() + "]");
        }
    }

    //

    protected void requireStampHeader() throws IOException {
        requireHeader(STAMP_TABLE_NAME, STAMP_TABLE_VERSION);
    }
    
    protected void requireContainerClassesHeader() throws IOException {
        requireHeader(CLASSES_TABLE_NAME, CLASSES_TABLE_VERSION);
    }
    
    protected void requireTargetsHeader() throws IOException {
        requireHeader(TARGETS_TABLE_NAME, TARGETS_TABLE_VERSION);
    }

    //

    protected void requireContainersHeader() throws IOException {
        requireHeader(CONTAINER_TABLE_NAME, CONTAINER_TABLE_VERSION);
    }

    protected void requireModuleClassesHeader() throws IOException {
        requireHeader(CLASSES_TABLE_NAME, CLASSES_TABLE_VERSION);
    }

    protected void requireResolvedRefsHeader() throws IOException {
        requireHeader(RESOLVED_REFS_NAME, RESOLVED_REFS_VERSION);
    }

    protected void requireUnresolvedRefsHeader() throws IOException {
        requireHeader(UNRESOLVED_REFS_NAME, UNRESOLVED_REFS_VERSION);
    }

    // Cases:
    //
    // (1) Container stamp
    // (2) Container classes
    // (3) Container targets
    //
    // (4) Container stamp, classes, and targets
    //
    // (5) Module Containers
    // (6) Module classes
    // (7) Module Resolved classes
    // (8) Module Unresolved classes

    //

    public void readEntire(TargetsTableTimeStampImpl stampTable) throws IOException {
        // the begin magic bytes are read when the reader was initialized
        readFragment(stampTable);
    }

    public void readEntire(TargetsTableClassesImpl classTable) throws IOException {
        // the begin magic bytes are read when the reader was initialized
        readFragment(classTable);
    }

    public void readEntire(
        TargetsTableClassesImpl classTable,
        TargetsTableAnnotationsImpl targetTable)
        throws IOException {

        readFragment(classTable);
        readFragment(targetTable);
    }

    public String readEntire(
        TargetsTableClassesImpl classTable,
        TargetsTableAnnotationsImpl targetTable,
        String currentStamp)
        throws IOException {

        readFragment(classTable);
        readFragment(targetTable);

        return null;
    }

    public void readEntire(TargetsTableAnnotationsImpl targetTable) throws IOException {
        readFragment(targetTable);
    }

    protected void readFragment(TargetsTableTimeStampImpl stampTable) throws IOException {
        requireHeader(STAMP_TABLE_NAME, STAMP_TABLE_VERSION);

        // Do not use the width for the name: The name cannot change.
        //
        // (That is fortunate: Handling all names would require a very
        // large width value.)
        //
        // Do use the width for the stamp.

        String componentName = bufInput.requireField(NAME_BYTE);
        String timeStamp = bufInput.requireField(STAMP_BYTE, HEADER_WIDTH);

        stampTable.setName(componentName);
        stampTable.setStamp(timeStamp);
    }

    protected void readFragment(TargetsTableClassesImpl classTable) throws IOException {
        requireContainerClassesHeader();

        bufInput.requireByte(DATA_BYTE);

        readPackages(classTable);
        readClasses(classTable);
    }
    

    public void readFragment(TargetsTableAnnotationsImpl targetTable) throws IOException {
        requireTargetsHeader();

        bufInput.requireByte(DATA_BYTE);

        readPackageTargets(targetTable);
        readClassTargets(targetTable);
    }

    //

    protected void readPackages(TargetsTableClassesImpl classTable) throws IOException {
        int numPackages = bufInput.readLargeInt();

        for ( int packageNo = 0; packageNo < numPackages; packageNo++ ) {
            String packageName = requireCompact(PACKAGE_BYTE);
            String i_packageName = classTable.internClassName(packageName);

            classTable.record(i_packageName);
        }
    }

    protected void readClasses(TargetsTableClassesImpl classTable) throws IOException {
        int numClasses = bufInput.readLargeInt();

        List<String> i_interfaceNames = new ArrayList<>();

        for ( int classNo = 0; classNo < numClasses; classNo++ ) {
            String className = requireCompact(CLASS_BYTE);
            String i_className = classTable.internClassName(className);

            String superClassName = requireCompact(SUPERCLASS_BYTE);
            String i_superClassName =
                ( superClassName.isEmpty() ? null : classTable.internClassName(superClassName) ); // ++ intern

            int numInterfaces = bufInput.readSmallInt();
            if ( numInterfaces > 0 ) {
                for ( int interfaceNo = 0; interfaceNo < numInterfaces; interfaceNo++ ) {
                    String interfaceName = requireCompact(INTERFACE_BYTE);
                    i_interfaceNames.add( classTable.internClassName(interfaceName) );
                }
            }

            bufInput.requireByte(MODIFIERS_BYTE);
            int modifiers = bufInput.readLargeInt();

            classTable.record(i_className, i_superClassName, i_interfaceNames, modifiers);

            i_interfaceNames.clear();
        }
    }

    //

    protected void readPackageTargets(TargetsTableAnnotationsImpl targetTable) throws IOException {
        int numPackages = bufInput.readLargeInt();

        for ( int packageNo = 0; packageNo < numPackages; packageNo++ ) {
            String packageName = requireCompact(PACKAGE_BYTE);
            String i_packageName = targetTable.internClassName(packageName);

            int numAnno = bufInput.readSmallInt();

            for ( int annoNo = 0; annoNo < numAnno; annoNo++ ) {
                String annoClassName = requireCompact(PACKAGE_ANNOTATION_BYTE);
                String i_annoClassName = targetTable.internClassName(annoClassName);

                targetTable.recordPackageAnnotation(i_packageName, i_annoClassName);
            }
        }
    }

    protected void readClassTargets(TargetsTableAnnotationsImpl targetTable) throws IOException {
        int numClasses = bufInput.readLargeInt();

        for ( int classNo = 0; classNo < numClasses; classNo++ ) {
            String className = requireCompact(CLASS_BYTE);
            String i_className = targetTable.internClassName(className);

            int numAnnotations = bufInput.readSmallInt();

            for ( int annoNo = 0; annoNo < numAnnotations; annoNo++ ) {
                byte annoTypeByte = (byte) bufInput.read();

                String annoClassName = requireCompact();
                String i_annoClassName = targetTable.internClassName(annoClassName);

                if ( annoTypeByte == CLASS_ANNOTATION_BYTE ) {
                    targetTable.recordClassAnnotation(i_className, i_annoClassName);
                } else if ( annoTypeByte == FIELD_ANNOTATION_BYTE ) {
                    targetTable.recordFieldAnnotation(i_className, i_annoClassName);
                } else if ( annoTypeByte == METHOD_ANNOTATION_BYTE ) {
                    targetTable.recordMethodAnnotation(i_className, i_annoClassName);
                } else {
                    throw new IOException("Unexpected annotation type [ " + annoTypeByte + " ] for class [ " + className + " ] in [ " + getPath() + " ]");
                }
            }
        }
    }

    //

    public void readEntire(TargetsTableContainersImpl containerTable) throws IOException {
        // the begin magic bytes are read when the reader was initialized
        readFragment(containerTable);
    }

    public void readFragment(TargetsTableContainersImpl containerTable) throws IOException {
        requireContainersHeader();

        bufInput.requireByte(DATA_BYTE);

        int numContainers = bufInput.readLargeInt();

        for ( int containerNo = 0; containerNo < numContainers; containerNo++ ) {
            String containerName = requireCompact(NAME_BYTE);
            if ( containerName.equals(TargetCache_ExternalConstants.CANONICAL_ROOT_CONTAINER_NAME) ) {
                containerName = TargetCache_ExternalConstants.ROOT_CONTAINER_NAME;
            }

            String containerPolicyText = requireCompact(POLICY_BYTE);
            ScanPolicy containerPolicy;
            try {
                containerPolicy = ScanPolicy.valueOf(containerPolicyText);
            } catch ( IllegalArgumentException e ) {
                throw new IOException(
                    "Unknown policy [ " + containerPolicyText + " ]" +
                    " of container [ " + containerName + " ]" +
                    " of [ " + getPath() + " ]"); 
            }

            containerTable.addName(containerName, containerPolicy);
        }
    }

    //

    public void readEntireResolvedRefs(
        Collection<String> i_resolvedClassNames,
        UtilImpl_InternMap internMap) throws IOException {

        // the begin magic bytes are read when the reader was initialized

        readFragmentResolvedRefs(i_resolvedClassNames, internMap);
    }

    public void readFragmentResolvedRefs(
        Collection<String> i_resolvedClassNames,
        UtilImpl_InternMap internMap) throws IOException {

        requireResolvedRefsHeader();

        bufInput.requireByte(DATA_BYTE);

        int numRefs = bufInput.readLargeInt();

        for ( int refNo = 0; refNo < numRefs; refNo++ ) {
            String className = bufInput.requireField(CLASS_BYTE);
            String i_className = internMap.intern(className);
            i_resolvedClassNames.add(i_className);
        }
    }

    //

    public void readEntireUnresolvedRefs(
        Collection<String> i_unresolvedClassNames,
        UtilImpl_InternMap internMap) throws IOException {

        // the begin magic bytes are read when the reader was initialized

        readFragmentUnresolvedRefs(i_unresolvedClassNames, internMap);
    }

    public void readFragmentUnresolvedRefs(
        Collection<String> i_unresolvedClassNames,
        UtilImpl_InternMap internMap) throws IOException {

        requireUnresolvedRefsHeader();

        bufInput.requireByte(DATA_BYTE);

        int numRefs = bufInput.readLargeInt();

        for ( int refNo = 0; refNo < numRefs; refNo++ ) {
            String className = bufInput.requireField(CLASS_BYTE);
            String i_className = internMap.intern(className);
            i_unresolvedClassNames.add(i_className);
        }
    }

    //

    public void readEntire(TargetsTableClassesMultiImpl classTable) throws IOException {
        // the begin magic bytes are read when the reader was initialized

    	requireModuleClassesHeader();

        bufInput.requireByte(DATA_BYTE);
        readPackages(classTable);
        readClasses(classTable);
    }

    protected void readPackages(TargetsTableClassesMultiImpl classTable) throws IOException {

        int numPackages = bufInput.readLargeInt();

        for ( int packageNo = 0; packageNo < numPackages; packageNo++ ) {
            String packageName = requireCompact(PACKAGE_BYTE);
            String i_packageName = classTable.internClassName(packageName);

            String sourceName = requireCompact(CLASS_SOURCE_BYTE);

            classTable.record(sourceName, i_packageName);
        }
    }

    protected void readClasses(TargetsTableClassesMultiImpl classTable) throws IOException {
        int numClasses = bufInput.readLargeInt();
        List<String> i_interfaceNames = new ArrayList<>();
        for ( int classNo = 0; classNo < numClasses; classNo++ ) {
            readClass(classTable, i_interfaceNames);
        }
    }

    protected void readClass(
        TargetsTableClassesMultiImpl classTable,
        List<String> i_interfaceNames) throws IOException {

        String className = requireCompact(CLASS_BYTE);
        String i_className = classTable.internClassName(className);

        String sourceName = requireCompact(CLASS_SOURCE_BYTE);

        String superClassName = requireCompact(SUPERCLASS_BYTE);
        String i_superClassName =
            ( superClassName.isEmpty() ? null : classTable.internClassName(superClassName) );

        int numInterfaces = bufInput.readSmallInt();
        for ( int interfaceNo = 0; interfaceNo < numInterfaces; interfaceNo++ ) {
            String interfaceName = requireCompact(INTERFACE_BYTE);
            String i_interfaceName = classTable.internClassName(interfaceName);
            i_interfaceNames.add(i_interfaceName);
        }

        bufInput.requireByte(MODIFIERS_BYTE);
        int modifiers = bufInput.readLargeInt();

        classTable.record(sourceName, i_className, i_superClassName, i_interfaceNames, modifiers);

        i_interfaceNames.clear();
    }
}
