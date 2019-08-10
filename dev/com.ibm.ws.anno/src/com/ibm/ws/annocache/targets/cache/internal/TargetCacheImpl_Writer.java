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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
// import java.util.ConcurrentModificationException;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.jandex.Index;
import org.objectweb.asm.Opcodes;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.jandex.internal.Jandex_Utils;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.targets.internal.TargetsTableAnnotationsImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableClassesImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableClassesMultiImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableContainersImpl;
import com.ibm.ws.annocache.targets.internal.TargetsTableTimeStampImpl;
import com.ibm.ws.annocache.util.internal.UtilImpl_BidirectionalMap;
import com.ibm.ws.annocache.util.internal.UtilImpl_Utils;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_ExternalConstants;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_InternalConstants;

public class TargetCacheImpl_Writer implements TargetCache_InternalConstants {
    private static final String CLASS_NAME = TargetCacheImpl_Writer.class.getSimpleName();

    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;

    //

    public static String getTimeStamp() {
        return UtilImpl_Utils.getDateAndTime();
    }

    @Trivial
    protected static String policiesText(int policies) {
        // Empty policies is unexpected, but handle it anyways.
        if ( policies == 0 ) {
            return "";

            // Common single policy settings.
            // SEED by itself is most common.
        } else if ( policies == ScanPolicy.SEED.getValue() ) {
            return ScanPolicy.SEED.toString();
        } else if ( policies == ScanPolicy.PARTIAL.getValue() ) {
            return ScanPolicy.PARTIAL.toString();
        } else if ( policies == ScanPolicy.PARTIAL.getValue() ) {
            return ScanPolicy.PARTIAL.toString();
        } else if ( policies == ScanPolicy.EXCLUDED.getValue() ) {
            return ScanPolicy.EXCLUDED.toString();
        } else if ( policies == ScanPolicy.EXTERNAL.getValue() ) {
            return ScanPolicy.EXTERNAL.toString();

            // Next common: Used by servlet container initializer queries
        } else if ( policies == (ScanPolicy.SEED.getValue() & ScanPolicy.PARTIAL.getValue()) ) {
            return ScanPolicy.SEED.toString() + ", " + ScanPolicy.PARTIAL.toString();

            // Uncommon combination: Iterate and build dynamically.
        } else {
            StringBuilder builder = new StringBuilder ();

            for ( ScanPolicy policy : ScanPolicy.values() ) {
                if ( (policies & policy.getValue()) == 0 ) {
                    continue;
                }
                if ( builder.length() > 0 ) {
                    builder.append(',');
                    builder.append(' ');
                }
                builder.append( policy.toString() );
            }
            return builder.toString();
        }
    }

    //

    public TargetCacheImpl_Writer(TargetCacheImpl_Factory factory,
                                  String path,
                                  OutputStream stream,
                                  String encoding) throws UnsupportedEncodingException {
        super();

        this.factory = factory;

        this.path = path;
        this.stream = stream;

        this.encoding = encoding;
        this.writer = new OutputStreamWriter(stream, encoding); // throws UnsupportedEncodingException
        this.bufferedWriter = new BufferedWriter(writer);
    }

    //

    protected final TargetCacheImpl_Factory factory;

    @Trivial
    public TargetCacheImpl_Factory getFactory() {
        return factory;
    }

    //

    protected final String path;
    protected final OutputStream stream;

    protected final String encoding;
    protected final OutputStreamWriter writer;
    protected final BufferedWriter bufferedWriter;

    @Trivial
    public String getPath() {
        return path;
    }

    @Trivial
    public OutputStream getStream() {
        return stream;
    }

    @Trivial
    public String getEncoding() {
        return encoding;
    }

    @Trivial
    public OutputStreamWriter getWriter() {
        return writer;
    }

    @Trivial
    public BufferedWriter getBufferedWriter() {
        return bufferedWriter;
    }

    //

    protected void writeLine(String methodName, String outputLine) throws IOException {
        bufferedWriter.append(outputLine); // throws IOException
        bufferedWriter.newLine(); // throws IOException
    }

    protected void close() {
        String methodName = "close";
        try {
            bufferedWriter.close(); // throws IOException
        } catch ( IOException e ) {
            // FFDC
            // CWWKC0101W: Annotation processing cache error: {0}
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION", e.getMessage());
        }
    }

