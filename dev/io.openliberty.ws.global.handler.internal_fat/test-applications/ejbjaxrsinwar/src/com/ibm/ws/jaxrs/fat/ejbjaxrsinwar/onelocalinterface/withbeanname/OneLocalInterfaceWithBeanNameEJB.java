/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.withbeanname;

import javax.ejb.Local;
import javax.ejb.Singleton;

import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.singletonwithbeanname.SingletonWithBeanNameEJBResource;

/**
 * This JAX-RS resource is a simple singleton session EJB with a no-interface
 * view with a customized bean name. See {@link SingletonWithBeanNameEJBResource} for more information on test
 * strategy. <br/>
 * This test also demonstrates a potential EJB bean. It is curious if this can
 * be made to work or if it should. If it doesn't work, then need to change it
 * out.
 */
@Singleton(name = "MyOneLocalInterfaceWithBeanNameEJB")
@Local(OneLocalInterfaceWithBeanNameView.class)
public class OneLocalInterfaceWithBeanNameEJB implements OneLocalInterfaceWithBeanNameView {

    private int counter = 0;

    @Override
    public String getCounter() {
        ++counter;
        return String.valueOf(counter);
    }

    @Override
    public void resetCounter() {
        counter = 0;
    }

}
