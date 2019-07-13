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
    "ADD_NEW": "Incluir nova",
    "CANCEL": "Cancelar",
    "CLEAR_SEARCH": "Limpar entrada de procura",
    "CLEAR_FILTER": "Limpar Filtro",
    "CLICK_TO_SORT": "Clique para classificar a coluna",
    "CLOSE": "Fechar",
    "COPY_TO_CLIPBOARD": "Copiar para a área de transferência",
    "COPIED_TO_CLIPBOARD": "Copiado para área de transferência",
    "DELETE": "Excluir",
    "DONE": "Pronto",
    "EDIT": "Editar",
    "GENERATE": "Gerar",
    "LOADING": "Carregando",
    "LOGOUT": "Logout",
    "NEXT_PAGE": "Próxima página",
    "NO_RESULTS_FOUND": "Nenhum Resultado Localizado",
    "PAGES": "{0} de {1} páginas",   // {0} - current page number; {1} - total pages
    "PAGE_SELECT": "Selecione o número da página para visualização",
    "PREVIOUS_PAGE": "Página anterior",
    "PROCESSING": "Processando",
    "REGENERATE": "Regenerar",
    "REGISTER": "Registrar",
    "TRY_AGAIN": "Tente novamente ...",
    "UPDATE": "Atualizar",

    // Common Column Names
    "CLIENT_NAME_COL": "Nome do Cliente",
    "EXPIRES_COL": "Expira em",
    "ISSUED_COL": "Emitido em",
    "NAME_COL": "Nome",
    "TYPE_COL": "Tipo",

    // Client Admin
    "CLIENT_ADMIN_TITLE": "Gerenciar clientes OAuth",
    "CLIENT_ADMIN_DESC": "Use esta ferramenta para incluir e editar clientes e para gerar novamente segredos do cliente.",
    "CLIENT_ADMIN_SEARCH_PLACEHOLDER": "Filtrar o nome do cliente OAuth",
    "ADD_NEW_CLIENT": "Incluir novo cliente OAuth",
    "CLIENT_NAME": "Nome do cliente",
    "CLIENT_ID": "ID Cliente",
    "EDIT_ARIA": "Editar o cliente OAuth {0}",      // {0} - name
    "DELETE_ARIA": "Excluir o cliente OAuth {0}",  // {0} - name
    "CLIENT_SECRET": "Segredo do cliente",
    "GRANT_TYPES": "Tipos de concessão",
    "SCOPE": "Escopo",
    "PREAUTHORIZED_SCOPE": "Escopo pré-autorizado (opcional)",
    "REDIRECT_URLS": "URLs de redirecionamento (optional)",
    "ADDITIONAL_PROPS": "Propriedades adicionais",
    "ADDITIONAL_PROPS_OPTIONAL": "Propriedades adicionais (opcional)",
    "CLIENT_SECRET_CHECKBOX": "Gerar novamente o segredo do cliente",
    "PROPERTY_PLACEHOLDER": "Propriedade",
    "VALUE_PLACEHOLDER": "Valor",
    "GRANT_TYPES_SELECTED": "Número de tipos de concessão selecionados",
    "GRANT_TYPES_NONE_SELECTED": "Nenhum selecionado",
    "MODAL_EDIT_TITLE": "Editar cliente OAuth",
    "MODAL_REGISTER_TITLE": "Registrar novo cliente OAuth",
    "MODAL_SECRET_REGISTER_TITLE": "Registro de OAuth salvo",
    "MODAL_SECRET_UPDATED_TITLE": "Registro de OAuth atualizado",
    "MODAL_DELETE_CLIENT_TITLE": "Excluir este cliente OAuth",
    "VALUE_COL": "Valor",
    "ADD": "Incluir",
    "DELETE_PROP": "Exclua a propriedade customizada",
    "RESET_GRANT_TYPE": "Limpe todos os tipos de concessão selecionados",
    "SELECT_ONE_GRANT_TYPE": "Selecione pelo menos um tipo de concessão",
    "OPEN_GRANT_TYPE": "Abrir lista de tipos de concessão",
    "CLOSE_GRANT_TYPE": "Fechar lista de tipos de concessão",
    "SPACE_HELPER_TEXT": "Valores separados por espaço",
    "REDIRECT_URL_HELPER_TEXT": "URLs de redirecionamento absoluto separadas por espaço",
    "DELETE_OAUTH_CLIENT_DESC": "Esta operação exclui o cliente registrado do serviço de registro do cliente.",
    "REGISTRATION_SAVED": "Um ID do Cliente e um Segredo do Cliente foram gerados e designados.",
    "REGISTRATION_UPDATED": "Um novo Segredo do Cliente foi gerado e designado para este cliente.",
    "REGISTRATION_UPDATED_NOSECRET": "O cliente OAuth {0} foi atualizado.",                 // {0} - client name
    "ERR_MULTISELECT_GRANT_TYPES": "Pelo menos um tipo de concessão deve ser selecionado.",
    "ERR_REDIRECT_URIS": "Os valores devem ser URIs absolutos.",
    "GENERIC_REGISTER_FAIL": "Erro ao registrar o cliente OAuth",
    "GENERIC_UPDATE_FAIL": "Erro ao atualizar o cliente OAuth",
    "GENERIC_DELETE_FAIL": "Erro ao excluir o cliente OAuth",
    "GENERIC_MISSING_CLIENT": "Erro ao recuperar o cliente OAuth",
    "GENERIC_REGISTER_FAIL_MSG": "Ocorreu um erro ao registrar o cliente OAuth {0}.",  // {0} - client name
    "GENERIC_UPDATE_FAIL_MSG": "Ocorreu um erro ao atualizar o cliente OAuth {0}.",       // {0} - client name
    "GENERIC_DELETE_FAIL_MSG": "Ocorreu um erro ao excluir o cliente OAuth {0}.",       // {0} - client name
    "GENERIC_MISSING_CLIENT_MSG": "O cliente OAuth {0} com o ID {1} não foi localizado.",     // {0} - client name; {1} - an ID
    "GENERIC_RETRIEVAL_FAIL_MSG": "Ocorreu um erro ao recuperar informações sobre o cliente OAuth {0}.", // {0} - client name
    "GENERIC_GET_CLIENTS_FAIL": "Erro ao recuperar clientes OAuth",
    "GENERIC_GET_CLIENTS_FAIL_MSG": "Ocorreu um erro ao recuperar a lista de clientes OAuth."
};
