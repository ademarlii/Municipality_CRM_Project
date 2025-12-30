-- V3__seed_admin_and_agent.sql
-- DEV seed: 1 admin + 1 agent
-- Admin login:
--   email: admin@local.com
--   pass : Admin123!
-- Agent login:
--   email: agent@local.com
--   pass : Agent123!

-- 1) ADMIN user
INSERT INTO users (email, phone, password_hash, enabled)
SELECT
    'admin@local.com',
    '05000000000',
    '$2b$10$XFxpyVDux9eet9XPjRUslOm5CEzq0xN.qTry0BJnhISxu6jAgl9Aa',
    true
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@local.com');

INSERT INTO user_roles (user_id, role)
SELECT u.id, 'ADMIN'
FROM users u
WHERE u.email = 'admin@local.com'
    ON CONFLICT DO NOTHING;

-- 2) AGENT user
INSERT INTO users (email, phone, password_hash, enabled)
SELECT
    'agent@local.com',
    '05000000001',
    '$2b$10$uDAEnCb4zOug64YZlukOwu8CmdWxqSgGVFZIlWkb.Nq30Ge/UJ78K',
    true
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'agent@local.com');

INSERT INTO user_roles (user_id, role)
SELECT u.id, 'AGENT'
FROM users u
WHERE u.email = 'agent@local.com'
    ON CONFLICT DO NOTHING;

-- 3) Agent'i Temizlik İşleri departmanına üye yap
-- (department_member UNIQUE(department_id, user_id) var, o yüzden upsert yaptık)
INSERT INTO department_member (department_id, user_id, member_role, active)
SELECT d.id, u.id, 'MEMBER', true
FROM department d
         JOIN users u ON u.email = 'agent@local.com'
WHERE d.name = 'Temizlik İşleri'
    ON CONFLICT (department_id, user_id)
DO UPDATE SET active = true, member_role = EXCLUDED.member_role;
