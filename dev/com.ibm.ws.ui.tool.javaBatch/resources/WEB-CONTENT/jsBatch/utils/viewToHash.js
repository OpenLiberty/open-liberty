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

define(['jsBatch/utils/hashUtils'],function(hashUtils){
  "use strict";

  /**
   * Builds the topic string.
   *
   * @param topicStr
   *          The topic string from which the hash is generated
   * @return The built hash string
   */
  function __buildHash(topicStr, seperator) {
    var t = hashUtils.getToolId();
    if(topicStr) {
      t += seperator + topicStr;
    }
    console.log('Constructed java batch resource hash [' + t + ']');
    return t;
  }

  return {
    getHash : function(resource) {
      if (resource === null) {
        return hashUtils.getToolId();
      }else{
        console.log("work here still in progress");
      }
    },

    updateView : function(newHash) {
      window.top.location.hash = __buildHash(newHash, "/");
    },


    lastUpdateHash : hashUtils.getToolId()
  };

});
