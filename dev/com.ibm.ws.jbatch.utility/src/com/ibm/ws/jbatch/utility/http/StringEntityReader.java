/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.utility.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and returns the Entity as a List<String>, one string per line.
 */
public class StringEntityReader implements EntityReader<List<String>> {

    @Override
    public List<String> readEntity(InputStream entityStream) throws IOException {

        List<String> retMe = new ArrayList<String>();
        
        if (entityStream == null) {
            return retMe;
        }
        
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(entityStream, StandardCharsets.UTF_8));
        while ( (line = br.readLine()) != null ) {
            retMe.add(line);
        }
        
        return retMe;
    }

}
