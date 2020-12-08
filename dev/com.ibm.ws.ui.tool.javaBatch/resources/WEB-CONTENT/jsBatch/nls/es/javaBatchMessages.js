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

define({
      ACCOUNTING_STRING : "Serie de cuentas",
      SEARCH_RESOURCE_TYPE_ALL : "Todo",
      SEARCH : "Buscar",
      JAVA_BATCH_SEARCH_BOX_LABEL : "Indique los criterios de búsqueda seleccionando el botón Añadir criterios de búsqueda y especificando un valor a continuación",
      SUBMITTED : "Enviado",
      JMS_QUEUED : "JMS en cola",
      JMS_CONSUMED : "JMS consumido",
      JOB_PARAMETER : "Parámetro de trabajo",
      DISPATCHED : "Asignado",
      FAILED : "Erróneo",
      STOPPED : "Detenido",
      COMPLETED : "Completado",
      ABANDONED : "Abandonado",
      STARTED : "Iniciado",
      STARTING : "Iniciando",
      STOPPING : "Deteniendo",
      REFRESH : "Renovar",
      INSTANCE_STATE : "Estado de instancia",
      APPLICATION_NAME : "Nombre de aplicación",
      APPLICATION: "Aplicación",
      INSTANCE_ID : "ID de instancia",
      LAST_UPDATE : "Última actualización",
      LAST_UPDATE_RANGE : "Rango de última actualización",
      LAST_UPDATED_TIME : "Hora de última actualización",
      DASHBOARD_VIEW : "Vista de panel de instrumentos",
      HOMEPAGE : "Página inicial",
      JOBLOGS : "Registros de trabajos",
      QUEUED : "En cola",
      ENDED : "Finalizados",
      ERROR : "Error",
      CLOSE : "Cerrar",
      WARNING : "Aviso",
      GO_TO_DASHBOARD: "Ir al panel de instrumentos",
      DASHBOARD : "Panel de control",
      BATCH_JOB_NAME: "Nombre de trabajo por lotes",
      SUBMITTER: "Originador",
      BATCH_STATUS: "Estado de lotes",
      EXECUTION_ID: "ID de ejecución de trabajos",
      EXIT_STATUS: "Estado de salida",
      CREATE_TIME: "Hora de creación",
      START_TIME: "Hora de inicio",
      END_TIME: "Hora de finalización",
      SERVER: "Servidor",
      SERVER_NAME: "Nombre del servidor",
      SERVER_USER_DIRECTORY: "Directorio de usuario",
      SERVERS_USER_DIRECTORY: "Directorio de usuario del servidor",
      HOST: "Host",
      NAME: "Nombre",
      JOB_PARAMETERS: "Parámetros de trabajo",
      JES_JOB_NAME: "Nombre de trabajo de JES",
      JES_JOB_ID: "ID de trabajo de JES",
      ACTIONS: "Acciones",
      VIEW_LOG_FILE: "Ver archivo de registro",
      STEP_NAME: "Nombre del paso",
      ID: "ID",
      PARTITION_ID: "Partición {0}",                               // Partition ID number
      VIEW_EXECUTION_DETAILS: "Ver detalles de ejecución de trabajos {0}",    // Job Execution ID number
      PARENT_DETAILS: "Detalles de la información de padre",
      TIMES: "Horas",      // Heading on section referencing create, start, and end timestamps
      STATUS: "Estado",
      SEARCH_ON: "Seleccione para filtrar en {1} {0}",                    // {0} Value, {1} Column name
      SEARCH_PILL_INPUT : "Especifique los criterios de búsqueda.",
      BREADCRUMB_JOB_INSTANCE : "Instancia de trabajo {0}",                // Job Instance ID
      BREADCRUMB_JOB_EXECUTION : "Ejecución de trabajo {0}",              // Job Execution ID
      BREADCRUMB_JOB_LOG : "Registro de trabajo {0}",
      BATCH_SEARCH_CRITERIA_INVALID : "Los criterios de búsqueda no son válidos.",
      ERROR_CANNOT_HAVE_MULTIPLE_SEARCHBY : "Los criterios de búsqueda no pueden tener varios filtros por parámetros de {0}.", // {0} will be another translated message key like SUBMITTER and BATCH_JOB_NAME

      INSTANCES_TABLE_IDENTIFIER: "Tabla de instancias de trabajos",
      EXECUTIONS_TABLE_IDENTIFIER: "Tabla de ejecuciones de trabajos",
      STEPS_DETAILS_TABLE_IDENTIFIER: "Tabla de detalles de pasos",
      LOADING_VIEW : "La página está cargando información actualmente",
      LOADING_VIEW_TITLE : "Cargando vista",
      LOADING_GRID : "Esperando los resultados de búsqueda para volver del servidor",
      PAGENUMBER : "Número de página",
      SELECT_QUERY_SIZE: "Seleccionar tamaño de consulta",
      LINK_EXPLORE_HOST: "Seleccionar para ver detalles sobre el host {0} en la herramienta Explorar.",      // Host name
      LINK_EXPLORE_SERVER: "Seleccionar para ver detalles sobre el servidor {0} en la herramienta Explorar.",  // Server name

      //ACTIONS
      RESTART: "Reiniciar",
      STOP: "Detener",
      PURGE: "Depurar",
      OK_BUTTON_LABEL: "Aceptar",
      INSTANCE_ACTIONS_BUTTON_LABEL: "Acciones de la instancia de trabajo {0}",     // Job Instance ID number
      INSTANCE_ACTIONS_MENU_LABEL:  "Menú de acciones de la instancia de trabajo",

      RESTART_INSTANCE_MESSAGE: "¿Desea reiniciar la ejecución del trabajo más reciente asociado a la instancia de trabajo {0}?",     // Job Instance ID number
      STOP_INSTANCE_MESSAGE: "¿Desea detener la ejecución del trabajo más reciente asociado a la instancia de trabajo {0}?",           // Job Instance ID number
      PURGE_INSTANCE_MESSAGE: "¿Desea depurar todas las entradas de base de datos y los registros de trabajo asociados a la instancia de trabajo {0}?",     // Job Instance ID number
      PURGE_JOBSTORE_ONLY: "Depurar únicamente el almacén de trabajos",

      RESTART_INST_ERROR_MESSAGE: "La solicitud de reinicio ha fallado.",
      STOP_INST_ERROR_MESSAGE: "La solicitud de detención ha fallado.",
      PURGE_INST_ERROR_MESSAGE: "La solicitud de depuración ha fallado.",
      ACTION_REQUEST_ERROR_MESSAGE: "La solicitud de acción ha fallado con el código de estado: {0}.  URL: {1}",  // Status Code number, URL string

      // RESTART JOB WITH PARAMETERS DIALOG
      REUSE_PARMS_TOGGLE_LABEL: "Reutilizar parámetros de la ejecución previa",
      JOB_PARAMETERS_EMPTY: "Si no se ha seleccionado '{0}', utilice esta área para especificar los parámetros del trabajo.",   // 0 - Checkbox label - REUSE_PARMS_TOGGLE_LABEL
      JOB_PARAMETER_NAME: "Nombre de parámetro",
      JOB_PARAMETER_VALUE: "Valor de parámetro",
      PARM_NAME_COLUMN_HEADER: "Parámetro",
      PARM_VALUE_COLUMN_HEADER: "Valor",
      PARM_ADD_ICON_TITLE: "Añadir parámetro",
      PARM_REMOVE_ICON_TITLE: "Eliminar parámetro",
      PARMS_ENTRY_ERROR: "Es necesario el nombre de parámetro.",
      JOB_PARAMETER_CREATE: "Seleccione {0} para añadir parámetros a la próxima ejecución de esta instancia de trabajo.",  // 0 - Button label
      JOB_PARAMETER_CREATE_BUTTON: "Añadir botón de parámetro a la cabecera de tabla.",

      //JOB LOGS PAGE MESSAGES
      JOBLOGS_LOG_CONTENT : "Contenido de registro de trabajo",
      FILE_DOWNLOAD : "Descarga de archivo",
      DOWNLOAD_DIALOG_DESCRIPTION : "¿Desea descargar el archivo de registro?",
      INCLUDE_ALL_LOGS : "Incluir todos los archivos de registro para ejecución de trabajos",
      LOGS_NAVIGATION_BAR : "Barra de navegación de registros de trabajos",
      DOWNLOAD : "Descargar",
      LOG_TOP : "Principio de registros",
      LOG_END : "Final de registros",
      PREVIOUS_PAGE : "Página anterior",
      NEXT_PAGE : "Página siguiente",
      DOWNLOAD_ARIA : "Descargar archivo",

      //Error messages for popups
      REST_CALL_FAILED : "La llamada para captar datos ha fallado.",
      NO_JOB_EXECUTION_URL : "No se ha proporcionado número de ejecución de trabajos en el URL o la instancia no tiene registros de ejecución de trabajos para mostrar",
      NO_VIEW : "Error de URL: No existe esta vista.",
      WRONG_TOOL_ID : "La serie de consulta del URL no se ha iniciado con el ID de herramienta {0}, sino con {1}.",   // {0} and {1} are both Strings
      URL_NO_LOGS : "Error de URL: No existen registros.",
      NOT_A_NUMBER : "Error de URL: {0} debe ser un número.",                                                // {0} is a field name
      PARAMETER_REPETITION : "Error de URL: {0} solo puede existir una vez en los parámetros.",                   // {0} is a field name
      URL_PAGE_PARAMS_ERROR : "Error de URL: el parámetro de página está fuera de rango.",
      INVALID_PARAMETER : "Error de URL: {0} no es un parámetro válido.",                                       // {0} is a String
      URL_MULTIPLE_ATTRIBUTES : "Error de URL: El URL puede especificar una ejecución de trabajos o una instancia de trabajo, pero no ambas.",
      MISSING_EXECUTION_ID_PARAM : "Falta el ID de parámetro de ejecución necesario.",
      PERSISTENCE_CONFIGURATION_REQUIRED : "Es necesaria una configuración de base de datos persistente por lotes de Java para utilizar la herramienta Java Batch.",
      IGNORED_SEARCH_CRITERIA : "Se han ignorado los criterios de búsqueda siguientes en los resultados: {0}",

      GRIDX_SUMMARY_TEXT : "Mostrando las últimas ${0} instancias de trabajo"

});

