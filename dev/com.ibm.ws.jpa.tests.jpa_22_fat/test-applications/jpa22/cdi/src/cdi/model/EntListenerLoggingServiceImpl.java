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

package cdi.model;

import java.util.logging.Logger;

import javax.enterprise.inject.Default;
import javax.inject.Singleton;

@Default
@Singleton
public class EntListenerLoggingServiceImpl extends AbstractLoggingService implements EntListenerLoggingService {
    public final static String CLASS_NAME = WidgetEntityListener.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public EntListenerLoggingServiceImpl() {
        super(svLogger, CLASS_NAME);
    }
}
