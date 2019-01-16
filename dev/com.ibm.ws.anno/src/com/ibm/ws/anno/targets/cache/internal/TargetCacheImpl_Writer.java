/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2014, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.targets.cache.internal;

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

import org.objectweb.asm.Opcodes;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.util.internal.UtilImpl_Utils;
import com.ibm.ws.anno.targets.internal.TargetsTableClassesImpl;
import com.ibm.ws.anno.targets.internal.TargetsTableClassesMultiImpl;
import com.ibm.ws.anno.targets.internal.TargetsTableContainersImpl;
import com.ibm.ws.anno.targets.internal.TargetsTableDetailsImpl;
import com.ibm.ws.anno.targets.internal.TargetsTableTimeStampImpl;
import com.ibm.ws.anno.targets.internal.TargetsTableAnnotationsImpl;
import com.ibm.ws.anno.util.internal.UtilImpl_BidirectionalMap;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.targets.cache.TargetCache_ExternalConstants;
import com.ibm.wsspi.anno.targets.cache.TargetCache_InternalConstants;

public class TargetCacheImpl_Writer implements TargetCache_InternalConstants {
    // private static final String CLASS_NAME = TargetCacheImpl_Writer.class.getSimpleName();

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

    @Trivial
    protected void closeWriter(String methodName) throws IOException {
        bufferedWriter.close(); // throws IOException
    }

    protected void flushWriter() throws IOException {
        bufferedWriter.flush(); // throws IOException
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

    public void writeResolvedRefs(Collection<String> resolvedClassNames) throws IOException {
        writeHeader(RESOLVED_REFS_TAG, RESOLVED_REFS_VERSION);

        writeComment(DELIMITER_TAG);
        writeComment(RESOLVED_REFS_SECTION);

        for ( String className : resolvedClassNames ) {
            writeValue(CLASS_TAG, className);
        }

        writeTrailer();
    }

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

        writeValue(NAME_TAG, stampTable.getName());
        writeValue(STAMP_TAG, stampTable.getStamp());

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
        
        private OpcodeTag(int opcode, String tag) {
            this.opcode = opcode;
            this.tag = tag;
        }
        
        private final int opcode;
        private final String tag;
        
        public int getOpcode() {
            return opcode;
        }
        
        public String getTag() {
            return tag;
        }

        //

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

                writeClass(classSourceName, i_className, i_superClassName, i_interfaces);
            
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
                              String superClassName, String[] interfaceNames)
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

    public void write(TargetsTableDetailsImpl detailTable) throws IOException {
        writeHeader(DETAILS_TABLE_TAG, DETAILS_TABLE_VERSION);

        writePackageDetail(detailTable);
        writeClassDetail(detailTable);

        writeTrailer();
    }

    protected void writePackageDetail(TargetsTableDetailsImpl detailTable) throws IOException {
        writeComment(DELIMITER_TAG);
        writeComment(PACKAGE_DETAILS_SECTION);

        Map<String, Map<String, String>> detailMap = detailTable.i_getPackageDetails();

        for ( Map.Entry<String, Map<String, String>> detailEntry : detailMap.entrySet() ) {
            String i_packageName = detailEntry.getKey();
            Map<String, String> i_packageAnnotations = detailEntry.getValue();

            writeValue(PACKAGE_TAG, i_packageName);

            for ( Map.Entry<String, String> i_annotationEntry : i_packageAnnotations.entrySet() ) {
                String i_annotationClassName = i_annotationEntry.getKey();
                String annotationDetail = i_annotationEntry.getValue();

                writeSubValue(ANNOTATION_TAG, i_annotationClassName);
                writeSubValue(DETAIL_TAG, annotationDetail);
            }
        }
    }

