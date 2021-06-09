/********************************************

s reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.query.entities.interfaces;

public interface IAddress {

    @Override
    public String toString();

    public String getStreet();

    public void setStreet(String street);

    public String getCity();

    public void setCity(String city);

    public String getState();

    public void setState(String state);

    public IEmbZipCode getZipcode();

    public void setZipcode(IEmbZipCode zipcode);
}
