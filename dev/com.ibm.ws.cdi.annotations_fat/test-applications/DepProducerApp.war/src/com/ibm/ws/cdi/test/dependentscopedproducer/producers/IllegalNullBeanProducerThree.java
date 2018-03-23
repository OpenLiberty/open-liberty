package com.ibm.ws.cdi.test.dependentscopedproducer.producers;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import com.ibm.ws.cdi.test.dependentscopedproducer.AppScopedSterotype;
import com.ibm.ws.cdi.test.dependentscopedproducer.NonNullBeanThree;

@ApplicationScoped
public class IllegalNullBeanProducerThree {
	
	private static boolean hasProduced = false;
	
	@Produces
	@AppScopedSterotype
	public NonNullBeanThree produceNull(){
		hasProduced = true;
		return null;
	}

	public static boolean hasProduced() {
		return hasProduced;
	}

}
