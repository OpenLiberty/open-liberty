/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
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
import java.io.OutputStream;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.targets.internal.TargetsTableAnnotationsImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableClassesImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableClassesMultiImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableContainersImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableTimeStampImpl;
import com.ibm.ws.annocache.util.internal.UtilImpl_BidirectionalMap;
import com.ibm.ws.annocache.util.internal.UtilImpl_Utils;
import com.ibm.ws.annocache.util.internal.UtilImpl_WriteBuffer;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_BinaryConstants;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_ExternalConstants;

public class TargetCacheImpl_WriterBinary implements TargetCache_BinaryConstants {
    private static final String CLASS_NAME = TargetCacheImpl_WriterBinary.class.getSimpleName();

    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;

    //

    public static String getTimeStamp() {
        return UtilImpl_Utils.getDateAndTime();
    }

    //

    private static final int WRITE_BUFFER_SIZE = 64 * 1024;

    public TargetCacheImpl_WriterBinary(
        TargetCacheImpl_Factory factory,
        String path, OutputStream output,
        String encoding) throws IOException {

        this.factory = factory;
        this.bufOutput = new UtilImpl_WriteBuffer(path, output, WRITE_BUFFER_SIZE, encoding);
        this.strings = new LinkedHashMap<String, Integer>();
    }

    //

    protected final TargetCacheImpl_Factory factory;

    @Trivial
    public TargetCacheImpl_Factory getFactory() {
        return factory;
    }

    //

    protected final UtilImpl_WriteBuffer bufOutput;

    @Trivial
    public String getPath() {
        return bufOutput.getPath();
    }

    @Trivial
    public String getEncoding() {
        return bufOutput.getEncoding();
    }

    @Trivial
    public UtilImpl_WriteBuffer getBuffer() {
        return bufOutput;
    }

    public void close() {
        String methodName = "close";
        try {
            bufOutput.close(); // throws IOException
        } catch ( IOException e ) {
            // FFDC
            // CWWKC0101W: Annotation processing cache error: {0}
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION", e.getMessage());
        }
    }

    //

    private final Map<String, Integer> strings;

    public int map(String value) {
        Integer priorOffset = strings.get(value);
        if ( priorOffset != null ) {
            return priorOffset.intValue();
        } else {
            int numValues = strings.size();
            strings.put(value, Integer.valueOf(numValues) );
            return numValues;
        }
    }

    public void writeCompact(byte fieldByte, String value) throws IOException {
        bufOutput.writeLargeInt( fieldByte, map(value) );
    }

    public void writeStrings() throws IOException {
        int tableOffset = bufOutput.getTotalWritten();

        bufOutput.write(STRINGS_BYTE);
        bufOutput.writeLargeInt(strings.size());
        for ( String value : strings.keySet() ) {
            bufOutput.write(value); // throws IOException
        }

        bufOutput.write(TRAILER_BYTE);
        bufOutput.writeLargeInt(tableOffset);
    }

    //

    public static final int BEGIN_SIZE = 4;
    public static final int END_SIZE = 4;

    public static final int STAMP_PREFIX_SIZE = 1 + (HEADER_WIDTH * 4);
    public static final int STAMP_NAME_SIZE = 1 + 2 + UtilImpl_WriteBuffer.MAX_STRING;
    public static final int STAMP_SUFFIX_SIZE = HEADER_WIDTH;

    /** Maximum required size for buffering a read of the stamp table. */
    public static final int STAMP_SIZE =
        BEGIN_SIZE +
        STAMP_PREFIX_SIZE +
        STAMP_NAME_SIZE +
        STAMP_SUFFIX_SIZE;

    //

    public void writeBegin() throws IOException {
        bufOutput.write(MAGIC_BEGIN_BYTES, 0, 4);
    }

    public void writeEnd() throws IOException {
        bufOutput.write(MAGIC_END_BYTES, 0, 4);
    }


    public void writeHeader(String tableTag, String tableVersion) throws IOException {
        bufOutput.write(HEADER_BYTE);

        bufOutput.write(ENCODING_BYTE,  getEncoding(),  HEADER_WIDTH);
        bufOutput.write(TABLE_BYTE,     tableTag,       HEADER_WIDTH);
        bufOutput.write(VERSION_BYTE,   tableVersion,   HEADER_WIDTH);
        bufOutput.write(TIMESTAMP_BYTE, getTimeStamp(), HEADER_WIDTH);
    }

