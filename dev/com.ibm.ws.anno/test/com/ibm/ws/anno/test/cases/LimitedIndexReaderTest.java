package com.ibm.ws.anno.test.cases;
import org.junit.*;

import java.beans.Transient;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import java.util.LinkedHashSet;
import java.util.LinkedList;

import com.ibm.ws.anno.jandex.internal.*;
import org.jboss.jandex.Index;
import com.ibm.ws.anno.test.data.*;


public class LimitedIndexReaderTest{

    static URL testIndex;
    static InputStream connection;
    static org.jboss.jandex.Index fullIndex;
    static LimitedIndex smallIndex;
    static Collection<org.jboss.jandex.ClassInfo> fullClasses;
    static Collection<com.ibm.ws.anno.jandex.internal.ClassInfo> limitedClasses;
    


    @BeforeClass
    public static void readInIndicies() throws IOException{

        int indexToUse = 0;

        String[] testIndiciesNames = {
            "com.ibm.ws.anno-jarV2.idx", //V2
            "jandex-2.0.6.Final-SNAPSHOT-jar.idx", //V2
            "com.ibm.websphere.appserver.api.basics-jar.idx", //V2
            "jandex-1.2.6.Final-SNAPSHOT-jar.idx", //V1
            "com.ibm.ws.anno-jarV1.idx", //V1
            "com.ibm.websphere.org.osgi.core-jar.idx" //V1
        };


        connection = new FileInputStream(testIndiciesNames[indexToUse]);
        fullIndex = Jandex_Utils.basicReadIndex(connection);
        connection.close();

        connection = new FileInputStream(testIndiciesNames[indexToUse]);
        smallIndex = Jandex_Utils.basicReadLimitedIndex(connection);
        connection.close();

        fullClasses = fullIndex.getKnownClasses();
        limitedClasses = smallIndex.classes();

    }

    @Test
    public void testSameNumberOfClasses(){
        Assert.assertTrue("The number of classes do not match between indicies",fullClasses.size() == limitedClasses.size());
    }

    @Test
    public void testSameFlags(){
        HashMap<String, Short> flags = new HashMap<String,Short>();
        for(org.jboss.jandex.ClassInfo fullClass : fullClasses){
            flags.put(fullClass.name().toString(), fullClass.flags());
        }

        Short flagPlaceholder;

        for(com.ibm.ws.anno.jandex.internal.ClassInfo limitedClass: limitedClasses){
            flagPlaceholder = flags.remove(limitedClass.name().toString());

            Assert.assertNotNull(limitedClass.name().toString() + " is missing from the full index",flagPlaceholder);
            Assert.assertTrue("The flags are not the same for class " + limitedClass.name().toString(), flagPlaceholder.shortValue() == limitedClass.flags());
        }

        Assert.assertEquals("There are more flags in the full index than the limited index", 0, flags.size());
    }

    
    @Test
    public void testSameNamesofClasses(){
        HashSet<String> names = new LinkedHashSet<>(fullClasses.size());

        //add the names from one of the indicies into a hashset
        for(org.jboss.jandex.ClassInfo fullClass: fullClasses){
            Assert.assertTrue("Duplicate Class Names in full Index - " + fullClass.name().toString(), names.add(fullClass.name().toString()));
        }

        //check the names from the other index against the hashset previously made
        for(com.ibm.ws.anno.jandex.internal.ClassInfo limitedClass: limitedClasses){
            Assert.assertFalse(limitedClass.name().toString() + " - is not in both the full and small Index",names.add(limitedClass.name().toString()));
        }
    }

    
    @Test
    public void testNumberOfFields(){
        HashMap<String, Integer> classesToFieldSize = new HashMap<String, Integer>();
        Integer numOfFields;
        int matchingCounter = 0;

        //store the size of the fields list for each class under the DotName.toString()
        for(org.jboss.jandex.ClassInfo fullClass: fullClasses){
            classesToFieldSize.put(fullClass.name().toString(), fullClass.fields().size());
        }

        //iterate over the classes in limited classes
        for(com.ibm.ws.anno.jandex.internal.ClassInfo limitedClass: limitedClasses){
            numOfFields = classesToFieldSize.remove(limitedClass.name().toString());

            //check each class in the small index has a fields list and that the sizes match
            Assert.assertNotNull(limitedClass.name().toString() + " - does not have a field size in full Index",numOfFields);
            Assert.assertTrue(limitedClass.name().toString() + " - does not have matching field sizes between small and full Index",numOfFields.intValue() == limitedClass.fields().size());
            matchingCounter++;
        }

        Assert.assertEquals("There are more classes in the full index than the limited index",0,classesToFieldSize.size());
        Assert.assertEquals("There are less matching fields than there are classes in full index",matchingCounter, fullClasses.size());
        Assert.assertEquals("There are less matching fields than there are classes in limited index",matchingCounter,  limitedClasses.size());
    }
    
