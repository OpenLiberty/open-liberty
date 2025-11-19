package io.openliberty.mcp.content;

import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbTypeAdapter;

/**
 * The sender or recipient of messages in a conversation.
 */
@JsonbTypeAdapter(Role.RoleAdapter.class)
public enum Role {

    ASSISTANT,
    USER;

    public String getName() {
        return toString().toLowerCase();
    }

    public static class RoleAdapter implements JsonbAdapter<Role, String> {
        @Override
        public String adaptToJson(Role role) {
            return role.toString().toLowerCase();
        }

        @Override
        public Role adaptFromJson(String val) throws Exception {
            return Role.valueOf(val.toUpperCase());
        }
    }

}