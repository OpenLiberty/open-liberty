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
package com.ibm.ws.app.manager.springboot.support;

import java.io.IOException;

import com.ibm.ws.app.manager.springboot.container.config.SpringConfiguration;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * A helper is used to create instances of an embedded container. For example,
 * a web container or a reactive container.
 *
 * @param <T>
 */
public interface ContainerInstanceFactory<T> {

    interface Instance {

        void start();

        void stop();
    }

    Class<T> getType();

    Instance intialize(SpringBootApplication app, String id, String virtualHostId, T helperParam,
                       SpringConfiguration additionalConfig) throws IOException, UnableToAdaptException, MetaDataException;
}