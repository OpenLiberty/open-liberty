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
