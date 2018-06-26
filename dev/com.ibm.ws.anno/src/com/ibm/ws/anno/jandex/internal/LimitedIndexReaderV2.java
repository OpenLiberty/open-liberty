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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;




/**
 * Reads a Jandex index file and returns the saved index. See {@link Indexer}
 * for a thorough description of how the Index data is produced.
 *
 * <p>
 * An IndexReader loads the stream passed to it's constructor and applies the
 * appropriate buffering. The Jandex index format is designed for efficient
 * reading and low final memory storage.
 *
 * <p>
 * <b>Thread-Safety</b>
 * </p>
 * IndexReader is not thread-safe and can not be shared between concurrent
 * threads. The resulting index, however, is.
 *
 * @author Jason T. Greene
 */
final class LimitedIndexReaderV2 extends IndexReaderImpl {
    static final int MIN_VERSION = 6;
    static final int MAX_VERSION = 6;
    static final int MAX_DATA_VERSION = 4;
    private static final byte NULL_TARGET_TAG = 0;
    private static final byte FIELD_TAG = 1;
    private static final byte METHOD_TAG = 2;
    private static final byte METHOD_PARAMATER_TAG = 3;
    private static final byte CLASS_TAG = 4;
    private static final byte EMPTY_TYPE_TAG = 5;
    private static final byte CLASS_EXTENDS_TYPE_TAG = 6;
    private static final byte TYPE_PARAMETER_TAG = 7;
    private static final byte TYPE_PARAMETER_BOUND_TAG = 8;
    private static final byte METHOD_PARAMETER_TYPE_TAG = 9;
    private static final byte THROWS_TYPE_TAG = 10;
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
    private final static byte[] INIT_METHOD_NAME = Utils.toUTF8("<init>");

    private PackedDataInputStream input;
    private byte[][] byteTable;
    private String[] stringTable;
    private DotName[] nameTable;
    private DotName[] typeTable;
    private DotName[][] typeListTable;
    private LimitedAnnotation[] annotationTable;
    private LimitedAnnotationHolder[] methodTable;
    private LimitedAnnotationHolder[] fieldTable;



    LimitedIndexReaderV2(PackedDataInputStream input) {
        this.input = input;
    }

    LimitedIndex read(int version) throws IOException {
        try {
            PackedDataInputStream stream = this.input;

            //read in the sizes for the implementors, masterannotations, and subclasses structures 
            int annotationsSize = stream.readPackedU32();
            int implementorsSize = stream.readPackedU32();
            int subclassesSize = stream.readPackedU32();



            //read in the byte table, string table, and name table to be used by the reader
            readByteTable(stream);
            readStringTable(stream);
            readNameTable(stream);

            //read in the sizes of the type table, type list table, and the annotations table
            typeTable = new DotName[stream.readPackedU32() + 1];
            typeListTable = new DotName[stream.readPackedU32() + 1][];
            annotationTable = new LimitedAnnotation[stream.readPackedU32() + 1];

            //fill in the class variables for the type table, typelist table, method table, and field table
            readTypeTable(stream);
            readTypeListTable(stream);
            readMethodTable(stream);
            readFieldTable(stream);
            
            
            return readClasses(stream, annotationsSize, implementorsSize, subclassesSize);
        } finally {
            byteTable = null;
            stringTable = null;
            nameTable = null;
            typeTable = null;
            typeListTable = null;
            annotationTable = null;
            methodTable = null;
            fieldTable = null;
        }
    }

    private void readByteTable(PackedDataInputStream stream) throws IOException {
        // Null is the implicit first entry
        int size = stream.readPackedU32() + 1;
        byte[][] byteTable = this.byteTable = new byte[size][];
        for (int i = 1; i < size; i++) {
            int len = stream.readPackedU32();
            byteTable[i] = new byte[len];
            stream.readFully(byteTable[i], 0, len);
        }
    }

    private void readStringTable(PackedDataInputStream stream) throws IOException {
        // Null is the implicit first entry
        int size = stream.readPackedU32() + 1;
        String[] stringTable = this.stringTable = new String[size];
        for (int i = 1; i < size; i++) {
            stringTable[i] = stream.readUTF();
        }
    }

