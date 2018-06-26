/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
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
 * https://github.com/wildfly/jandex/blob/master/src/main/java/org/jboss/jandex/IndexReaderV1.java
 * commit - 36c2b049b7858205c6504308a5e162a4e943ff21
 */

package com.ibm.ws.anno.jandex.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;





/**
 * Reads a Jandex index file and returns the saved index. See {@link org.jboss.jandex.Indexer}
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
final class LimitedIndexReaderV1 extends IndexReaderImpl {
    
    static final int MIN_VERSION = 2;
    static final int MAX_VERSION = 3;
    private static final byte FIELD_TAG = 1;
    private static final byte METHOD_TAG = 2;
    private static final byte METHOD_PARAMATER_TAG = 3;
    private static final byte CLASS_TAG = 4;

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
    
    private PackedDataInputStream input;
    private DotName[] classTable;
    private String[] stringTable;


    /**
     * Constructs a new IndedReader using the passed stream. The stream is not
     * read from until the read method is called.
     *
     * @param input a stream which points to a jandex index file
     */
    LimitedIndexReaderV1(PackedDataInputStream input) {
        this.input = input;
    }

    /**
     * Read the index at the associated stream of this reader. This method can be called multiple
     * times if the stream contains multiple index files.
     *
     * @return the Index contained in the stream
     * @throws java.io.IOException if an I/O error occurs
     * @throws IllegalArgumentException if the stream does not point to Jandex index data
     * @throws org.jboss.jandex.UnsupportedVersion if the index data is tagged with a version not known to this reader
     */
    LimitedIndex read(int version) throws IOException {
        try {
            PackedDataInputStream stream = this.input;
            //masterAnnotations = new HashMap<DotName, List<AnnotationInstance>>();
            readClassTable(stream);
            readStringTable(stream);
            return readClasses(stream, version);
        } finally {
            classTable = null;
            stringTable = null;
        }
    }


    private LimitedIndex readClasses(PackedDataInputStream stream, int version) throws IOException {
        int entries = stream.readPackedU32();
        //HashMap<DotName, List<ClassInfo>> subclasses = new HashMap<DotName, List<ClassInfo>>();
        //HashMap<DotName, List<ClassInfo>> implementors = new HashMap<DotName, List<ClassInfo>>();
        HashMap<DotName, ClassInfo> classes = new HashMap<DotName, ClassInfo>();
        //masterAnnotations = new HashMap<DotName, List<AnnotationInstance>>();

        for (int i = 0; i < entries; i++) {

            //read in the name of the current class and the name of the super class
            DotName name = classTable[stream.readPackedU32()];
            DotName superName = classTable[stream.readPackedU32()];

            //read in the short flag
            //short flags = stream.readShort();
            stream.readShort();

            //read depricated NoArgsConstructor flag
            // No copyParameters supported in version 3+
            //boolean hasNoArgsConstructor = version >= 3 && stream.readBoolean();
            stream.readBoolean();

            //read in the number of interfaces for class
            int numIntfs = stream.readPackedU32();

            //Create list of the interfaces and read in the names
            List<DotName> interfaces = new ArrayList<DotName>(numIntfs);
            for (int j = 0; j < numIntfs; j++) {
                interfaces.add(classTable[stream.readPackedU32()]);
            }
            
            //convert the list into an array to be stored
            //Type[] interfaceTypes = interfaces.toArray(new Type[interfaces.size()]);

            //create a map of annotations DotName -> AnnotationInstance
            //Map<DotName, List<AnnotationInstance>> annotations = new HashMap<DotName, List<AnnotationInstance>>();
            
            //create a ClassType to hold the name of the super class if it exists
            //Type superClassType = superName == null ? null : new ClassType(superName);

            //create a new ClassInfo object for the current entry
            ClassInfo clazz = new ClassInfo(name, superName, (short) 0 ,interfaces.toArray(new DotName[numIntfs]));

            //add it to the master list of classes for the index
            classes.put(name, clazz);

            //add this class in the subclasses map under the name of the super
            //addClassToMap(subclasses, superName, clazz);

            //Add the current class to the implementors map under the name of the implementor
            /*
            for (Type interfaceName : interfaces) {
                addClassToMap(implementors, interfaceName.name(), clazz);
            }
            */

            /*
                After the properties of a class in an entry there are the annotations that belong to that class
            */
            readAnnotations(stream, clazz);
        }

        //TODO REMOVE THE EXTRA PARAMETER FROM THE INDEX.CREATE() METHOD
        return new LimitedIndex(classes);
    }


    private void readAnnotations(PackedDataInputStream stream, ClassInfo clazz)
            throws IOException {
        //read in the number of annotations from the index file and iterate over them
        int numAnnotations = stream.readPackedU32();
        for (int j = 0; j < numAnnotations; j++) {

            //read in the name of the annotation
            //also like the type of annotation, @Override etc.
            DotName annotationName = classTable[stream.readPackedU32()];

            //read in the number of targets and iterate over each target
            int numTargets = stream.readPackedU32();
            for (int k = 0; k < numTargets; k++) {

                /*
                    Read in the tag about the annotation target
                    Tags can be for
                        Fields
                            gets the name, type, and flags for a field
                        Methods
                            Information about the current method
                        Method Parameters
                            target will be a MethodParameterInfo object
                        Clazz
                            which is the current Class Entry
                */
                int tag = stream.readPackedU32();
                //AnnotationTarget target;
                switch (tag) {
                    
                    case FIELD_TAG: {
                        //read in the information about the field, only the name is really needed

                        //String name = stringTable[stream.readPackedU32()];
                        DotName name = DotName.createSimple(stringTable[stream.readPackedU32()]);
                        
                        //read in the type, this is needed to store the name of the field
                        //Type type = readType(stream);
                        movePastReadType(stream);

                        //read in the flag but it doesn't need to be stored
                        //short flags = stream.readShort();
                        stream.readShort();
                        
                        //TODO GET RID OF THE PARAMETER THAT ISN'T NEEDED FROM FIELDINFO CONSTRUCTOR
                        //target = new FieldInfo(clazz, Utils.toUTF8(name), type, (short)0);
                        clazz.fieldAnnotations().add(new LimitedAnnotation(annotationName, name));
                        clazz.fields().add(name);
                        break;
                    }
                    case METHOD_TAG: {

                        //read the method and generate the Annotation target
                        //target = readMethod(clazz, stream);
                        //String name = movePastReadMethod(stream);
                        DotName name = DotName.createSimple(movePastReadMethod(stream));

                        clazz.methodAnnotations().add(new LimitedAnnotation(annotationName,name));
                        clazz.methods().add(name);

                        break;
                    }
                    case METHOD_PARAMATER_TAG: {

                        //create method parameter info target,, this isn't needed
                        //MethodInfo method = readMethod(clazz, stream);

                        //move the stream up past the method parameter information from the readMethod method
                        movePastReadMethod(stream);


                        //since method parameterinfo isn't needed just set the target to null
                        //target = new MethodParameterInfo(method, (short)stream.readPackedU32());
                        stream.readPackedU32();

                        break;
                    }
                    case CLASS_TAG: {
                        //target = clazz;
                        clazz.classAnnotations().add(annotationName);
                        break;
                    }
                    default:
                        throw new UnsupportedOperationException();
                }

                //read in the values of the annotations
                //values not needed from annotations, just the names
                //AnnotationValue[] values = readAnnotationValues(stream);
                movePastReadAnnotationValues(stream);

                //create the final Annotation instance with the target and value
                //values and targets not needed
                //TODO remove the parameters not needed from the constructor
                //AnnotationInstance instance = new AnnotationInstance(annotationName, null, null);

                //add the annotation instance to the masterAnnotations map under the name of the annotation
                //recordAnnotation(masterAnnotations, annotationName, instance);

                //add the annotationInstance to the list of Annotations fora a class
                //recordAnnotation(annotations, annotationName, instance);

            }
        }
    }



    private void movePastReadAnnotationValues(PackedDataInputStream stream){
        try{
        //get the number of values and iterate over each one
        int numValues = stream.readPackedU32();
        for(int i = 0; i < numValues; i++){
            //String name
            stream.readPackedU32();

            //read in the tag that denotes what the value type is
            int tag = stream.readByte();

            //analyze the tag and move the stream past the value type
            switch(tag)
            {
                case AVALUE_BYTE:
                    //value = new AnnotationValue.ByteValue(name, stream.readByte());
                    stream.readByte();
                    break;
                case AVALUE_SHORT:
                    //value = new AnnotationValue.ShortValue(name, (short) stream.readPackedU32());
                    stream.readPackedU32();
                    break;
                case AVALUE_INT:
                    //value = new AnnotationValue.IntegerValue(name, stream.readPackedU32());
                    stream.readPackedU32();
                    break;
                case AVALUE_CHAR:
                    //value = new AnnotationValue.CharacterValue(name, (char) stream.readPackedU32());
                    stream.readPackedU32();
                    break;
                case AVALUE_FLOAT:
                    //value = new AnnotationValue.FloatValue(name, stream.readFloat());
                    stream.readFloat();
                    break;
                case AVALUE_DOUBLE:
                    //value = new AnnotationValue.DoubleValue(name, stream.readDouble());
                    stream.readDouble();
                    break;
                case AVALUE_LONG:
                    //value = new AnnotationValue.LongValue(name, stream.readLong());
                    stream.readLong();
                    break;
                case AVALUE_BOOLEAN:
                    //value = new AnnotationValue.BooleanValue(name, stream.readBoolean());
                    stream.readBoolean();
                    break;
                case AVALUE_STRING:
                    //value = new AnnotationValue.StringValue(name, stringTable[stream.readPackedU32()]);
                    stream.readPackedU32();
                    break;
                case AVALUE_CLASS:
                    //value = new AnnotationValue.ClassValue(name, readType(stream));
                    movePastReadType(stream);
                    break;
                case AVALUE_ENUM:
                    //value = new AnnotationValue.EnumValue(name, classTable[stream.readPackedU32()], stringTable[stream.readPackedU32()]);
                    stream.readPackedU32();
                    stream.readPackedU32();
                    break;
                case AVALUE_ARRAY:
                    //value = new AnnotationValue.ArrayValue(name, readAnnotationValues(stream));
                    movePastReadAnnotationValues(stream);
                    break;
                case AVALUE_NESTED: {
                    //DotName nestedName = classTable[stream.readPackedU32()];
                    stream.readPackedU32();
                    //AnnotationInstance nestedInstance = new AnnotationInstance(nestedName, null, readAnnotationValues(stream));
                    movePastReadAnnotationValues(stream);

                    //make the value for this instance a nested instance
                    //value = new AnnotationValue.NestedAnnotation(name, nestedInstance);
                    break;
                }
                default:
                    throw new IllegalStateException("Invalid annotation value tag:" + tag);
            }
        }

        }catch(IOException e){
            e.printStackTrace();
        } 
    }


    /*
        Skipping the method parameter annotations require the stream to be moved past that current entry.
        This method is used to make calls to the stream the same amount of times as readMethod()
    */
    private String movePastReadMethod(PackedDataInputStream stream){
        String name = null;
        try{
        //String name
        name = stringTable[stream.readPackedU32()];

        //get the number of args to know how many to skip
        int numArgs = stream.readPackedU32();

        //skip the readType() call in readMethod()
        for(int i = 0 ; i < numArgs; i++){
            //Type.Kind kind
            stream.readByte();
            //DotName name
            stream.readPackedU32();
        }

        //Type returnType = readType(stream)
        stream.readByte();
        stream.readPackedU32();

        //short flags
        stream.readShort();

        
        }catch(IOException e){e.printStackTrace();}

        return name;
    }



    private void movePastReadType(PackedDataInputStream stream){
        try{
        stream.readByte();
        stream.readPackedU32();
        }catch(IOException e){e.printStackTrace();}
    }


    private void readStringTable(PackedDataInputStream stream) throws IOException {
        int entries = stream.readPackedU32();
        stringTable = new String[entries];

        for (int i = 0; i < entries; i++) {
            stringTable[i] = stream.readUTF();
        }
    }


    private void readClassTable(PackedDataInputStream stream) throws IOException {
        int entries = stream.readPackedU32();
        int lastDepth = -1;
        DotName curr = null;

        // Null is the implicit first entry
        classTable = new DotName[++entries];
        for (int i = 1; i < entries; i++) {
            int depth = stream.readPackedU32();
            String local = stream.readUTF();

            if (depth <= lastDepth) {
                while (lastDepth-- >= depth)
                    curr = curr.prefix();
            }

            classTable[i] = curr = new DotName(curr, local, true, false);
            lastDepth = depth;
        }
    }

    int toDataVersion(int version) {
        // From 1 to 3, every version changed the available data

        return version;
    }
}
