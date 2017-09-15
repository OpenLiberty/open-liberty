/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.anno.classsource;

public interface ClassSource_MappedDirectory extends ClassSource {
    String getDirPath();

    boolean getConvertResourceNames();

    @Override
    String inconvertResourceName(String externalResourceName);

    @Override
    String outconvertResourceName(String internalResourceName);
}