    private void readNameTable(PackedDataInputStream stream) throws IOException {
        // Null is the implicit first entry
        int entries = stream.readPackedU32() + 1;
        int lastDepth = -1;
        DotName curr = null;

        nameTable = new DotName[entries];
        for (int i = 1; i < entries; i++) {
            int depth = stream.readPackedU32();
            boolean inner = (depth & 1) == 1;
            depth >>= 1;

            String local = stringTable[stream.readPackedU32()];

            if (depth <= lastDepth) {
                while (lastDepth-- >= depth) {
                    assert curr != null;
                    curr = curr.prefix();
                }
            }

            nameTable[i] = curr = new DotName(curr, local, true, inner);
            lastDepth = depth;
        }
    }

    private void readTypeTable(PackedDataInputStream stream) throws IOException {
        // Null is the implicit first entry
        for (int i = 1; i < typeTable.length; i++) {
            typeTable[i] = movePastReadTypeEntry(stream);
        }
    }

    private int findNextNull(Object[] array, int start) {
        while (start < array.length) {
            if (array[start] == null) {
                return start;
            }
            start++;
        }

        return array.length;
    }

    private void readTypeListTable(PackedDataInputStream stream) throws IOException {
        // Null is the implicit first entry
        DotName[][] typeListTable = this.typeListTable;
        // Already emitted entries are omitted as gaps in the table portion
        for (int i = findNextNull(typeListTable, 1); i < typeListTable.length; i = findNextNull(typeListTable, i)) {
            typeListTable[i] = readTypeListEntry(stream);
        }
    }

    private LimitedAnnotation[] readAnnotations(PackedDataInputStream stream, DotName target) throws IOException {
        int size = stream.readPackedU32();
        if (size == 0) {
            return LimitedAnnotation.EMPTY_ARRAY;
        }
        LimitedAnnotation[] annotations = new LimitedAnnotation[size];
        for (int i = 0; i < size; i++) {
            //read in the reference number from the annotationTable
            int reference = stream.readPackedU32();

            //if the current annotation hasn't been read before then read it in from the stream
            //and add it to the annotation table under reference
            if (annotationTable[reference] == null) {
                annotationTable[reference] = readAnnotationEntry(stream, target);
            }

            annotations[i] = annotationTable[reference];
        }
        return annotations;
    }

    private void movePastAnnotationValues(PackedDataInputStream stream) throws IOException {
        int numValues = stream.readPackedU32();
        for (int i = 0; i < numValues; i++) {

            //String name = stringTable[stream.readPackedU32()];
            stream.readPackedU32();

            int tag = stream.readByte();

            switch (tag) {
                case AVALUE_BYTE:
                    stream.readByte();
                    break;
                case AVALUE_SHORT:
                    stream.readPackedU32();
                    break;
                case AVALUE_INT:
                    stream.readPackedU32();
                    break;
                case AVALUE_CHAR:
                    stream.readPackedU32();
                    break;
                case AVALUE_FLOAT:
                    stream.readFloat();
                    break;
                case AVALUE_DOUBLE:
                    stream.readDouble();
                    break;
                case AVALUE_LONG:
                    stream.readLong();
                    break;
                case AVALUE_BOOLEAN:
                    stream.readBoolean();
                    break;
                case AVALUE_STRING:
                    stream.readPackedU32();
                    break;
                case AVALUE_CLASS:
                    stream.readPackedU32();
                    break;
                case AVALUE_ENUM:
                    stream.readPackedU32();
                    stream.readPackedU32();
                    break;
                case AVALUE_ARRAY:
                    movePastAnnotationValues(stream);
                    break;
                case AVALUE_NESTED: {
                    int reference = stream.readPackedU32();
                    LimitedAnnotation nestedInstance = annotationTable[reference];
                    if (nestedInstance == null) {
                        nestedInstance = annotationTable[reference] = readAnnotationEntry(stream, null);
                    }

                    break;
                }
                default:
                    throw new IllegalStateException("Invalid annotation value tag:" + tag);
            }


        }
    }

