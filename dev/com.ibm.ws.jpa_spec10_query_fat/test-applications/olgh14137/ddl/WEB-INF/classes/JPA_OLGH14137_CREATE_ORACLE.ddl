CREATE TABLE ${schemaname}.ct_override_entity_b (entity_b_ct_entity_b NUMBER, ct_b_override_value NUMBER, value2 NUMBER, ct_b_override_nested_value NUMBER, nested_value2 NUMBER);
CREATE TABLE ${schemaname}.override_entity_b (b_id NUMBER NOT NULL, PRIMARY KEY (b_id));
CREATE INDEX I_CT_VY_B_ENTITY_B_CT_ENTITY_B ON ${schemaname}.ct_override_entity_b (entity_b_ct_entity_b);