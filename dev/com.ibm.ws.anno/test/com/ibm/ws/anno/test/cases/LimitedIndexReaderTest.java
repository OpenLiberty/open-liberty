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

//import javax.lang.model.util.ElementScanner6;

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
    static Collection<org.jboss.jandex.ClassInfo> originalClasses;
    static Collection<com.ibm.ws.anno.jandex.internal.ClassInfo> limitedClasses;
    


    @BeforeClass
    public static void readInIndicies() throws IOException{

        int indexToUse = 5;

        String[] testIndiciesNames = {
            "com.ibm.ws.anno-jarV2.idx", //V2
            "jandex-2.0.6.Final-SNAPSHOT-jar.idx", //V2
            "com.ibm.websphere.appserver.api.basics-jar.idx", //V2
            "jandex-1.2.6.Final-SNAPSHOT-jar.idx", //V1
            "com.ibm.ws.anno-jarV1.idx", //V1
            "com.ibm.websphere.org.osgi.core-jar.idx" //V1
        };

        
        
        //open a stream to index and read in full index
        //connection = testIndex.openStream();
        connection = new FileInputStream(testIndiciesNames[indexToUse]);
        fullIndex = Jandex_Utils.basicReadIndex(connection);
        connection.close();
        
        //open a stream to index and read in index using the limited reader
        connection = new FileInputStream(testIndiciesNames[indexToUse]);
        smallIndex = Jandex_Utils.basicReadLimitedIndex(connection);
        connection.close();

        //retrieve the classes from both indicies
        originalClasses = fullIndex.getKnownClasses();
        limitedClasses = smallIndex.classes();

    }

    @Test
    public void testSameNumberOfClasses(){
        Assert.assertTrue(originalClasses.size() == limitedClasses.size());
        
    }

    @Test
    public void testSameFlags(){
        HashMap<String, Short> flags = new HashMap<String,Short>();
        for(org.jboss.jandex.ClassInfo temp : originalClasses){
            flags.put(temp.name().toString(), temp.flags());
        }

        Short placeholder;

        for(com.ibm.ws.anno.jandex.internal.ClassInfo temp: limitedClasses){
            placeholder = flags.get(temp.name().toString());
            if(placeholder == null){
                Assert.fail(temp.name().toString() + " is missing from the full index");
            }

            Assert.assertTrue("The flags are not the same for class " + temp.name().toString(), placeholder.shortValue() == temp.flags());
        }
    }

    
    @Test
    public void testSameNamesofClasses(){
        HashSet<String> names = new LinkedHashSet<>(originalClasses.size());

        //add the names from one of the indicies into a hashset
        for(org.jboss.jandex.ClassInfo temp: originalClasses){
            if(names.add(temp.name().toString()) == false){
                Assert.fail("Duplicate Class Names in full Index");
            }
        }

        //check the names from the other index against the hashset previously made
        for(com.ibm.ws.anno.jandex.internal.ClassInfo temp: limitedClasses){
            if(names.add(temp.name().toString()) != false){
                Assert.fail(temp.name().toString() + " - is not in both the full and small Index");
            }
        }
    }

    
    @Test
    public void testNumberOfFields(){
        HashMap<String, Integer> classesToFieldSize = new HashMap<String, Integer>();
        Integer size;
        int counter = 0;

        //store the size of the fields list for each class under the DotName.toString()
        for(org.jboss.jandex.ClassInfo temp: originalClasses){
            classesToFieldSize.put(temp.name().toString(), temp.fields().size());
        }

        //iterate over the classes in limited classes
        for(com.ibm.ws.anno.jandex.internal.ClassInfo temp: limitedClasses){
            size = classesToFieldSize.get(temp.name().toString());

            //check each class in the small index has a fields list and that the sizes match
            if(size == null)
                Assert.fail(temp.name().toString() + " - does not have a field size in full Index");
            else if(size.intValue() != temp.fields().size())
                Assert.fail(temp.name().toString() + " - does not have matching field sizes between small and full Index");
            else{
                counter++;
            }

            
        }
        //make sure the amount of correct class comparisons match the size of the classes
        Assert.assertTrue("The amount of field sizes matching should be the same as the number of classes", counter == originalClasses.size() && counter == limitedClasses.size());

    }
    
    @Test
    public void testSameNamesOfFields(){
        HashMap<String,List<String>> namesToClasses = new HashMap<String,List<String>>();
        List<String> placeholder;
        //List<com.ibm.ws.anno.jandex.internal.DotName> placeholder;

        for(com.ibm.ws.anno.jandex.internal.ClassInfo temp : limitedClasses){
            placeholder = new LinkedList<String>();
            
            for(com.ibm.ws.anno.jandex.internal.DotName fieldNames : temp.fields()){
                placeholder.add(fieldNames.toString());
            }

            namesToClasses.put(temp.name().toString(), placeholder);
        }

        for(org.jboss.jandex.ClassInfo temp: originalClasses){
            for(org.jboss.jandex.FieldInfo fields: temp.fields()){
                Assert.assertTrue("Limited List class " + temp.name().toString() + "does not have field " + fields.name() ,namesToClasses.get(temp.name().toString()).contains(fields.name()));
            }
        }
    }

    @Test
    public void testNumberOfClassAnnotation(){
        HashMap<String, Integer> classToAnnoSize = new HashMap<String, Integer>();
        for(org.jboss.jandex.ClassInfo temp: originalClasses){
            classToAnnoSize.put(temp.name().toString(), temp.classAnnotations().size());
        }

        Integer value;
        for(com.ibm.ws.anno.jandex.internal.ClassInfo temp: limitedClasses){
            value = classToAnnoSize.get(temp.name().toString());
            if(value == null){
                Assert.fail(temp.name().toString() + " class is missing from the full sized index");
            }
            else{
                if(value.intValue() != temp.classAnnotations().size()){
                    Assert.fail("Class Annotation sizes don't match for class - " + temp.name().toString()); //+ " ("+value.intValue()+","+temp.classAnnotations().size()+")" + "\n"+temp.classAnnotations().get(0).toString()
                    //+ "\n"+temp.getTest().getName().toString() + "\n"+temp.getTest().getTargetName().toString());
                }
            }
            
        }

    }

    @Test
    public void testSameClassAnnotations(){
        HashMap<String, List<String>> classToAnno = new HashMap<String, List<String>>();
        List<String> placeholder;
        for(org.jboss.jandex.ClassInfo temp: originalClasses){
            placeholder = new LinkedList<String>();
            for(org.jboss.jandex.AnnotationInstance temp2: temp.classAnnotations()){
                placeholder.add(temp2.name().toString());
            }
            classToAnno.put(temp.name().toString(),placeholder);
        }

        for(com.ibm.ws.anno.jandex.internal.ClassInfo temp: limitedClasses){
            placeholder = classToAnno.get(temp.name().toString());
            if(placeholder == null){
                Assert.fail(temp.name().toString() + " class is missing from the full sized index");
            }
            else{
                for(DotName temp2 : temp.classAnnotations()){
                    if(placeholder.contains(temp2.toString()) == false){
                        Assert.fail(temp.name().toString()+" class does not have annotation " + temp2.toString()+" in the full index");
                    }
                }
            }
            
            
        }
    }

    @Test
    public void testNumberOfMethods(){
        HashMap<String, Integer> classMethodSize = new HashMap<String,Integer>();
        for(org.jboss.jandex.ClassInfo temp: originalClasses){
            classMethodSize.put(temp.name().toString(), temp.methods().size());
        }

        Integer value;
        for(com.ibm.ws.anno.jandex.internal.ClassInfo temp: limitedClasses){
            value = classMethodSize.get(temp.name().toString());
            if(value == null){
                Assert.fail(temp.name().toString() + " class is missing from the full sized index");
            }
            else{
                if(value.intValue() != temp.methods().size()){
                    Assert.fail("Method Size is different for class " + temp.name().toString());
                }
            }
            
        }
    }

    @Test
    public void testSameNamesOfMethods(){
        HashMap<String, List<String>> classToMethods = new HashMap<String, List<String>>();
        List<String> placeholder;
        for(org.jboss.jandex.ClassInfo temp: originalClasses){
            placeholder = new LinkedList<String>();
            for(org.jboss.jandex.MethodInfo temp2: temp.methods()){
                placeholder.add(temp2.name());
            }
            classToMethods.put(temp.name().toString(), placeholder);
        }

        for(com.ibm.ws.anno.jandex.internal.ClassInfo temp: limitedClasses){
            placeholder = classToMethods.get(temp.name().toString());
            if(placeholder == null){
                Assert.fail(temp.name().toString() + " class is missing from the full sized index");
            }
            else{
                for(DotName temp2: temp.methods()){
                    if(placeholder.contains(temp2.toString()) == false){
                        Assert.fail("Full Index doesn't have method " + temp2.toString() + " in class "+temp.name().toString());
                    }
                }
            }
        }
    }

    @Test
    public void testSuperNamesAlign(){
        HashMap<String, String> classToSuper = new HashMap<String,String>();
        String placeholder;
        for(org.jboss.jandex.ClassInfo temp: originalClasses){
            classToSuper.put(temp.name().toString(), temp.superName().toString());
        }
        for(com.ibm.ws.anno.jandex.internal.ClassInfo temp: limitedClasses){
            placeholder = classToSuper.get(temp.name().toString());
            if(placeholder == null){
                Assert.fail(temp.name().toString() + " class is missing from the full sized index");
            }
            else{
                Assert.assertTrue("Super names don't match up for class " + temp.name().toString()
                                  ,placeholder.equals(temp.superName().toString()));
            }
        }

    }

    @Test
    public void testNumberOfInterfaces(){
        HashMap<String, Integer> classToInterfaceSize = new HashMap<String,Integer>();
        Integer placeholder;
        for(org.jboss.jandex.ClassInfo temp: originalClasses){
            classToInterfaceSize.put(temp.name().toString(), temp.interfaceNames().size());
        }

        for(com.ibm.ws.anno.jandex.internal.ClassInfo temp: limitedClasses){
            placeholder = classToInterfaceSize.get(temp.name().toString());
            if(placeholder == null){
                Assert.fail(temp.name().toString() + " class is missing from the full sized index");
            }
            else{
                Assert.assertTrue("Interface Sizes for class "+temp.name().toString() +" do not match", temp.interfaceNames().length == placeholder.intValue());
            }
        }
    }

    @Test
    public void testInterfacesHaveSameNames(){
        HashMap<String,List<String>> classToInterfaceName = new HashMap<String,List<String>>();
        List<String> placeholder;
        for(org.jboss.jandex.ClassInfo temp: originalClasses){
            placeholder = new LinkedList<String>();
            for(org.jboss.jandex.DotName temp2 : temp.interfaceNames()){
                placeholder.add(temp2.toString());
            }
            classToInterfaceName.put(temp.name().toString(), placeholder);
        }

        for(com.ibm.ws.anno.jandex.internal.ClassInfo temp: limitedClasses){
            placeholder = classToInterfaceName.get(temp.name().toString());
            if(placeholder == null){
                Assert.fail(temp.name().toString() + " class is missing from the full sized index");
            }
            else{
                for(com.ibm.ws.anno.jandex.internal.DotName temp2: temp.interfaceNames()){
                    Assert.assertTrue(placeholder.remove(temp2.toString()));
                }

                Assert.assertTrue("There are more interfaces in full index for class" + temp.name().toString(), placeholder.size() == 0);
            }
        }
    }


    
}