    @Test
    public void testSameNamesOfFields(){
        HashMap<String,List<String>> namesToClasses = new HashMap<String,List<String>>();
        List<String> fieldNameHolder;

        for(com.ibm.ws.anno.jandex.internal.ClassInfo limitedClass : limitedClasses){
            fieldNameHolder = new LinkedList<String>();
            
            for(com.ibm.ws.anno.jandex.internal.DotName fieldName : limitedClass.fields()){
                fieldNameHolder.add(fieldName.toString());
            }
            namesToClasses.put(limitedClass.name().toString(), fieldNameHolder);
        }

        for(org.jboss.jandex.ClassInfo fullClass: fullClasses){
            for(org.jboss.jandex.FieldInfo fields: fullClass.fields()){
                Assert.assertTrue("Limited List class " + fullClass.name().toString() + "does not have field " + fields.name() ,namesToClasses.get(fullClass.name().toString()).contains(fields.name()));
            }
        }
    }

    @Test
    public void testNumberOfClassAnnotation(){
        HashMap<String, Integer> classToAnnoSize = new HashMap<String, Integer>();

        for(org.jboss.jandex.ClassInfo fullClass: fullClasses){
            classToAnnoSize.put(fullClass.name().toString(), fullClass.classAnnotations().size());
        }

        Integer numOfClassAnno;

        for(com.ibm.ws.anno.jandex.internal.ClassInfo limitedClass: limitedClasses){
            numOfClassAnno = classToAnnoSize.get(limitedClass.name().toString());
            Assert.assertNotNull(limitedClass.name().toString() + " class is missing from the full sized index",numOfClassAnno);
            Assert.assertTrue("Class Annotation sizes don't match for class - " + limitedClass.name().toString(),numOfClassAnno.intValue() == limitedClass.classAnnotations().size());

        }

    }

    @Test
    public void testSameClassAnnotations(){
        HashMap<String, List<String>> classToAnno = new HashMap<String, List<String>>();
        List<String> classAnnoHolder;

        for(org.jboss.jandex.ClassInfo fullClass: fullClasses){
            classAnnoHolder = new LinkedList<String>();
            for(org.jboss.jandex.AnnotationInstance classAnnotation: fullClass.classAnnotations()){
                classAnnoHolder.add(classAnnotation.name().toString());
            }
            classToAnno.put(fullClass.name().toString(),classAnnoHolder);
        }

        for(com.ibm.ws.anno.jandex.internal.ClassInfo limitedClass: limitedClasses){
            classAnnoHolder = classToAnno.get(limitedClass.name().toString());

            Assert.assertNotNull(limitedClass.name().toString() + " class is missing from the full sized index",classAnnoHolder);
            for(DotName classAnnoName: limitedClass.classAnnotations()){
                Assert.assertTrue(limitedClass.name().toString()+" class does not have annotation " + classAnnoName.toString()+" in the full index",classAnnoHolder.contains(classAnnoName.toString()));
            }
        }
    }

    @Test
    public void testNumberOfMethods(){
        HashMap<String, Integer> classMethodSize = new HashMap<String,Integer>();
        for(org.jboss.jandex.ClassInfo fullClass: fullClasses){
            classMethodSize.put(fullClass.name().toString(), fullClass.methods().size());
        }

        Integer numOfMethods;
        for(com.ibm.ws.anno.jandex.internal.ClassInfo limitedClass: limitedClasses){
            numOfMethods = classMethodSize.get(limitedClass.name().toString());
            Assert.assertNotNull(limitedClass.name().toString() + " class is missing from the full sized index",numOfMethods);
            Assert.assertTrue("Method Size is different for class " + limitedClass.name().toString(),numOfMethods.intValue() == limitedClass.methods().size());
            
        }
    }

