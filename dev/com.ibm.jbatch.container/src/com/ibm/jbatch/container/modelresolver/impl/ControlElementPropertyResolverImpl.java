/*
 * Copyright 2012 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.ibm.jbatch.container.modelresolver.impl;

import java.util.Properties;



import com.ibm.jbatch.jsl.model.End;
import com.ibm.jbatch.jsl.model.Fail;
import com.ibm.jbatch.jsl.model.Next;
import com.ibm.jbatch.jsl.model.Stop;
import com.ibm.jbatch.jsl.model.helper.TransitionElement;

public class ControlElementPropertyResolverImpl extends AbstractPropertyResolver<TransitionElement> {

    public ControlElementPropertyResolverImpl(boolean isPartitionStep) {
		super(isPartitionStep);
	}

	@Override
    public TransitionElement substituteProperties(final TransitionElement controlElement, final Properties submittedProps,
            final Properties parentProps) {

        if (controlElement instanceof End) {
            ((End)controlElement).setOn(this.replaceAllProperties(((End)controlElement).getOn(), submittedProps, parentProps));
            ((End)controlElement).setExitStatus(this.replaceAllProperties(((End)controlElement).getExitStatus(), submittedProps, parentProps));
            
        } else if (controlElement instanceof Fail) {
            ((Fail)controlElement).setOn(this.replaceAllProperties(((Fail)controlElement).getOn(), submittedProps, parentProps));
            ((Fail)controlElement).setExitStatus(this.replaceAllProperties(((Fail)controlElement).getExitStatus(), submittedProps, parentProps));
            
        } else if (controlElement instanceof Next) {
            ((Next)controlElement).setOn(this.replaceAllProperties(((Next)controlElement).getOn(), submittedProps, parentProps));
            ((Next)controlElement).setTo(this.replaceAllProperties(((Next)controlElement).getTo(), submittedProps, parentProps));
   
        } else if (controlElement instanceof Stop) {
            ((Stop)controlElement).setOn(this.replaceAllProperties(((Stop)controlElement).getOn(), submittedProps, parentProps));
            ((Stop)controlElement).setExitStatus(this.replaceAllProperties(((Stop)controlElement).getExitStatus(), submittedProps, parentProps));
            ((Stop)controlElement).setRestart(this.replaceAllProperties(((Stop)controlElement).getRestart(), submittedProps, parentProps));
        }

        return controlElement;
    }

}
