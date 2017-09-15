/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc.cdi.weld;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.wsoc.injection.InjectionProvider12;

/**
 *
 */
public class InjectionImpl implements InjectionProvider12 {

	private static final TraceComponent tc = Tr.register(InjectionImpl.class);

	@Override
	public <T> T getManagedEndpointInstance(Class<T> endpointClass,
			ConcurrentHashMap map) throws InstantiationException {
		BeanManager manager = ServiceManager.getCurrentBeanManager();
		if (manager == null) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "Bean Manager is null. not sure how we got here.");
			}
			return null;
		} else {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc,
						"Bean Manager is not null. Assuming CDI is enabled.  Bean Manager: "
								+ manager);
			}

			try {
				Set<Bean<?>> set = manager.getBeans(endpointClass);

				if (set != null) {
					Bean<T> bean = null;

					if (set.size() == 0) {
						if (tc.isDebugEnabled()) {
							Tr.debug(tc,
									"Unable to resolve any Web Beans of type: "
											+ endpointClass);
						}
					} else if (set.size() > 1) {
						if (tc.isDebugEnabled()) {
							Tr.debug(tc,
									"More than one bean available for type: "
											+ endpointClass);
						}
					} else {
						bean = (Bean<T>) set.iterator().next();
					}

					if (bean != null) {
						if (tc.isDebugEnabled()) {
							Tr.debug(tc, "got bean of: " + bean);
						}

						CreationalContext<?> c = manager
								.createCreationalContext(bean);
						T ep = (T) manager.getReference(bean, endpointClass, c);
						if (tc.isDebugEnabled()) {
							Tr.debug(tc,
									"InjectionImpl: create a context of, hc: "
											+ c.hashCode());
							Tr.debug(tc, "InjectionImpl: using a key of: " + ep);
							Tr.debug(tc, "InjectionImpl: using a key of, hc: "
									+ ep.hashCode());
						}
						map.put(ep, c);
						return ep;
					}
				}
			} catch (Throwable t) {
				if (tc.isDebugEnabled()) {
					Tr.debug(tc,
							"Caught exception while trying to instantiate a bean using CDI: "
									+ t);
				}
			}

			if (tc.isDebugEnabled()) {
				Tr.debug(
						tc,
						"could not create the bean via the CDI service.");
			}

		}

		return null;
	}

	@Override
	public void releaseCC(Object key, ConcurrentHashMap map) {
		CreationalContext<?> cc = null;
		cc = (CreationalContext<?>) map.remove(key);

		if (cc != null) {
			cc.release();
		}
	}

}
