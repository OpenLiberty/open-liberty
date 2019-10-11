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
var messages = {
    // Common Strings
    "ADD_NEW": "Añadir nuevo",
    "CANCEL": "Cancelar",
    "CLEAR": "Borrar entrada de búsqueda",
    "CLICK_TO_SORT": "Pulsar para ordenar columna",
    "CLOSE": "Cerrar",
    "COPY_TO_CLIPBOARD": "Copiar en el portapapeles",
    "COPIED_TO_CLIPBOARD": "Copiado en el portapapeles",
    "DELETE": "Suprimir",
    "DONE": "Hecho",
    "EDIT": "Editar",
    "FALSE": "Falso",
    "GENERATE": "Generar",
    "LOADING": "Cargando",
    "LOGOUT": "Cierre de sesión",
    "NEXT_PAGE": "Página siguiente",
    "NO_RESULTS_FOUND": "No se han encontrado resultados.",
    "PAGES": "{0} de {1} páginas",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "Seleccione el número de página que visualizar",
    "PREVIOUS_PAGE": "Página anterior",
    "PROCESSING": "Procesando",
    "REGENERATE": "Volver a generar",
    "REGISTER": "Registrar",
    "TABLE_FIELD_SORT_ASC": "La tabla está ordenada por {0} en orden ascendente.",   // {0} - column name (ie. 'Name', 'Client ID')
    "TABLE_FIELD_SORT_DESC": "La tabla está ordenada por {0} en orden descendente.", // {0} - column name (ie. 'Name', 'Client ID')
    "TRUE": "Verdadero",
    "TRY_AGAIN": "Vuelva a intentarlo...",
    "UPDATE": "Actualizar",

    // Common Column Names
    "EXPIRES_COL": "Caduca el",
    "ISSUED_COL": "Emitido el",
    "NAME_COL": "Nombre",
    "TYPE_COL": "Tipo",

    // Account Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - the user defined name given the app-password or app-token
    "ACCT_MGR_TITLE": "Gestionar señales personales",
    "ACCT_MGR_DESC": "Crear, suprimir y volver a generar app-passwords o app-tokens.",
    "ADD_NEW_AUTHENTICATION": "Añadir nueva app-password o app-token.",
    "NAME_IDENTIFIER": "Nombre: {0}",
    "ADD_NEW_TITLE": "Registrar nueva autenticación",
    "NOT_GENERATED_PLACEHOLDER": "No se ha generado",
    "AUTHENTICAION_GENERATED": "Autenticación generada",
    "GENERATED_APP_PASSWORD": "app-password generada",
    "GENERATED_APP_TOKEN": "app-token generada",
    "COPY_APP_PASSWORD": "Copiar app-password en el portapapeles",
    "COPY_APP_TOKEN": "Copiar app-token en el portapapeles",
    "REGENERATE_APP_PASSWORD": "Volver a generar app-password",
    "REGENERATE_PW_WARNING": "Esta acción sobrescribirá la app-password actual.",
    "REGENERATE_PW_PLACEHOLDER": "Contraseña generada anteriormente el {0}",        // 0 - date
    "REGENERATE_APP_TOKEN": "Volver a generar app-token",
    "REGENERATE_TOKEN_WARNING": "Esta acción sobrescribirá la app-token actual.",
    "REGENERATE_TOKEN_PLACEHOLDER": "Señal generada anteriormente el {0}",        // 0 - date
    "DELETE_PW": "Suprimir esta app-password",
    "DELETE_TOKEN": "Suprimir esta app-token",
    "DELETE_WARNING_PW": "Esta acción eliminará la app-password asignada actualmente.",
    "DELETE_WARNING_TOKEN": "Esta acción eliminará la app-token asignada actualmente.",
    "REGENERATE_ARIA": "Volver a generar {0} para {1}",     // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_ARIA": "Suprimir la {0} denominada {1}",       // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_GENERATE_FAIL": "Error al generar {0}", // 0 - 'App-Password' or 'App-Token'
    "GENERIC_GENERATE_FAIL_MSG": "Se ha producido un error al generar una nueva {0} con el nombre {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "ERR_NAME": "El nombre ya está asociado con una {0} o es demasiado largo.", // 0 - 'app-password' or 'app-token'
    "GENERIC_DELETE_FAIL": "Error al suprimir {0}",     // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "Se ha producido un error al suprimir la {0} denominada {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL": "Error al volver a generar {0}",  // 0 - 'App-Password' or 'App-Token'
    "GENERIC_REGENERATE_FAIL_MSG": "Se ha producido un error al volver a generar la {0} denominada {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_REGENERATE_FAIL_CREATE_MSG": "Se ha producido un error al volver a generar la {0} denominada {1}. La {0} se ha suprimido pero no se ha vuelto a crear,", // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_FAIL": "Error al recuperar las autenticaciones.",
    "GENERIC_FETCH_FAIL_MSG": "No se puede obtener la lista actual de app-passwords o app-tokens.",
    "GENERIC_NOT_CONFIGURED": "Cliente no configurado",
    "GENERIC_NOT_CONFIGURED_MSG": "Los atributos de cliente appPasswordAllowed y appTokenAllowed no están configurados.  No se pueden recuperar los datos.",
    "APP_PASSWORD_NOT_CONFIGURED": "El atributo de cliente appPasswordAllowed no está configurado.",  // 'appPasswordAllowed' is a config option. Do not translate.
    "APP_TOKEN_NOT_CONFIGURED": "El atributo de cliente appTokenAllowed no está configurado."         // 'appTokenAllowed' is a config option.  Do not translate.
};
