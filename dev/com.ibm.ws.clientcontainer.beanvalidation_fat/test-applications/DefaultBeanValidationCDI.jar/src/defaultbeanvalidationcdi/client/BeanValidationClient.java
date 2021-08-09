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
package defaultbeanvalidationcdi.client;

import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import defaultbeanvalidationcdi.test.BValAtInjection;

public class BeanValidationClient {
    
    public static void main(String[] args) {
        System.out.println("\nEntering Bval BeanValidation Application Client.");
        boolean passed = true;
        
        BValAtInjection bValAtInjection = (BValAtInjection) getManagedBean(BValAtInjection.class);
        try {
        	bValAtInjection.testConstraintValidatorInjection();
        	System.out.println("BValAtInjection.testConstraintValidatorInjection passed.\n");
        } catch (Exception ex){
        	passed = false;
            System.out.println("BValAtInjection.testConstraintValidatorInjection failed.\n");
            ex.printStackTrace();
        }
        
        if(passed) {
        	System.out.println("\nDefaultBeanValidationCDI Application Client Completed.");
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
