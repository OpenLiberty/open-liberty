/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.admin;

public interface JsReloadableComponent {
	/**
	   * Reload the given engine component with its new config object, if it has one
	   * otherwise config will be null. The new config should be activated when the
	   * engine component's engineReloaded() method is called. It is then up to the given
	   * component to determine when it is suitable for that config to become active.
	   *
	   * @param config the JsEObject which corresponds to the instance of the implementing class, null if it does not have one.
	   */
	  public void reloadComponent(JsEObject config);
	  
	  /**
	   * If the dynamic config change has determined this engine component has now been deleted
	   * from the config, then this method will be called to determine if the runtime can allow
	   * its instance to be stopped and destroyed.
	   */
	  public boolean isDeleteable();
	  
	  /**
	   * Set a new or existing Messaging engine custom property to the new/changed value.
	   * for all custom properties defined on the Messaging Engine in the WCCM configuration.
	   * <p>
	   * The implementation of this method should check for the existence of any expected
	   * properties as required. Unexpected properties should simply be ignored, as these
	   * may be intended for use by other JsEngineComponents. The use of a default "null"
	   * implementation will be sufficient if the class does not use any custom properties.
	   * 
	   * @param name  the name of the custom property
	   * @param value the value of the custom property
	   */
	  public void setCustomPropertyByReload(String name, String value);
	  
	  /**
	   * Unset an existing Messaging engine custom property that had previously been set.
	   * The implementation should handle a property that has already been unset.
	   * 
	   * @param name  the name of the custom property
	   */
	  public void unsetCustomPropertyByReload(String name);

}
