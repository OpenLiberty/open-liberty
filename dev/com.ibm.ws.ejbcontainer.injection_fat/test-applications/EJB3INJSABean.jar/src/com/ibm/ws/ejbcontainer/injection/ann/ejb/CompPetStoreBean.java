/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.ann.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.rmi.RemoteException;

import javax.annotation.Resource;
import javax.ejb.CreateException;
import javax.ejb.EJB;
import javax.ejb.EJBs;
import javax.ejb.LocalHome;
import javax.ejb.Remove;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;

/**
 * Component/Compatibility Stateful Bean implementation for testing EJB injection.
 **/
@EJBs({
        @EJB(name = "ejb/dogCompCls", beanName = "CompDog", beanInterface = DogEJBLocalHome.class),
        @EJB(name = "ejb/dog30Cls", beanName = "Dog", beanInterface = DogLocal.class)
})
@Stateful(name = "CompPetStore")
@LocalHome(PetStoreEJBLocalHome.class)
public class CompPetStoreBean implements SessionBean {
    private static final long serialVersionUID = -1011833403953214385L;
    private static final String PASSED = "Passed";

    @Resource
    private SessionContext ctx;

    @Resource
    private SessionContext ivContext;

    @EJB(beanInterface = CatEJBLocalHome.class)
    private CatEJBLocalHome catFldHome;

    @EJB
    private DogEJBLocalHome dogFldHome;

    @EJB(beanName = "CompDog")
    private DogEJBLocalHome dogFldHome2;

    private CatEJBLocalHome catMthdRefHome;

    @EJB(name = "ejb/catCompMthd")
    public void setCatCompMthd(CatEJBLocalHome catMthdRefHome) {
        this.catMthdRefHome = catMthdRefHome;
    }

    //  3.0 injections

    @EJB(beanInterface = DogLocal.class)
    private AnimalLocal dogLikeAnimal;

    //private DogLocal castedDogLikeAnimal = (DogLocal)dogLikeAnimal;

    @EJB(beanName = "Dog")
    private AnimalLocal dogLikeAnimal2;

    @EJB(beanName = "Cat")
    private CatLocal cat;

    @EJB
    private DogLocal dog;

    private CatLocal catMthd;

    @EJB(name = "ejb/cat30Mthd")
    public void setCatMthd(CatLocal catMthd) {
        this.catMthd = catMthd;
    }

    //  Component methods
    public String getDogClsComp(int testpoint) {
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "Session context was injected.", ctx);
        ++testpoint;

        try {

            // Lookup the component stateful bean home using an injected session context,
            // using the ENC JNDI entry added by class level injection
            DogEJBLocalHome injectedHome = (DogEJBLocalHome) ctx.lookup("ejb/dogCompCls");
            DogEJBLocal injectedBean = injectedHome.create();
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "Looking up the stateful bean home using an injected session context using the " +
                          "ENC JNDI entry: \"ejb/dogCompCls\" added by the class level injection and then creating the bean.", injectedBean);
            ++testpoint;

