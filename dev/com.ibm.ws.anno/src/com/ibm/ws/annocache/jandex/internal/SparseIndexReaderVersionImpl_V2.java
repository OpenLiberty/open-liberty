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
 * 
 * Have been merged from the following, which update the Jandex supported version from
 * Jandex 2.0.3.Final to Jandex 2.1.2.Final:
 *
 * "JANDEX-46 Add support for default annotation attribute"
 * [updates version from 6 to 7]
 * wildfly/jandex@c208ff5#diff-a696ceb786df083f5910564662f55bc9
 *
 * "Support method parameters"
 * [updates version from 7 to 8]
 * wildfly/jandex@452bbd4#diff-a696ceb786df083f5910564662f55bc9
 *
 * "Fix JANDEX-49: Rregression introduced by 6da4d88, which was incomplete"
 * [updates version from 8 to 9]
 * wildfly/jandex@ce4dd9a#diff-a696ceb786df083f5910564662f55bc9
 *
 * "Ensure class is set for annotation targets of MethodParameterInfo"
 * [bug fix; no version update]
 * wildfly/jandex@a5cd95a#diff-a696ceb786df083f5910564662f55bc9
 */
package com.ibm.ws.annocache.jandex.internal;

import java.io.IOException;
import java.util.Map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public final class SparseIndexReaderVersionImpl_V2 implements SparseIndexReaderVersion {
    // Handled range of index versions.

    // Max version updated to 7 per:
    //   JANDEX-46 Add support for default annotation attribute
    //   wildfly/jandex@c208ff5#diff-a696ceb786df083f5910564662f55bc9

    // Max version updated to 8 per:
    //   Support method parameters
    //   wildfly/jandex@452bbd4#diff-a696ceb786df083f5910564662f55bc9

    // Max version updated to 9 per:
    //   Fix JANDEX-49: Rregression introduced by 6da4d88, which was incomplete
    //   wildfly/jandex@ce4dd9a#diff-a696ceb786df083f5910564662f55bc9

    // The following update does not change max version, not is it applicable to the sparse reader:
    //   Ensure class is set for annotation targets of MethodParameterInfo
    //   wildfly/jandex@a5cd95a#diff-a696ceb786df083f5910564662f55bc9
    
    // Max version updated to 10 per:
    //   #84 Index classes used in the constant pool
    //   wildfly/jandex@57b3afe
    //   #58 Indexer option to control ClassInfo sorting
    //   wildfly/jandex@6507a3d
    //   #121 Include relevant type-use annotations in MethodInfo/FieldInfo
    //   wildfly/jandex@cd5437e
    //   #119 Support modules and records
    //   wildfly/jandex@32cdeb0
    //   (None of this new data is extracted by the sparse reader)

    public static final int MIN_VERSION = 6;
    public static final int MAX_VERSION = 10;

    public static boolean accept(int version) {
        return ( (version >= SparseIndexReaderVersionImpl_V2.MIN_VERSION) && 
                 (version <= SparseIndexReaderVersionImpl_V2.MAX_VERSION) );
    }

    // Annotation target type tags:

    protected enum AnnoTarget {
        NULL((byte) 0, (byte) 0),
        FIELD((byte) 1, (byte) 0),
        METHOD((byte) 2, (byte) 0),
        METHOD_PARAMATER((byte) 3, (byte) 1),
        CLASS((byte) 4, (byte) 0),
        EMPTY_TYPE((byte) 5, (byte) 2),
        CLASS_EXTENDS_TYPE((byte) 6, (byte) 2),
        TYPE_PARAMETER((byte) 7, (byte) 2),
        TYPE_PARAMETER_BOUND((byte) 8, (byte) 3),
        METHOD_PARAMETER_TYPE((byte) 9, (byte) 2),
        THROWS_TYPE((byte) 10, (byte) 2),
        RECORD_COMPONENT((byte) 11, (byte) 0);

        private AnnoTarget(byte tag, byte seekCount) {
            this.tag = tag;
            this.seekCount = seekCount;
        }

        public final byte tag;

        public final byte seekCount;
        
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
        UNUSED((byte) 0, 0, 0), // Dummy: Zero base the values array.
        BYTE((byte) 1, 1, 0),
        SHORT((byte) 2, 0, 1),
        INT((byte) 3, 0, 1),
        CHAR((byte) 4, 0, 1),
        FLOAT((byte) 5, 4, 0),
        DOUBLE((byte) 6, 8, 0),
        LONG((byte) 7, 8, 0),
        BOOLEAN((byte) 8, 1, 0),
        STRING((byte) 9, 0, 1),
        CLASS((byte) 10, 0, 1),
        ENUM((byte) 11, 0, 2),
        ARRAY((byte) 12, 0, 0),
        NESTED((byte) 13, 0, 0);

        private AnnoValue(byte tag, int skipCount, int readCount) {
            this.tag = tag;
            this.skipCount = skipCount;
            this.readCount = readCount;
        }

        public final byte tag;

        public byte getTag() {
            return tag;
        }

        public final int skipCount;

        public int getSkipCount() {
            return skipCount;
        }

        public final int readCount;

        public int getReadCount() {
            return readCount;
        }

        public static AnnoValue select(int tag) {
            for ( AnnoValue value : AnnoValue.values() ) {
                if ( value.tag == 0 ) {
                    continue; // Skip the dummy!
                }
                if ( value.tag == tag ) {
                    return value;
                }
            }
            return null;
        }
    }

    //

    protected SparseIndexReaderVersionImpl_V2(PackedDataInputStream input, int version) {
        this.input = input;
        this.version = version;

        // System.out.println("Version [ " + version + " ]");

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
        int usersLength = 0;
        input.seekPackedU32(); // annotationSize
        input.seekPackedU32(); // implementorSize
        input.seekPackedU32(); // subclassesSize
        if (version >= 10) {
            usersLength = input.readPackedU32();
        }

        readByteTable();
        readStringTable();
        readNameTable();

        typeTable = new SparseDotName[input.readPackedU32() + 1];
        typeListTable = new SparseDotName[input.readPackedU32() + 1][];
        annoClassNameTable = new SparseDotName[input.readPackedU32() + 1];
        annoTypeTable  = new byte[ annoClassNameTable.length ];

        readTypeTable();
        readTypeListTable();
        if (version >= 10) {
            readUsersTable(usersLength);
        }

        readMethodTable();
        readFieldTable();
        if (version >= 10) {
            readRecordComponentTable();
        }

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
        // Method parameter annotations are currently ignored; meaning, no updates are
        // needed.
        //   Ensure class is set for annotation targets of MethodParameterInfo [bug fix; no version update]
        //   wildfly/jandex@a5cd95a#diff-a696ceb786df083f5910564662f55bc9

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

        int seekCount = AnnoTarget.values()[tag].seekCount;

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
            movePastAnnotationValue();
        }
    }
    
    private void movePastAnnotationValue() throws IOException {
        input.seekPackedU32(); // name

        byte valueType = input.readByte();
        // System.out.println("Value [ " + AnnoValue.select(valueType) + " ]");

        if ( valueType == AnnoValue.ARRAY.tag ) {
            movePastAnnotationValues();

        } else if ( valueType == AnnoValue.NESTED.tag ) {
            @SuppressWarnings("unused")
            int annoOffset = readAnnotation();

        } else {
            AnnoValue annoValue = AnnoValue.values()[valueType];

            int skipCount = annoValue.skipCount;
            if ( skipCount != 0 ) {
                input.skipBytes(skipCount);
            } else {
                int readCount = annoValue.readCount;
                input.seekPackedU32();
                if ( readCount == 2 ) {
                    input.seekPackedU32();
                }
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
    
    private void readUsersTable(int usersLength) throws IOException {
        for (int i = 0; i < usersLength; i++) {
            input.seekPackedU32(); // user name index
            int usesCount = input.readPackedU32();
            for (int j = 0; j < usesCount; j++) {
                input.seekPackedU32(); // used name index
            }
        }
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

        input.seekPackedU32(); // flags // Skip unused method data.
        input.seekPackedU32(); // type parameters
        input.seekPackedU32(); // receiver type
        input.seekPackedU32(); // return type
        input.seekPackedU32(); // parameters
        input.seekPackedU32(); // exceptions

        // Annotation default values are written to the index starting with index format version 7.
        //   JANDEX-46 Add support for default annotation attribute
        //   wildfly/jandex@c208ff5#diff-a696ceb786df083f5910564662f55bc9
        if ( version >= 7 ) {
            boolean hasDefaultValue = ( input.readByte() > 0 );
            if ( hasDefaultValue ) {
                movePastAnnotationValue(); // default value
            }
        }

        // Method parameters are written to the index starting with index format version 8.
        //   Support method parameters
        //   wildfly/jandex@452bbd4#diff-a696ceb786df083f5910564662f55bc9

        if ( version >= 8 ) {
            int numParms = input.readPackedU32(); 
            for ( int parmNo = 0; parmNo < numParms; parmNo++ ) {
                input.seekPackedU32(); // parm bytes
            }
        }

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
    
    private void readRecordComponentTable() throws IOException {
        int size = input.readPackedU32();
        for (int i = 0; i < size; i++) {
            input.seekPackedU32(); // name index
            input.seekPackedU32(); // type index
            readAnnotations();
        }
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

        // Nesting information became a two bit field in version 9.
        // Also, the nesting information was moved to before the enclosing class information.
        //   Fix JANDEX-49: Rregression introduced by 6da4d88, which was incomplete [updates version from 8 to 9]
        //   wildfly/jandex@ce4dd9a#diff-a696ceb786df083f5910564662f55bc9

        if ( version >= 9 ) {
            // Starting with v9, both the enclosing class and the enclosing
            // method are optional.  The enclosure bits are a two bit field.
            // But, the method bit cannot be set unless the class bit is set.
            // [ 0, 0 ], [ 1, 0 ], [ 1, 1 ] are allowed; [ 0, 1 ] is not allowed.

            // Starting with v9, the enclosure bits are placed before the enclosing
            // class information.

            int enclosureBits = input.readUnsignedByte();
            // System.out.println("Class name [ " + className + " ] Enclosure bits [ " + enclosureBits + " ]");

            if ( enclosureBits > 0 ) {
                readPastEnclosingClass();
                if ( (enclosureBits & 2) == 2 ) {
                    readPastEnclosingMethod();
                }
            }

        } else {
            // Prior to v9, enclosing class information is always present.
            // And the enclosure bits are a single bit field.

            // Note that the enclosure bits are after the enclosing class information.

            readPastEnclosingClass();

            int enclosureBits = input.readUnsignedByte();
            // System.out.println("Class name [ " + className + " ] Enclosure bits [ " + enclosureBits + " ]");

            // Should be set as just bit 1, but the Wyldfly code tests this way.
            // hasEnclosingMethod = ((enclosureBits & 1) == 1);
            if ( enclosureBits == 1 ) {
                readPastEnclosingMethod();
            }
        }

        int numAnnotations = input.readPackedU32();
        // System.out.println("Next class annotations [ " + numAnnotations + " ]");

        readFields(classInfo);
        if (version >= 10) {
            input.seekPackedU32(); // field position array byte table index
        }
        
        readMethods(classInfo);
        if (version >= 10) {
            input.seekPackedU32(); // method position array byte table index
        }
        
        if (version >= 10) {
            readRecordComponents();
            input.seekPackedU32(); // record component position array byte table index
        }

        for ( int annoNo = 0; annoNo < numAnnotations; annoNo++ ) {
            if ( readClassAnnotations(className, classAnnoClassNames) ) {
                classInfo.addClassAnnotations(classAnnoClassNames);
                classAnnoClassNames.clear();
            }
        }

        return classInfo;
    }

    private void readPastEnclosingClass() throws IOException {
        input.seekPackedU32(); // enclosing class // Skip unused class data.
        input.seekPackedU32(); // simple name
    }

    private void readPastEnclosingMethod() throws IOException {
        // Detection of the enclosing method now happens when detecting the enclosing method.
        //   Fix JANDEX-49: Rregression introduced by 6da4d88, which was incomplete
        //   wildfly/jandex@ce4dd9a#diff-a696ceb786df083f5910564662f55bc9

        input.seekPackedU32(); //eName
        input.seekPackedU32(); //eClass
        input.seekPackedU32(); //returnType
        input.seekPackedU32(); //parameters
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
    
    private void readRecordComponents() throws IOException {
        int numRecordComponents = input.readPackedU32();
        // System.out.println("Next record components [ " + numRecordComponents + " ]");
        for (int i = 0; i < numRecordComponents; i++) {
            input.seekPackedU32(); // record component index
        }
    }
}
 
