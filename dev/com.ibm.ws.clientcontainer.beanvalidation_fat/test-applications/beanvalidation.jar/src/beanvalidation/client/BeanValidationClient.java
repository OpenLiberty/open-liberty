/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package beanvalidation.client;

import javax.annotation.Resource;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import beanvalidation.test.BeanValidation;
import beanvalidation.test.BeanValidationInjection;

public class BeanValidationClient {

	private static boolean passed = true;

    @Resource(name = "TestValidatorFactory")
    private static ValidatorFactory ivVFactory;

    @Resource(name = "TestValidator")
    private static Validator ivValidator;

    public static void main(String[] args) {
        System.out.println("\nEntering Bval BeanValidation Application Client.");
        
    	beanValidationTests();
    	beanValidationInjectionTests();
    	
        if(passed) {
        	System.out.println("\nBeanvalidation Application Client Completed.");
        }
    }

    private static void beanValidationTests() {      
        BeanValidation beanValidation = new BeanValidation(); 
        try {
        	beanValidation.testBuildDefaultValidatorFactory();
        	System.out.println("BeanValidation.testBuildDefaultValidatorFactory passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BeanValidation.testBuildDefaultValidatorFactory failed.\n");
            ex.printStackTrace();
        }

        try {
        	beanValidation.testLookupJavaCompValidator();
        	System.out.println("BeanValidation.testLookupJavaCompValidator passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BeanValidation.testLookupJavaCompValidator failed.\n");
            ex.printStackTrace();
        }
        
        try {
        	beanValidation.testLookupJavaCompValidatorFactory();
        	System.out.println("BeanValidation.testLookupJavaCompValidatorFactory passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BeanValidation.testLookupJavaCompValidatorFactory failed.\n");
            ex.printStackTrace();
        }

        try {
        	beanValidation.testValidatingAnnBeanWithConstraints();
        	System.out.println("BeanValidation.testValidatingAnnBeanWithConstraints passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BeanValidation.testValidatingAnnBeanWithConstraints failed.\n");
            ex.printStackTrace();
        }
        
        try {
        	beanValidation.testValidatingAnnBeanWithConstraintsToFail();
        	System.out.println("BeanValidation.testValidatingAnnBeanWithConstraintsToFail passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BeanValidation.testValidatingAnnBeanWithConstraintsToFail failed.\n");
            ex.printStackTrace();
        }

        try {
        	beanValidation.testValidatingMixBeanWithConstraints();
        	System.out.println("BeanValidation.testValidatingMixBeanWithConstraints passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BeanValidation.testValidatingMixBeanWithConstraints failed.\n");
            ex.printStackTrace();
        }
        
        try {
        	beanValidation.testValidatingMixBeanWithConstraintsToFail();
        	System.out.println("BeanValidation.testValidatingMixBeanWithConstraintsToFail passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BeanValidation.testValidatingMixBeanWithConstraintsToFail failed.\n");
            ex.printStackTrace();
        }
        
        try {
        	beanValidation.testValidatingXMLBeanWithConstraints();
        	System.out.println("BeanValidation.testValidatingXMLBeanWithConstraints passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BeanValidation.testValidatingXMLBeanWithConstraints failed.\n");
            ex.printStackTrace();
        }

        try {
        	beanValidation.testValidatingXMLBeanWithConstraintsToFail();
        	System.out.println("BeanValidation.testValidatingXMLBeanWithConstraintsToFail passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BeanValidation.testValidatingXMLBeanWithConstraintsToFail failed.\n");
            ex.printStackTrace();
        }
    }
    
    private static void beanValidationInjectionTests() {
        BeanValidationInjection beanValidationInjection = new BeanValidationInjection(); 
        try {
        	beanValidationInjection.testCustomConstraintValidatorFactory(ivVFactory);
        	System.out.println("BeanValidationInjection.testCustomConstraintValidatorFactory passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BeanValidationInjection.testCustomConstraintValidatorFactory failed.\n");
            ex.printStackTrace();
        }

        try {
        	beanValidationInjection.testCustomParameterNameProvider(ivVFactory);
        	System.out.println("BeanValidationInjection.testCustomParameterNameProvider passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BeanValidationInjection.testCustomParameterNameProvider failed.\n");
            ex.printStackTrace();
        }
        
        try {
        	beanValidationInjection.testCustomTraversableResolver(ivVFactory);
        	System.out.println("BeanValidationInjection.testCustomTraversableResolver passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BeanValidationInjection.testCustomTraversableResolver failed.\n");
            ex.printStackTrace();
        }

        try {
        	beanValidationInjection.testInjectionAndLookupValidator(ivValidator);
        	System.out.println("BeanValidationInjection.testInjectionAndLookupValidator passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BeanValidationInjection.testInjectionAndLookupValidator failed.\n");
            ex.printStackTrace();
        }
        
        try {
        	beanValidationInjection.testInjectionAndLookupValidatorFactory(ivVFactory);
        	System.out.println("BeanValidationInjection.testInjectionAndLookupValidatorFactory passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BeanValidationInjection.testInjectionAndLookupValidatorFactory failed.\n");
            ex.printStackTrace();
        }
    }
}
