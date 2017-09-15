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
if(!document.querySelectorAll||!window.XMLHttpRequest){(function(){var A=myfaces._impl.core._Runtime;A.getXHRObject=function(){if(window.XMLHttpRequest){var C=new XMLHttpRequest();if(!A.XHR_LEVEL){var B=A.exists;A.XHR_LEVEL=(B(C,"sendAsBinary"))?1.5:1;A.XHR_LEVEL=(B(C,"upload")&&"undefined"!=typeof FormData)?2:A.XHR_LEVEL;}return C;}try{A.XHR_LEVEL=1;return new ActiveXObject("Msxml2.XMLHTTP");}catch(D){}return new ActiveXObject("Microsoft.XMLHTTP");};})();}