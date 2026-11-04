/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.wlm.ejb.mdb;

import java.util.Dictionary;

/**
 * MDB Classifier.
 */
public interface MDBClassifier {

    /**
     * Updates internal configuration data. Called when the wlmClassification element is udpated.
     *
     * @param properties The list of PIDs associated with the wlmClassification configuration element.
     */
    public void update(Dictionary<?, ?> properties);

    /**
     * Cleans up internal configuration data. Called when wlmClassification element is removed.
     */
    public void cleanup();
}
