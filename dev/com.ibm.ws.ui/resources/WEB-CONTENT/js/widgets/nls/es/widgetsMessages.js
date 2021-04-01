/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 
 define({
    LIBERTY_HEADER_TITLE: "Centro de administración de Liberty",
    LIBERTY_HEADER_PROFILE: "Preferencias",
    LIBERTY_HEADER_LOGOUT: "Finalizar sesión",
    LIBERTY_HEADER_LOGOUT_USERNAME: "Finalizar sesión {0}",
    TOOLBOX_BANNER_LABEL: "Banner de {0}",  // TOOLBOX_TITLE
    TOOLBOX_TITLE: "Caja de herramientas",
    TOOLBOX_TITLE_LOADING_TOOL: "Cargando herramienta...",
    TOOLBOX_TITLE_EDIT: "Editar caja de herramientas",
    TOOLBOX_EDIT: "Editar",
    TOOLBOX_DONE: "Hecho",
    TOOLBOX_SEARCH: "Filtro",
    TOOLBOX_CLEAR_SEARCH: "Borrar criterios de filtro",
    TOOLBOX_END_SEARCH: "Terminar filtro",
    TOOLBOX_ADD_TOOL: "Añadir herramienta",
    TOOLBOX_ADD_CATALOG_TOOL: "Añadir herramienta",
    TOOLBOX_ADD_BOOKMARK: "Añadir marcador",
    TOOLBOX_REMOVE_TITLE: "Eliminar herramienta {0}",
    TOOLBOX_REMOVE_TITLE_NO_TOOL_NAME: "Eliminar herramienta",
    TOOLBOX_REMOVE_MESSAGE: "¿Está seguro de que desea eliminar {0}?",
    TOOLBOX_BUTTON_REMOVE: "Eliminar",
    TOOLBOX_BUTTON_OK: "Aceptar",
    TOOLBOX_BUTTON_GO_TO: "Ir a la caja de herramientas",
    TOOLBOX_BUTTON_CANCEL: "Cancelar",
    TOOLBOX_BUTTON_BGTASK: "Tareas en segundo plano",
    TOOLBOX_BUTTON_BACK: "Anterior",
    TOOLBOX_BUTTON_USER: "Usuario",
    TOOLBOX_ADDTOOL_ERROR_MESSAGE: "Se ha producido un error al añadir la herramienta {0}: {1}",
    TOOLBOX_REMOVETOOL_ERROR_MESSAGE: "Se ha producido un error al eliminar la herramienta {0}: {1}",
    TOOLBOX_GET_ERROR_MESSAGE: "Se ha producido un error al recuperar las herramientas de la caja de herramientas: {0}",
    TOOLCATALOG_TITLE: "Catálogo de herramientas",
    TOOLCATALOG_ADDTOOL_TITLE: "Añadir herramienta",
    TOOLCATALOG_ADDTOOL_MESSAGE: "¿Está seguro de que desea añadir la herramienta {0} a su caja de herramientas?",
    TOOLCATALOG_BUTTON_ADD: "Añadir",
    TOOL_FRAME_TITLE: "Marco de herramientas",
    TOOL_DELETE_TITLE: "Suprimir {0}",
    TOOL_ADD_TITLE: "Añadir {0}",
    TOOL_ADDED_TITLE: "{0} ya se ha añadido",
    TOOL_LAUNCH_ERROR_MESSAGE_TITLE: "No se ha encontrado la herramienta",
    TOOL_LAUNCH_ERROR_MESSAGE: "La herramienta solicitada no se inició porque no está en el catálogo.",
    LIBERTY_UI_ERROR_MESSAGE_TITLE: "Error",
    LIBERTY_UI_WARNING_MESSAGE_TITLE: "Aviso",
    LIBERTY_UI_INFO_MESSAGE_TITLE: "Información",
    LIBERTY_UI_CATALOG_GET_ERROR: "Se ha producido un error al obtener el catálogo: {0}",
    LIBERTY_UI_CATALOG_GET_TOOL_ERROR: "Se ha producido un error al obtener la herramienta {0} del catálogo: {1}",
    PREFERENCES_TITLE: "Preferencias",
    PREFERENCES_SECTION_TITLE: "Preferencias",
    PREFERENCES_ENABLE_BIDI: "Habilitar soporte bidireccional",
    PREFERENCES_BIDI_TEXTDIR: "Dirección del texto",
    PREFERENCES_BIDI_TEXTDIR_LTR: "De izquierda a derecha",
    PREFERENCES_BIDI_TEXTDIR_RTL: "De derecha a izquierda",
    PREFERENCES_BIDI_TEXTDIR_CONTEXTUAL: "Contextual",
    PREFERENCES_SET_ERROR_MESSAGE: "Se ha producido un error al establecer las preferencias de usuario de la caja de herramientas: {0}",
    BGTASKS_PAGE_LABEL: "Tareas en segundo plano",
    BGTASKS_DEPLOYMENT_INSTALLATION_POPUP: "Desplegar instalación {0}", // {0} is the token number associated with the deployment
    BGTASKS_DEPLOYMENT_INSTALLATION: "Desplegar instalación {0} - {1}", // {0} is the token number associated with the deployment, {1} is the server name
    BGTASKS_STATUS_RUNNING: "Ejecutándose",
    BGTASKS_STATUS_FAILED: "Errónea",
    BGTASKS_STATUS_SUCCEEDED: "Finalizado", 
    BGTASKS_STATUS_WARNING: "Satisfactorio parcialmente",
    BGTASKS_STATUS_PENDING: "Pendiente",
    BGTASKS_INFO_DIALOG_TITLE: "Detalles",
    //BGTASKS_INFO_DIALOG_DESC: "Description:",
    BGTASKS_INFO_DIALOG_STDOUT: "Salida estándar:",
    BGTASKS_INFO_DIALOG_STDERR: "Error estándar:",
    BGTASKS_INFO_DIALOG_EXCEPTION: "Excepción:",
    //BGTASKS_INFO_DIALOG_RETURN_CODE: "Return code:",
    BGTASKS_INFO_DIALOG_RESULT: "Resultado:",
    BGTASKS_INFO_DIALOG_DEPLOYED_ARTIFACT_NAME: "Nombre de servidor:",
    BGTASKS_INFO_DIALOG_DEPLOYED_USER_DIR: "Directorio de usuario:",
    BGTASKS_POPUP_RUNNING_TASK_TITLE: "Tareas en segundo plano activas",
    BGTASKS_POPUP_RUNNING_TASK_NONE: "Ninguna",
    BGTASKS_POPUP_RUNNING_TASK_NO_ACTIVE_TASK: "No hay tareas en segundo plano activas",
    BGTASKS_DISPLAY_BUTTON: "Detalles e historial de las tareas",
    BGTASKS_EXPAND: "Expandir sección",
    BGTASKS_COLLAPSE: "Contraer la sección",
    PROFILE_MENU_HELP_TITLE: "Ayuda",
    DETAILS_DESCRIPTION: "Descripción",
    DETAILS_OVERVIEW: "Descripción general",
    DETAILS_OTHERVERSIONS: "Otras versiones",
    DETAILS_VERSION: "Versión: {0}",
    DETAILS_UPDATED: "Actualizado: {0}",
    DETAILS_NOTOPTIMIZED: "No está optimizado para el dispositivo actual.",
    DETAILS_ADDBUTTON: "Añadir a Mi caja de herramientas",
    DETAILS_OPEN: "Abrir",
    DETAILS_CATEGORY: "Categoría {0}",
    DETAILS_ADDCONFIRM: "La herramienta {0} se ha añadido satisfactoriamente a la caja de herramientas.",
    CONFIRM_DIALOG_HELP: "Ayuda",
    YES_BUTTON_LABEL: "{0} sí",  // insert is dialog title
    NO_BUTTON_LABEL: "{0} no",  // insert is dialog title

    YES: "Sí",
    NO: "No",

    TOOL_OIDC_ACCESS_DENIED: "El usuario no está en el rol que tiene permiso para completar esta solicitud.",
    TOOL_OIDC_GENERIC_ERROR: "Se ha producido un error. Revise el error en el registro para obtener más información.",
    TOOL_DISABLE: "El usuario no tiene permiso para utilizar esta herramienta. Solo los usuarios con el rol de Administrador tienen permiso para utilizar esta herramienta." 
});
