/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi12.transientpassivationtest;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.SessionScoped;
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
    BeanHolder bh;

    /*
     * protected byte[] serialize(Object instance) throws IOException {
     * ByteArrayOutputStream bytes = new ByteArrayOutputStream();
     * ObjectOutputStream out = new ObjectOutputStream(bytes);
     * out.writeObject(instance);
     * return bytes.toByteArray();
     * }
     * 
     * protected Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
     * ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
     * return in.readObject();
     * }
     */
    public static Bean<?> ensureUniqueBean(Type type, Set<Bean<?>> beans) {
        if (beans.size() == 0) {
            throw new UnsatisfiedResolutionException("Unable to resolve any Web Beans of " + type);
        } else if (beans.size() > 1) {
            throw new AmbiguousResolutionException("More than one bean available for type " + type);
        }
        return beans.iterator().next();
    }

    @Inject
    public void transientVisit(@TransientReference MyStatefulSessionBean bean) {
        bean.setMessage("This bean was injected into PassivatationBean and it has been destroyed");
        bean.doNothing();
    }

    public static <T> T getInstanceByType(BeanManager manager, Class<T> beanType, Annotation... bindings) {
        Bean<T> bean = (Bean<T>) ensureUniqueBean(beanType, manager.getBeans(beanType, bindings));
        return (T) manager.getReference(bean, beanType, manager.createCreationalContext(bean));
    }

    //@Inject
    //transient BeanManager beanManager; // LIBERTY added
    @Inject
    BeanManager beanManager; // remove transient to test serialize-ability

    InjectionPoint ip1 = null;
    InjectionPoint ip2 = null;
    InjectionPoint ip3 = null;

    public String getState() throws Exception {
        String msg = "Reused ";
        if (ip1 == null) {
            ip1 = getInstanceByType(beanManager, FieldInjectionPointBean.class).getInjectedBean().getInjectedMetadata();
            ip2 = getInstanceByType(beanManager, MethodInjectionPointBean.class).getInjectedBean().getInjectedMetadata();
            ip3 = getInstanceByType(beanManager, ConstructorInjectionPointBean.class).getInjectedBean().getInjectedMetadata();
            msg = "Initialized ";
        }

        bh.doNothing();
        String beanState = "";

        for (String s : GlobalState.getOutput()) {
            beanState = beanState + s;
        }

        String extra = "...beanManager: " + beanManager.toString() + "...beans: ";

        Set<Bean<?>> beans = beanManager.getBeans(Object.class);
        for (Bean<?> b : beans) {
            String beanName = b.getName();
            if (beanName != null) {
                extra = extra + "..." + beanName;
            }
        }

        if ((ip1.getType().equals(BeanWithInjectionPointMetadata.class)) &&
            (ip2.getType().equals(BeanWithInjectionPointMetadata.class)) &&
            (ip3.getType().equals(BeanWithInjectionPointMetadata.class))) {
            return msg + "InjectionPoint passivation capability PASSED " + extra + beanState;
        } else {
            Boolean bPass1 = false;
            Boolean bPass2 = false;
            Boolean bPass3 = false;

            if (ip1.getType().equals(BeanWithInjectionPointMetadata.class))
                bPass1 = true;
            if (ip2.getType().equals(BeanWithInjectionPointMetadata.class))
                bPass2 = true;
            if (ip3.getType().equals(BeanWithInjectionPointMetadata.class))
                bPass3 = true;

            return msg + "InjectionPoint passivation capability (" + bPass1 + ", " + bPass2 + ", " + bPass3 + ") FAILED "
                   + extra + beanState;
        }
    }

}
