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

public class MetadataCompleteForModulesEntry extends TaskEntry {

    public MetadataCompleteForModulesEntry(String[] data, MultiEntryApplicationTask task) {
        super(data, task);
    }

    /**
     * The name of a module in the installed (or deployed) application.
     */
    public String getModule() {
        return super.getModule();
    }

    /**
     * The location of the module relative to the root of the application (EAR file).
     */
    public String getUri() {
        return super.getUri();
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
     * @return True if the metadata is locked.
     */
    public boolean getMetadataComplete() {
        return super.getBoolean(AppConstants.APPDEPL_LOCK_MODULE_DD);
    }

    protected void setModule(String value) {
        super.setModule(value);
    }

    protected void setUri(String value) {
        super.setUri(value);
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
    public void setMetadataComplete(boolean value) {
        task.setModified();
        super.setBooleanTrueFalse(AppConstants.APPDEPL_LOCK_MODULE_DD, value);
    }

}
