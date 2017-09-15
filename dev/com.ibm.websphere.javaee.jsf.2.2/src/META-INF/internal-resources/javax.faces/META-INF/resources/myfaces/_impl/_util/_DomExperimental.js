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
if(_MF_SINGLTN){_MF_SINGLTN(_PFX_UTIL+"_DomExperimental",myfaces._impl._util._Dom,{constructor_:function(){this._callSuper("constructor_");myfaces._impl._util._Dom=this;},html5FormDetection:function(B){var A=this._RT.browser;if(A.isIEMobile&&A.isIEMobile<=8){return null;}var C=this.getAttribute(B,"form");return(C)?this.byId(C):null;},getNamedElementFromForm:function(B,A){return B[A];}});}