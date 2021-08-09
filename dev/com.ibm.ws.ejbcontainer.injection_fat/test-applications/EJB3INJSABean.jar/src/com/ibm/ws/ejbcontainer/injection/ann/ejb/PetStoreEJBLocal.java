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

import javax.ejb.EJBLocalObject;

/**
 * Compatibility EJBLocal interface for CompPetStoreBean
 **/
public interface PetStoreEJBLocal extends EJBLocalObject {
    //   methods that test an injected component bean
    /**
     * Looks up the component stateful bean home using an injected session context that is
     * using the ENC JNDI entry specified by class level injection. Uses this home to create
     * a compEJB and then calls this bean's whatAmI() method and returns the results. It also
     * calls a superclass method, animalDef(), to ensure we can access it from the bean
     * that extends it.
     */
    public String getDogClsComp(int testpoint);

    /**
     * The name attribute is specifed in the EJB injection annotation.
     *
     * Creates a component bean from the method level injected EJB home. This bean then calls
     * the careInst() and checks for expected results. Next, this method looks up the component
     * stateful bean home using an injected session context that is using the ENC JNDI entry
     * specified by method level injection. Then uses this home to create a compEJB and then
     * calls this bean's whatAmI() method and returns the results. It also
     * calls a superclass method, animalDef(), to ensure we can access it from the bean
     * that extends it.
     */
    public String getCatMthdComp(int testpoint);

    /**
     * The beanInterface attribute is specifed in the EJB injection annotation.
     *
     * Creates a component bean from the field level injected EJB home. This bean then calls
     * the favToy() and checks for expected results. Next, this method looks up the component
     * stateful bean home using an injected session context that is using the default ENC JNDI
     * entry created at the field level injection. Then uses this home to create a compEJB and then
     * calls this bean's whatAmI() method and returns the results. It also
     * calls a superclass method, animalDef(), to ensure we can access it from the bean
     * that extends it.
     */
    public String getCatFldComp(int testpoint);

    /**
     * Tests auto-link (no attributes specified in the EJB injection annotation).
     *
     * Creates a component bean from the field level injected EJB home. This bean then calls
     * the favToy() and checks for expected results. Next, this method looks up the component
     * stateful bean home using an injected session context that is using the default ENC JNDI
     * entry created at the field level injection. Then uses this home to create a compEJB and then
     * calls this bean's whatAmI() method and returns the results. It also
     * calls a superclass method, animalDef(), to ensure we can access it from the bean
     * that extends it.
     */
    public String getDogFldComp(int testpoint);

    /**
     * Tests the beanName attribute specified in the EJB injection annotation.
     *
     * Creates a component bean from the field level injected EJB home. This bean then calls
     * the careInst() and checks for expected results. Next, this method looks up the component
     * stateful bean home using an injected session context that is using the default ENC JNDI
     * entry created at the field level injection. Then uses this home to create a compEJB and then
     * calls this bean's dogDef() method and returns the results. It also
     * calls a superclass method, animalDef(), to ensure we can access it from the bean
     * that extends it.
     */
    public String getDogFldComp2(int testpoint);

    //   methods that test an injected 3.0 bean

    /**
     * Looks up the 3.0 stateful bean using an injected session context that is
     * using the ENC JNDI entry specified by class level injection. Then calls
     * this bean's whatAmI() method and returns the results. It also
     * calls a superclass method, animalDef(), to ensure we can access it from the bean
     * that extends it.
     */
    public String getDogCls(int testpoint); // d452259

    /**
     * The name attribute is specifed in the EJB method level(setter method) injection annotation.
     *
     * Uses the injected 3.0 bean to call the careInst() and checks for expected results.
     * Next, this method looks up the stateful bean using an injected session context
     * that is using the ENC JNDI entry specified at the field level injection
     * and then calls this bean's whatAmI() method and returns the results. It also
     * calls a superclass method, animalDef(), to ensure we can access it from the bean
     * that extends it.
     */
    public String getCatMthd(int testpoint);

    /**
     * Does a field level EJB injection with attribute beanInterface=DogLocal on
     * AnimalLocal dogLikeAnimal to verify that we get dog like responses from
     * dogLikeAnimal and not generic animal responses.
     *
     * Uses the injected 3.0 bean to call the favToy() and checks for expected results.
     * Next, this method looks up the stateful bean using an injected session context
     * that is using the ENC JNDI entry specified at the field level injection
     * and then calls this bean's whatAmI() method and returns the results. It also
     * calls a superclass method, animalDef(), to ensure we can access it from the bean
     * that extends it.
     */
    public String getDogLikeAnml(int testpoint);

    /**
     * Does a field level EJB injection with attribute beanName=Dog on
     * AnimalLocal dogLikeAnimal to verify that we get dog like responses from
     * dogLikeAnimal and not generic animal responses.
     *
     * Uses the injected 3.0 bean to call the careInst() and checks for expected results.
     * Next, this method looks up the stateful bean using an injected session context
     * that is using the ENC JNDI entry specified at the field level injection
     * and then calls this bean's whatAmI() method and returns the results. It also
     * calls a superclass method, animalDef(), to ensure we can access it from the bean
     * that extends it.
     */
    public String getDogLikeAnml2(int testpoint);

    /**
     * The beanName attribute is specified in the EJB injection annotation.
     *
     * Uses the injected 3.0 bean to call the favToy() and checks for expected results.
     * Next, this method looks up the stateful bean using an injected session context
     * that is using the default ENC JNDI entry created at the field level injection
     * and then calls this bean's catDef() method and returns the results. It also
     * calls a superclass method, animalDef(), to ensure we can access it from the bean
     * that extends it.
     */
    public String getCatFld(int testpoint);

    /**
     * Tests auto-link (no attributes specified in the EJB injection annotation).
     *
     * Uses the injected 3.0 bean to call the favToy() and checks for expected results.
     * Next, this method looks up the stateful bean using an injected session context
     * that is using the default ENC JNDI entry created at the field level injection
     * and then calls this bean's dogDef() method and returns the results. It also
     * calls a superclass method, animalDef(), to ensure we can access it from the bean
     * that extends it.
     */
    public String getDogFld(int testpoint);

    /**
     * Clean up the bean if it is a SFSB
     */
    public void finish();
}
