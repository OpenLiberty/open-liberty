/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.config.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.logging.Logger;

import javax.faces.context.ExternalContext;

import org.apache.myfaces.shared.util.ClassUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * SAX EntityResolver to provide JSF-specific resources (DTDs) for well-known identifiers.
 *
 * @author Manfred Geiler (latest modification by $Author: struberg $)
 * @author Thomas Spiegl
 * @version $Revision: 1188895 $ $Date: 2011-10-25 20:31:51 +0000 (Tue, 25 Oct 2011) $
 */
public class FacesConfigEntityResolver
    implements EntityResolver
{
    //private static final Log log = LogFactory.getLog(FacesConfigEntityResolver.class);
    private static final Logger log = Logger.getLogger(FacesConfigEntityResolver.class.getName());

    private static final String FACES_CONFIG_1_0_DTD_SYSTEM_ID = "http://java.sun.com/dtd/web-facesconfig_1_0.dtd";
    private static final String FACES_CONFIG_1_0_DTD_RESOURCE
            = "org.apache.myfaces.resource".replace('.', '/') + "/web-facesconfig_1_0.dtd";
    private static final String FACES_CONFIG_1_1_DTD_SYSTEM_ID = "http://java.sun.com/dtd/web-facesconfig_1_1.dtd";
    private static final String FACES_CONFIG_1_1_DTD_RESOURCE
            = "org.apache.myfaces.resource".replace('.', '/') + "/web-facesconfig_1_1.dtd";

    private ExternalContext _externalContext = null;

    public FacesConfigEntityResolver(ExternalContext context)
    {
        _externalContext = context;
    }

    public FacesConfigEntityResolver()
    {
    }

    public InputSource resolveEntity(String publicId,
                                     String systemId)
        throws IOException
    {
        InputStream stream;
        if (systemId.equals(FACES_CONFIG_1_0_DTD_SYSTEM_ID))
        {
            stream = ClassUtils.getResourceAsStream(FACES_CONFIG_1_0_DTD_RESOURCE);
        }
        else if (systemId.equals(FACES_CONFIG_1_1_DTD_SYSTEM_ID))
        {
            stream = ClassUtils.getResourceAsStream(FACES_CONFIG_1_1_DTD_RESOURCE);
        }

        else if (systemId.startsWith("jar:"))
        {
            URL url = new URL(systemId);
            JarURLConnection conn = (JarURLConnection) url.openConnection();
            
            // see MYFACES-1982
            conn.setUseCaches(false);
            JarEntry jarEntry = conn.getJarEntry();
            if (jarEntry == null)
            {
                log.severe("JAR entry '" + systemId + "' not found.");
            }
            //_jarFile.getInputStream(jarEntry);
            stream = conn.getJarFile().getInputStream(jarEntry);
        }
        else
        {
            if (_externalContext == null)
            {
                stream = ClassUtils.getResourceAsStream(systemId);
            }
            else
            {
                if (systemId.startsWith("file:"))
                {
                    systemId = systemId.substring(7); // remove file://
                }
                stream = _externalContext.getResourceAsStream(systemId);
            }
        }

        if (stream == null)
        {
            return null;
        }
        InputSource is = new InputSource(stream);
        is.setPublicId(publicId);
        is.setSystemId(systemId);
        is.setEncoding("ISO-8859-1");
        return is;
    }

}
