/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Copied from Apache BatchEE and modified
package chunktests.artifacts;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;

public class SimpleIntegerCounterReader implements ItemReader {

    private final static Logger logger = Logger.getLogger("com.ibm.ws.jbatch.open_fat");

    Integer next = 0;

    @BatchProperty(name = "max")
    String maxStr = "9";

    int max = 0;

    @Override
    public void open(Serializable checkpoint) throws Exception {

        max = Integer.parseInt(maxStr);

        if (checkpoint != null) {
            next = (Integer) checkpoint;
        }
        logger.fine("In open(), next = " + next);
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public Object readItem() throws Exception {
        logger.fine("readItem: " + next.toString());
        if (next < max) {
            return next++;
        } else {
            logger.fine("Reached max, exiting.");
            return null;

        }
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return next;
    }

}
