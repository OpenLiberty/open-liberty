CREATE TABLE ct_override_entity_b (entity_b_ct_entity_b INT, ct_b_override_value INT, value2 INT, ct_b_override_nested_value INT, nested_value2 INT);
CREATE TABLE override_entity_b (b_id INT NOT NULL, PRIMARY KEY (b_id));
CREATE INDEX I_CT_VY_B_ENTITY_B_CT_ENTITY_B ON ct_override_entity_b (entity_b_ct_entity_b);