    protected void flush() {
        String methodName = "flush";
        try {
            bufferedWriter.flush(); // throws IOException
        } catch ( IOException e ) {
            // FFDC
            // CWWKC0101W: Annotation processing cache error: {0}
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_TARGETS_CACHE_EXCEPTION", e.getMessage());            logger.logp(Level.WARNING, CLASS_NAME, methodName, "Cache error", e);
        }
    }

    //

    public void write(TargetsTableContainersImpl containerTable) throws IOException {
        writeHeader(CONTAINER_TABLE_TAG, CONTAINER_TABLE_VERSION);

        writeComment(DELIMITER_TAG);
        writeComment(CONTAINERS_SECTION);

        for ( String name : containerTable.getNames() ) {
            ScanPolicy policy = containerTable.getPolicy(name);

            String writeName;
            if ( name.equals(TargetCache_ExternalConstants.CANONICAL_ROOT_CONTAINER_NAME) ) {
                writeName = TargetCache_ExternalConstants.ROOT_CONTAINER_NAME;
            } else {
                writeName = name;
            }

            writeValue(NAME_TAG, writeName);
            writeValue(POLICY_TAG, policy.toString());
        }

        writeTrailer();
    }

    @Trivial
    public void writeResolvedRefs(Collection<String> resolvedClassNames) throws IOException {
        writeHeader(RESOLVED_REFS_TAG, RESOLVED_REFS_VERSION);

        writeComment(DELIMITER_TAG);
        writeComment(RESOLVED_REFS_SECTION);

        for ( String className : resolvedClassNames ) {
            writeValue(CLASS_TAG, className);
        }

        writeTrailer();
    }

    @Trivial
    public void writeUnresolvedRefs(Collection<String> unresolvedClassNames) throws IOException {
        writeHeader(UNRESOLVED_REFS_TAG, UNRESOLVED_REFS_VERSION);

        writeComment(DELIMITER_TAG);
        writeComment(UNRESOLVED_REFS_SECTION);

        for ( String className : unresolvedClassNames ) {
            writeValue(CLASS_TAG, className);
        }

        writeTrailer();
    }

    //

    public void write(TargetsTableTimeStampImpl stampTable) throws IOException {
        writeHeader(STAMP_TABLE_TAG, STAMP_TABLE_VERSION);

        // Do not use the width for the name:
        // The name should not change.
        // (That is fortunate: The width must be longer than
        // the values which use the width, and the name can
        // be very long.)
        //
        // Do use the width for the stamp.

        writeValue(NAME_TAG, stampTable.getName());
        writeValue(STAMP_TAG, stampTable.getStamp(), HEADER_WIDTH);

        writeTrailer();
    }

    //

    public void write(TargetsTableClassesImpl classTable) throws IOException {
        writeHeader(CLASSES_TABLE_TAG, CLASSES_TABLE_VERSION);

        writePackages(classTable);
        writeClasses(classTable);

        writeTrailer();
    }

    protected void writePackages(TargetsTableClassesImpl classTable) throws IOException {
        writeComment(DELIMITER_TAG);
        writeComment(PACKAGES_SECTION);

        for ( String i_packageName : classTable.i_getPackageNames() ) {
            writeValue(PACKAGE_TAG, i_packageName);
        }
    }

    protected void writeClasses(TargetsTableClassesImpl classTable) throws IOException {
        writeComment(DELIMITER_TAG);
        writeComment(CLASSES_SECTION);

//        classTable.clearClassRecord();
//        
//        boolean onFirst = true;

        for ( String i_className : classTable.i_getClassNames() ) {
//            if ( onFirst ) {
//                classTable.verifyClassRecord();
//                onFirst = false;
//            }

            String i_superClassName = classTable.i_getSuperclassName(i_className);
            String[] i_interfaces = classTable.i_getInterfaceNames(i_className);
            Integer modifiers = classTable.i_getModifiers(i_className);
            
            writeClass(i_className, i_superClassName, i_interfaces, modifiers);

//            classTable.verifyClassRecord();
        }
    }

