
-- All IDs are BIGSERIAL (Long)

CREATE TABLE department (
                            id         BIGSERIAL PRIMARY KEY,
                            name       VARCHAR(200) NOT NULL UNIQUE,
                            active     BOOLEAN      NOT NULL DEFAULT true,
                            created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE complaint_category (
                                    id                    BIGSERIAL PRIMARY KEY,
                                    name                  VARCHAR(200) NOT NULL UNIQUE,
                                    default_department_id BIGINT       NOT NULL REFERENCES department(id),
                                    active                BOOLEAN      NOT NULL DEFAULT true
);

CREATE TABLE users (
                       id            BIGSERIAL PRIMARY KEY,
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       phone         VARCHAR(50)  NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       enabled       BOOLEAN      NOT NULL DEFAULT true,
                       created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
                            user_id BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            role    VARCHAR(50) NOT NULL,
                            PRIMARY KEY (user_id, role)
);

CREATE TABLE complaint (
                           id            BIGSERIAL PRIMARY KEY,
                           tracking_code VARCHAR(100) NOT NULL UNIQUE,
                           created_by    BIGINT       NOT NULL REFERENCES users(id),
                           category_id   BIGINT       NOT NULL REFERENCES complaint_category(id),
                           department_id BIGINT       NOT NULL REFERENCES department(id),
                           title         VARCHAR(500) NOT NULL,
                           description   TEXT,
                           status        VARCHAR(50)  NOT NULL DEFAULT 'NEW',
                           lat           DOUBLE PRECISION,
                           lon           DOUBLE PRECISION,
                           created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                           updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                           resolved_at   TIMESTAMPTZ,
                           closed_at     TIMESTAMPTZ,
                           public_answer TEXT
);

CREATE INDEX idx_complaint_created_by  ON complaint(created_by);
CREATE INDEX idx_complaint_category    ON complaint(category_id);
CREATE INDEX idx_complaint_department  ON complaint(department_id);
CREATE INDEX idx_complaint_status      ON complaint(status);

CREATE TABLE status_history (
                                id           BIGSERIAL PRIMARY KEY,
                                complaint_id BIGINT      NOT NULL REFERENCES complaint(id) ON DELETE CASCADE,
                                from_status  VARCHAR(50),
                                to_status    VARCHAR(50) NOT NULL,
                                changed_by   BIGINT REFERENCES users(id),
                                note         TEXT,
                                created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_status_history_complaint ON status_history(complaint_id);
CREATE INDEX idx_status_history_created   ON status_history(created_at);

CREATE TABLE assignment (
                            id                     BIGSERIAL PRIMARY KEY,
                            complaint_id           BIGINT      NOT NULL REFERENCES complaint(id) ON DELETE CASCADE,
                            assigned_department_id BIGINT REFERENCES department(id),
                            assigned_to            BIGINT REFERENCES users(id),
                            assigned_by            BIGINT REFERENCES users(id),
                            status                 VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                            assigned_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
                            ended_at               TIMESTAMPTZ,
                            note                   TEXT
);

CREATE INDEX idx_assignment_complaint ON assignment(complaint_id);
CREATE INDEX idx_assignment_status    ON assignment(status);

CREATE TABLE attachment (
                            id            BIGSERIAL PRIMARY KEY,
                            complaint_id  BIGINT NOT NULL REFERENCES complaint(id) ON DELETE CASCADE,
                            uploaded_by   BIGINT REFERENCES users(id),
                            type          VARCHAR(50),
                            bucket_name   VARCHAR(255),
                            object_key    VARCHAR(1024),
                            public_url    VARCHAR(2048),
                            original_name VARCHAR(1024),
                            content_type  VARCHAR(200),
                            size_bytes    BIGINT,
                            created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_attachment_complaint ON attachment(complaint_id);

CREATE TABLE sla_rule (
                          id                     BIGSERIAL PRIMARY KEY,
                          category_id            BIGINT REFERENCES complaint_category(id) ON DELETE CASCADE,
                          priority               VARCHAR(50),
                          first_response_minutes INTEGER,
                          resolution_minutes     INTEGER,
                          active                 BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE feedback (
                          id           BIGSERIAL PRIMARY KEY,
                          complaint_id BIGINT UNIQUE REFERENCES complaint(id) ON DELETE CASCADE,
                          citizen_id   BIGINT REFERENCES users(id),
                          rating       INTEGER,
                          comment      TEXT,
                          created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ✅ FIX: complaint_id eklendi (entity’de var)
CREATE TABLE notification (
                              id           BIGSERIAL PRIMARY KEY,
                              user_id      BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                              complaint_id BIGINT REFERENCES complaint(id) ON DELETE SET NULL,
                              title        VARCHAR(255) NOT NULL,
                              body         TEXT,
                              link         VARCHAR(1024),
                              is_read      BOOLEAN      NOT NULL DEFAULT false,
                              created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_user      ON notification(user_id);
CREATE INDEX idx_notification_complaint ON notification(complaint_id);

CREATE TABLE department_member (
                                   id            BIGSERIAL PRIMARY KEY,
                                   department_id BIGINT NOT NULL REFERENCES department(id) ON DELETE CASCADE,
                                   user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                   member_role   VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
                                   active        BOOLEAN NOT NULL DEFAULT true,
                                   created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                                   UNIQUE(department_id, user_id)
);

CREATE INDEX idx_department_member_dept ON department_member(department_id);
CREATE INDEX idx_department_member_user ON department_member(user_id);

-- ---------- SEED ----------
INSERT INTO department (name, active)
VALUES ('Temizlik İşleri', true),
       ('Zabıta', true),
       ('Fen İşleri', true),
       ('Park Bahçeler', true);

-- id hardcode yok: isimden buluyoruz
INSERT INTO complaint_category (name, default_department_id, active)
VALUES
    ('Çöp Toplama', (SELECT id FROM department WHERE name='Temizlik İşleri'), true),
    ('Gürültü',     (SELECT id FROM department WHERE name='Zabıta'), true),
    ('Yol Onarımı', (SELECT id FROM department WHERE name='Fen İşleri'), true),
    ('Ağaç Budama', (SELECT id FROM department WHERE name='Park Bahçeler'), true);