    public void writeTrailer(int offset) throws IOException {
        bufOutput.write(TRAILER_BYTE);
        bufOutput.writeLargeInt(offset);
    }

    //

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

    public void writeEntire(TargetsTableTimeStampImpl stampTable) throws IOException {
        // Omit the strings section:
        // No compact strings are generated when writing the stamp table.

        writeBegin();
        writeFragment(stampTable);
        writeEnd();
    }
    
    public void writeEntire(TargetsTableClassesImpl classTable) throws IOException {
        writeBegin();

        writeFragment(classTable);
        writeStrings();

        writeEnd();
    }

    public void writeEntire(
        TargetsTableClassesImpl classTable,
        TargetsTableAnnotationsImpl targetTable) throws IOException {

        writeBegin();

        writeFragment(classTable);
        writeFragment(targetTable);
        writeStrings();

        writeEnd();
    }

    public void rewrite(TargetsTableTimeStampImpl stampTable) throws IOException {
        writeBegin();

        writeFragment(stampTable);
    }

    public void writeEntire(TargetsTableAnnotationsImpl targetTable) throws IOException {
        writeBegin();

        writeFragment(targetTable);
        writeStrings();

        writeEnd();
    }

    protected void writeFragment(TargetsTableTimeStampImpl stampTable) throws IOException {
        writeHeader(STAMP_TABLE_NAME, STAMP_TABLE_VERSION);

        // Do not use the width for the name: The name cannot change.
        //
        // (That is fortunate: Handling all names would require a very
        // large width value.)
        //
        // Do use the width for the stamp.

        bufOutput.write(NAME_BYTE, stampTable.getName());
        bufOutput.write(STAMP_BYTE, stampTable.getStamp(), HEADER_WIDTH);
    }

    protected void writeFragment(TargetsTableClassesImpl classTable) throws IOException {
        writeHeader(CLASSES_TABLE_NAME, CLASSES_TABLE_VERSION);

        bufOutput.write(DATA_BYTE);

        writePackages(classTable);
        writeClasses(classTable);
    }
    

    public void writeFragment(TargetsTableAnnotationsImpl targetTable) throws IOException {
        writeHeader(TARGETS_TABLE_NAME, TARGETS_TABLE_VERSION);

        bufOutput.write(DATA_BYTE);

        writePackageTargets(targetTable);
        writeClassTargets(targetTable);
    }

    //

    protected void writePackages(TargetsTableClassesImpl classTable) throws IOException {
        Set<String> i_packageNames = classTable.i_getPackageNames();

        bufOutput.writeLargeInt( i_packageNames.size() );

        for ( String i_packageName : i_packageNames ) {
            writeCompact(PACKAGE_BYTE, i_packageName);
        }
    }

    protected void writeClasses(TargetsTableClassesImpl classTable) throws IOException {
        Set<String> i_classNames = classTable.i_getClassNames();

        bufOutput.writeLargeInt( i_classNames.size() );

        for ( String i_className : i_classNames ) {
            String i_superClassName = classTable.i_getSuperclassName(i_className);
            String[] i_interfaces = classTable.i_getInterfaceNames(i_className);
            Integer modifiers = classTable.i_getModifiers(i_className);

            writeClass(i_className, i_superClassName, i_interfaces, modifiers);
        }
    }

    @SuppressWarnings("null")
    protected void writeClass(String className, String superClassName, String[] interfaceNames, Integer modifiers)
        throws IOException {

        writeCompact(CLASS_BYTE, className);

        if ( superClassName == null ) {
            superClassName = "";
        }
        writeCompact(SUPERCLASS_BYTE, superClassName);

        int numInterfaces = ( (interfaceNames == null) ? 0 : interfaceNames.length );
        bufOutput.writeSmallInt(numInterfaces);
        if ( numInterfaces > 0 ) {
            for ( String interfaceName : interfaceNames ) {
                writeCompact(INTERFACE_BYTE, interfaceName);
            }
        }

        int modifiersValue = ( (modifiers == null) ? 0 : modifiers.intValue() );
        bufOutput.writeLargeInt(MODIFIERS_BYTE, modifiersValue);
    }

    //

