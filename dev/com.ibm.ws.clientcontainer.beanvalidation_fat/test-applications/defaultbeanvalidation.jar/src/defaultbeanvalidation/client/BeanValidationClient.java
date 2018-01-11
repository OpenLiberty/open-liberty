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
package defaultbeanvalidation.client;

import javax.annotation.Resource;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import defaultbeanvalidation.test.DefaultBeanValidation;
import defaultbeanvalidation.test.DefaultBeanValidationInjection;

public class BeanValidationClient {

	private static boolean passed = true;

    @Resource(name = "TestValidatorFactory")
    private static ValidatorFactory ivVFactory;

    @Resource(name = "TestValidator")
    private static Validator ivValidator;
    
    public static void main(String[] args) {
        System.out.println("\nEntering Bval BeanValidation Application Client.");
        
        defaultBeanValidationTests();
        defaultBeanValidationInjectionTests();
        
        if(passed) {
        	System.out.println("\nDefaultbeanvalidation Application Client Completed.");
        }
    }
    
    private static void defaultBeanValidationTests() {  
        DefaultBeanValidation defaultBeanValidation = new DefaultBeanValidation(); 
        try {
        	defaultBeanValidation.testDefaultBuildDefaultValidatorFactory();
        	System.out.println("DefaultBeanValidation.testDefaultBuildDefaultValidatorFactory passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("DefaultBeanValidation.testDefaultBuildDefaultValidatorFactory failed.\n");
            ex.printStackTrace();
        }

        try {
        	defaultBeanValidation.testDefaultLookupJavaCompValidator();
        	System.out.println("DefaultBeanValidation.testDefaultLookupJavaCompValidator passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("DefaultBeanValidation.testDefaultLookupJavaCompValidator failed.\n");
            ex.printStackTrace();
        }
        
        try {
        	defaultBeanValidation.testDefaultLookupJavaCompValidatorFactory();
        	System.out.println("DefaultBeanValidation.testDefaultLookupJavaCompValidatorFactory passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("DefaultBeanValidation.testDefaultLookupJavaCompValidatorFactory failed.\n");
            ex.printStackTrace();
        }
        
        try {
        	defaultBeanValidation.testDefaultValidatingBeanWithConstraints();
        	System.out.println("DefaultBeanValidation.testDefaultValidatingBeanWithConstraints passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("DefaultBeanValidation.testDefaultValidatingBeanWithConstraints failed.\n");
            ex.printStackTrace();
        }
        
        try {
        	defaultBeanValidation.testDefaultValidatingBeanWithConstraintsToFail();
        	System.out.println("DefaultBeanValidation.testDefaultValidatingBeanWithConstraintsToFail passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("DefaultBeanValidation.testDefaultValidatingBeanWithConstraintsToFail failed.\n");
            ex.printStackTrace();
        }
    }
    
    private static void defaultBeanValidationInjectionTests() {  
        DefaultBeanValidationInjection defaultBeanValidationInjection = new DefaultBeanValidationInjection(); 
        try {
        	defaultBeanValidationInjection.testDefaultInjectionAndLookupValidator(ivValidator);
        	System.out.println("DefaultBeanValidationInjection.testDefaultInjectionAndLookupValidator passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("DefaultBeanValidationInjection.testDefaultInjectionAndLookupValidator failed.\n");
            ex.printStackTrace();
        }
        
        try {
        	defaultBeanValidationInjection.testDefaultInjectionAndLookupValidatorFactory(ivVFactory);
        	System.out.println("DefaultBeanValidationInjection.testDefaultInjectionAndLookupValidatorFactory passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("DefaultBeanValidationInjection.testDefaultInjectionAndLookupValidatorFactory failed.\n");
            ex.printStackTrace();
        }
    }
}
