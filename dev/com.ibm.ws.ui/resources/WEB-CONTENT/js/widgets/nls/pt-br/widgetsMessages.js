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
    LIBERTY_HEADER_TITLE: "Liberty Admin Center",
    LIBERTY_HEADER_PROFILE: "Preferências",
    LIBERTY_HEADER_LOGOUT: "Efetuar logout",
    LIBERTY_HEADER_LOGOUT_USERNAME: "Efetuar logout de {0}",
    TOOLBOX_BANNER_LABEL: "{0} banner",  // TOOLBOX_TITLE
    TOOLBOX_TITLE: "Caixa de ferramentas",
    TOOLBOX_TITLE_LOADING_TOOL: "Carregando ferramenta...",
    TOOLBOX_TITLE_EDIT: "Editar caixa de ferramentas",
    TOOLBOX_EDIT: "Editar",
    TOOLBOX_DONE: "Pronto",
    TOOLBOX_SEARCH: "Filtrar",
    TOOLBOX_CLEAR_SEARCH: "Limpar os critérios de filtro",
    TOOLBOX_END_SEARCH: "Terminar filtro",
    TOOLBOX_ADD_TOOL: "Incluir ferramenta",
    TOOLBOX_ADD_CATALOG_TOOL: "Incluir ferramenta",
    TOOLBOX_ADD_BOOKMARK: "Incluir marcador",
    TOOLBOX_REMOVE_TITLE: "Remover ferramenta {0}",
    TOOLBOX_REMOVE_TITLE_NO_TOOL_NAME: "Remover ferramenta",
    TOOLBOX_REMOVE_MESSAGE: "Tem certeza de que deseja remover {0}?",
    TOOLBOX_BUTTON_REMOVE: "Remover",
    TOOLBOX_BUTTON_OK: "OK",
    TOOLBOX_BUTTON_GO_TO: "Acesse a caixa de ferramentas",
    TOOLBOX_BUTTON_CANCEL: "Cancelar",
    TOOLBOX_BUTTON_BGTASK: "Tarefas em segundo plano",
    TOOLBOX_BUTTON_BACK: "Voltar",
    TOOLBOX_BUTTON_USER: "Usuário",
    TOOLBOX_ADDTOOL_ERROR_MESSAGE: "Ocorreu um erro ao incluir a ferramenta {0}: {1}",
    TOOLBOX_REMOVETOOL_ERROR_MESSAGE: "Ocorreu um erro ao remover a ferramenta {0}: {1}",
    TOOLBOX_GET_ERROR_MESSAGE: "Ocorreu um erro ao recuperar as ferramentas na caixa de ferramentas: {0}",
    TOOLCATALOG_TITLE: "Catálogo de ferramentas",
    TOOLCATALOG_ADDTOOL_TITLE: "Incluir ferramenta",
    TOOLCATALOG_ADDTOOL_MESSAGE: "Tem certeza de que deseja incluir a ferramenta {0} em sua caixa de ferramentas?",
    TOOLCATALOG_BUTTON_ADD: "Incluir",
    TOOL_FRAME_TITLE: "Quadro de ferramentas",
    TOOL_DELETE_TITLE: "Excluir {0}",
    TOOL_ADD_TITLE: "Incluir {0}",
    TOOL_ADDED_TITLE: "{0} já incluído",
    TOOL_LAUNCH_ERROR_MESSAGE_TITLE: "Ferramenta não encontrada",
    TOOL_LAUNCH_ERROR_MESSAGE: "A ferramenta solicitada não foi ativada porque ela não está no catálogo.",
    LIBERTY_UI_ERROR_MESSAGE_TITLE: "Erro",
    LIBERTY_UI_WARNING_MESSAGE_TITLE: "Aviso",
    LIBERTY_UI_INFO_MESSAGE_TITLE: "Informações",
    LIBERTY_UI_CATALOG_GET_ERROR: "Ocorreu um erro ao obter o catálogo: {0}",
    LIBERTY_UI_CATALOG_GET_TOOL_ERROR: "Ocorreu um erro ao obter a ferramenta {0} do catálogo: {1}",
    PREFERENCES_TITLE: "Preferências",
    PREFERENCES_SECTION_TITLE: "Preferências",
    PREFERENCES_ENABLE_BIDI: "Ativar o suporte bidirecional",
    PREFERENCES_BIDI_TEXTDIR: "Direção do texto",
    PREFERENCES_BIDI_TEXTDIR_LTR: "Esquerda para direita",
    PREFERENCES_BIDI_TEXTDIR_RTL: "Direita para esquerda",
    PREFERENCES_BIDI_TEXTDIR_CONTEXTUAL: "Contextual",
    PREFERENCES_SET_ERROR_MESSAGE: "Ocorreu um erro ao configurar as preferências do usuário na caixa de ferramentas: {0}",
    BGTASKS_PAGE_LABEL: "Tarefas em segundo plano",
    BGTASKS_DEPLOYMENT_INSTALLATION_POPUP: "Implementar Instalação {0}", // {0} is the token number associated with the deployment
    BGTASKS_DEPLOYMENT_INSTALLATION: "Implementar Instalação {0} - {1}", // {0} is the token number associated with the deployment, {1} is the server name
    BGTASKS_STATUS_RUNNING: "Em execução",
    BGTASKS_STATUS_FAILED: "Erro",
    BGTASKS_STATUS_SUCCEEDED: "Concluído", 
    BGTASKS_STATUS_WARNING: "Parcial bem-sucedido",
    BGTASKS_STATUS_PENDING: "Pendente",
    BGTASKS_INFO_DIALOG_TITLE: "Detalhes",
    //BGTASKS_INFO_DIALOG_DESC: "Description:",
    BGTASKS_INFO_DIALOG_STDOUT: "Saída padrão:",
    BGTASKS_INFO_DIALOG_STDERR: "Erro padrão:",
    BGTASKS_INFO_DIALOG_EXCEPTION: "Exceção:",
    //BGTASKS_INFO_DIALOG_RETURN_CODE: "Return code:",
    BGTASKS_INFO_DIALOG_RESULT: "Resultado:",
    BGTASKS_INFO_DIALOG_DEPLOYED_ARTIFACT_NAME: "Nome do Servidor:",
    BGTASKS_INFO_DIALOG_DEPLOYED_USER_DIR: "Diretório do usuário:",
    BGTASKS_POPUP_RUNNING_TASK_TITLE: "Tarefas em segundo plano ativas",
    BGTASKS_POPUP_RUNNING_TASK_NONE: "Nenhum(a)",
    BGTASKS_POPUP_RUNNING_TASK_NO_ACTIVE_TASK: "Nenhuma tarefa em segundo plano ativa",
    BGTASKS_DISPLAY_BUTTON: "Detalhes da Tarefa e Histórico",
    BGTASKS_EXPAND: "Expandir seção",
    BGTASKS_COLLAPSE: "Reduzir seção",
    PROFILE_MENU_HELP_TITLE: "Ajuda",
    DETAILS_DESCRIPTION: "Descrição",
    DETAILS_OVERVIEW: "Visão Geral",
    DETAILS_OTHERVERSIONS: "Outras versões",
    DETAILS_VERSION: "Versão: {0}",
    DETAILS_UPDATED: "Atualizado: {0}",
    DETAILS_NOTOPTIMIZED: "Não otimizado para o dispositivo atual.",
    DETAILS_ADDBUTTON: "Incluir em minha caixa de ferramentas",
    DETAILS_OPEN: "Abrir",
    DETAILS_CATEGORY: "Categoria {0}",
    DETAILS_ADDCONFIRM: "A ferramenta {0} foi incluída com sucesso na caixa de ferramentas.",
    CONFIRM_DIALOG_HELP: "Ajuda",
    YES_BUTTON_LABEL: "{0} sim",  // insert is dialog title
    NO_BUTTON_LABEL: "{0} não",  // insert is dialog title

    YES: "Sim",
    NO: "Não",

    TOOL_OIDC_ACCESS_DENIED: "O usuário não está na função que tem permissão para concluir essa solicitação.",
    TOOL_OIDC_GENERIC_ERROR: "Ocorreu um erro. Revise o erro no log para obter mais informações.",
    TOOL_DISABLE: "O usuário não tem permissão para usar essa ferramenta. Apenas os usuários na função de Administrador têm permissão para usar essa ferramenta." 
});