    protected void writePackageTargets(TargetsTableAnnotationsImpl targetTable) throws IOException {
        UtilImpl_BidirectionalMap targetMap = targetTable.i_getPackageAnnotations();

        Set<String> i_packageNames = targetMap.getHolderSet();

        bufOutput.writeLargeInt( i_packageNames.size() );

        for ( String i_packageName : i_packageNames ) {
            writeCompact(PACKAGE_BYTE, i_packageName);

            Set<String> i_annotationClassNames = targetMap.selectHeldOf(i_packageName);
            int numAnnotations = i_annotationClassNames.size();

            bufOutput.writeSmallInt(numAnnotations);

            for ( String i_annotationClassName : i_annotationClassNames ) {
                writeCompact(PACKAGE_ANNOTATION_BYTE, i_annotationClassName);
            }
        }
    }

    protected void writeClassTargets(TargetsTableAnnotationsImpl targetTable) throws IOException {
        Map<String, String> i_annotatedClassNames = new IdentityHashMap<String, String>();

        UtilImpl_BidirectionalMap classTargetMap = targetTable.i_getClassAnnotations();
        UtilImpl_BidirectionalMap fieldTargetMap = targetTable.i_getFieldAnnotations();
        UtilImpl_BidirectionalMap methodTargetMap = targetTable.i_getMethodAnnotations();

        for ( String i_annotationClassName : classTargetMap.getHolderSet() ) {
            i_annotatedClassNames.put(i_annotationClassName, i_annotationClassName);
        }
        for ( String i_annotationClassName : fieldTargetMap.getHolderSet() ) {
            i_annotatedClassNames.put(i_annotationClassName, i_annotationClassName);
        }
        for ( String i_annotationClassName : methodTargetMap.getHolderSet() ) {
            i_annotatedClassNames.put(i_annotationClassName, i_annotationClassName);
        }

        Set<String> i_classNames = i_annotatedClassNames.keySet();

        bufOutput.writeLargeInt( i_classNames.size() );

        for ( String i_className : i_classNames ) {
            writeCompact(CLASS_BYTE, i_className);

            Set<String> i_classAnnotations = classTargetMap.selectHeldOf(i_className);
            Set<String> i_fieldAnnotations = fieldTargetMap.selectHeldOf(i_className);
            Set<String> i_methodAnnotations = methodTargetMap.selectHeldOf(i_className);

            bufOutput.writeSmallInt(
                i_classAnnotations.size() +
                i_fieldAnnotations.size() +
                i_methodAnnotations.size() );

            for ( String i_annotationClassName : i_classAnnotations ) {
                writeCompact(CLASS_ANNOTATION_BYTE, i_annotationClassName);
            }
            for ( String i_annotationClassName : i_fieldAnnotations ) {
                writeCompact(FIELD_ANNOTATION_BYTE, i_annotationClassName);
            }
            for ( String i_annotationClassName : i_methodAnnotations ) {
                writeCompact(METHOD_ANNOTATION_BYTE, i_annotationClassName);
            }
        }
    }

    //

    public void writeEntire(TargetsTableContainersImpl containerTable) throws IOException {
        writeBegin();

        writeFragment(containerTable);
        writeStrings();

        writeEnd();
    }

    public void writeFragment(TargetsTableContainersImpl containerTable) throws IOException {
        writeHeader(CONTAINER_TABLE_NAME, CONTAINER_TABLE_VERSION);

        bufOutput.write(DATA_BYTE);

        Collection<String> containerNames = containerTable.getNames();

        bufOutput.writeLargeInt( containerNames.size() );

        for ( String name : containerNames ) {
            ScanPolicy policy = containerTable.getPolicy(name);

            String writeName;
            if ( name.equals(TargetCache_ExternalConstants.CANONICAL_ROOT_CONTAINER_NAME) ) {
                writeName = TargetCache_ExternalConstants.ROOT_CONTAINER_NAME;
            } else {
                writeName = name;
            }

            writeCompact(NAME_BYTE, writeName);
            writeCompact(POLICY_BYTE, policy.toString());
        }
    }

    //

    @Trivial
    public void writeResolvedRefsEntire(Collection<String> resolvedClassNames) throws IOException {
        // The current compaction algorithm has no use on
        // the resolved class names, since they are distinct.

        writeBegin();
        writeResolvedRefsFragment(resolvedClassNames);
        writeEnd();
    }

