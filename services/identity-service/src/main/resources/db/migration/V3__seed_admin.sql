-- Compte administrateur de démonstration.
-- Connexion : admin@unchk.sn  /  mot de passe : Admin123!
-- Hash BCrypt ($2a$, coût 12). Migration idempotente (ne réinsère pas si déjà présent).
INSERT INTO users (id, email, password_hash, full_name, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@unchk.sn',
    '$2a$12$e/N3M4LHPVK8oIgVymn08OUKcYmukDJmoqaXgLZD/JPJ0mdZdGFV6',
    'Administrateur UNCHK',
    true
)
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role)
VALUES ('00000000-0000-0000-0000-000000000001', 'admin')
ON CONFLICT DO NOTHING;