    private LimitedAnnotation readAnnotationEntry(PackedDataInputStream stream, DotName caller) throws IOException {
        DotName name = nameTable[stream.readPackedU32()];

        movePastAnnotationTarget(stream);
        movePastAnnotationValues(stream);

        return new LimitedAnnotation(name,caller);
    }

    private DotName[] readTypeListReference(PackedDataInputStream stream) throws IOException {
        int reference = stream.readPackedU32();
        DotName[] types = typeListTable[reference];
        if (types != null) {
            return types;
        }

        return typeListTable[reference] = readTypeListEntry(stream);
    }


    private DotName[] readTypeListEntry(PackedDataInputStream stream) throws IOException {
        int size = stream.readPackedU32();
        if (size == 0) {
            return DotName.PLACEHOLDER_ARRAY;
        }

        DotName[] types = new DotName[size];
        for (int i = 0; i < size; i++) {
            types[i] = typeTable[stream.readPackedU32()];
        }

        return types;
    }


    private DotName movePastReadTypeEntry(PackedDataInputStream stream) throws IOException{
        //Type.Kind kind = Type.Kind.fromOrdinal(stream.readUnsignedByte());
        int kind = (int) stream.readUnsignedByte();
        //0 - class
        //1 - Array
        //2 - Primitive
        //3 - void
        //4 - Type_variable
        //5 - Unresolved_TypeVariable
        //6 - WildCard Type
        //7 - Parametrized Type
        //default - 3

        switch (kind) {
            case 0: {
                DotName name = nameTable[stream.readPackedU32()];
                //AnnotationInstance[] annotations = readAnnotations(stream, null);
                readAnnotations(stream,null);
                return name;
            }
            case 1: {
                //int dimensions = stream.readPackedU32();
                stream.readPackedU32();
                //Type component = typeTable[stream.readPackedU32()];
                stream.readPackedU32();
                // AnnotationInstance[] annotations = readAnnotations(stream, null);
                readAnnotations(stream, null);
                //return new ArrayType(component, dimensions, annotations);
                return DotName.PLACEHOLDER;
            }
            case 2: {
                /*
                int primitive = stream.readUnsignedByte();
                Type type = PrimitiveType.fromOridinal(primitive);
                AnnotationInstance[] annotations = readAnnotations(stream, null);
                return annotations.length > 0 ? type.copyType(annotations) : type;
                */
                stream.readUnsignedByte();
                readAnnotations(stream, null);
                return DotName.PLACEHOLDER;
            }
            default:
            case 3: {
                /*
                Type type = VoidType.VOID;
                AnnotationInstance[] annotations = readAnnotations(stream, null);
                return annotations.length > 0 ? type.copyType(annotations) : type;
                */
                readAnnotations(stream, null);
                return DotName.PLACEHOLDER;
            }
            case 4: {
                /*
                String identifier = stringTable[stream.readPackedU32()];
                Type[] bounds = readTypeListReference(stream);
                AnnotationInstance[] annotations = readAnnotations(stream, null);
                return new TypeVariable(identifier, bounds, annotations);
                */
                stream.readPackedU32();
                //readTypeListReference
                readTypeListReference(stream);
                readAnnotations(stream, null);
                return DotName.PLACEHOLDER;
            }
            case 5: {
                /*
                String identifier = stringTable[stream.readPackedU32()];
                AnnotationInstance[] annotations = readAnnotations(stream, null);
                return new UnresolvedTypeVariable(identifier, annotations);
                */
                stream.readPackedU32();
                readAnnotations(stream, null);
                return DotName.PLACEHOLDER;
            }
            case 6: {
                /*
                boolean isExtends = stream.readPackedU32() == 1;
                Type bound = typeTable[stream.readPackedU32()];
                AnnotationInstance[] annotations = readAnnotations(stream, null);
                return new WildcardType(bound, isExtends, annotations);
                */
                stream.readPackedU32();
                stream.readPackedU32();
                readAnnotations(stream, null);
                return DotName.PLACEHOLDER;
            }
            case 7: {
                /*
                DotName name = nameTable[stream.readPackedU32()];
                int reference = stream.readPackedU32();
                Type owner = typeTable[reference];
                Type[] parameters = readTypeListReference(stream);
                AnnotationInstance[] annotations = readAnnotations(stream, null);
                return new ParameterizedType(name, parameters, owner, annotations);
                */
                DotName name = nameTable[stream.readPackedU32()];
                //stream.readPackedU32();
                stream.readPackedU32();
                //readTypeListReference
                readTypeListReference(stream);
                readAnnotations(stream, null);
                return name;

            }

        }
    }