            String expected = "I am a dog.";
            // Call a method on the bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Expected: " + expected + " Received: " + injectedBean.whatAmI() +
                         " If they match the bean was successfully injected.", expected, injectedBean.whatAmI());
            ++testpoint;

            // Call a superclass method to ensure we can access it from the bean that extends it.
            String expected2 = "Animal: any member of the kingdom Animalia.";
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Expected2: " + expected2 + " Received2: " + injectedBean.animalDef() +
                         " If they match the bean was successfully injected.", expected2, injectedBean.animalDef());
            ++testpoint;
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: IllegalArguemntException occured.  This likely means the lookup failed. " +
                 "Exception: " + iae);
            ++testpoint;
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: unexpected exception:(" + t.getClass().getSimpleName() +
                 ") : " + t.getMessage());
            ++testpoint;
        }

        return PASSED;
    }

    public String getCatMthdComp(int testpoint) {
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "Session context was injected.", ctx);
        ++testpoint;

        try {
            String expected = "Give me milk and tuna.";
            // Create a bean from the injected home ref
            CatEJBLocal catMthdRefBean = catMthdRefHome.create();

            // Call a method on the injectedRef bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean catMthdRefBean --> Expected: " + expected + " Received: " + catMthdRefBean.careInst() +
                         " If they match the bean was successfully injected.", expected, catMthdRefBean.careInst());
            ++testpoint;

            // Lookup the stateful bean home using the ENC JNDI entry that was
            // specified in the method level injection
            CatEJBLocalHome injectedMthHome = (CatEJBLocalHome) ctx.lookup("ejb/catCompMthd");
            CatEJBLocal injectedMthBean = injectedMthHome.create();

            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "Looking up the stateful bean home using an injected session context using the " +
                          "ENC JNDI entry: \"ejb/catCompMthd\" added by the method level injection and " +
                          "then creating the bean.", injectedMthBean);
            ++testpoint;

            String expected2 = "Cat: any of several carnivores of the family Felidae.";
            // Call a method on the injectedMthBean bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean injejctedMthBean --> Expected2: " + expected2 + " Received2: " + injectedMthBean.catDef() +
                         " If they match the bean was successfully injected.", expected2, injectedMthBean.catDef());
            ++testpoint;

            // Call a superclass method to ensure we can access it from the bean that extends it.
            String expected3 = "Animal: any member of the kingdom Animalia.";
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean injectedMthBean --> Expected3: " + expected3 + " Received3: " + injectedMthBean.animalDef() +
                         " If they match the bean was successfully injected.", expected3, injectedMthBean.animalDef());
            ++testpoint;

        } catch (IllegalArgumentException iae) {
            iae.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: IllegalArguemntException occured.  This likely means the lookup failed. " +
                 "Exception: " + iae);
            ++testpoint;
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: unexpected exception:(" + t.getClass().getSimpleName() +
                 ") : " + t.getMessage());
            ++testpoint;
        }

        return PASSED;
    }

    public String getCatFldComp(int testpoint) {
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "Session context was injected.", ctx);
        ++testpoint;

        try {
            String expected = "Just a ball of string.";
            // Create a bean from the injected home ref
            CatEJBLocal catFldRefBean = catFldHome.create();

            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "Successfully created a catFldRefBean.", catFldRefBean);
            ++testpoint;

            // Call a method on the injectedRef bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean catFldRefBean --> Expected: " + expected + " Received: " + catFldRefBean.favToy() +
                         " If they match the bean was successfully injected.", expected, catFldRefBean.favToy());
            ++testpoint;

            // Lookup the stateful bean home using the ENC JNDI entry that was
            // specified in the field level injection
            CatEJBLocalHome injectedFldHome = (CatEJBLocalHome) ctx.lookup("com.ibm.ws.ejbcontainer.injection.ann.ejb.CompPetStoreBean/catFldHome");
            CatEJBLocal injectedFldBean = injectedFldHome.create();

            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "Looking up the stateful bean home using an injected session context using the " +
                          "ENC JNDI entry: \"com.ibm.ws.ejbcontainer.injection.ann.ejb.CompPetStoreBean/catFldHome\" " +
                          "added by default at the field level injection and then creating the bean.", injectedFldBean);
            ++testpoint;

            String expected2 = "I am a cat.";
            // Call a method on the injectedFldBean bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean injectedFldBean --> Expected2: " + expected2 + " Received2: " + injectedFldBean.whatAmI() +
                         " If they match the bean was successfully injected.", expected2, injectedFldBean.whatAmI());
            ++testpoint;

            // Call a superclass method to ensure we can access it from the bean that extends it.
            String expected3 = "Animal: any member of the kingdom Animalia.";
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean injectedFldBean --> Expected3: " + expected3 + " Received3: " + injectedFldBean.animalDef() +
                         " If they match the bean was successfully injected.", expected3, injectedFldBean.animalDef());
            ++testpoint;

        } catch (IllegalArgumentException iae) {
            iae.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: IllegalArguemntException occured.  This likely means the lookup failed. " +
                 "Exception: " + iae);
            ++testpoint;
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: unexpected exception:(" + t.getClass().getSimpleName() +
                 ") : " + t.getMessage());
            ++testpoint;
        }

        return PASSED;
    }

    public String getDogFldComp(int testpoint) {
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "Session context was injected.", ctx);
        ++testpoint;

        try {
            String expected = "Just a bone.";
            // Create a bean from the injected home ref
            DogEJBLocal dogFldRefBean = dogFldHome.create();

            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "Successfully created a dogFldRefBean.", dogFldRefBean);
            ++testpoint;

            // Call a method on the injectedRef bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean dogFldRefBean --> Expected: " + expected + " Received: " + dogFldRefBean.favToy() +
                         " If they match the bean was successfully injected.", expected, dogFldRefBean.favToy());
            ++testpoint;

            // Lookup the stateful bean home using the ENC JNDI entry that was
            // specified in the field level injection
            DogEJBLocalHome injectedFldHome = (DogEJBLocalHome) ctx.lookup("com.ibm.ws.ejbcontainer.injection.ann.ejb.CompPetStoreBean/dogFldHome");
            DogEJBLocal injectedFldBean = injectedFldHome.create();

            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "Looking up the stateful bean home using an injected session context using the " +
                          "ENC JNDI entry: \"com.ibm.ws.ejbcontainer.injection.ann.ejb.CompPetStoreBean/dogFldHome\" " +
                          "added by default at the field level injection and then creating the bean.", injectedFldBean);
            ++testpoint;

            String expected2 = "I am a dog.";
            // Call a method on the injectedFldBean bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean injectedFldBean --> Expected2: " + expected2 + " Received2: " + injectedFldBean.whatAmI() +
                         " If they match the bean was successfully injected.", expected2, injectedFldBean.whatAmI());
            ++testpoint;

            // Call a superclass method to ensure we can access it from the bean that extends it.
            String expected3 = "Animal: any member of the kingdom Animalia.";
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean injectedFldBean --> Expected3: " + expected3 + " Received3: " + injectedFldBean.animalDef() +
                         " If they match the bean was successfully injected.", expected3, injectedFldBean.animalDef());
            ++testpoint;

        } catch (IllegalArgumentException iae) {
            iae.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: IllegalArguemntException occured.  This likely means the lookup failed. " +
                 "Exception: " + iae);
            ++testpoint;
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: unexpected exception:(" + t.getClass().getSimpleName() +
                 ") : " + t.getMessage());
            ++testpoint;
        }

        return PASSED;
    }

    public String getDogFldComp2(int testpoint) {
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "Session context was injected.", ctx);
        ++testpoint;

        try {
            String expected = "Give me water and Puppy Chow.";
            // Create a bean from the injected home ref
            DogEJBLocal dogFldRefBean2 = dogFldHome2.create();

            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "Successfully created a dogFldRefBean2.", dogFldRefBean2);
            ++testpoint;

            // Call a method on the injectedRef bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean dogFldRefBean2 --> Expected: " + expected + " Received: " + dogFldRefBean2.careInst() +
                         " If they match the bean was successfully injected.", expected, dogFldRefBean2.careInst());
            ++testpoint;

            // Lookup the stateful bean home using the ENC JNDI entry that was
            // specified in the field level injection
            DogEJBLocalHome injectedFldHome = (DogEJBLocalHome) ctx.lookup("com.ibm.ws.ejbcontainer.injection.ann.ejb.CompPetStoreBean/dogFldHome2");
            DogEJBLocal injectedFldBean = injectedFldHome.create();

            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "Looking up the stateful bean home using an injected session context using the " +
                          "ENC JNDI entry: \"com.ibm.ws.ejbcontainer.injection.ann.ejb.CompPetStoreBean/dogFldHome2\" " +
                          "added by default at the field level injection and then creating the bean.", injectedFldBean);
            ++testpoint;

            String expected2 = "Dog: any carnivore of the dogfamily Canidae.";
            // Call a method on the injectedFldBean bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean injectedFldBean --> Expected2: " + expected2 + " Received2: " + injectedFldBean.dogDef() +
                         " If they match the bean was successfully injected.", expected2, injectedFldBean.dogDef());
            ++testpoint;

            // Call a superclass method to ensure we can access it from the bean that extends it.
            String expected3 = "Animal: any member of the kingdom Animalia.";
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean injectedFldBean --> Expected3: " + expected3 + " Received3: " + injectedFldBean.animalDef() +
                         " If they match the bean was successfully injected.", expected3, injectedFldBean.animalDef());
            ++testpoint;

        } catch (IllegalArgumentException iae) {
            iae.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: IllegalArguemntException occured.  This likely means the lookup failed. " +
                 "Exception: " + iae);
            ++testpoint;
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: unexpected exception:(" + t.getClass().getSimpleName() +
                 ") : " + t.getMessage());
            ++testpoint;
        }

        return PASSED;
    }

    // 3.0 methods
    public String getDogCls(int testpoint) {
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "Session context was injected.", ctx);
        ++testpoint;

        try {
            String expected = "I am a dog.";

            DogLocal injectedBean = (DogLocal) ctx.lookup("ejb/dog30Cls");
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "Successfully completed the lookup of \"ejb/dog30Cls\" and created the bean \"injectedBean\".", injectedBean);
            ++testpoint;

            // Call a method on the bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Expected: " + expected + " Received: " + injectedBean.whatAmI() +
                         " If they match the bean was successfully injected.", expected, injectedBean.whatAmI());
            ++testpoint;

            // Call a superclass method to ensure we can access it from the bean that extends it.
            String expected2 = "Animal: any member of the kingdom Animalia.";
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Expected2: " + expected2 + " Received2: " + injectedBean.animalDef() +
                         " If they match the bean was successfully injected.", expected2, injectedBean.animalDef());
            ++testpoint;
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: IllegalArguemntException occured.  This likely means the lookup failed. " +
                 "Exception: " + iae);
            ++testpoint;
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: unexpected exception:(" + t.getClass().getSimpleName() +
                 ") : " + t.getMessage());
            ++testpoint;
        }

        return PASSED;
    }

    public String getCatMthd(int testpoint) {
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "Session context was injected.", ctx);
        ++testpoint;

        try {
            String expected = "Give me milk and tuna.";

            // Call a method on the injectedRef bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean catMthd --> Expected: " + expected + " Received: " + catMthd.careInst() +
                         " If they match the bean was successfully injected.", expected, catMthd.careInst());
            ++testpoint;

            // Lookup the stateful bean home using the ENC JNDI entry that was
            // specified in the method level injection
            CatLocal injectedMthBean = (CatLocal) ctx.lookup("ejb/cat30Mthd");

            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "Looking up the stateful bean using an injected session context using the " +
                          "ENC JNDI entry: \"ejb/cat30Mthd\" added by the method level injection and " +
                          "then creating the bean.", injectedMthBean);
            ++testpoint;

            // Call a superclass method to ensure we can access it from the bean that extends it.
            String expected2 = "Animal: any member of the kingdom Animalia.";
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean injectedMthBean --> Expected2: " + expected2 + " Received2: " + injectedMthBean.animalDef() +
                         " If they match the bean was successfully injected.", expected2, injectedMthBean.animalDef());
            ++testpoint;

            // Call a method on the injectedMthBean bean to ensure that the ref is valid
            String expected3 = "I am a cat.";
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean injectedMthBean --> Expected: " + expected3 + " Received: " + injectedMthBean.whatAmI() +
                         " If they match the bean was successfully injected.", expected3, injectedMthBean.whatAmI());
            ++testpoint;

        } catch (IllegalArgumentException iae) {
            iae.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: IllegalArguemntException occured.  This likely means the lookup failed. " +
                 "Exception: " + iae);
            ++testpoint;
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: unexpected exception:(" + t.getClass().getSimpleName() +
                 ") : " + t.getMessage());
            ++testpoint;
        }

        return PASSED;
    }

    public String getDogLikeAnml(int testpoint) {
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "Session context was injected.", ctx);
        ++testpoint;

        try {
            String expected = "Just a bone.";

            // Call a method on the injectedRef bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean dogLikeAnimal --> Expected: " + expected + " Received: " + dogLikeAnimal.favToy() +
                         " If they match the bean was successfully injected with the beanInterface attribute specified.", expected, dogLikeAnimal.favToy());
            ++testpoint;

            // Lookup the stateful bean using the ENC JNDI entry that should
            // have been defaulted in the field level injection
            AnimalLocal dogAnml = (AnimalLocal) ctx.lookup("com.ibm.ws.ejbcontainer.injection.ann.ejb.CompPetStoreBean/dogLikeAnimal");

            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "Looking up the stateful bean using an injected session context using the " +
                          "default ENC JNDI entry: \"com.ibm.ws.ejbcontainer.injection.ann.ejb.CompPetStoreBean/dogLikeAnimal\" " +
                          "added by the field level injection and then creating the bean.", dogAnml);
            ++testpoint;

            String expected2 = "I am a dog.";
            // Call a method on the dogAnml bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean dogAnml --> Expected2: " + expected2 + " Received2: " + dogAnml.whatAmI() +
                         " If they match the bean was successfully injected.", expected2, dogAnml.whatAmI());
            ++testpoint;

            // Call a superclass method to ensure we can access it from the bean that extends it.
            String expected3 = "Animal: any member of the kingdom Animalia.";
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean dogAnml --> Expected3: " + expected3 + " Received3: " + dogAnml.animalDef() +
                         " If they match the bean was successfully injected.", expected3, dogAnml.animalDef());
            ++testpoint;

            // Call a method on the casted instance of the injectedRef bean to ensure that the casted ref is valid
            // Lookup the stateful bean using the ENC JNDI entry that should
            // have been defaulted in the field level injection
            AnimalLocal dogAnml2 = (AnimalLocal) ctx.lookup("com.ibm.ws.ejbcontainer.injection.ann.ejb.CompPetStoreBean/dogLikeAnimal");

            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "Looking up the stateful bean using an injected session context using the " +
                          "default ENC JNDI entry: \"com.ibm.ws.ejbcontainer.injection.ann.ejb.CompPetStoreBean/dogLikeAnimal\" " +
                          "added by the field level injection and then creating the bean --> dogAnml2.", dogAnml2);
            ++testpoint;

            DogLocal castedDogLikeAnimal = (DogLocal) dogAnml2;
            String expected4 = "Dog: any carnivore of the dogfamily Canidae.";
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean castedDogLikeAnimal --> Expected4: " + expected4 + " Received4: " + castedDogLikeAnimal.dogDef() +
                         " If they match the bean was successfully injected with the beanInterface attribute specified.", expected4, castedDogLikeAnimal.dogDef());
            ++testpoint;
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: IllegalArguemntException occured.  This likely means the lookup failed. " +
                 "Exception: " + iae);
            ++testpoint;
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: unexpected exception:(" + t.getClass().getSimpleName() +
                 ") : " + t.getMessage());
            ++testpoint;
        }

        return PASSED;
    }

    public String getDogLikeAnml2(int testpoint) {
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "Session context was injected.", ctx);
        ++testpoint;

        try {
            String expected = "Give me water and Puppy Chow.";

            // Call a method on the injectedRef bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean dogLikeAnimal2 --> Expected: " + expected + " Received: " + dogLikeAnimal2.careInst() +
                         " If they match the bean was successfully injected with the beanName attribute specified.", expected, dogLikeAnimal2.careInst());
            ++testpoint;

            // Lookup the stateful bean using the ENC JNDI entry that should
            // have been defaulted in the field level injection
            AnimalLocal dogAnml = (AnimalLocal) ctx.lookup("com.ibm.ws.ejbcontainer.injection.ann.ejb.CompPetStoreBean/dogLikeAnimal2");

            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "Looking up the stateful bean using an injected session context using the " +
                          "default ENC JNDI entry: \"com.ibm.ws.ejbcontainer.injection.ann.ejb.CompPetStoreBean/dogLikeAnimal2\" " +
                          "added by the field level injection and then creating the bean.", dogAnml);
            ++testpoint;

            String expected2 = "I am a dog.";
            // Call a method on the dogAnml bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean dogAnml --> Expected2: " + expected2 + " Received2: " + dogAnml.whatAmI() +
                         " If they match the bean was successfully injected.", expected2, dogAnml.whatAmI());
            ++testpoint;

            // Call a superclass method to ensure we can access it from the bean that extends it.
            String expected3 = "Animal: any member of the kingdom Animalia.";
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean dogAnml --> Expected3: " + expected3 + " Received3: " + dogAnml.animalDef() +
                         " If they match the bean was successfully injected.", expected3, dogAnml.animalDef());
            ++testpoint;
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: IllegalArguemntException occured.  This likely means the lookup failed. " +
                 "Exception: " + iae);
            ++testpoint;
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: unexpected exception:(" + t.getClass().getSimpleName() +
                 ") : " + t.getMessage());
            ++testpoint;
        }

        return PASSED;
    }

    public String getCatFld(int testpoint) {
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "Session context was injected.", ctx);
        ++testpoint;

        try {
            String expected = "Just a ball of string.";
            // Call a method on the injectedRef bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean cat --> Expected: " + expected + " Received: " + cat.favToy() +
                         " If they match the bean was successfully injected.", expected, cat.favToy());
            ++testpoint;

            // Lookup the stateful bean home using the ENC JNDI entry that was
            // specified in the field level injection
            CatLocal injectedFldBean = (CatLocal) ctx.lookup("com.ibm.ws.ejbcontainer.injection.ann.ejb.CompPetStoreBean/cat");

            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "Looking up the stateful bean using an injected session context using the " +
                          "ENC JNDI entry: \"com.ibm.ws.ejbcontainer.injection.ann.ejb.CompPetStoreBean/cat\" " +
                          "added by default at the field level injection and then creating the bean.", injectedFldBean);
            ++testpoint;

            String expected2 = "Cat: any of several carnivores of the family Felidae.";
            // Call a method on the injectedFldBean bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean injectedFldBean --> Expected2: " + expected2 + " Received2: " + injectedFldBean.catDef() +
                         " If they match the bean was successfully injected.", expected2, injectedFldBean.catDef());
            ++testpoint;

            // Call a superclass method to ensure we can access it from the bean that extends it.
            String expected3 = "Animal: any member of the kingdom Animalia.";
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean injectedFldBean --> Expected3: " + expected3 + " Received3: " + injectedFldBean.animalDef() +
                         " If they match the bean was successfully injected.", expected3, injectedFldBean.animalDef());
            ++testpoint;

        } catch (IllegalArgumentException iae) {
            iae.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: IllegalArguemntException occured.  This likely means the lookup failed. " +
                 "Exception: " + iae);
            ++testpoint;
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: unexpected exception:(" + t.getClass().getSimpleName() +
                 ") : " + t.getMessage());
            ++testpoint;
        }

        return PASSED;
    }

    public String getDogFld(int testpoint) {
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "Session context was injected.", ctx);
        ++testpoint;

        try {
            String expected = "Just a bone.";

            // Call a method on the injectedRef bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean dog --> Expected: " + expected + " Received: " + dog.favToy() +
                         " If they match the bean was successfully injected.", expected, dog.favToy());
            ++testpoint;

            // Lookup the stateful bean home using the ENC JNDI entry that was
            // specified in the field level injection
            DogLocal injectedFldBean = (DogLocal) ctx.lookup("com.ibm.ws.ejbcontainer.injection.ann.ejb.CompPetStoreBean/dog");

            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "Looking up the stateful bean using an injected session context using the " +
                          "ENC JNDI entry: \"com.ibm.ws.ejbcontainer.injection.ann.ejb.CompPetStoreBean/dog\" " +
                          "added by default at the field level injection and then creating the bean.", injectedFldBean);
            ++testpoint;

            String expected2 = "Dog: any carnivore of the dogfamily Canidae.";
            // Call a method on the injectedFldBean bean to ensure that the ref is valid
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean injectedFldBean --> Expected2: " + expected2 + " Received2: " + injectedFldBean.dogDef() +
                         " If they match the bean was successfully injected.", expected2, injectedFldBean.dogDef());
            ++testpoint;

            // Call a superclass method to ensure we can access it from the bean that extends it.
            String expected3 = "Animal: any member of the kingdom Animalia.";
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "Bean injectedFldBean --> Expected3: " + expected3 + " Received3: " + injectedFldBean.animalDef() +
                         " If they match the bean was successfully injected.", expected3, injectedFldBean.animalDef());
            ++testpoint;

        } catch (IllegalArgumentException iae) {
            iae.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: IllegalArguemntException occured.  This likely means the lookup failed. " +
                 "Exception: " + iae);
            ++testpoint;
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            fail(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                 "Failed: unexpected exception:(" + t.getClass().getSimpleName() +
                 ") : " + t.getMessage());
            ++testpoint;
        }

        return PASSED;
    }

    /** Remove method **/
    @Remove
    public void finish() {
        // Intentionally blank
    }

    // Provided for compatibility with SLSB
    public void discardInstance() {
        finish();
    }

    public CompPetStoreBean() {
        // Intentionally blank
    }

    public void ejbCreate() throws CreateException, RemoteException {
        // Intentionally blank
    }

    @Override
    public void ejbRemove() throws RemoteException {
        // Intentionally blank
    }

    @Override
    public void ejbActivate() throws RemoteException {
        // Intentionally blank
    }

    @Override
    public void ejbPassivate() throws RemoteException {
        // Intentionally blank
    }

    @Override
    public void setSessionContext(SessionContext sc) {
        ivContext = sc;
    }
}
