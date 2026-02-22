-- E4 rulesets migration for H2 test/local profile.

CREATE TABLE rule_sets (
    id UUID PRIMARY KEY,
    rule_set_key VARCHAR(100) NOT NULL,
    version_no INT NOT NULL,
    dsl_json CLOB NOT NULL,
    checksum_sha256 CHAR(64) NOT NULL,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_rule_sets_key_version UNIQUE (rule_set_key, version_no),
    CONSTRAINT ck_rule_sets_version_no CHECK (version_no >= 1)
);

CREATE INDEX idx_rule_sets_key_version_desc
    ON rule_sets (rule_set_key, version_no DESC);