    @Test
    public void testSameNamesOfMethods(){
        HashMap<String, List<String>> classToMethods = new HashMap<String, List<String>>();
        List<String> methodNameHolder;
        for(org.jboss.jandex.ClassInfo fullClass: fullClasses){
            methodNameHolder = new LinkedList<String>();
            for(org.jboss.jandex.MethodInfo method: fullClass.methods()){
                methodNameHolder.add(method.name());
            }
            classToMethods.put(fullClass.name().toString(), methodNameHolder);
        }

        for(com.ibm.ws.anno.jandex.internal.ClassInfo limitedClass: limitedClasses){
            methodNameHolder = classToMethods.get(limitedClass.name().toString());
            Assert.assertNotNull(limitedClass.name().toString() + " class is missing from the full sized index",methodNameHolder);
            for(DotName methodName: limitedClass.methods()){
                Assert.assertTrue("Full Index doesn't have method " + methodName.toString() + " in class "+limitedClass.name().toString(),methodNameHolder.contains(methodName.toString()));
            }
        }
    }

    @Test
    public void testNumberOfFieldAnnotations(){
        HashMap<String, HashMap<String,Integer>> classToField = new HashMap<String,HashMap<String,Integer>>();
        HashMap<String,Integer> fieldToNumOfAnno;
        Integer numOfAnnoWrapper;
        
        for(org.jboss.jandex.ClassInfo fullClass: fullClasses){
            fieldToNumOfAnno = new HashMap<String,Integer>();

            for(org.jboss.jandex.FieldInfo field: fullClass.fields()){
                fieldToNumOfAnno.put(field.name(), field.annotations().size());
            }
            classToField.put(fullClass.name().toString(),fieldToNumOfAnno);
        }

        for(com.ibm.ws.anno.jandex.internal.ClassInfo limitedClass: limitedClasses){
            fieldToNumOfAnno = classToField.remove(limitedClass.name().toString());

            Assert.assertNotNull("Full index is missing class " + limitedClass.name().toString(),fieldToNumOfAnno);
            for(com.ibm.ws.anno.jandex.internal.DotName fieldName: limitedClass.fields()){
                numOfAnnoWrapper = fieldToNumOfAnno.remove(fieldName.toString());

                Assert.assertNotNull("Full Index Class " + limitedClass.name().toString() + "is missing field " + fieldName.toString(), numOfAnnoWrapper);
                Assert.assertEquals("The amount of annotations for field " + fieldName + "do not match",numOfAnnoWrapper.intValue(),limitedClass.fieldAnnotations(fieldName).size());
            }

            Assert.assertTrue("There are more fields in full index than limited",fieldToNumOfAnno.size() == 0);
        }
    }

    /*
    *   Annotations are relative to the name of the method and not the signature
    */
    @Test
    public void testNumberOfMethodAnnotations(){
        HashMap<String, HashMap<String,Integer>> classToMethod = new HashMap<String,HashMap<String,Integer>>();
        HashMap<String,Integer> methodToNumOfAnno;
        List<String> checkedMethods = new LinkedList<String>();
        Integer numOfAnnoWrapper;

        
        for(org.jboss.jandex.ClassInfo fullClass: fullClasses){
            methodToNumOfAnno = new HashMap<String,Integer>();

            for(org.jboss.jandex.MethodInfo method: fullClass.methods()){
                if(methodToNumOfAnno.containsKey(method.name())){
                    methodToNumOfAnno.put(method.name(), method.annotations().size() + methodToNumOfAnno.get(method.name()));
                }
                else{
                    methodToNumOfAnno.put(method.name(), method.annotations().size());
                }
            }
            classToMethod.put(fullClass.name().toString(),methodToNumOfAnno);
        }

        for(com.ibm.ws.anno.jandex.internal.ClassInfo limitedClass: limitedClasses){
            
            methodToNumOfAnno = classToMethod.remove(limitedClass.name().toString());

            Assert.assertNotNull("Full index is missing class " + limitedClass.name().toString(),methodToNumOfAnno);
            for(com.ibm.ws.anno.jandex.internal.DotName methodName: limitedClass.methods()){
                if(checkedMethods.contains(methodName.toString()) == false){
                    numOfAnnoWrapper = methodToNumOfAnno.remove(methodName.toString());

                    Assert.assertNotNull("Full Index Class " + limitedClass.name().toString() + "is missing method " + methodName.toString(), numOfAnnoWrapper);
                    Assert.assertEquals("The amount of annotations for method " + methodName + "do not match",numOfAnnoWrapper.intValue(),limitedClass.methodAnnotations(methodName).size());
                    checkedMethods.add(methodName.toString());
                }

            }

            checkedMethods.clear();
            Assert.assertTrue("There are more methods in full index than limited for class "+limitedClass.name() + ", " + methodToNumOfAnno.size()+" extra",methodToNumOfAnno.size() == 0);
        }

    }


