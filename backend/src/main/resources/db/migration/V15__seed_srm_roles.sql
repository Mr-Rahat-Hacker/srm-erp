-- Seed the roles required by authenticated first-release workflows.
INSERT INTO srm.role (role_code, role_name) VALUES
    ('SUPER_ADMIN', 'Super Admin'),
    ('SRM_ADMIN', 'SRM Admin'),
    ('SUPPLIER_USER', 'Supplier User'),
    ('SUPPLIER_VERIFIER', 'Supplier Verifier'),
    ('PROCUREMENT_BUYER', 'Procurement Buyer'),
    ('PROCUREMENT_APPROVER', 'Procurement Approver'),
    ('AUDITOR', 'Auditor')
ON CONFLICT (role_code) DO NOTHING;
