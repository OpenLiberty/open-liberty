/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.concurrent.persistent.feature.internal;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.wsspi.concurrent.persistent.TaskStore;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

/**
 * User feature service that serves as an executor service factory.
 */
@Component(configurationPid = "concurrent.persistent.feature.test",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = {"creates.objectClass=java.util.concurrent.ScheduledExecutorService"})
public class ExecutorFactoryServiceImpl implements ResourceFactory {

	/**
	 * ScheduledExecutorService derived from the configured persistentExecutor reference.
	 */
    ScheduledExecutorService executorService;

    /**
     * Class name constant
     */
    final String CLASS_NAME = this.getClass().getName();

    /**
     * Declarative Services method to activate this component.
     * 
     * @param bundleContext The bundle context.
     */
    @Activate
    protected void activate(BundleContext bundleContext, Map<String, Object> properties) {
        System.out.println(CLASS_NAME + ".activate.Entry. "+ "Properties: " + properties);

        String jndiName = (String) properties.get("jndiName");

        System.out.println(CLASS_NAME + ".activate.Exit. " + "JNDIName: " + jndiName);
    }

    /**
     * Declarative Services method to deactivate this component.
     * 
     * @param context context for this component
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        System.out.println(CLASS_NAME + ".deactivate.Entry. " + "ComponentContext: " + context);
        System.out.println(CLASS_NAME + ".deactivate.Exit.");
    }

    public TaskStore getTaskStore() throws Exception {
    	Field f = executorService.getClass().getDeclaredField("taskStore");
    	f.setAccessible(true);
    	return (TaskStore) f.get(executorService);
    }

    /**
     * Sets a ScheduledExecutorService instance.
     * @param executorService The scheduledExecutorService instance.
     */
    @Reference (service = ScheduledExecutorService.class,
		        target = "(id=unbound)")
    protected void setPersistentExecutor(ScheduledExecutorService executorService) {
        System.out.println(CLASS_NAME + ".setScheduledExecutorService.Entry. " +"ExecutorService: "+ executorService);
    	this.executorService = executorService;
        System.out.println(CLASS_NAME + ".setScheduledExecutorService.Exit. ");
    }

    /**
     * Unsets the ScheduledExecutorService instance.
     * @param executorService The scheduledExecutorService instance.
     */
    protected void unsetPersistentExecutor(ScheduledExecutorService ExecutorService) {
        System.out.println(CLASS_NAME + ".unsetScheduledExecutorService.Entry. " +"ExecutorService: "+ executorService);
        this.executorService = null;
        System.out.println(CLASS_NAME + ".unsetScheduledExecutorService.Exit. ");
    }

    /** {@inheritDoc} */
	@Override
	public Object createResource(ResourceInfo info) throws Exception {
        System.out.println(CLASS_NAME + ".createResource.Entry. " +"ResourceInfo: "+ info);
        System.out.println(CLASS_NAME + ".createResource.Exit. " +"ExecutorService: "+ executorService); 
		return executorService;
	}
}
