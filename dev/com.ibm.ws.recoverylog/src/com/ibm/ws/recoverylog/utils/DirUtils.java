/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.utils;

import java.io.File;
import java.util.StringTokenizer;


import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.recoverylog.spi.TraceConstants;

public final class DirUtils
{
    private static final TraceComponent tc = Tr.register(DirUtils.class, TraceConstants.TRACE_GROUP, null);


    /**
     * Replaces forward and backward slashes in the source string with 'File.separator'
     * characters.
    */
    public static String createDirectoryPath(String source)
    {
       if (tc.isEntryEnabled()) Tr.entry(tc, "createDirectoryPath",source);

       String directoryPath = null;

       if (source != null)
       {
           directoryPath = "";

           final StringTokenizer tokenizer = new StringTokenizer(source,"\\/");

           while (tokenizer.hasMoreTokens())
           {
             final String pathChunk = tokenizer.nextToken();

             directoryPath += pathChunk;

             if (tokenizer.hasMoreTokens())
             {
               directoryPath += File.separator;
             }

           }
       }

       if (tc.isEntryEnabled()) Tr.exit(tc, "createDirectoryPath",directoryPath);
       return directoryPath;
    }

}