    private enum OpcodeTag {
        ACC_PUBLIC(Opcodes.ACC_PUBLIC, "public"),
        ACC_PRIVATE(Opcodes.ACC_PRIVATE, "private"),
        ACC_PROTECTED(Opcodes.ACC_PROTECTED, "protected"),
        ACC_STATIC(Opcodes.ACC_STATIC, "static"),
        ACC_FINAL(Opcodes.ACC_FINAL, "final"),
        ACC_SUPER(Opcodes.ACC_SUPER, "super"),
        ACC_SYNCHRONIZED(Opcodes.ACC_SYNCHRONIZED, "synchronized"),
        ACC_VOLATILE(Opcodes.ACC_VOLATILE, "volatile"),
        ACC_BRIDGE(Opcodes.ACC_BRIDGE, "bridge"),
        ACC_VARARGS(Opcodes.ACC_VARARGS, "varargs"),
        ACC_TRANSIENT(Opcodes.ACC_TRANSIENT, "transient"),
        ACC_NATIVE(Opcodes.ACC_NATIVE, "native"),
        ACC_INTERFACE(Opcodes.ACC_INTERFACE, "interface"),
        ACC_ABSTRACT(Opcodes.ACC_ABSTRACT, "abstract"),
        ACC_STRICT(Opcodes.ACC_STRICT, "strict"),
        ACC_SYNTHETIC(Opcodes.ACC_SYNTHETIC, "synthetic"),
        ACC_ANNOTATION(Opcodes.ACC_ANNOTATION, "annotation"),
        ACC_ENUM(Opcodes.ACC_ENUM, "enum"),
        ACC_DEPRECATED(Opcodes.ACC_DEPRECATED, "deprecated");

        @Trivial
        private OpcodeTag(int opcode, String tag) {
            this.opcode = opcode;
            this.tag = tag;
        }

        private final int opcode;
        private final String tag;

        @Trivial
        public int getOpcode() {
            return opcode;
        }

        @Trivial
        public String getTag() {
            return tag;
        }

        //

        @Trivial
        public static String asText(int modifiers) {
            if ( modifiers == 0 ) {
                return null;
            }

            String singleTag = null;
            StringBuilder multiTag = null;
            
            for ( OpcodeTag modifiersText : OpcodeTag.values() ) {
                int opcode = modifiersText.getOpcode();
                if ( (modifiers & opcode) != 0 ) {
                    modifiers &= ~opcode;

                    String modifierTag = modifiersText.getTag();
                    if ( singleTag == null ) {
                        singleTag = modifierTag;
                    } else {
                        if ( multiTag == null ) {
                            multiTag = new StringBuilder(singleTag);
                        }
                        multiTag.append(" ");
                        multiTag.append(modifierTag);
                    }                    
                }
            }

            String unknownText;
            if ( modifiers != 0 ) {
                unknownText = "UNKNOWN(" + Integer.toHexString(modifiers);
            } else {
                unknownText = null;
            }
            
            String text;
            if ( singleTag == null ) {
                text = unknownText;
            } else if ( multiTag == null ) {
                if ( unknownText == null ) {
                    text = singleTag;
                } else {
                    text = singleTag + " " + unknownText;
                }
            } else {
                if ( unknownText != null ) {
                    multiTag.append(" ");
                    multiTag.append(unknownText);
                }
                text = multiTag.toString();
            }
            
            return text;
        }
    }

    protected void writeClass(String className, String superClassName, String[] interfaceNames, Integer modifiers)
        throws IOException {

        writeValue(CLASS_TAG, className);

        if ( superClassName != null ) {
            writeSubValue(SUPERCLASS_TAG, superClassName);
        }

        if ( interfaceNames != null ) {
            for ( String interfaceName : interfaceNames ) {
                writeSubValue(INTERFACE_TAG, interfaceName);
            }
        }

        if ( modifiers != null ) {
            int modifiersValue = modifiers.intValue();
            if ( modifiersValue != 0 ) {
                String modifiersText =
                        "0x" +  Integer.toHexString(modifiersValue) +
                        " " + OpcodeTag.asText(modifiersValue);
                writeSubValue(MODIFIERS_TAG, modifiersText); 
            }
        }
    }

