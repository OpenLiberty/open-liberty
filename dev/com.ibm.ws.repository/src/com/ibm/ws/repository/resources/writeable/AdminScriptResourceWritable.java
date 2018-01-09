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
package com.ibm.ws.repository.resources.writeable;

import java.util.Collection;

import com.ibm.ws.repository.resources.AdminScriptResource;

/**
 * Represents an Admin Script Resource which can be uploaded to a repository.
 * <p>
 * This interface allows write access to fields which are specific to admin scripts.
 */
public interface AdminScriptResourceWritable extends AdminScriptResource, RepositoryResourceWritable, ApplicableToProductWritable {

    /**
     * Sets the language that the script is written in
     *
     * @param scriptLang the language the script is written in
     */
    public void setScriptLanguage(String scriptLang);

    /**
     * Sets the list of required features for this admin script
     *
     * @param requireFeature the symbolic names of the required features
     */
    public void setRequireFeature(Collection<String> requireFeature);

}