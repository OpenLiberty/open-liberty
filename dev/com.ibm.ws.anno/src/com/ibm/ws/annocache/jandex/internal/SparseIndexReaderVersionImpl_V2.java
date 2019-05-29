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
package com.ibm.ws.annocache.jandex.internal;

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

    protected enum AnnoTarget {
        NULL((byte) 0),
        FIELD((byte) 1),
        METHOD((byte) 2),
        METHOD_PARAMATER((byte) 3),
        CLASS((byte) 4),
        EMPTY_TYPE((byte) 5),
        CLASS_EXTENDS_TYPE((byte) 6),
        TYPE_PARAMETER((byte) 7),
        TYPE_PARAMETER_BOUND((byte) 8),
        METHOD_PARAMETER_TYPE((byte) 9),
        THROWS_TYPE((byte) 10);

        private AnnoTarget(byte tag) {
            this.tag = tag;
        }

        public final byte tag;

        public byte getTag() {
            return tag;
        }
        
        public static AnnoTarget select(byte tag) {
            for ( AnnoTarget target : AnnoTarget.values() ) {
                if ( target.tag == tag ) {
                    return target;
                }
            }
            return null;
        }
    }

    // Annotation value tags.

    protected enum AnnoValue {
        BYTE(1),
        SHORT(2),
        INT(3),
        CHAR(4),
        FLOAT(5),
        DOUBLE(6),
        LONG(7),
        BOOLEAN(8),
        STRING(9),
        CLASS(10),
        ENUM(11),
        ARRAY(12),
        NESTED(13);

        private AnnoValue(int tag) {
            this.tag = tag;
        }

        public final int tag;

        public int getTag() {
            return tag;
        }

        public static AnnoValue select(int tag) {
            for ( AnnoValue value : AnnoValue.values() ) {
                if ( value.tag == tag ) {
                    return value;
                }
            }
            return null;
        }        
    }
    
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
        // System.out.println("Byte table [ " + numEntries + " ]");
        byte[][] useByteTable = new byte[numEntries][];

        // Element 0 is forced to null.  Note that that allows
        // byte[] references to obtain null as the referenced value.

        useByteTable[0] = null;

        for ( int entryNo = 1; entryNo < numEntries; entryNo++ ) {
            int numBytes = input.readPackedU32();
            useByteTable[entryNo] = new byte[numBytes];
            input.readFully(useByteTable[entryNo], 0, numBytes);
            // System.out.println("  Byte entry [ " + entryNo + " ] [ " + numBytes + " ]");
        }

        this.byteTable = useByteTable;
        this.simpleNameTable = new SparseDotName[useByteTable.length];
    }

    private void readStringTable() throws IOException {
        int numEntries = input.readPackedU32() + 1;
        // System.out.println("String table [ " + numEntries + " ]");
        String[] useStringTable = new String[numEntries];

        // Element 0 is forced to null.  Note that that allows
        // string references to obtain null as the referenced value.

        useStringTable[0] = null;

        for ( int entryNo = 1; entryNo < numEntries; entryNo++ ) {
            useStringTable[entryNo] = input.readUTF();
            // System.out.println("  String entry [ " + entryNo + " ] [ " + useStringTable[entryNo] + " ]");            
        }

        this.stringTable = useStringTable;
    }

    private void readNameTable() throws IOException {
        int numEntries = input.readPackedU32() + 1;
        // System.out.println("DotName table [ " + numEntries + " ]");
        SparseDotName[] useNameTable = new SparseDotName[numEntries];

        // Element 0 is forced to null.  Note that that allows
        // name references to obtain null as the referenced value.

        useNameTable[0] = null;

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
            useNameTable[nameNo] = name;

            lastName = name; 
            lastDepth = depth;

            // System.out.println("  String entry [ " + nameNo + " ] [ " + name + " ]");
        }

        this.nameTable = useNameTable;
    }

    // Part 1.5: Reading annotations ...

    private void readAnnotations() throws IOException {
        int numAnno = input.readPackedU32();
        // System.out.println("Annotations [ " + numAnno + " ]");
        if ( numAnno == 0 ) {
            return;
        }

        for ( int annoNo = 0; annoNo < numAnno; annoNo++ ) {
            @SuppressWarnings("unused")
            int annoOffset = readAnnotation();
        }
    }

//        System.out.println(
//                "E [ " + targetName + " ]" +
//               " A [ " + annoClassName + " ]" +
//                " T [ " + Integer.toHexString(annoTypeTable[annoOffset]) + " ]" +
//               " ET [ " + Integer.toHexString(targetType) + " ]");
//    }

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
        // System.out.println("Class annotations [ " + numAnno + " ]");
        if ( numAnno == 0 ) {
            return false;
        }