    @Test
    public void testNamesOfFieldAnnotations(){
        HashMap<String, HashMap<String,List<String>>> classToField = new HashMap<String,HashMap<String,List<String>>>();
        HashMap<String,List<String>> fieldToAnnosMap;
        
        List<String> annotationNames;

        
        for(org.jboss.jandex.ClassInfo fullClass: fullClasses){
            fieldToAnnosMap = new HashMap<String,List<String>>();
            
            for(org.jboss.jandex.FieldInfo field: fullClass.fields()){
                annotationNames = new LinkedList<String>();

                for(org.jboss.jandex.AnnotationInstance fieldAnno: field.annotations()){
                    annotationNames.add(fieldAnno.name().toString());
                }
                fieldToAnnosMap.put(field.name(), annotationNames);
            }
            classToField.put(fullClass.name().toString(),fieldToAnnosMap);
        }

        for(com.ibm.ws.anno.jandex.internal.ClassInfo limitedClass: limitedClasses){
            fieldToAnnosMap = classToField.remove(limitedClass.name().toString());

            Assert.assertNotNull("Full index is missing class " + limitedClass.name().toString(),fieldToAnnosMap);
            for(com.ibm.ws.anno.jandex.internal.DotName fieldName: limitedClass.fields()){
                annotationNames = fieldToAnnosMap.remove(fieldName.toString());

                Assert.assertNotNull("Full Index Class " + limitedClass.name().toString() + " is missing field " + fieldName.toString(), annotationNames);
                for(com.ibm.ws.anno.jandex.internal.LimitedAnnotation fieldAnno: limitedClass.fieldAnnotations(fieldName)){
                    Assert.assertTrue("Field" + fieldName.toString()+" is missing "+fieldAnno.getName().toString()+" annotation in class " + limitedClass.name().toString(),annotationNames.remove(fieldAnno.getName().toString()));
                }
                Assert.assertTrue("There are more annotations for field "+fieldName.toString()+" in the full index in class "+limitedClass.name().toString(),annotationNames.size() == 0);
            }

            Assert.assertTrue("There are more fields for class  "+ limitedClass.name().toString()+" in the full index",fieldToAnnosMap.size() == 0);
        }
    }


