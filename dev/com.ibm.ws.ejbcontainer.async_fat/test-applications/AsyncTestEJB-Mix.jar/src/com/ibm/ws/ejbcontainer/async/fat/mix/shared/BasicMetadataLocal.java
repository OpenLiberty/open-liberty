/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat.mix.shared;

/**
 * Local interface for Session bean for metadata-complete testing.
 **/
public interface BasicMetadataLocal {
    public void test_metaDataComplete();

    public void test_metaDataComplete(String param);

    public void test_metaDataCompleteSync(String param);
}