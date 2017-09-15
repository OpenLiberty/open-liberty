/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.application.tasks;

import com.ibm.websphere.simplicity.application.AppConstants;
import com.ibm.websphere.simplicity.application.AssetModule;
import com.ibm.websphere.simplicity.exception.TaskEntryNotFoundException;

public class MetadataCompleteForModulesTask extends MultiEntryApplicationTask {

    public MetadataCompleteForModulesTask() {

    }

    public MetadataCompleteForModulesTask(String[][] taskData) {
        super(AppConstants.MetadataCompleteForModulesTask, taskData);
        for (int i = 1; i < taskData.length; i++) {
            String[] data = taskData[i];
            this.entries.add(new MetadataCompleteForModulesEntry(data, this));
        }
    }

    public MetadataCompleteForModulesTask(String[] columns) {
        super(AppConstants.MetadataCompleteForModulesTask, columns);
    }

    @Override
    public MetadataCompleteForModulesEntry get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (MetadataCompleteForModulesEntry) entries.get(i);
    }

    /**
     * Determines whether the specified module exists in this application task.
     * 
     * @param module The module to find.
     * @return True if the module is found in this application task.
     */
    public boolean hasModule(AssetModule module) {
        return getEntry(AppConstants.APPDEPL_URI, module.getURI()) != null;
    }

    /**
     * Specifies whether to write the complete module deployment descriptor, including
     * deployment information from annotations, to extensible markup language (XML) format.
     * <p>
     * The default is not to write out a module deployment descriptor.
     * <p>
     * If your EJB 3.0 or Web 2.5 module does not have a metadata-complete attribute or the
     * metadata-complete attribute is set to false, you can set this value to true and instruct
     * the product to write out a module deployment descriptor.
     * <p>
     * Avoid trouble: If your Java EE 5 application uses annotations and a shared library,
     * do not enable the metadata-complete attribute. When your application uses annotations and
     * a shared library, setting the metadata-complete attribute to true causes the product
     * to incorrectly represent an @EJB annotation in the deployment descriptor as &lt;ejb-ref&gt;
     * rather than &lt;ejb-local-ref&gt;. For Web modules, setting the metadata-complete attribute
     * to true might cause InjectionException errors. If you must set the metadata-complete
     * attribute to true, avoid errors by not using a shared library, by placing the shared
     * library in either the classes or lib directory of the application server, or by fully
     * specifying the metadata in the deployment descriptors.
     * 
     * @param value True to lock the module deployment descriptor.
     */
    public void setMetadataComplete(AssetModule module, boolean value) throws Exception {
        if (!hasModule(module))
            throw new TaskEntryNotFoundException();
        modified = true;
        MetadataCompleteForModulesEntry entry = (MetadataCompleteForModulesEntry) getEntry(AppConstants.APPDEPL_URI, module.getURI());
        entry.setMetadataComplete(value);
    }

    /**
     * Specifies whether to write the complete module deployment descriptor, including
     * deployment information from annotations, to extensible markup language (XML) format.
     * <p>
     * The default is not to write out a module deployment descriptor.
     * <p>
     * If your EJB 3.0 or Web 2.5 module does not have a metadata-complete attribute or the
     * metadata-complete attribute is set to false, you can set this value to true and instruct
     * the product to write out a module deployment descriptor.
     * <p>
     * Avoid trouble: If your Java EE 5 application uses annotations and a shared library,
     * do not enable the metadata-complete attribute. When your application uses annotations and
     * a shared library, setting the metadata-complete attribute to true causes the product
     * to incorrectly represent an @EJB annotation in the deployment descriptor as &lt;ejb-ref&gt;
     * rather than &lt;ejb-local-ref&gt;. For Web modules, setting the metadata-complete attribute
     * to true might cause InjectionException errors. If you must set the metadata-complete
     * attribute to true, avoid errors by not using a shared library, by placing the shared
     * library in either the classes or lib directory of the application server, or by fully
     * specifying the metadata in the deployment descriptors.
     * 
     * @param value True to lock the module deployment descriptor.
     */
    public void setMetadataComplete(boolean value) throws Exception {
        modified = true;
        for (int i = 0; i < this.size(); i++)
            get(i).setMetadataComplete(value);
    }

}
