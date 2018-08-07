/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * All modifications made by IBM from initial source -
 * https://github.com/wildfly/jandex/blob/master/src/main/java/org/jboss/jandex/IndexReaderV2.java
 * commit - 36c2b049b7858205c6504308a5e162a4e943ff21
 */
package com.ibm.ws.anno.jandex.internal;

import java.io.IOException;
import java.util.Map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SparseIndexReaderVersionImpl_V2 implements SparseIndexReaderVersion {
    // Handled range of index versions.

    public static final int MIN_VERSION = 6;
    public static final int MAX_VERSION = 6;

    public static boolean accept(int version) {
        return ( (version >= SparseIndexReaderVersionImpl_V2.MIN_VERSION) && 
                 (version <= SparseIndexReaderVersionImpl_V2.MAX_VERSION) );
    }

    // Annotation target type tags:

    protected static final byte NULL_TARGET_TAG = 0;
    protected static final byte FIELD_TAG = 1;
    protected static final byte METHOD_TAG = 2;
    protected static final byte METHOD_PARAMATER_TAG = 3;
    protected static final byte CLASS_TAG = 4;
    protected static final byte EMPTY_TYPE_TAG = 5;
    protected static final byte CLASS_EXTENDS_TYPE_TAG = 6;
    protected static final byte TYPE_PARAMETER_TAG = 7;
    protected static final byte TYPE_PARAMETER_BOUND_TAG = 8;
    protected static final byte METHOD_PARAMETER_TYPE_TAG = 9;
    protected static final byte THROWS_TYPE_TAG = 10;

    // Annotation value tags.

    private static final int AVALUE_BYTE = 1;
    private static final int AVALUE_SHORT = 2;
    private static final int AVALUE_INT = 3;
    private static final int AVALUE_CHAR = 4;
    private static final int AVALUE_FLOAT = 5;
    private static final int AVALUE_DOUBLE = 6;
    private static final int AVALUE_LONG = 7;
    private static final int AVALUE_BOOLEAN = 8;
    private static final int AVALUE_STRING = 9;
    private static final int AVALUE_CLASS = 10;
    private static final int AVALUE_ENUM = 11;
    private static final int AVALUE_ARRAY = 12;
    private static final int AVALUE_NESTED = 13;
    private static final int HAS_ENCLOSING_METHOD = 1;

    //

    protected SparseIndexReaderVersionImpl_V2(PackedDataInputStream input, int version) {
        this.input = input;
        this.version = version;

        // Set this to an expected maximum number of class annotations.
        // The list will be enlarged if a class has more than the preset
        // allocation.

        this.classAnnoClassNames = new ArrayList<SparseDotName>(20);
    }

    //

    private final PackedDataInputStream input;

    //

    private final int version;

    @Override
    public int getVersion() {
        return version;
    }

    // Constants ...

    private byte[][] byteTable;
    private SparseDotName[] simpleNameTable; // Avoid re-recreating byte encoded simple names.

    private SparseDotName getSimpleName(int nameNo) {
        SparseDotName simpleName = simpleNameTable[nameNo];
        if ( simpleName == null ) {
            byte[] nameBytes = byteTable[nameNo];
            simpleName = SparseDotName.createSimple(nameBytes);
            simpleNameTable[nameNo] = simpleName;
        }
        return simpleName;
    }

    private String[] stringTable;
    private SparseDotName[] nameTable;

    // Annotations ...

    private SparseDotName[] annoClassNameTable;
    private byte[] annoTypeTable;

    // Types and type lists ...

    private SparseDotName[] typeTable;
    private SparseDotName[][] typeListTable;

    // Methods, fields, and classes ...

    private SparseAnnotationHolder[] methodTable;
    private SparseAnnotationHolder[] fieldTable;
    private Map<SparseDotName, SparseClassInfo> classTable;

    //

    @Override
    public SparseIndex read() throws IOException {
        input.seekPackedU32(); // annotationSize
        input.seekPackedU32(); // implementorSize
        input.seekPackedU32(); // subclassesSize

        readByteTable();
        readStringTable();
        readNameTable();

        typeTable = new SparseDotName[input.readPackedU32() + 1];
        typeListTable = new SparseDotName[input.readPackedU32() + 1][];
        annoClassNameTable = new SparseDotName[input.readPackedU32() + 1];
        annoTypeTable  = new byte[ annoClassNameTable.length ];

        readTypeTable();
        readTypeListTable();

        readMethodTable();
        readFieldTable();

        classTable = readClassTable();

        return new SparseIndex(classTable);
    }

    // Part 1: Tables of constant values:

    private void readByteTable() throws IOException {
        int numEntries = input.readPackedU32() + 1;
        byte[][] byteTable = new byte[numEntries][];

        // Element 0 is forced to null.  Note that that allows
        // byte[] references to obtain null as the referenced value.

        byteTable[0] = null;

        for ( int entryNo = 1; entryNo < numEntries; entryNo++ ) {
            int numBytes = input.readPackedU32();
            byteTable[entryNo] = new byte[numBytes];
            input.readFully(byteTable[entryNo], 0, numBytes);
        }

        this.byteTable = byteTable;
        this.simpleNameTable = new SparseDotName[byteTable.length];
    }

    private void readStringTable() throws IOException {
        int numEntries = input.readPackedU32() + 1;
        String[] stringTable = new String[numEntries];

        // Element 0 is forced to null.  Note that that allows
        // string references to obtain null as the referenced value.

        stringTable[0] = null;

        for ( int entryNo = 1; entryNo < numEntries; entryNo++ ) {
            stringTable[entryNo] = input.readUTF();
        }

        this.stringTable = stringTable;
    }

    private void readNameTable() throws IOException {
        int numEntries = input.readPackedU32() + 1;

        SparseDotName[] nameTable = new SparseDotName[numEntries];

        // Element 0 is forced to null.  Note that that allows
        // name references to obtain null as the referenced value.

        nameTable[0] = null;

        int lastDepth = -1;
        SparseDotName lastName = null;

        for ( int nameNo = 1; nameNo < numEntries; nameNo++ ) {
            int packedDepth = input.readPackedU32();
            String tail = stringTable[input.readPackedU32()];

            // [packedDepth] == [depthBits][innerClassBit]
            boolean isInnerClass = ((packedDepth & 0x01) == 0x01);
            int depth = packedDepth >> 1;

            // a,      a, 0 [head == null]     [(a)]
            // a.b,    b, 1 [head == (a)]      [((a),b)]
            // a.b.c,  c, 2 [head == ((a),b)]  [(((a),(b)),c)]
            // a.b.d,  d, 2 [head == ((a),b)]  [(((a),(b)),d)]
            // b       b, 0 [head == null]     [(b)]

            SparseDotName head = lastName;
            if ( lastDepth >= depth ) {
                while ( lastDepth-- >= depth ) {
                    head = head.prefix();
                }
            }

            SparseDotName name = new SparseDotName(head, tail, !SparseDotName.SIMPLE, isInnerClass);
            nameTable[nameNo] = name;

            lastName = name; 
            lastDepth = depth;
        }

        this.nameTable = nameTable;
    }

    // Part 1.5: Reading annotations ...

    private void readAnnotations() throws IOException {
        int numAnno = input.readPackedU32();
        if ( numAnno == 0 ) {
            return;
        }

        for ( int annoNo = 0; annoNo < numAnno; annoNo++ ) {
        	@SuppressWarnings("unused")
			int annoOffset = readAnnotation();
        }
    }

//   	 System.out.println(
//        		"E [ " + targetName + " ]" +
//               " A [ " + annoClassName + " ]" +
//        		" T [ " + Integer.toHexString(annoTypeTable[annoOffset]) + " ]" +
//               " ET [ " + Integer.toHexString(targetType) + " ]");
//	}

    private SparseDotName[] readElementAnnotations(SparseDotName targetName) throws IOException {
        int numAnno = input.readPackedU32();

        if ( numAnno == 0 ) {
            return SparseDotName.EMPTY_ARRAY;
        }

        SparseDotName[] annoClassNames = new SparseDotName[numAnno];
        int numPlaceholders = 0;

        for ( int annoNo = 0; annoNo < numAnno; annoNo++ ) {
            int annoOffset = readAnnotation();
            SparseDotName annoClassName = annoClassNameTable[annoOffset]; 

            if ( annoClassName.isPlaceholder() ) {
                numPlaceholders++;
            } else {
                annoClassNames[annoNo - numPlaceholders] = annoClassName;
            }
        }

        if ( numPlaceholders != 0 ) {
            if ( numPlaceholders == numAnno ) {
                return SparseDotName.EMPTY_ARRAY;
            } else {
                return Arrays.copyOf(annoClassNames, numAnno - numPlaceholders);
            }
        } else {
            return annoClassNames;
        }
    }

    private boolean readClassAnnotations(SparseDotName targetName, List<SparseDotName> annoClassNames) throws IOException {
        int numAnno = input.readPackedU32();
        if ( numAnno == 0 ) {
            return false;
        }

//        boolean doLog;
//        if ( targetName.toString().equals("com.ibm.ws.anno.info.internal.FieldInfoImpl") ) {
//        	System.out.println("Processing [ FieldInfoImpl ]");
//        	doLog = true;
//        } else {
//        	doLog = false;
//        }

        boolean didAdd = false;

        for ( int annotationNo = 0; annotationNo < numAnno; annotationNo++ ) {
            int annoOffset = readAnnotation();

//            if ( doLog ) {
//            	 System.out.println(
//            	 	"T [ " + targetName + " ]" +
//                     " A [ " + annoClassNameTable[annoOffset] + " ]" +
//            	 	" [ " + Integer.toHexString(annoTypeTable[annoOffset]) + " ]");
//            }

            SparseDotName annoClassName = annoClassNameTable[annoOffset]; 
            if ( annoClassName.isPlaceholder() ) {
            	continue;
            }

            byte annoType = annoTypeTable[annoOffset];
            if ( !isClass(annoType) ) {
            	continue;
            }

            annoClassNames.add(annoClassName);
            didAdd = true;
        }

        return didAdd;
    }

    private boolean isClass(byte targetType) {
    	return ( targetType == SparseIndexReaderVersionImpl_V2.CLASS_TAG );
    }

    private boolean selectType(byte targetType) {
        return ( (targetType == SparseIndexReaderVersionImpl_V2.CLASS_TAG) ||
                 (targetType == SparseIndexReaderVersionImpl_V2.FIELD_TAG) ||
                 (targetType == SparseIndexReaderVersionImpl_V2.METHOD_TAG) );
    }

    private int readAnnotation() throws IOException {
        int annoOffset = input.readPackedU32();

        SparseDotName annoClassName = annoClassNameTable[annoOffset];
        if ( annoClassName != null ) {
        	return annoOffset;
        }

        annoClassName = nameTable[input.readPackedU32()];
        byte targetType = readAnnotationTarget();
        movePastAnnotationValues();

        // The liberty annotations code cares only about class, field, and method annotations.
        // Jandex places method parameter and method parameter type annotations in the
        // method annotations collection, and distinguishes these by their type and their
        // specific target information.
        //
        // A non-null annotation must be answered, but the consumer must also be informed
        // when the annotation is not one of the types which are of interest.

        if ( !selectType(targetType) ) {
            annoClassName = SparseDotName.PLACEHOLDER;
        }

        annoClassNameTable[annoOffset] = annoClassName;
        annoTypeTable[annoOffset] = targetType;

        return annoOffset;
    }

    private byte readAnnotationTarget() throws IOException {
        byte tag = input.readByte();

        switch(tag){
            case NULL_TARGET_TAG:
                return tag;
            case CLASS_TAG:
            case FIELD_TAG:
            case METHOD_TAG:
                return tag;
            case METHOD_PARAMATER_TAG: {
                input.seekPackedU32();
                return tag;
            }
            case EMPTY_TYPE_TAG: {
                input.seekPackedU32();
                input.seekPackedU32();
                return tag;
            }
            case CLASS_EXTENDS_TYPE_TAG: {
                input.seekPackedU32();
                input.seekPackedU32();
                return tag;
            }
            case TYPE_PARAMETER_TAG: {
                input.seekPackedU32();
                input.seekPackedU32();
                return tag;
            }
            case TYPE_PARAMETER_BOUND_TAG: {
                input.seekPackedU32();
                input.seekPackedU32();
                input.seekPackedU32();
                return tag;
            }
            case METHOD_PARAMETER_TYPE_TAG: {
                input.seekPackedU32();
                input.seekPackedU32();
                return tag;
            }
            case THROWS_TYPE_TAG: {
                input.seekPackedU32();
                input.seekPackedU32();
                return tag;
            }
            default:
                throw new IllegalStateException("Invalid tag: " + tag);
        }
    }

    private void movePastAnnotationValues() throws IOException {
        int numValues = input.readPackedU32();

        for ( int valueNo = 0; valueNo < numValues; valueNo++ ) {
            input.seekPackedU32();

            int valueType = input.readByte();

            switch (valueType) {
                case AVALUE_BYTE:
                    input.skipBytes(1);
                    // input.readByte();
                    break;
                case AVALUE_SHORT:
                    input.seekPackedU32();
                    break;
                case AVALUE_INT:
                    input.seekPackedU32();
                    break;
                case AVALUE_CHAR:
                    input.seekPackedU32();
                    break;
                case AVALUE_FLOAT:
                    input.skipBytes(4);
                    // input.readFloat();
                    break;
                case AVALUE_DOUBLE:
                    input.skipBytes(8);
                    // input.readDouble();
                    break;
                case AVALUE_LONG:
                    input.skipBytes(4);
                    // input.readLong();
                    break;
                case AVALUE_BOOLEAN:
                    input.skipBytes(1);
                    // input.readBoolean();
                    break;
                case AVALUE_STRING:
                    input.seekPackedU32();
                    break;
                case AVALUE_CLASS:
                    input.seekPackedU32();
                    break;
                case AVALUE_ENUM:
                    input.seekPackedU32();
                    input.seekPackedU32();
                    break;
                case AVALUE_ARRAY:
                    movePastAnnotationValues();
                    break;
                case AVALUE_NESTED: {
                	@SuppressWarnings("unused")
					int annoOffset = readAnnotation();
                    break;
                }
                default:
                    throw new IllegalStateException("Invalid annotation value tag:" + valueType);
            }
        }
    }

    // Part 2: Types and type lists ...

    private void readTypeTable() throws IOException {
        // Element 0 is forced to null.  Note that that allows
        // type references to obtain null as the referenced value.

        typeTable[0] = null;

        for ( int entryNo = 1; entryNo < typeTable.length; entryNo++ ) {
            typeTable[entryNo] = movePastReadTypeEntry();
        }
    }

    private SparseDotName movePastReadTypeEntry() throws IOException{
        int kind = (int) input.readUnsignedByte();

        switch (kind) {
            case 0: { //class
                SparseDotName className = nameTable[input.readPackedU32()];
                readAnnotations();
                return className;
            }

            case 1: { //Array
                input.seekPackedU32();
                input.seekPackedU32();
                readAnnotations();
                return SparseDotName.PLACEHOLDER;
            }

            case 2: { //primitive
                input.readUnsignedByte();
                readAnnotations();
                return SparseDotName.PLACEHOLDER;
            }

            default:
            case 3: { //void
                readAnnotations();
                return SparseDotName.PLACEHOLDER;
            }

            case 4: { //type_variable
                input.seekPackedU32();
                readTypeListReference();
                readAnnotations();
                return SparseDotName.PLACEHOLDER;
            }

            case 5: { //unresolved_typevariable
                input.seekPackedU32();
                readAnnotations();
                return SparseDotName.PLACEHOLDER;
            }

            case 6: { //wildcard type
                input.seekPackedU32();
                input.seekPackedU32();
                readAnnotations();
                return SparseDotName.PLACEHOLDER;
            }

            case 7: { //parametrized type
                SparseDotName name = nameTable[input.readPackedU32()];
                input.seekPackedU32();
                readTypeListReference();
                readAnnotations();
                return name;
            }
        }
    }

    private SparseDotName[] readTypeListReference() throws IOException {
        int listOffset = input.readPackedU32();
        SparseDotName[] typeNames = typeListTable[listOffset];

        if ( typeNames != null ) {
            return typeNames;
        }

        return typeListTable[listOffset] = readTypeList();
    }

    private void readTypeListTable() throws IOException {
        SparseDotName[][] typeListTable = this.typeListTable;

        // Element 0 is forced to null.  Note that that allows
        // type list references to obtain null as the referenced value.

        typeListTable[0] = null;

        // Some of the type lists are read while
        // reading types (see 'readTypeListReference'.
        //
        // Read any which were not yet read.

        for ( int entryNo = findNextNull(typeListTable, 1);
              entryNo < typeListTable.length;
              entryNo = findNextNull(typeListTable, entryNo)) {

            typeListTable[entryNo] = readTypeList();
        }
    }

    private int findNextNull(Object[] array, int start) {
        while ( start < array.length ) {
            if ( array[start] == null ) {
                return start;
            }
            start++;
        }
        return array.length;
    }

    private SparseDotName[] readTypeList() throws IOException {
        int numNames = input.readPackedU32();
        if ( numNames == 0 ) {
            return SparseDotName.PLACEHOLDER_ARRAY;
        }

        SparseDotName[] typeNames = new SparseDotName[numNames];

        for ( int nameNo = 0; nameNo < numNames; nameNo++ ) {
            typeNames[nameNo] = typeTable[input.readPackedU32()];
        }

        return typeNames;
    }

    // Part 3: Methods, fields, and classes ...

    private void readMethodTable() throws IOException {
        int numMethods = input.readPackedU32() + 1;
        SparseAnnotationHolder[] methodTable = new SparseAnnotationHolder[numMethods];

        // Element 0 is forced to null.  Note that that allows
        // method references to obtain null as the referenced value.

        methodTable[0] = null;

        for ( int methodNo = 1; methodNo < numMethods; methodNo++ ) {
            methodTable[methodNo] = readMethod();
        }

        this.methodTable = methodTable;
    }

    private SparseAnnotationHolder readMethod() throws IOException {
        SparseDotName methodName = getSimpleName( input.readPackedU32() );

        // System.out.println("Reading method [ " + methodName + " ]");

        input.seekPackedU32(); // Skip unused method data.
        input.seekPackedU32();
        input.seekPackedU32();
        input.seekPackedU32();
        input.seekPackedU32();
        input.seekPackedU32();

        SparseDotName[] methodAnnotations = readElementAnnotations(methodName);

        return new SparseAnnotationHolder(methodName, methodAnnotations);
    }

    private void readFieldTable() throws IOException {
        int numFields = input.readPackedU32() + 1;
        SparseAnnotationHolder[] fieldTable = new SparseAnnotationHolder[numFields];

        // Element 0 is forced to null.  Note that that allows
        // field references to obtain null as the referenced value.
        fieldTable[0] = null;

        for ( int fieldNo = 1; fieldNo < numFields; fieldNo++ ) {
            fieldTable[fieldNo] = readField();
        }

        this.fieldTable = fieldTable;
    }

    private SparseAnnotationHolder readField() throws IOException {
        SparseDotName fieldName = getSimpleName( input.readPackedU32() );

        input.seekPackedU32(); // Skip unused field data.
        input.seekPackedU32();

        SparseDotName[] fieldAnnotations = readElementAnnotations(fieldName);

        return new SparseAnnotationHolder(fieldName, fieldAnnotations);
    }

    //

    private Map<SparseDotName, SparseClassInfo> readClassTable() throws IOException {
        int numClasses = input.readPackedU32();

        Map<SparseDotName, SparseClassInfo> classes = new HashMap<SparseDotName, SparseClassInfo>(numClasses);

        for ( int classNo = 0; classNo < numClasses; classNo++ ) {
            SparseClassInfo classInfo = readClass();
            classes.put(classInfo.name(), classInfo);
        }

        return classes;
    }

    // Buffer used to accumulate annotation class names
    // while reading class annotations.  Shared between loops
    // to avoid re-allocations.

    private final List<SparseDotName> classAnnoClassNames;

    private SparseClassInfo readClass() throws IOException {
        SparseDotName className = nameTable[input.readPackedU32()];
        short flags = (short) input.readPackedU32();
        SparseDotName superClassName = typeTable[input.readPackedU32()];

        input.seekPackedU32(); // Skip unused class data.

        SparseDotName[] interfaceNames = typeListTable[input.readPackedU32()];

        SparseClassInfo classInfo = new SparseClassInfo(className, superClassName, flags, interfaceNames);

        input.seekPackedU32(); // Skip unused class data.
        input.seekPackedU32();
        readPastEnclosingMethod();

        int numAnnotations = input.readPackedU32();

        readFields(classInfo);
        readMethods(classInfo);

        for ( int annoNo = 0; annoNo < numAnnotations; annoNo++ ) {
            if ( readClassAnnotations(classInfo.name(), classAnnoClassNames) ) {
                classInfo.addClassAnnotations(classAnnoClassNames);
                classAnnoClassNames.clear();
            }
        }

        return classInfo;
    }

    private void readPastEnclosingMethod() throws IOException{
        if ( input.readUnsignedByte() == HAS_ENCLOSING_METHOD ) {
            input.seekPackedU32(); //eName
            input.seekPackedU32(); //eClass
            input.seekPackedU32(); //returnType
            input.seekPackedU32(); //parameters
        }
    }

    // Do our best to avoid unnecessary reallocations of the
    // field and method offset arrays.

    private int[] offsets;

    private int[] getOffsets(int size) {
        if ( (offsets == null) || (size > offsets.length) ) {
            offsets = new int[size];
        }
        return offsets;
    }

    private void readFields(SparseClassInfo classInfo) throws IOException {
        int numFields = input.readPackedU32();
        if ( numFields == 0 ) {
            return;
        }

        int[] fieldOffsets = getOffsets(numFields);

        // Do an extra loop to count the field annotations.
        // That requires the allocation of an offsets array,
        // but prevents us from reallocating the field annocations
        // array of the class.
        //
        // The extra loop is very cheap, and we are more concerned with
        // memory use than performance.

        int numFieldAnnos = 0;
        for ( int fieldNo = 0; fieldNo < numFields; fieldNo++ ) {
            int fieldOffset = input.readPackedU32();
            fieldOffsets[fieldNo] = fieldOffset;

            SparseAnnotationHolder fieldAnnoHolder = fieldTable[fieldOffset];
            numFieldAnnos += fieldAnnoHolder.getAnnotations().length;
        }
        classInfo.allocateFieldAnnotations(numFieldAnnos);

        for ( int fieldNo = 0; fieldNo < numFields; fieldNo++ ) {
            int fieldOffset = fieldOffsets[fieldNo];
            SparseAnnotationHolder fieldAnnoHolder = fieldTable[fieldOffset];
            classInfo.addAllocatedFieldAnnotations( fieldAnnoHolder.getAnnotations() );
        }
    }

    private void readMethods(SparseClassInfo classInfo) throws IOException {
        int numMethods = input.readPackedU32();
        if ( numMethods == 0 ) {
            return;
        }

        int[] methodOffsets = getOffsets(numMethods);

        // Do an extra loop to count the method annotations.
        // That requires the allocation of an offsets array,
        // but prevents us from reallocating the method annocations
        // array of the class.
        //
        // The extra loop is very cheap, and we are more concerned with
        // memory use than performance.

        int numMethodAnnos = 0;
        for ( int methodNo = 0; methodNo < numMethods; methodNo++ ) {
            int methodOffset = input.readPackedU32();
            methodOffsets[methodNo] = methodOffset;

            SparseAnnotationHolder methodAnnoHolder = methodTable[methodOffset];
            numMethodAnnos += methodAnnoHolder.getAnnotations().length;
        }
        classInfo.allocateMethodAnnotations(numMethodAnnos);

        for ( int methodNo = 0; methodNo < numMethods; methodNo++ ) {
            int methodOffset = methodOffsets[methodNo];
            SparseAnnotationHolder methodAnnoHolder = methodTable[methodOffset];
            classInfo.addAllocatedMethodAnnotations( methodAnnoHolder.getAnnotations() );
        }
    }
}
 