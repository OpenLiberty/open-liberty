/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.alterablecontext.test.extension;

import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Context;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

public class AlterableContextExtension implements Extension {

    public void listenForInjection(@Observes AfterBeanDiscovery afterBeanDiscoveryEvent, BeanManager bm) {

        Bean<AlterableContextBean> bean = (Bean<AlterableContextBean>) bm.resolve(bm.getBeans(AlterableContextBean.class));
        Context context = bm.getContext(bean.getScope());
        AlterableContextBean acb = context.get(bean, bm.createCreationalContext(bean));

        AlterableContext ac = (AlterableContext) context;

        Object before = ac.get(bean);
        String sBefore = before.toString();
        DirtySingleton.addString("I got this from my alterablecontext: " + sBefore);

        DirtySingleton.addString("I'm going to delete it");
        ac.destroy(bean);

        Object after = ac.get(bean);
        String sAfter = "null";
        if (after != null) {
            sAfter = after.toString();
        }

        DirtySingleton.addString("Now the command returns: " + sAfter);

    }

}