    @Trivial
    public void writeResolvedRefsFragment(Collection<String> resolvedClassNames) throws IOException {
        writeHeader(RESOLVED_REFS_NAME, RESOLVED_REFS_VERSION);

        bufOutput.write(DATA_BYTE);
        bufOutput.writeLargeInt( resolvedClassNames.size() );
        for ( String className : resolvedClassNames ) {
            bufOutput.write(CLASS_BYTE, className); // No compaction, for now.
        }
    }

    //

    @Trivial
    public void writeUnresolvedRefsEntire(Collection<String> unresolvedClassNames) throws IOException {
        // The current compaction algorithm has no use on
        // the unresolved class names, since they are distinct.

        writeBegin();
        writeUnresolvedRefsFragment(unresolvedClassNames);
        writeEnd();
    }

    @Trivial
    public void writeUnresolvedRefsFragment(Collection<String> unresolvedClassNames) throws IOException {
        writeHeader(UNRESOLVED_REFS_NAME, UNRESOLVED_REFS_VERSION);

        bufOutput.write(DATA_BYTE);
        bufOutput.writeLargeInt( unresolvedClassNames.size() );
        for ( String className : unresolvedClassNames ) {
            bufOutput.write(CLASS_BYTE, className); // No compaction, for now.
        }
    }

    //

    public void writeEntire(TargetsTableClassesMultiImpl classTable) throws IOException {
        writeBegin();

        writeFragment(classTable);
        writeStrings();

        writeEnd();
    }

    public void writeFragment(TargetsTableClassesMultiImpl classTable) throws IOException {
        writeHeader(CLASSES_TABLE_NAME, CLASSES_TABLE_VERSION);

        bufOutput.write(DATA_BYTE);
        writePackages(classTable);
        writeClasses(classTable);
    }

    protected void writePackages(TargetsTableClassesMultiImpl classTable) throws IOException {
        Map<String, String> i_packageMap = classTable.i_getPackageNameClassSourceMap();

        bufOutput.writeLargeInt( i_packageMap.size() );

        for (Map.Entry<String, String> i_packageEntry : i_packageMap.entrySet() ) {
            String i_packageName = i_packageEntry.getKey();
            String classSourceName = i_packageEntry.getValue();

            writeCompact(PACKAGE_BYTE, i_packageName);
            writeCompact(CLASS_SOURCE_BYTE, classSourceName);
        }
    }

    protected void writeClasses(TargetsTableClassesMultiImpl classTable) throws IOException {
        Map<String, String> i_classMap = classTable.i_getClassNameClassSourceMap();
        
        bufOutput.writeLargeInt( i_classMap.size() );

        for ( Map.Entry<String, String> i_classEntry : i_classMap.entrySet() ) {
            String i_className = i_classEntry.getKey();
            String classSourceName = i_classEntry.getValue();

            String i_superClassName = classTable.i_getSuperclassName(i_className);
            String[] i_interfaces = classTable.i_getInterfaceNames(i_className);

            Integer modifiers = classTable.i_getModifiers(i_className);

            writeClass(classSourceName, i_className, i_superClassName, i_interfaces, modifiers);
        }
    }

    @SuppressWarnings("null")
    protected void writeClass(String classSourceName,
                              String className,
                              String superClassName, String[] interfaceNames,
                              Integer modifiers)
        throws IOException {

        writeCompact(CLASS_BYTE, className);
        writeCompact(CLASS_SOURCE_BYTE, classSourceName);

        if ( superClassName == null ) {
            superClassName = "";
        }
        writeCompact(SUPERCLASS_BYTE, superClassName);

        int numInterfaceNames;
        if ( interfaceNames == null ) {
            numInterfaceNames = 0;
        } else {
            numInterfaceNames = interfaceNames.length;
        }
        bufOutput.writeSmallInt(numInterfaceNames);
        if ( numInterfaceNames > 0 ) {
            for ( String interfaceName : interfaceNames ) {
                writeCompact(INTERFACE_BYTE, interfaceName);
            }
        }

        int modifiersValue = ( (modifiers == null) ? 0 : modifiers.intValue() );
        bufOutput.writeLargeInt(MODIFIERS_BYTE, modifiersValue);
    }
}