    /*
    *   Annotations are relative to the name of the method and not the signature
    */
    @Test
    public void testNamesOfMethodAnnotations(){
        HashMap<String, HashMap<String,List<String>>> classToMethod = new HashMap<String,HashMap<String,List<String>>>();
        HashMap<String,List<String>> methodToAnnosMap;
        List<String> checkedMethods = new LinkedList<String>();
        List<String> annotationNames;

        
        for(org.jboss.jandex.ClassInfo fullClass: fullClasses){
            methodToAnnosMap = new HashMap<String,List<String>>();
            
            for(org.jboss.jandex.MethodInfo method: fullClass.methods()){
                if(methodToAnnosMap.containsKey(method.name())){
                    annotationNames = methodToAnnosMap.get(method.name());
                }
                else{
                    annotationNames = new LinkedList<String>();
                }
                

                for(org.jboss.jandex.AnnotationInstance methodAnno: method.annotations()){
                    annotationNames.add(methodAnno.name().toString());
                }
                methodToAnnosMap.put(method.name(), annotationNames);
            }
            classToMethod.put(fullClass.name().toString(),methodToAnnosMap);
        }

        for(com.ibm.ws.anno.jandex.internal.ClassInfo limitedClass: limitedClasses){
            methodToAnnosMap = classToMethod.remove(limitedClass.name().toString());

            Assert.assertNotNull("Full index is missing class " + limitedClass.name().toString(),methodToAnnosMap);
            for(com.ibm.ws.anno.jandex.internal.DotName methodName: limitedClass.methods()){
                if(checkedMethods.contains(methodName.toString()) == false){
                    annotationNames = methodToAnnosMap.remove(methodName.toString());

                    Assert.assertNotNull("Full Index Class " + limitedClass.name().toString() + " is missing method " + methodName.toString(), annotationNames);
                    for(com.ibm.ws.anno.jandex.internal.LimitedAnnotation methodAnno: limitedClass.methodAnnotations(methodName)){
                        Assert.assertTrue("Method" + methodName.toString()+" is missing "+methodAnno.getName().toString()+" annotation in class " + limitedClass.name().toString(),annotationNames.remove(methodAnno.getName().toString()));
                    }
                    Assert.assertTrue("There are more annotations for method "+methodName.toString()+" in the full index in class "+limitedClass.name().toString(),annotationNames.size() == 0);
                }
                checkedMethods.add(methodName.toString());
            }
            checkedMethods.clear();
            Assert.assertTrue("There are more methods for class  "+ limitedClass.name().toString()+" in the full index",methodToAnnosMap.size() == 0);
        }
    }


    @Test
    public void testSuperNamesAlign(){
        HashMap<String, String> classToSuper = new HashMap<String,String>();
        String nameOfSuper;
        for(org.jboss.jandex.ClassInfo fullClass: fullClasses){
            classToSuper.put(fullClass.name().toString(), fullClass.superName().toString());
        }
        for(com.ibm.ws.anno.jandex.internal.ClassInfo limitedClass: limitedClasses){
            nameOfSuper = classToSuper.get(limitedClass.name().toString());
            Assert.assertNotNull(limitedClass.name().toString() + " class is missing from the full sized index",nameOfSuper);
            Assert.assertTrue("Super names don't match up for class " + limitedClass.name().toString(),nameOfSuper.equals(limitedClass.superName().toString()));

        }

    }

    @Test
    public void testNumberOfInterfaces(){
        HashMap<String, Integer> classToInterfaceSize = new HashMap<String,Integer>();
        Integer numOfInterfaces;
        for(org.jboss.jandex.ClassInfo fullClass: fullClasses){
            classToInterfaceSize.put(fullClass.name().toString(), fullClass.interfaceNames().size());
        }

        for(com.ibm.ws.anno.jandex.internal.ClassInfo limitedClass: limitedClasses){
            numOfInterfaces = classToInterfaceSize.get(limitedClass.name().toString());
            Assert.assertNotNull(limitedClass.name().toString() + " class is missing from the full sized index",numOfInterfaces);
            Assert.assertTrue("Interface Sizes for class "+limitedClass.name().toString() +" do not match", limitedClass.interfaceNames().length == numOfInterfaces.intValue());
            
        }
    }

    @Test
    public void testInterfacesHaveSameNames(){
        HashMap<String,List<String>> classToInterfaceNames = new HashMap<String,List<String>>();
        List<String> nameHolder;
        for(org.jboss.jandex.ClassInfo fullClass: fullClasses){
            nameHolder = new LinkedList<String>();
            for(org.jboss.jandex.DotName interfaceName : fullClass.interfaceNames()){
                nameHolder.add(interfaceName.toString());
            }
            classToInterfaceNames.put(fullClass.name().toString(), nameHolder);
        }

        for(com.ibm.ws.anno.jandex.internal.ClassInfo limitedClass: limitedClasses){
            nameHolder = classToInterfaceNames.get(limitedClass.name().toString());
            Assert.assertNotNull(limitedClass.name().toString() + " class is missing from the full sized index",nameHolder);
            for(com.ibm.ws.anno.jandex.internal.DotName interfaceName: limitedClass.interfaceNames()){
                Assert.assertTrue(nameHolder.remove(interfaceName.toString()));
            }

            Assert.assertTrue("There are more interfaces in full index for class" + limitedClass.name().toString(), nameHolder.size() == 0);

        }
    }


    
}