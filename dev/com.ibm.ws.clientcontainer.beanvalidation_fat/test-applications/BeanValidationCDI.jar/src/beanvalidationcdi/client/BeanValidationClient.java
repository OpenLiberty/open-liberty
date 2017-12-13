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
package beanvalidationcdi.client;

import java.lang.reflect.Type;
import java.util.Set;

import javax.annotation.Resource;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.validation.ValidatorFactory;

import beanvalidationcdi.test.BVal;
import beanvalidationcdi.test.BValAtInject;
import beanvalidationcdi.test.BValAtResource;

public class BeanValidationClient {

	private static boolean passed = true;
	   
    @Resource
	private static ValidatorFactory resourceValidatorFactory;
    
    public static void main(String[] args) {
        System.out.println("\nEntering Bval BeanValidation Application Client.");
        
    	bValTests();
    	bValAtInjectTests();
    	bValAtResourceTests();
    	
        if(passed) {
        	System.out.println("\nBeanValidationCDI Application Client Completed.");
        }
    }

    private static void bValTests() {       
    	BVal bVal = (BVal) getManagedBean(BVal.class);
        try {
        	bVal.testCDIInjectionInConstraintValidatorFactoryLookup();
        	System.out.println("BVal.testCDIInjectionInConstraintValidatorFactoryLookup passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BVal.testCDIInjectionInConstraintValidatorFactoryLookup failed.\n");
            ex.printStackTrace();
        }
        
        try {
        	bVal.testCDIInjectionInInterpolatorLookup();
        	System.out.println("BVal.testCDIInjectionInInterpolatorLookup passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BVal.testCDIInjectionInInterpolatorLookup failed.\n");
            ex.printStackTrace();
        }
        
        try {
        	bVal.testConstructorValidation();
        	System.out.println("BVal.testConstructorValidation passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BVal.testConstructorValidation failed.\n");
            ex.printStackTrace();
        }
        
        try {
        	bVal.testMethodValidation();
        	System.out.println("BVal.testMethodValidation passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BVal.testMethodValidation failed.\n");
            ex.printStackTrace();
        }
    }
    
    private static void bValAtInjectTests() {
    	BValAtInject bValAtInject = (BValAtInject) getManagedBean(BValAtInject.class);
        try {
        	bValAtInject.testCDIInjectionInInterpolatorAtInject();
        	System.out.println("BValAtInject.testCDIInjectionInInterpolatorAtInject passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BValAtInject.testCDIInjectionInInterpolatorAtInject failed.\n");
            ex.printStackTrace();
        }
    }
    
    private static void bValAtResourceTests() {
    	BValAtResource bValAtInject = new BValAtResource(); 
        try {
        	bValAtInject.testCDIInjectionInInterpolatorAtResource(resourceValidatorFactory);
        	System.out.println("BValAtResource.testCDIInjectionInInterpolatorAtResource passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BValAtResource.testCDIInjectionInInterpolatorAtResource failed.\n");
            ex.printStackTrace();
        }
    }
    
    
	private static <T> Object getManagedBean(Class<T> clazz){
        BeanManager cdiBeanManager = CDI.current().getBeanManager();
        if (cdiBeanManager != null) {
            System.out.println("Got BeanManager from CDI: " + cdiBeanManager.getClass());
        }

        Context c;
        BeanManager jndiBeanManager = null;
        try {
            c = new InitialContext();
            jndiBeanManager = (BeanManager) c.lookup("java:comp/BeanManager");
            if (jndiBeanManager != null) {
                System.out.println("Got BeanManager from JNDI: " + jndiBeanManager.getClass());
            }
        } catch (NamingException e) {
            System.out.println("JNDI lookup failed");
            e.printStackTrace();
        }

        if (cdiBeanManager != null && jndiBeanManager != null) {
            System.out.println("Bean managers are " + (cdiBeanManager == jndiBeanManager ? "the same" : "different"));
            System.out.println("Bean managers are " + (cdiBeanManager.equals(jndiBeanManager) ? "equal" : "not equal"));
        }

        Type beanType = clazz;
        Set<Bean<?>> beans = cdiBeanManager.getBeans(beanType);
        Bean<?> bean = cdiBeanManager.resolve(beans);
        CreationalContext<?> creationalContext = cdiBeanManager.createCreationalContext(bean);

        Object appBean = cdiBeanManager.getReference(bean, beanType, creationalContext);
        if (appBean != null) {
            System.out.println("Got AppBean");
        }
        return appBean;
    }
}
