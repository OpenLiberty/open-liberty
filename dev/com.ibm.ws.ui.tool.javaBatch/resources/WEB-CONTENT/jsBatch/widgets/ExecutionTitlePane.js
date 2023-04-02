/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
define([ 
    'dojo/_base/declare',
    'dijit/TitlePane',
    'dojo/text!./templates/ExecutionTitlePane.html'
    ], 
function(declare, TitlePane, template){
  "use strict";

  /**
   * ExecutionTitlePane
   * 
   *   TitlePane used on the Execution Details View that has a transparent background and
   *   sets the expando-widget on the far right of the title, instead of to the left of the title.
   *   
   */

  var ExecutionTitlePane = declare("ExecutionTitlePane", TitlePane, {

    templateString: template

  });

  return ExecutionTitlePane;
});