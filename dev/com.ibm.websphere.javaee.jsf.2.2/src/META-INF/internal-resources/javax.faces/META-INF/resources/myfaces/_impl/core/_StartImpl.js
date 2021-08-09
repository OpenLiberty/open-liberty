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
(function(){var D=window||document.body;var A="myfaces._impl.";var E={_PFX_UTIL:A+"_util.",_PFX_CORE:A+"core.",_PFX_XHR:A+"xhrCore.",_PFX_I18N:A+"i18n."};if("undefined"!=typeof D.myfaces){var C=myfaces._impl.core._Runtime;E._MF_CLS=C.extendClass;E._MF_SINGLTN=C.singletonExtendClass;}else{E._MF_CLS=false;E._MF_SINGLTN=false;D.myfaces={};}D.myfaces._implTemp={};for(var B in E){D.myfaces._implTemp[B]=D[B];D[B]=E[B];}})();