package com.ibm.ws.cdi.test.dependentscopedproducer;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import com.ibm.ws.cdi.test.dependentscopedproducer.producers.NullBeanProducer;

@RequestScoped
public class SingularNullBeanHolder {

/*	@Inject NullBeanFour bean;
	
	public String getMessage() {
		String msg = "";
		if (bean == null) {
			msg += "nullBeanFour was null ";
			if (NullBeanProducer.isNullFour()) {
				msg += ("and it should be null ");
			} else {
				msg += ("but it shouldn't be null ");
			}
		} else {
			msg += "nullBeanFour was not null ";
			if (! NullBeanProducer.isNullFour()) {
				msg += ("and it shouldn't be null ");
			} else {
				msg += ("but it should be null");
			}
		}
		return msg;
	}
	
	public boolean passed() {
		if (bean == null && NullBeanProducer.isNullFour()) {
			return true;
		} else if (bean != null && ! NullBeanProducer.isNullFour()) {
			return true;
		}
		return false;
	}*/
}
