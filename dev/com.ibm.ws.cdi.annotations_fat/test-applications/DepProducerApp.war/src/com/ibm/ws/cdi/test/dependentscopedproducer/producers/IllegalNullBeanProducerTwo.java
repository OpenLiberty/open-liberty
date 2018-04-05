package com.ibm.ws.cdi.test.dependentscopedproducer.producers;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import com.ibm.ws.cdi.test.dependentscopedproducer.NonNullBeanTwo;

@ApplicationScoped
public class IllegalNullBeanProducerTwo {
	
	private static boolean hasProduced = false;
	
	@Produces @ApplicationScoped
	public NonNullBeanTwo produceNull(){
		hasProduced = true;
		return null;
	}

	public static boolean hasProduced() {
		return hasProduced;
	}

}
