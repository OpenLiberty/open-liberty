/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.metatype;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 *
 */
public interface SchemaGenerator {

    public void generate(OutputStream out, SchemaGeneratorOptions options) throws IOException;

    public void generate(Writer writer, SchemaGeneratorOptions options) throws IOException;
}