    public void write(TargetsTableClassesMultiImpl classTable) throws IOException {
        writeHeader(CLASSES_TABLE_TAG, CLASSES_TABLE_VERSION);

        writePackages(classTable);
        writeClasses(classTable);

        writeTrailer();
    }

    protected void writePackages(TargetsTableClassesMultiImpl classTable) throws IOException {
        writeComment(DELIMITER_TAG);
        writeComment(PACKAGES_SECTION);

        for (Map.Entry<String, String> i_packageEntry : classTable.i_getPackageNameClassSourceMap().entrySet() ) {
            String i_packageName = i_packageEntry.getKey();
            String classSourceName = i_packageEntry.getValue();

            writeValue(PACKAGE_TAG, i_packageName);
            writeSubValue(CLASS_SOURCE_TAG, classSourceName);
        }
    }

    protected void writeClasses(TargetsTableClassesMultiImpl classTable) throws IOException {
        writeComment(DELIMITER_TAG);
        writeComment(CLASSES_SECTION);

//        try {
//            classTable.clearClassRecord();

//            boolean onFirst = true;
        
            for ( Map.Entry<String, String> i_classEntry : classTable.i_getClassNameClassSourceMap().entrySet() ) {
//                if ( onFirst ) {
//                    classTable.verifyClassRecord();
//                    onFirst = false;
//                }
//            
                String i_className = i_classEntry.getKey();
                String classSourceName = i_classEntry.getValue();

                String i_superClassName = classTable.i_getSuperclassName(i_className);
                String[] i_interfaces = classTable.i_getInterfaceNames(i_className);

                Integer modifiers = classTable.i_getModifiers(i_className);

                writeClass(classSourceName, i_className, i_superClassName, i_interfaces, modifiers);
            
//                classTable.verifyClassRecord();            
            }
//        } catch ( ConcurrentModificationException e ) {
//            System.out.println("Concurrent modification");
//            classTable.verifyClassRecord();
//            
//            throw e;
//        }
    }

    protected void writeClass(String classSourceName,
                              String className,
                              String superClassName, String[] interfaceNames,
                              Integer modifiers)
        throws IOException {

        writeValue(CLASS_TAG, className);
        writeSubValue(CLASS_SOURCE_TAG, classSourceName);

        if ( superClassName != null ) {
            writeSubValue(SUPERCLASS_TAG, superClassName);
        }

        if ( interfaceNames != null ) {
            for ( String interfaceName : interfaceNames ) {
                writeSubValue(INTERFACE_TAG, interfaceName);
            }
        }

        if ( modifiers != null ) {
            int modifiersValue = modifiers.intValue();
            if ( modifiersValue != 0 ) {
                String modifiersText =
                        "0x" +  Integer.toHexString(modifiersValue) +
                        " " + OpcodeTag.asText(modifiersValue);
                writeSubValue(MODIFIERS_TAG, modifiersText); 
            }
        }
    }

    //

    public void write(TargetsTableAnnotationsImpl targetTable) throws IOException {
        writeHeader(TARGETS_TABLE_TAG, TARGETS_TABLE_VERSION);

        writePackageTargets(targetTable);
        writeClassTargets(targetTable);

        writeTrailer();
    }

    protected void writePackageTargets(TargetsTableAnnotationsImpl targetTable) throws IOException {
        writeComment(DELIMITER_TAG);
        writeComment(PACKAGE_TARGETS_SECTION);

        UtilImpl_BidirectionalMap targetMap = targetTable.i_getPackageAnnotations();

        for ( String i_packageName : targetMap.getHolderSet() ) {
            Set<String> i_annotationClassNames = targetMap.selectHeldOf(i_packageName);
            if ( !i_annotationClassNames.isEmpty() ) {
                writeValue(PACKAGE_TAG, i_packageName);
                for ( String i_annotationClassName : i_annotationClassNames ) {
                    writeSubValue(PACKAGE_ANNOTATION_TAG, i_annotationClassName);
                }
            }
        }
    }

