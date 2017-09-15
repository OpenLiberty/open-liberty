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
package org.apache.myfaces.view.facelets.util;

/**
 * @author Jacob Hookom
 * @version $Id: Path.java 1542444 2013-11-16 01:41:08Z lu4242 $
 */
public final class Path
{

    public static String normalize(String path)
    {
        if (path.length() == 0)
        {
            return path;
        }
        String n = path;
        boolean abs = false;
        while (n.indexOf('\\') >= 0)
        {
            n = n.replace('\\', '/');
        }
        if (n.charAt(0) != '/')
        {
            n = '/' + n;
            abs = true;
        }
        int idx = 0;
        while (true)
        {
            idx = n.indexOf("%20");
            if (idx == -1)
            {
                break;
            }
            n = n.substring(0, idx) + " " + n.substring(idx + 3);
        }
        while (true)
        {
            idx = n.indexOf("/./");
            if (idx == -1)
            {
                break;
            }
            n = n.substring(0, idx) + n.substring(idx + 2);
        }
        if (abs)
        {
            n = n.substring(1);
        }
        return n;
    }

    public static String relative(String ctx, String path)
    {
        if (path.length() == 0)
        {
            return context(ctx);
        }
        String c = context(normalize(ctx));
        String p = normalize(path);
        p = c + p;

        int idx = 0;
        while (true)
        {
            idx = p.indexOf("/../");
            if (idx == -1)
            {
                break;
            }
            int s = p.lastIndexOf('/', idx - 3);
            if (s == -1)
            {
                break;
            }
            p = p.substring(0, s) + p.substring(idx + 3);
        }
        return p;
    }

    public static String context(String path)
    {
        int idx = path.lastIndexOf('/');
        if (idx == -1)
        {
            return "/";
        }
        return path.substring(0, idx + 1);
    }

}
