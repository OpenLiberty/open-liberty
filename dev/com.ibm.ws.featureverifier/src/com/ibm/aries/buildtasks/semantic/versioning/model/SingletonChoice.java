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
package com.ibm.aries.buildtasks.semantic.versioning.model;

public class SingletonChoice implements Comparable<SingletonChoice> {
    private final String choiceName;
    private final boolean choiceIsPreferred;

    public SingletonChoice(String choiceName, boolean preferred) {
        this.choiceName = choiceName;
        this.choiceIsPreferred = preferred;
    }

    public String getChoiceFeatureName() {
        return choiceName;
    }

    public boolean isPreferred() {
        return choiceIsPreferred;
    }

    @Override
    public String toString() {
        return (choiceIsPreferred ? "##" : "") + choiceName;
    }

    //hashCode & equals.. ignores the choiceIsPreferred flag. 

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((choiceName == null) ? 0 : choiceName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SingletonChoice other = (SingletonChoice) obj;
        if (choiceName == null) {
            if (other.choiceName != null)
                return false;
        } else if (!choiceName.equals(other.choiceName))
            return false;
        return true;
    }

    @Override
    public int compareTo(SingletonChoice o) {
        //a little simpler, because setId can never be null.
        if (o != null)
            return this.choiceName.compareTo(o.choiceName);
        else
            return -1;
    }
}