    protected void writeClassTargets(TargetsTableAnnotationsImpl targetTable) throws IOException {
        writeComment(DELIMITER_TAG);
        writeComment(CLASS_TARGETS_SECTION);

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

        for ( String i_className : i_annotatedClassNames.keySet() ) {
            writeValue(CLASS_TAG, i_className);

            for ( String i_annotationClassName : classTargetMap.selectHeldOf(i_className) ) {
                writeSubValue(CLASS_ANNOTATION_TAG, i_annotationClassName);
            }
            for ( String i_annotationClassName : fieldTargetMap.selectHeldOf(i_className) ) {
                writeSubValue(FIELD_ANNOTATION_TAG, i_annotationClassName);
            }
            for ( String i_annotationClassName : methodTargetMap.selectHeldOf(i_className) ) {
                writeSubValue(METHOD_ANNOTATION_TAG, i_annotationClassName);
            }
        }
    }

    protected void writeTargets(UtilImpl_BidirectionalMap targetMap,
                                String targetTag, String annotationTag)
        throws IOException {

        for ( String i_packageName : targetMap.getHolderSet() ) {
            Set<String> i_annotationClassNames = targetMap.selectHeldOf(i_packageName);
            if ( !i_annotationClassNames.isEmpty() ) {
                writeValue(targetTag, i_packageName);
                for ( String i_annotationClassName : i_annotationClassNames ) {
                    writeSubValue(annotationTag, i_annotationClassName);
                }
            }
        }
    }

    //

    protected void writeHeader(String tableTag, String tableVersion) throws IOException {
        writeComment(DELIMITER_TAG);

        writeValue(ENCODING_TAG,  getEncoding(),  HEADER_WIDTH);
        writeValue(TABLE_TAG,     tableTag,       HEADER_WIDTH);
        writeValue(VERSION_TAG,   tableVersion,   HEADER_WIDTH);

        writeValue(TIMESTAMP_TAG, getTimeStamp(), HEADER_WIDTH);
    }

    protected void writeTrailer() throws IOException {
        writeComment(END_DELIMITER_TAG);
    }

    //

    protected void writeComment(String commentText) throws IOException {
        String methodName = "writeComment";

        String commentLine =
            COMMENT_TAG + SPACE_TAG +
            commentText;

        writeLine(methodName, commentLine);
    }

    protected void writeComment(String commentName, String commentValue) throws IOException {
        String commentText =
            commentName + COLON_TAG + SPACE_TAG +
            commentValue;

        writeComment(commentText);
    }

    protected void writeValue(String name, String value) throws IOException {
        String methodName = "writeValue";

        String valueText =
            name + COLON_TAG + SPACE_TAG +
            value;

        writeLine(methodName, valueText);
    }

    private static final String SPACES = "                                                                                ";
    public static final int MAX_SPACES = 80;

    /**
     * Write a name and value as a single line.
     * 
     * Place ": " after the name and before the value.
     * 
     * Places spaces before the value sufficient to shift
     * the last character of the value at the specified width.
     * The name and ": " are taken into account when placing
     * spaces.
     *
     * Only up to {@link #MAX_SPACES} spaces will be added.
     *
     * @param name A name to display with the value.
     * @param value A value to display.
     * @param width The width to which to display the value.
     *
     * @throws IOException Thrown if the write failed.
     */
    protected void writeValue(String name, String value, int width) throws IOException {
        String methodName = "writeValue";

        String valueText;

        // The default format has:
        //     name + ": " + value

        int missingSpaces = width - ((name.length() + 2) + value.length());

        // Add spaces to reach the target width:
        //     name + ": " + spaces + value

        if ( missingSpaces > 0 ) {
            if ( missingSpaces > MAX_SPACES ) {
                missingSpaces = MAX_SPACES;
            }
            valueText =
                name + COLON_TAG + SPACE_TAG +
                SPACES.substring(0, missingSpaces) + value;

        } else if ( missingSpaces == 0 ) {
            valueText =
                name + COLON_TAG + SPACE_TAG +
                value;

        } else {
            // The write is used to produce a fixed format header.
            // That the value does not fit is an error.

            throw new IllegalArgumentException(
                "Name [ " + name + " ] and value [ " + value + " ]" +
                " are too large for width [ " + Integer.toString(width) + " ]");
        }

        writeLine(methodName, valueText);
    }

    protected void writeSubValue(String name, String value) throws IOException {
        String methodName = "writeSubValue";

        String valueText =
            SPACE_TAG + SPACE_TAG +
            name + COLON_TAG + SPACE_TAG +
            value;

        writeLine(methodName, valueText);
    }

