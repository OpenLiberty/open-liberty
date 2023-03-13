/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.lifecycle.apps.transientReferenceWithPersistenceWar;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.TransientReference;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@SessionScoped
public class PassivationBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private BeanHolder bh;

    @Inject
    public void transientVisit(@TransientReference MyStatefulSessionBean bean) {
        bean.setDestroyMessage("MyStatefulSessionBean injected into PassivationBean destroyed");
        bean.doNothing();
    }

    @Inject
    private Event<TestEvent> event;

    @Inject
    private BeanManager beanManager; // not transient to test serialize-ability

    private InjectionPoint ip1 = null;
    private InjectionPoint ip2 = null;
    private InjectionPoint ip3 = null;

    /**
     * Called first, does setup and ensures that preconditions for the test are correct
     */
    public void testInitialize() {
        // Should be a fresh instance, injection points not yet initialized
        assertNull("ip1 should be null", ip1);
        assertNull("ip2 should be null", ip2);
        assertNull("ip3 should be null", ip3);

        ip1 = getInstanceByType(beanManager, FieldInjectionPointBean.class).getInjectedBean().getInjectedMetadata();
        ip2 = getInstanceByType(beanManager, MethodInjectionPointBean.class).getInjectedBean().getInjectedMetadata();
        ip3 = getInstanceByType(beanManager, ConstructorInjectionPointBean.class).getInjectedBean().getInjectedMetadata();

        // Assert injection points are as we expect
        assertEquals(BeanWithInjectionPointMetadata.class, ip1.getType());
        assertEquals(BeanWithInjectionPointMetadata.class, ip2.getType());
        assertEquals(BeanWithInjectionPointMetadata.class, ip3.getType());

        // Call bh to ensure it gets instantiated and injected
        bh.doNothing();

        // Fire test event to ensure it gets instantiated
        assertNotNull("event should not be null", event);
        event.fire(new TestEvent());

        // Assert that our transient references have been injected and the beans destroyed
        assertThat(GlobalState.getOutput(), containsInAnyOrder("MyStatefulSessionBean injected into BeanHolder destroyed",
                                                               "MyStatefulSessionBean injected into PassivationBean destroyed"));
    }

    /**
     * Called after an app restart but using the same session. The same instance of PassivationBean should be used.
     */
    public void testReuse() {
        // Should be the same instance, restored from passivation after app restart
        assertNotNull("ip1 should not be null", ip1);
        assertNotNull("ip2 should not be null", ip2);
        assertNotNull("ip3 should not be null", ip3);

        // Assert we can still use the injection point instances
        assertEquals(BeanWithInjectionPointMetadata.class, ip1.getType());
        assertEquals(BeanWithInjectionPointMetadata.class, ip2.getType());
        assertEquals(BeanWithInjectionPointMetadata.class, ip3.getType());

        // Call bh to ensure it gets instantiated and injected
        bh.doNothing();

        // Fire test event to ensure it gets instantiated
        assertNotNull("event should not be null", event);
        event.fire(new TestEvent());

        // Assert that there's no transient reference injected into PassivationBean
        // It's session scoped and should be passivated -> no new instance -> no injection of transient reference
        // Note that GlobalState will have been wiped when the app was restarted
        assertThat(GlobalState.getOutput(), containsInAnyOrder("MyStatefulSessionBean injected into BeanHolder destroyed"));
    }

    public static <T> T getInstanceByType(BeanManager manager, Class<T> beanType, Annotation... bindings) {
        Bean<?> bean = ensureUniqueBean(beanType, manager.getBeans(beanType, bindings));
        Object instance = manager.getReference(bean, beanType, manager.createCreationalContext(bean));
        return beanType.cast(instance);
    }

    public static Bean<?> ensureUniqueBean(Type type, Set<Bean<?>> beans) {
        if (beans.size() == 0) {
            throw new UnsatisfiedResolutionException("Unable to resolve any Web Beans of " + type);
        } else if (beans.size() > 1) {
            throw new AmbiguousResolutionException("More than one bean available for type " + type);
        }
        return beans.iterator().next();
    }

}