    private void movePastAnnotationTarget(PackedDataInputStream stream) throws IOException {
        byte tag = stream.readByte();
        switch(tag){
            case NULL_TARGET_TAG:
                return;
            case CLASS_TAG:
            case FIELD_TAG:
            case METHOD_TAG:
                return;
            case METHOD_PARAMATER_TAG: {
                stream.readPackedU32();
                return;
            }
            case EMPTY_TYPE_TAG: {
                stream.readPackedU32();
                stream.readPackedU32();
                return;
            }
            case CLASS_EXTENDS_TYPE_TAG: {
                stream.readPackedU32();
                stream.readPackedU32();
                return;
            }
            case TYPE_PARAMETER_TAG: {
                stream.readPackedU32();
                stream.readPackedU32();
                return;
            }
            case TYPE_PARAMETER_BOUND_TAG: {
                stream.readPackedU32();
                stream.readPackedU32();
                stream.readPackedU32();
                return;
            }
            case METHOD_PARAMETER_TYPE_TAG: {
                stream.readPackedU32();
                stream.readPackedU32();
                return;
            }
            case THROWS_TYPE_TAG: {
                stream.readPackedU32();
                stream.readPackedU32();
                return;
            }
        }
    }

    private void readMethodTable(PackedDataInputStream stream) throws IOException {
        // Null holds the first slot
        int size = stream.readPackedU32() + 1;
        methodTable = new LimitedAnnotationHolder[size];
        for (int i = 1; i < size; i++) {
            methodTable[i] = readMethodEntry(stream);
        }

    }

    private void readFieldTable(PackedDataInputStream stream) throws IOException {
        // Null holds the first slot
        int size = stream.readPackedU32() + 1;
        fieldTable = new LimitedAnnotationHolder[size];
        for (int i = 1; i < size; i++) {
            fieldTable[i] = readFieldEntry(stream);
        }
    }

    private LimitedAnnotationHolder readMethodEntry(PackedDataInputStream stream) throws IOException {
        byte[] name = byteTable[stream.readPackedU32()];
        //Utils.fromUTF8(name) is the String rep of the name
        //short flags = (short) stream.readPackedU32();
        //Type[] typeParameters = typeListTable[stream.readPackedU32()];
        //int reference = stream.readPackedU32();
        //Type receiverType = typeTable[reference];
        //Type returnType = typeTable[stream.readPackedU32()];
        //Type[] parameters = typeListTable[stream.readPackedU32()];
        //Type[] exceptions = typeListTable[stream.readPackedU32()];

        stream.readPackedU32();
        stream.readPackedU32();
        stream.readPackedU32();
        stream.readPackedU32();
        stream.readPackedU32();
        stream.readPackedU32();

        //MethodInfo methodInfo = new MethodInfo();
        LimitedAnnotation[] annotations = readAnnotations(stream, DotName.createSimple(Utils.fromUTF8(name)));
        //MethodInternal methodInternal = new MethodInternal(name, parameters, returnType, flags,
        //        receiverType, typeParameters,
        //        exceptions, annotations);
        //methodInfo.setMethodInternal(methodInternal);
        return new LimitedAnnotationHolder(DotName.createSimple(Utils.fromUTF8(name)), annotations);
    }