    protected void writeSubComment(String commentText) throws IOException {
        commentText = SPACE_TAG + SPACE_TAG + commentText;
        writeComment(commentText);
    }

    protected void writeSubSubValue(String name, String value) throws IOException {
        String methodName = "writeSubSubValue";

        String valueText =
            SPACE_TAG + SPACE_TAG +
            SPACE_TAG + SPACE_TAG +
            name + COLON_TAG + SPACE_TAG +
            value;

        writeLine(methodName, valueText);
    }

    //

    public void writeQueryHeader() throws IOException {
        writeHeader(QUERIES_TABLE_TAG, QUERIES_TABLE_VERSION);
    }

    // # ============================================================
    // Query: <title>
    // Timestamp: <Date-Time>
    // Policies: (SEED, PARTIAL, EXCLUDED, EXTERNAL)
    // Type: (PACKAGE, CLASS, CLASS INHERITED, FIELD, METHOD)
    // [ Specific: <specific source class> ]
    // Annotation: <annotation class>
    // Result: <result class>

    // Query: <title>
    // Timestamp: <Date-Time>
    // Source: <source name>
    // Type: (PACKAGE, CLASS, CLASS INHERITED, FIELD, METHOD)
    // [ Specific: <specific source class> ]
    // Annotation: <annotation class>
    // Result: <result class>

    // Query: <title>
    // Timestamp: <Date-Time>
    // Policies: (SEED, PARTIAL, EXCLUDED, EXTERNAL)
    // Source: <source name>
    // Type: (PACKAGE, CLASS, CLASS INHERITED, FIELD, METHOD)
    // [ Specific: <specific source class> ]
    // Annotation: <annotation class>
    // Result: <result class>

    public void writeQuery(
        String className, String methodName,
        String title,
        int policies, String type,
        Collection<String> specificClasses, String annotationClass,
        Collection<String> resultClasses) throws IOException {

        writeQuery(
            className, methodName,
            title, getTimeStamp(),
            policies, type,
            specificClasses, annotationClass,
            resultClasses);
        // 'writeQuery' throws IOException
    }

    public void writeQuery(
        String className, String methodName,
        String title, String timeStamp,
        int policies, String type,
        Collection<String> specificClasses, String annotationClass,
        Collection<String> resultClasses) throws IOException {

        writeComment(DELIMITER_TAG);

        writeValue(QUERY_TAG, title);
        writeValue(QUERY_CLASS_NAME_TAG, className);
        writeValue(QUERY_METHOD_NAME_TAG, methodName);
        writeValue(QUERY_TIMESTAMP_TAG, timeStamp);

        writeValue(QUERY_POLICIES_TAG, policiesText(policies));
        writeValue(QUERY_TYPE_TAG, type);

        if ( specificClasses != null ) {
            for ( String specificClass : specificClasses ) {
                writeSubValue(QUERY_SPECIFIC_TAG, specificClass);
            }
        }
        writeValue(QUERY_ANNOTATION_TAG, annotationClass);
        for ( String resultClass : resultClasses ) {
            writeSubValue(QUERY_RESULT_TAG, resultClass);
        }

        flush();
    }

    public void writeQuery(
        String className, String methodName,
        String title,
        Collection<String> sources, String type,
        Collection<String> specificClasses, String annotationClass,
        Collection<String> resultClasses) throws IOException {

        writeQuery(
            className, methodName,
            title, getTimeStamp(),
            sources, type, specificClasses,
            annotationClass,
            resultClasses);
        // 'writeQuery' throws IOException
    }

    public void writeQuery(
        String className, String methodName,
        String title, String timeStamp,
        Collection<String> sources, String type,
        Collection<String> specificClasses, String annotationClass,
        Collection<String> resultClasses) throws IOException {

        writeComment(DELIMITER_TAG);

        writeValue(QUERY_TAG, title);
        writeValue(QUERY_CLASS_NAME_TAG, className);
        writeValue(QUERY_METHOD_NAME_TAG, methodName);
        writeValue(QUERY_TIMESTAMP_TAG, timeStamp);

        for ( String source : sources ) {
            writeValue(QUERY_SOURCE_TAG, source);
        }
        writeValue(QUERY_TYPE_TAG, type);

        if ( specificClasses != null ) {
            for ( String specificClass : specificClasses ) {
                writeSubValue(QUERY_SPECIFIC_TAG, specificClass);
            }
        }
        writeValue(QUERY_ANNOTATION_TAG, annotationClass);
        for ( String resultClass : resultClasses ) {
            writeSubValue(QUERY_RESULT_TAG, resultClass);
        }

        flush();
    }