    protected void writeClassDetail(TargetsTableDetailsImpl detailTable) throws IOException {
        writeComment(DELIMITER_TAG);
        writeComment(CLASS_DETAILS_SECTION);

        Map<String, String> i_annotatedClassNames = new IdentityHashMap<String, String>();

        Map<String, Map<String, String>> i_classDetails = detailTable.i_getClassDetails();
        Map<String, Map<String, Map<String, String>>> i_classFieldDetails = detailTable.i_getFieldDetails();
        Map<String, Map<String, Map<String, String>>> i_classMethodDetails = detailTable.i_getMethodDetails();

        for ( String i_className : i_classDetails.keySet() ) {
            i_annotatedClassNames.put(i_className, i_className);
        }
        for ( String i_className : i_classFieldDetails.keySet() ) {
            i_annotatedClassNames.put(i_className, i_className);
        }
        for ( String i_className : i_classMethodDetails.keySet() ) {
            i_annotatedClassNames.put(i_className, i_className);
        }

        for ( String i_className : i_annotatedClassNames.keySet() ) {
            writeValue(CLASS_TAG, i_className);

            Map<String, String> i_classAnnotations = i_classDetails.get(i_className);
            if ( i_classAnnotations != null ) {
                for ( Map.Entry<String, String> i_annotationEntry : i_classAnnotations.entrySet() ) {
                    String i_annotationClassName = i_annotationEntry.getKey();
                    String annotationDetail = i_annotationEntry.getValue();

                    writeSubValue(ANNOTATION_TAG, i_annotationClassName);
                    writeSubValue(DETAIL_TAG, annotationDetail);
                }
            }

            Map<String, Map<String, String>> i_classFieldAnnotations = i_classFieldDetails.get(i_className);
            if ( i_classFieldAnnotations != null ) {
                for ( Map.Entry<String, Map<String, String>> i_fieldDetails : i_classFieldAnnotations.entrySet() ) {
                    String i_fieldName = i_fieldDetails.getKey();
                    Map<String, String> i_fieldAnnotations = i_fieldDetails.getValue();

                    writeSubValue(FIELD_TAG, i_fieldName);

                    for ( Map.Entry<String, String> i_annotationEntry : i_fieldAnnotations.entrySet() ) {
                        String i_annotationClassName = i_annotationEntry.getKey();
                        String annotationDetail = i_annotationEntry.getValue();

                        writeSubSubValue(ANNOTATION_TAG, i_annotationClassName);
                        writeSubSubValue(DETAIL_TAG, annotationDetail);
                    }
                }
            }

            Map<String, Map<String, String>> i_classMethodAnnotations = i_classMethodDetails.get(i_className);
            if ( i_classMethodAnnotations != null ) {
                for ( Map.Entry<String, Map<String, String>> i_methodDetails : i_classMethodAnnotations.entrySet() ) {
                    String i_methodSignature = i_methodDetails.getKey();
                    Map<String, String> i_methodAnnotations = i_methodDetails.getValue();

                    writeSubValue(METHOD_TAG, i_methodSignature);

                    for ( Map.Entry<String, String> i_annotationEntry : i_methodAnnotations.entrySet() ) {
                        String i_annotationClassName = i_annotationEntry.getKey();
                        String annotationDetail = i_annotationEntry.getValue();

                        writeSubSubValue(ANNOTATION_TAG, i_annotationClassName);
                        writeSubSubValue(DETAIL_TAG, annotationDetail);
                    }
                }
            }
        }
    }

    //

    protected void writeHeader(String tableTag, String tableVersion) throws IOException {
        writeComment(DELIMITER_TAG);

        writeValue(ENCODING_TAG, getEncoding());
        writeValue(TABLE_TAG, tableTag);
        writeValue(VERSION_TAG, tableVersion);

        writeValue(TIMESTAMP_TAG, getTimeStamp());
    }

    protected void writeTrailer() throws IOException {
        String methodName = "writeTrailer";

        writeComment(DELIMITER_TAG);

        closeWriter(methodName);
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

    public String getTimeStamp() {
        return UtilImpl_Utils.getDateAndTime();
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
        String title,
        int policies, String type,
        Collection<String> specificClasses, String annotationClass,
        Collection<String> resultClasses) throws IOException {

        writeQuery(title, getTimeStamp(), policies, type, specificClasses, annotationClass, resultClasses);
        // 'writeQuery' throws IOException
    }

    public void writeQuery(
        String title, String timeStamp,
        int policies, String type,
        Collection<String> specificClasses, String annotationClass,
        Collection<String> resultClasses) throws IOException {

        writeComment(DELIMITER_TAG);

        writeValue(QUERY_TAG, title);
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

        flushWriter();
    }

    public void writeQuery(
        String title,
        Collection<String> sources, String type,
        Collection<String> specificClasses, String annotationClass,
        Collection<String> resultClasses) throws IOException {

        writeQuery(title, getTimeStamp(), sources, type, specificClasses, annotationClass, resultClasses);
        // 'writeQuery' throws IOException
    }

    public void writeQuery(
        String title, String timeStamp,
        Collection<String> sources, String type,
        Collection<String> specificClasses, String annotationClass,
        Collection<String> resultClasses) throws IOException {

        writeComment(DELIMITER_TAG);

        writeValue(QUERY_TAG, title);
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

        flushWriter();
    }

    public void writeQuery(
        String title,
        int policies, Collection<String> sources, String type,
        Collection<String> specificClasses, String annotationClass,
        Collection<String> resultClasses) throws IOException {

        writeQuery(title, getTimeStamp(),
                   policies, sources, type,
                   specificClasses, annotationClass,
                   resultClasses);
        // 'writeQuery' throws IOException
    }

    public void writeQuery(
        String title, String timeStamp,
        int policies, Collection<String> sources, String type,
        Collection<String> specificClasses, String annotationClass,
        Collection<String> resultClasses) throws IOException {

        writeComment(DELIMITER_TAG);

        writeValue(QUERY_TAG, title);
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

        flushWriter();
    }

    protected String policiesText(int policies) {
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
            StringBuffer result = new StringBuffer();

            for ( ScanPolicy policy : ScanPolicy.values() ) {
                if ( (policies & policy.getValue()) == 0 ) {
                    continue;
                }
                if ( result.length() > 0 ) {
                    result.append(',');
                    result.append(' ');
                }
                result.append( policy.toString() );
            }
            return result.toString();
        }
    }


}