    private LimitedAnnotationHolder readFieldEntry(PackedDataInputStream stream) throws IOException {
        byte[] name = byteTable[stream.readPackedU32()];
        //Utils.fromUTF8(name) is the string rep of the name
        //short flags = (short) stream.readPackedU32();
        //Type type = typeTable[stream.readPackedU32()];
        stream.readPackedU32();
        stream.readPackedU32();

        //FieldInfo fieldInfo = new FieldInfo();
        LimitedAnnotation[] annotations = readAnnotations(stream, DotName.createSimple(Utils.fromUTF8(name)));
        //FieldInternal fieldInternal = new FieldInternal(name, type, flags, annotations);
        //fieldInfo.setFieldInternal(fieldInternal);

        return new LimitedAnnotationHolder(DotName.createSimple(Utils.fromUTF8(name)), annotations);
    }

    private ClassInfo readClassEntry(PackedDataInputStream stream) throws IOException {
        
        //read in the code for the current class and pull it from the nameTable
        DotName name  = nameTable[stream.readPackedU32()];

        //read in the flags for the class
        short flags = (short) stream.readPackedU32();


        //read in the codes for the super type, parameter types, and interface types and retieve them from the tables
        DotName superType = typeTable[stream.readPackedU32()];
        
        //Type[] typeParameters = typeListTable[stream.readPackedU32()];
        //Type[] typeParameters = null;
        stream.readPackedU32();

        
        DotName[] interfaceTypes = typeListTable[stream.readPackedU32()];

        /*
        DotName[] interfaces = new DotName[interfaceTypes.length];
        int counter = 0;
        for(Type temp : interfaceTypes){
            interfaces[counter] = interfaceTypes[counter].name();
        }
        */

        //read in the enclosing class and simple name codes then retrieve it from the tables
        //DotName enclosingClass = nameTable[stream.readPackedU32()];
        //String simpleName = stringTable[stream.readPackedU32()];
        DotName enclosingClass = null;
        String simpleName = null;
        stream.readPackedU32();
        stream.readPackedU32();

        //read in the enclosing method
        //ClassInfo.EnclosingMethodInfo enclosingMethod = readEnclosingMethod(stream);
        //ClassInfo.EnclosingMethodInfo enclosingMethod = null;
        readPastEnclosingMethod(stream);

        //read in the size of the annotations
        int size = stream.readPackedU32();
        
        //Map<DotName, List<AnnotationInstance>> annotations = new HashMap<DotName, List<AnnotationInstance>>(size);
        
        //create the ClassInfo object for the current entry
        ClassInfo clazz = new ClassInfo(name, superType, flags, interfaceTypes);

        //set the internal variables of ClassInfo to the objects retrieved from the tables
        /*
        clazz.setTypeParameters(typeParameters);
        clazz.setEnclosingMethod(enclosingMethod);
        clazz.setInnerClassInfo(enclosingClass, simpleName);
        */
        //get the fieldinternal array and set that as the fields in the class
        //FieldInternal[] fields = readClassFields(stream, clazz);
        //clazz.setFieldArray(fields);
        readClassFields(stream,clazz);

        //get the method internal array and set that as the methods associated with the current class
        //MethodInternal[] methods = readClassMethods(stream, clazz);
        //clazz.setMethodArray(methods);
        readClassMethods(stream,clazz);
        //iterate over all the annotations
        for (int i = 0; i < size; i++) {
            //read in the annotations and create instances
            //List<AnnotationInstance> instances = convertToList(readAnnotations(stream, clazz.name()));
            List<LimitedAnnotation> instances = Arrays.asList(readAnnotations(stream,clazz.name()));

            //if there are annotations instances for this class then add the list to the master maps and the class map
            if (instances.size() > 0) {
                //DotName annotationName = instances.get(0).name();
                //annotations.put(annotationName, instances);
                //addToMaster(masterAnnotations, annotationName, instances);
                /*
                if(instances.get(0).getName().toString().equals("java.lang.Deprecated"))
                    clazz.setTest(instances.get(0));

                */
                for(LimitedAnnotation temp: instances){
                    if(temp.getTargetName().equals(clazz.name())){
                        clazz.classAnnotations().add(temp.getName());
                    }
                }
                //clazz.classAnnotations().add(instances.get(0).getName());
            }
        }



        return clazz;
    }

