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
package jakarta.faces.component;

import jakarta.el.MethodExpression;

/**
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">Faces Specification</a>
 */
public interface ActionSource
{
    public boolean isImmediate();

    public void setImmediate(boolean immediate);

    public void addActionListener(jakarta.faces.event.ActionListener listener);

    public jakarta.faces.event.ActionListener[] getActionListeners();

    public void removeActionListener(jakarta.faces.event.ActionListener listener);

    default MethodExpression getActionExpression()
    {
        throw new UnsupportedOperationException();
    }

    default void setActionExpression(MethodExpression action)
    {
        throw new UnsupportedOperationException();
    }
}