//        boolean doLog;
//        if ( targetName.toString().equals("com.ibm.ws.annocache.info.internal.FieldInfoImpl") ) {
//            System.out.println("Processing [ FieldInfoImpl ]");
//            doLog = true;
//        } else {
//            doLog = false;
//        }

        boolean didAdd = false;

        for ( int annotationNo = 0; annotationNo < numAnno; annotationNo++ ) {
            int annoOffset = readAnnotation();

//            if ( doLog ) {
//                 System.out.println(
//                     "T [ " + targetName + " ]" +
//                     " A [ " + annoClassNameTable[annoOffset] + " ]" +
//                     " [ " + Integer.toHexString(annoTypeTable[annoOffset]) + " ]");
//            }

            SparseDotName annoClassName = annoClassNameTable[annoOffset];
            // System.out.println("Anno [ " + annotationNo + " ] [ " + annoClassName + " ]");

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
        return ( targetType == SparseIndexReaderVersionImpl_V2.AnnoTarget.CLASS.tag );
    }

    private boolean selectType(byte targetType) {
        return ( (targetType == SparseIndexReaderVersionImpl_V2.AnnoTarget.CLASS.tag) ||
                 (targetType == SparseIndexReaderVersionImpl_V2.AnnoTarget.FIELD.tag) ||
                 (targetType == SparseIndexReaderVersionImpl_V2.AnnoTarget.METHOD.tag) );
    }

    private int readAnnotation() throws IOException {
        int annoOffset = input.readPackedU32();

        SparseDotName annoClassName = annoClassNameTable[annoOffset];
        if ( annoClassName != null ) {
            // System.out.println("Annotation class name [ " + annoClassName + " ] Offset [ " + annoOffset + " ]");
            return annoOffset;
        }

        annoClassName = nameTable[input.readPackedU32()];
        // System.out.println("Annotation class name [ " + annoClassName + " ]");

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

        // System.out.println(
        //     "Annotation [ " + annoOffset + " ]" +
        //     " [ " + annoClassName + " ]" +
        //     " [ " + AnnoTarget.select(targetType) + " ]");

        return annoOffset;
    }

    // private byte tag;
    // private byte lastTag;

    private byte readAnnotationTarget() throws IOException {
        // lastTag = tag;

        byte tag = input.readByte();
        // System.out.println("Anno Target Tag [ " + tag + " ]");

        int seekCount;
        if ( tag == AnnoTarget.NULL.tag ) {
            seekCount = 0;
        } else if ( (tag == AnnoTarget.CLASS.tag) || 
                    (tag == AnnoTarget.FIELD.tag) ||
                    (tag == AnnoTarget.METHOD.tag) ) {
            seekCount = 0;
        } else if ( tag == AnnoTarget.METHOD_PARAMATER.tag ) {
            seekCount = 1;
        } else if ( tag == AnnoTarget.EMPTY_TYPE.tag ) {
            seekCount = 2;
        } else if ( tag == AnnoTarget.CLASS_EXTENDS_TYPE.tag ) {
            seekCount = 2;
        } else if ( tag == AnnoTarget.TYPE_PARAMETER.tag ) {
            seekCount = 2;
        } else if ( tag == AnnoTarget.TYPE_PARAMETER_BOUND.tag ) {
            seekCount = 3;
        } else if ( tag == AnnoTarget.METHOD_PARAMETER_TYPE.tag ) {
            seekCount = 2;
        } else if ( tag == AnnoTarget.THROWS_TYPE.tag ) {
            seekCount = 2;
        } else {
            // throw new IllegalStateException("Invalid tag: " + tag + " prior tag " + lastTag);
            throw new IllegalStateException("Invalid tag: " + tag);
        }

        if ( seekCount == 3 ) {
            input.seekPackedU32();
            input.seekPackedU32();
            input.seekPackedU32();
        } else if ( seekCount == 2 ) {
            input.seekPackedU32();
            input.seekPackedU32();
        } else if ( seekCount == 1 ) {
            input.seekPackedU32();
        }

        // System.out.println("Anno Target [ " + AnnoTarget.select(tag) + " ]");

        return tag;
    }

    private void movePastAnnotationValues() throws IOException {
        int numValues = input.readPackedU32();
        // System.out.println("Skip values [ " + numValues + " ]");

        for ( int valueNo = 0; valueNo < numValues; valueNo++ ) {
            input.seekPackedU32();

            int valueType = input.readByte();

            if (valueType == AnnoValue.BYTE.tag) {
                input.skipBytes(1);
                // input.readByte();
            } else if (valueType == AnnoValue.SHORT.tag) {
                input.seekPackedU32();
            } else if (valueType == AnnoValue.INT.tag) {
                input.seekPackedU32();
            } else if (valueType == AnnoValue.CHAR.tag) {
                input.seekPackedU32();
            } else if (valueType == AnnoValue.FLOAT.tag) {
                input.skipBytes(4);
                // input.readFloat();
            } else if (valueType == AnnoValue.DOUBLE.tag) {
                input.skipBytes(8);
                // input.readDouble();
            } else if (valueType == AnnoValue.LONG.tag) {
                input.skipBytes(8);
                // input.readLong();
            } else if (valueType == AnnoValue.BOOLEAN.tag) {
                input.skipBytes(1);
                // input.readBoolean();
            } else if (valueType == AnnoValue.STRING.tag) {
                input.seekPackedU32();
            } else if (valueType == AnnoValue.CLASS.tag) {
                input.seekPackedU32();
            } else if (valueType == AnnoValue.ENUM.tag) {
                input.seekPackedU32();
                input.seekPackedU32();
            } else if (valueType == AnnoValue.ARRAY.tag) {
                movePastAnnotationValues();
            } else if (valueType == AnnoValue.NESTED.tag) {
                @SuppressWarnings("unused")
                int annoOffset = readAnnotation();
            } else {
                throw new IllegalStateException("Invalid annotation value tag:" + valueType);
            }

            // System.out.println("Value [ " + AnnoValue.select(valueType) + " ]");
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
        int kind = input.readUnsignedByte();

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
        SparseDotName[][] useTypeListTable = this.typeListTable;

        // Element 0 is forced to null.  Note that that allows
        // type list references to obtain null as the referenced value.

        useTypeListTable[0] = null;

        // Some of the type lists are read while
        // reading types (see 'readTypeListReference'.
        //
        // Read any which were not yet read.

        for ( int entryNo = findNextNull(useTypeListTable, 1);
              entryNo < useTypeListTable.length;
              entryNo = findNextNull(useTypeListTable, entryNo)) {

            useTypeListTable[entryNo] = readTypeList();
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
        SparseAnnotationHolder[] useMethodTable = new SparseAnnotationHolder[numMethods];

        // Element 0 is forced to null.  Note that that allows
        // method references to obtain null as the referenced value.

        useMethodTable[0] = null;

        for ( int methodNo = 1; methodNo < numMethods; methodNo++ ) {
            useMethodTable[methodNo] = readMethod();
        }

        this.methodTable = useMethodTable;
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
        SparseAnnotationHolder[] useFieldTable = new SparseAnnotationHolder[numFields];

        // Element 0 is forced to null.  Note that that allows
        // field references to obtain null as the referenced value.
        useFieldTable[0] = null;

        for ( int fieldNo = 1; fieldNo < numFields; fieldNo++ ) {
            useFieldTable[fieldNo] = readField();
        }

        this.fieldTable = useFieldTable;
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
        // System.out.println("Class table [ " + numClasses + " ]");
        Map<SparseDotName, SparseClassInfo> classes =
            new HashMap<SparseDotName, SparseClassInfo>(numClasses);

        for ( int classNo = 0; classNo < numClasses; classNo++ ) {
            SparseClassInfo classInfo = readClass();
            // System.out.println("Class [ " + classNo + " ] [ " + classInfo.name() + " ]");
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
        // System.out.println("Next class name [ " + className + " ]");

        short flags = (short) input.readPackedU32();
        SparseDotName superClassName = typeTable[input.readPackedU32()];

        input.seekPackedU32(); // Skip unused class data.

        SparseDotName[] interfaceNames = typeListTable[input.readPackedU32()];

        SparseClassInfo classInfo = new SparseClassInfo(className, superClassName, flags, interfaceNames);

        input.seekPackedU32(); // Skip unused class data.
        input.seekPackedU32();
        readPastEnclosingMethod();

        int numAnnotations = input.readPackedU32();
        // System.out.println("Next class annotations [ " + numAnnotations + " ]");

        readFields(classInfo);
        readMethods(classInfo);

        for ( int annoNo = 0; annoNo < numAnnotations; annoNo++ ) {
            if ( readClassAnnotations(className, classAnnoClassNames) ) {
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
        // System.out.println("Next fields [ " + numFields + " ]");
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
        // System.out.println("Next methods [ " + numMethods + " ]");
        if ( numMethods == 0 ) {
            return;
        }

        int[] methodOffsets = getOffsets(numMethods);

        // Do an extra loop to count the method annotations.
        // That requires the allocation of an offsets array,
        // but prevents us from reallocating the method annotations
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
 