    private void readClassFields(PackedDataInputStream stream, ClassInfo clazz) throws IOException {
        //read in the number of fields
        int len = stream.readPackedU32();
        //FieldInternal[] fields = new FieldInternal[len];
        //FieldInternal temp;
        
        for (int i = 0; i < len; i++) {
            //pull the field object from the table in order to assign it to current classinfo
            LimitedAnnotationHolder field = fieldTable[stream.readPackedU32()];
            clazz.fields().add(field.getName());
            for(LimitedAnnotation temp: field.getAnnotations()){
                clazz.fieldAnnotations().add(temp);
            }
            //iterate over all the annotations in the current field and update the annotations taget class info
            //updateAnnotationTargetInfo(field.annotationArray(), clazz);

            //fields[i] = field;
        }
    }

    private void readClassMethods(PackedDataInputStream stream, ClassInfo clazz) throws IOException {
        int len = stream.readPackedU32();
        
        for (int i = 0; i < len; i++) {
            
            //retrieve method information from table to assign to current ClassInfo
            LimitedAnnotationHolder method = methodTable[stream.readPackedU32()];

            //update the annotations target class to the current ClassInfo
            //updateAnnotationTargetInfo(method.annotationArray(), clazz);
            
            //methods[i] = method;
            clazz.methods().add(method.getName());
            for(LimitedAnnotation temp: clazz.methodAnnotations()){
                clazz.methodAnnotations().add(temp);
            }

            //if the method has no parameters and it is a constructor set the flag to true
            /*
            if (method.parameterArray().length == 0 && Arrays.equals(INIT_METHOD_NAME, method.nameBytes())) {
                clazz.setHasNoArgsConstructor(true);
            }
            */
        }
       
    }

    private void readPastEnclosingMethod(PackedDataInputStream stream) throws IOException{
        //check if there is an enclosing method
        if (stream.readUnsignedByte() != HAS_ENCLOSING_METHOD) {
            return;
        }
        else{
            //eName
            stream.readPackedU32();
            //eClass
            stream.readPackedU32();
            //returnType
            stream.readPackedU32();
            //parameters
            stream.readPackedU32();

        }
    }

    private LimitedIndex readClasses(PackedDataInputStream stream,
                              int annotationsSize, int implementorsSize, int subclassesSize) throws IOException {
        //read in the amount of classes and initialize the master structures
        int classesSize = stream.readPackedU32();
        HashMap<DotName, ClassInfo> classes = new HashMap<DotName, ClassInfo>(classesSize);
        //HashMap<DotName, List<ClassInfo>> subclasses = new HashMap<DotName, List<ClassInfo>>(subclassesSize);
        //HashMap<DotName, List<ClassInfo>> implementors = new HashMap<DotName, List<ClassInfo>>(implementorsSize);
        //HashMap<DotName, List<AnnotationInstance>> masterAnnotations =
        //        new HashMap<DotName, List<AnnotationInstance>>(annotationsSize);
        
        //iterate over each class entry
        for (int i = 0; i < classesSize; i++) {
            
            //read the entry and generate the ClassInfo object
            ClassInfo clazz = readClassEntry(stream);
            
            //add the current class to the subclasses master structure
            //addClassToMap(subclasses, clazz.superName(), clazz);

            //Iterate over all the interfaces the current class implements and add the current class to the master list
            //under the name of the interface
            /*
            for (Type interfaceType : clazz.interfaceTypeArray()) {
                addClassToMap(implementors, interfaceType.name(), clazz);
            }
            */

            //store the current class into the master classes list for the index
            classes.put(clazz.name(), clazz);
        }

        //create then intex with the master lists for annotations, subclasses, implementors, and classes
        return new LimitedIndex(classes);
    }

    int toDataVersion(int version) {
        return MAX_DATA_VERSION;
    }
}
 