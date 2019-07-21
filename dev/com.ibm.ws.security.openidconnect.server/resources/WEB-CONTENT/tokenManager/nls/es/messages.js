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
    "CLEAR_SEARCH": "Borrar entrada de búsqueda",
    "CLEAR_FILTER": "Borrar filtro",
    "CLICK_TO_SORT": "Pulsar para ordenar columna",
    "CLOSE": "Cerrar",
    "COPY_TO_CLIPBOARD": "Copiar en el portapapeles",
    "COPIED_TO_CLIPBOARD": "Copiado en el portapapeles",
    "DELETE": "Suprimir",
    "DONE": "Hecho",
    "EDIT": "Editar",
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
    "TABLE_BATCH_BAR": "Barra de acciones de tabla",
    "TRY_AGAIN": "Vuelva a intentarlo...",
    "UPDATE": "Actualizar",

    // Common Column Names
    "CLIENT_NAME_COL": "Nombre de cliente",
    "EXPIRES_COL": "Caduca el",
    "ISSUED_COL": "Emitido el",
    "NAME_COL": "Nombre",
    "TYPE_COL": "Tipo",

    // Token Manager
    // 'app-token' and 'app-password' are keywords - don't translate
    // name - user defined name given to the app-password/app-token; user id - user's login name
    "TOKEN_MGR_TITLE": "Suprimir señales",
    "TOKEN_MGR_DESC": "Suprimir contraseñas de aplicación o señales de aplicación para un usuario específico.",
    "TOKEN_MGR_SEARCH_PLACEHOLDER": "Especificar el ID de usuario",
    "DELETE_SELECTED": "Suprimir las contraseñas de aplicación o señales de aplicación seleccionadas",
    "DELETE_ARIA": "Suprimir la {0} denominada {1}",         // 0 - 'app-password' or 'app-token'; 1 - name
    "DELETE_PW": "Suprimir esta contraseña de aplicación",
    "DELETE_TOKEN": "Suprimir esta señal de aplicación",
    "DELETE_FOR_USERID": "{0} para {1}",                // 0 - name; 1 - user id
    "DELETE_WARNING_PW": "Esta acción eliminará la contraseña de aplicación asignada actualmente.",
    "DELETE_WARNING_TOKEN": "Esta acción eliminará la señal de aplicación asignada actualmente.",
    "DELETE_MANY": "Suprimir contraseñas de aplicación/señales de aplicación",
    "DELETE_MANY_FOR": "Asignada a {0}",              // 0 - user id
    "DELETE_ONE_MESSAGE": "Esta acción suprimirá la contraseña de aplicación/señal de aplicación seleccionada",
    "DELETE_MANY_MESSAGE": "Esta acción suprimirá las contraseñas de aplicación/señales de aplicación {0} seleccionadas.",  // 0 - number
    "DELETE_ALL_MESSAGE": "Esta acción suprimirá todas las contraseñas de aplicación/señales de aplicación que pertenecen a {0}.", // 0 - user id
    "DELETE_NONE": "Seleccionar para supresión",
    "DELETE_NONE_MESSAGE": "Seleccione un recuadro para indicar qué contraseñas de aplicación/señales de aplicación deben suprimirse.",
    "SINGLE_ITEM_SELECTED": "1 elemento seleccionado",
    "ITEMS_SELECTED": "{0} elementos seleccionados",            // 0 - number
    "SELECT_ALL_AUTHS": "Seleccione todas las contraseñas de aplicación/señales de aplicación de este usuario.",
    "SELECT_SPECIFIC": "Seleccione la {0} denominada {1} para supresión",  // 0 - 'app-password' or 'app-token; 1 - name
    "NO_QUERY": "¿Está buscando algo? Especifique un ID de usuario para ver sus contraseñas de aplicación/señales de aplicación.",
    "GENERIC_FETCH_FAIL": "Error al recuperar {0}",      // 0 - 'App-Passwords' or 'App-Tokens'
    "GENERIC_FETCH_FAIL_MSG": "No se puede obtener la lista de {0} que pertenecen a {1}.", // 0 - 'app-passwords' or 'app-tokens; 1 - user id
    "GENERIC_DELETE_FAIL": "Error al suprimir {0}",       // 0 - 'App-Password' or 'App-Token'
    "GENERIC_DELETE_FAIL_MSG": "Se ha producido un error al suprimir la {0} denominada {1}.",  // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_DELETE_ALL_FAIL_MSG": "Se ha producido un error al suprimir la {0} de {1}.",     // 0 - 'app-passwords' or 'app-tokens'; 1 - user id
    "GENERIC_DELETE_FAIL_NOTYPES": "Error al suprimir",
    "GENERIC_DELETE_FAIL_NOTYPES_ONE_MSG": "Se ha producido un error al suprimir la siguiente contraseña de aplicación o señal de aplicación:",
    "GENERIC_DELETE_FAIL_NOTYPES_MSG": "Se ha producido un error al suprimir las siguientes contraseñas de aplicación o señales de aplicación {0}:",  // 0 - number
    "IDENTIFY_AUTH": "{0} {1}",   // 0 - 'app-password' or 'app-token'; 1 - name
    "GENERIC_FETCH_ALL_FAIL": "Error al recuperar las autenticaciones.",
    "GENERIC_FETCH_ALL_FAIL_MSG": "No se puede obtener la lista de contraseñas de aplicación y señales de aplicación que pertenecen a {0}.",   // 0 - user id
    "GENERIC_NOT_CONFIGURED": "Cliente no configurado",
    "GENERIC_NOT_CONFIGURED_MSG": "Los atributos de cliente appPasswordAllowed y appTokenAllowed no están configurados.  No se pueden recuperar los datos."
};