    public void writeQuery(
        String className, String methodName,
        String title,
        int policies, Collection<String> sources, String type,
        Collection<String> specificClasses, String annotationClass,
        Collection<String> resultClasses) throws IOException {

        writeQuery(
            className, methodName,
            title, getTimeStamp(),
            policies, sources, type,
            specificClasses, annotationClass,
            resultClasses);
        // 'writeQuery' throws IOException
    }

    public void writeQuery(
        String className, String methodName,
        String title, String timeStamp,
        int policies, Collection<String> sources, String type,
        Collection<String> specificClasses, String annotationClass,
        Collection<String> resultClasses) throws IOException {

        writeComment(DELIMITER_TAG);

        writeValue(QUERY_TAG, title);
        writeValue(QUERY_CLASS_NAME_TAG, className);
        writeValue(QUERY_METHOD_NAME_TAG, methodName);
        writeValue(QUERY_TIMESTAMP_TAG, timeStamp);

        writeValue(QUERY_POLICIES_TAG, policiesText(policies));
        for ( String source : sources ) {
            writeValue(QUERY_SOURCE_TAG, source);
        }
        writeValue(QUERY_TYPE_TAG, type);

        if ( specificClasses != null ) {
            for ( String specificClass : specificClasses ) {
                writeSubValue(QUERY_SPECIFIC_TAG, specificClass);
            }
        }
        writeValue(QUERY_ANNOTATION_TAG, annotationClass);
        for ( String resultClass : resultClasses ) {
            writeSubValue(QUERY_RESULT_TAG, resultClass);
        }

        flush();
    }

    //

