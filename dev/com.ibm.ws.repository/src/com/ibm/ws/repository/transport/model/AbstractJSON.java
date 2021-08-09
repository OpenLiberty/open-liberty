/*******************************************************************************
 * Copyright (c) 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.ibm.ws.repository.transport.model;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import com.ibm.ws.repository.transport.client.JSONAssetConverter;

public abstract class AbstractJSON {

    public String toString() {
        String ret = null;
        try {
            ret = this.getClass().getName() + ":"
                    + JSONAssetConverter.writeValueAsString(this);
        } catch (IOException io) {
            ret = super.toString();
        }
        return ret;
    }

    public void dump(OutputStream stream) {
        try {
            JSONAssetConverter.writeValue(stream, this);
        } catch (IOException io) {
            if (stream instanceof PrintStream) {
                io.printStackTrace((PrintStream) stream);
            } else {
                io.printStackTrace(new PrintStream(stream));
            }
        }
    }

}
