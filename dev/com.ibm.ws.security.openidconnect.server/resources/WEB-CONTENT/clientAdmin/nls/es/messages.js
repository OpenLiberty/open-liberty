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
    "LOGOUT": "Cerrar sesión",
    "NEXT_PAGE": "Página siguiente",
    "NO_RESULTS_FOUND": "No se han encontrado resultados.",
    "PAGES": "{0} de {1} páginas",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "Seleccione el número de página que visualizar",
    "PREVIOUS_PAGE": "Página anterior",
    "PROCESSING": "Procesando",
    "REGENERATE": "Volver a generar",
    "REGISTER": "Registrar",
    "TRY_AGAIN": "Vuelva a intentarlo...",
    "UPDATE": "Actualizar",

    // Common Column Names
    "CLIENT_NAME_COL": "Nombre de cliente",
    "EXPIRES_COL": "Caduca el",
    "ISSUED_COL": "Emitido el",
    "NAME_COL": "Nombre",
    "TYPE_COL": "Tipo",

    // Client Admin
    "CLIENT_ADMIN_TITLE": "Gestionar clientes OAuth",
    "CLIENT_ADMIN_DESC": "Utilice esta herramienta para añadir y editar clientes y para volver a generar secretos de cliente.",
    "CLIENT_ADMIN_SEARCH_PLACEHOLDER": "Filtrar nombre de cliente OAuth",
    "ADD_NEW_CLIENT": "Añadir nuevo cliente OAuth",
    "CLIENT_NAME": "Nombre de cliente",
    "CLIENT_ID": "ID de cliente",
    "EDIT_ARIA": "Editar el cliente OAuth {0}",      // {0} - name
    "DELETE_ARIA": "Suprimir el cliente OAuth {0}",  // {0} - name
    "CLIENT_SECRET": "Secreto de cliente",
    "GRANT_TYPES": "Tipos de concesión",
    "SCOPE": "Ámbito",
    "PREAUTHORIZED_SCOPE": "Ámbito autorizado previamente (opcional)",
    "REDIRECT_URLS": "URL de redirección (opcional)",
    "ADDITIONAL_PROPS": "Propiedades adicionales",
    "ADDITIONAL_PROPS_OPTIONAL": "Propiedades adicionales (opcional)",
    "CLIENT_SECRET_CHECKBOX": "Volver a generar secreto de cliente",
    "PROPERTY_PLACEHOLDER": "Propiedad",
    "VALUE_PLACEHOLDER": "Valor",
    "GRANT_TYPES_SELECTED": "Número de tipos de concesión seleccionados",
    "GRANT_TYPES_NONE_SELECTED": "No hay nada seleccionado",
    "MODAL_EDIT_TITLE": "Editar cliente OAuth",
    "MODAL_REGISTER_TITLE": "Registrar cliente OAuth",
    "MODAL_SECRET_REGISTER_TITLE": "Registro OAuth guardado",
    "MODAL_SECRET_UPDATED_TITLE": "Registro OAuth actualizado",
    "MODAL_DELETE_CLIENT_TITLE": "Suprimir este cliente OAuth",
    "VALUE_COL": "Valor",
    "ADD": "Añadir",
    "DELETE_PROP": "Suprimir la propiedad personalizada",
    "RESET_GRANT_TYPE": "Borrar todos los tipos de concesión seleccionados",
    "SELECT_ONE_GRANT_TYPE": "Seleccione al menos un tipo de concesión",
    "OPEN_GRANT_TYPE": "Abrir lista de tipos de concesión",
    "CLOSE_GRANT_TYPE": "Cerrar lista de tipos de concesión",
    "SPACE_HELPER_TEXT": "Valores separados por espacios",
    "REDIRECT_URL_HELPER_TEXT": "URL de redirección absoluta separados por comas",
    "DELETE_OAUTH_CLIENT_DESC": "Esta operación suprime el cliente registrado del servicio de registro de cliente.",
    "REGISTRATION_SAVED": "Se han generado y asignado un ID de cliente y un Secreto de cliente.",
    "REGISTRATION_UPDATED": "Se ha generado y asignado un nuevo Secreto de cliente para este cliente",
    "REGISTRATION_UPDATED_NOSECRET": "El cliente {0} se ha actualizado.",                 // {0} - client name
    "ERR_MULTISELECT_GRANT_TYPES": "Al menos se debe seleccionar un tipo de concesión.",
    "ERR_REDIRECT_URIS": "Los valores deben ser URIs absolutas.",
    "GENERIC_REGISTER_FAIL": "Error al registrar cliente OAuth",
    "GENERIC_UPDATE_FAIL": "Error al actualizar cliente OAuth",
    "GENERIC_DELETE_FAIL": "Error al suprimir cliente OAuth",
    "GENERIC_MISSING_CLIENT": "Error al recuperar cliente OAuth",
    "GENERIC_REGISTER_FAIL_MSG": "Se ha producido un error al registrar el cliente OAuth {0}.",  // {0} - client name
    "GENERIC_UPDATE_FAIL_MSG": "Se ha producido un error al actualizar el cliente OAuth {0}.",       // {0} - client name
    "GENERIC_DELETE_FAIL_MSG": "Se ha producido un error al suprimir el cliente OAuth {0}.",       // {0} - client name
    "GENERIC_MISSING_CLIENT_MSG": "No se ha encontrado el cliente OAuth {0} con el ID {1}.",     // {0} - client name; {1} - an ID
    "GENERIC_RETRIEVAL_FAIL_MSG": "Se ha producido un error al recuperar información del cliente OAuth {0}.", // {0} - client name
    "GENERIC_GET_CLIENTS_FAIL": "Error al recuperar clientes OAuth",
    "GENERIC_GET_CLIENTS_FAIL_MSG": "Se ha producido un error al recuperar la lista de clientes OAuth."
};