    /**
     * Write a jandex index to the bound stream.
     * 
     * Ignore the writer: This is a binary write.
     *
     * @param jandexIndex The jandex index which is to be written.
     *
     * @throws IOException Thrown if the write fails.
     */
    public void write(Index jandexIndex) throws IOException {
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, "write", "Jandex write [ " + getPath() + " ] [ " + jandexIndex.getKnownClasses().size() + " ]");
        }
        Jandex_Utils.basicWriteIndex( getStream(), jandexIndex, "Jandex write [ " + getPath() + " ]" );
        // 'basicWriteIndex' throws IOException
    }

    //

    @Trivial
    protected static void logLine(StringBuilder builder, String outputLine) {
        builder.append(outputLine);
        builder.append("/n");
    }

    @Trivial
    protected static void logComment(StringBuilder builder, String commentText) {
        String commentLine =
            COMMENT_TAG + SPACE_TAG +
            commentText;

        logLine(builder, commentLine);
    }

    @Trivial
    protected static void logComment(
        StringBuilder builder,
        String commentName, String commentValue) {

        String commentText =
            commentName + COLON_TAG + SPACE_TAG +
            commentValue;

        logComment(builder, commentText);
    }

    @Trivial
    protected static void logValue(StringBuilder builder, String name, String value) {
        String valueText =
            name + COLON_TAG + SPACE_TAG +
            value;

        logLine(builder, valueText);
    }

    @Trivial
    protected static void logSubValue(StringBuilder builder, String name, String value) {
        String valueText =
            SPACE_TAG + SPACE_TAG +
            name + COLON_TAG + SPACE_TAG +
            value;

        logLine(builder, valueText);
    }

    // Query: <title>
    // Timestamp: <Date-Time>
    // Source: <source name>
    // Type: (PACKAGE, CLASS, CLASS INHERITED, FIELD, METHOD)
    // [ Specific: <specific source class> ]
    // Annotation: <annotation class>
    // Result: <result class>

    // Query: <title>
    // Timestamp: <Date-Time>
    // Policies: (SEED, PARTIAL, EXCLUDED, EXTERNAL)
    // Source: <source name>
    // Type: (PACKAGE, CLASS, CLASS INHERITED, FIELD, METHOD)
    // [ Specific: <specific source class> ]
    // Annotation: <annotation class>
    // Result: <result class>

    @Trivial
    public static void logQuery(
        StringBuilder builder,
        String title,
        int policies, String type,
        Collection<String> specificClasses, String annotationClass,
        Collection<String> resultClasses) {

        logQuery(builder,
            title, getTimeStamp(),
            policies, type, specificClasses, annotationClass, resultClasses);
    }

    @Trivial
    public static void logQuery(
        StringBuilder builder,
        String title, String timeStamp,
        int policies, String type,
        Collection<String> specificClasses, String annotationClass,
        Collection<String> resultClasses) {

        logComment(builder, DELIMITER_TAG);

        logValue(builder, QUERY_TAG, title);
        logValue(builder, QUERY_TIMESTAMP_TAG, timeStamp);

        logValue(builder, QUERY_POLICIES_TAG, policiesText(policies));
        logValue(builder, QUERY_TYPE_TAG, type);

        if ( specificClasses != null ) {
            for ( String specificClass : specificClasses ) {
                logSubValue(builder, QUERY_SPECIFIC_TAG, specificClass);
            }
        }
        logValue(builder, QUERY_ANNOTATION_TAG, annotationClass);
        for ( String resultClass : resultClasses ) {
            logSubValue(builder, QUERY_RESULT_TAG, resultClass);
        }
    }

    @Trivial
    public static void logQuery(
        StringBuilder builder,
        String title,
        Collection<String> sources, String type,
        Collection<String> specificClasses, String annotationClass,
        Collection<String> resultClasses) {

        logQuery(builder,
            title, getTimeStamp(),
            sources, type,
            specificClasses, annotationClass,
            resultClasses);
    }

    @Trivial
    public static void logQuery(
        StringBuilder builder,
        String title, String timeStamp,
        Collection<String> sources, String type,
        Collection<String> specificClasses, String annotationClass,
        Collection<String> resultClasses) {

        logComment(builder, DELIMITER_TAG);

        logValue(builder, QUERY_TAG, title);
        logValue(builder, QUERY_TIMESTAMP_TAG, timeStamp);

        for ( String source : sources ) {
            logValue(builder, QUERY_SOURCE_TAG, source);
        }
        logValue(builder, QUERY_TYPE_TAG, type);

        if ( specificClasses != null ) {
            for ( String specificClass : specificClasses ) {
                logSubValue(builder, QUERY_SPECIFIC_TAG, specificClass);
            }
        }
        logValue(builder, QUERY_ANNOTATION_TAG, annotationClass);
        for ( String resultClass : resultClasses ) {
            logSubValue(builder, QUERY_RESULT_TAG, resultClass);
        }
    }

    @Trivial
    public static void logQuery(
        StringBuilder builder,
        String title,
        int policies, Collection<String> sources, String type,
        Collection<String> specificClasses, String annotationClass,
        Collection<String> resultClasses) {

        logQuery(builder,
            title, getTimeStamp(),
            policies, sources, type,
            specificClasses, annotationClass,
            resultClasses);
    }

    @Trivial
    public static void logQuery(
        StringBuilder builder,
        String title, String timeStamp,
        int policies, Collection<String> sources, String type,
        Collection<String> specificClasses, String annotationClass,
        Collection<String> resultClasses) {

        logComment(builder, DELIMITER_TAG);

        logValue(builder, QUERY_TAG, title);
        logValue(builder, QUERY_TIMESTAMP_TAG, timeStamp);

        logValue(builder, QUERY_POLICIES_TAG, policiesText(policies));
        for ( String source : sources ) {
            logValue(builder, QUERY_SOURCE_TAG, source);
        }
        logValue(builder, QUERY_TYPE_TAG, type);

        if ( specificClasses != null ) {
            for ( String specificClass : specificClasses ) {
                logSubValue(builder, QUERY_SPECIFIC_TAG, specificClass);
            }
        }
        logValue(builder, QUERY_ANNOTATION_TAG, annotationClass);
        for ( String resultClass : resultClasses ) {
            logSubValue(builder, QUERY_RESULT_TAG, resultClass);
        }
    }
}
