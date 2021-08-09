/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package apachebvalconfig.client;

import javax.annotation.Resource;
import javax.validation.ValidatorFactory;

import apachebvalconfig.test.BeanValidation;

public class BeanValidationClient {

    @Resource
    static ValidatorFactory validatorFactory;
    
    public static void main(String[] args) {
        System.out.println("\nEntering Bval BeanValidation Application Client.");
        boolean passed = true;
        
        BeanValidation beanValidation = new BeanValidation(); 
        try {
        	beanValidation.testApacheBvalImplClassVisibility();
        	System.out.println("BeanValidation.testApacheBvalImplClassVisibility passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BeanValidation.testApacheBvalImplClassVisibility failed.\n");
            ex.printStackTrace();
        }

        try {
        	beanValidation.testBuildApacheConfiguredValidatorFactory(validatorFactory);
        	System.out.println("BeanValidation.testBuildApacheConfiguredValidatorFactory passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BeanValidation.testBuildApacheConfiguredValidatorFactory failed.\n");
            ex.printStackTrace();
        }
        
        if(passed) {
        	System.out.println("\nApacheBvalConfig Application Client Completed.");
        }
    }
}
