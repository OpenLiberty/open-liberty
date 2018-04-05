package com.ibm.ws.cdi.test.dependentscopedproducer.producers;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;

import com.ibm.ws.cdi.test.dependentscopedproducer.NonNullBean;
import com.ibm.ws.cdi.test.dependentscopedproducer.NullBean;
import com.ibm.ws.cdi.test.dependentscopedproducer.NullBeanFour;
import com.ibm.ws.cdi.test.dependentscopedproducer.NullBeanThree;
import com.ibm.ws.cdi.test.dependentscopedproducer.NullBeanTwo;

@ApplicationScoped
public class IllegalNullBeanProducer {
	
	private static boolean hasProduced = false;
	
	@ApplicationScoped
	@Produces
	public NullBean produceNull(){
		hasProduced = true;
		return null;
	}
	
	@ApplicationScoped
	@Produces
	public NullBeanTwo produceNullTwo(){
		hasProduced = true;
		return null;
	}
	
	@ApplicationScoped
	@Produces
	public NullBeanThree produceNullThree(){
		hasProduced = true;
		return null;
	}
	
	@ApplicationScoped
	@Produces
	public NullBeanFour produceNullFour(){
		hasProduced = true;
		return null;
	}
	
	@ApplicationScoped
	@Produces
	public NonNullBean produceNullFive(){
		hasProduced = true;
		return null;
	}

	public static boolean hasProduced() {
		return hasProduced;
	}

}
