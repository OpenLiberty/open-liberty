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
package org.apache.myfaces.shared.view;

import java.io.IOException;
import java.io.Writer;

/**
 * Delegates the standard Writer-methods (append, close, flush, write)
 * to the given Writer, if the ResponseSwitch is enabled
 */
class SwitchableWriter extends Writer
{

    Writer _delegate = null;
    ResponseSwitch _responseSwitch = null;

    public SwitchableWriter(Writer delegate, ResponseSwitch responseSwitch)
    {
        _delegate = delegate;
        _responseSwitch = responseSwitch;
    }

    public void write(String s, int start, int end) throws IOException
    {
        if (_responseSwitch.isEnabled())
        {
            _delegate.write(s, start, end);
        }
    }

    public void write(String s) throws IOException
    {
        if (_responseSwitch.isEnabled())
        {
            _delegate.write(s);
        }
    }

    public void write(char[] c, int start, int end) throws IOException
    {
        if (_responseSwitch.isEnabled())
        {
            _delegate.write(c, start, end);
        }
    }

    public void write(char[] c) throws IOException
    {
        if (_responseSwitch.isEnabled())
        {
            _delegate.write(c);

        }
    }

    public void write(int i) throws IOException
    {
        if (_responseSwitch.isEnabled())
        {
            _delegate.write(i);
        }
    }

    public void flush() throws IOException
    {
        if (_responseSwitch.isEnabled())
        {
            _delegate.flush();
        }
    }

    public void close() throws IOException
    {
        if (_responseSwitch.isEnabled())
        {
            _delegate.close();
        }
    }

    public Writer append(char c) throws IOException
    {
        if (_responseSwitch.isEnabled())
        {
            return _delegate.append(c);
        }
        return this;
    }

    public Writer append(CharSequence csq, int start, int end)
            throws IOException
    {
        if (_responseSwitch.isEnabled())
        {
            return _delegate.append(csq, start, end);
        }
        return this;
    }

    public Writer append(CharSequence csq) throws IOException
    {
        if (_responseSwitch.isEnabled())
        {
            return _delegate.append(csq);
        }
        return this;
    }
}
