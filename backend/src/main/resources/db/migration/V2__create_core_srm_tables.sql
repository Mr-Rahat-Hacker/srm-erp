-- V2: Core SRM relational model (initial implementation baseline)
SET search_path TO srm, public;

CREATE TABLE app_user (
  user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(254) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(160) NOT NULL,
  account_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_app_user_email UNIQUE (email),
  CONSTRAINT ck_app_user_status CHECK (account_status IN ('PENDING','ACTIVE','SUSPENDED','DISABLED'))
);

CREATE TABLE role (
  role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  role_code VARCHAR(40) NOT NULL UNIQUE,
  role_name VARCHAR(100) NOT NULL
);

CREATE TABLE user_role (
  user_id UUID NOT NULL REFERENCES app_user(user_id),
  role_id UUID NOT NULL REFERENCES role(role_id),
  assigned_by UUID REFERENCES app_user(user_id),
  assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, role_id)
);

CREATE TABLE supplier_organization (
  supplier_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  supplier_number VARCHAR(30) NOT NULL UNIQUE,
  legal_name VARCHAR(200) NOT NULL,
  trade_name VARCHAR(200),
  gstin VARCHAR(20),
  pan VARCHAR(10),
  lifecycle_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_supplier_status CHECK (lifecycle_status IN
    ('DRAFT','SUBMITTED','UNDER_REVIEW','CORRECTION_REQUIRED','ON_HOLD','APPROVED','PENDING_SAP_MAPPING','ACTIVE','SUSPENDED','INACTIVE','REJECTED'))
);

CREATE TABLE supplier_user_membership (
  supplier_id UUID NOT NULL REFERENCES supplier_organization(supplier_id),
  user_id UUID NOT NULL REFERENCES app_user(user_id),
  membership_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (supplier_id, user_id)
);

CREATE TABLE supplier_address (
  address_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  supplier_id UUID NOT NULL REFERENCES supplier_organization(supplier_id),
  address_type VARCHAR(30) NOT NULL,
  address_line_1 VARCHAR(200) NOT NULL,
  address_line_2 VARCHAR(200),
  city VARCHAR(100) NOT NULL,
  state_region VARCHAR(100),
  postal_code VARCHAR(20),
  country_code CHAR(2) NOT NULL,
  is_primary BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE supplier_contact (
  contact_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  supplier_id UUID NOT NULL REFERENCES supplier_organization(supplier_id),
  full_name VARCHAR(160) NOT NULL,
  email VARCHAR(254),
  phone VARCHAR(40),
  designation VARCHAR(100),
  contact_type VARCHAR(30)
);

CREATE TABLE supplier_document (
  document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  supplier_id UUID NOT NULL REFERENCES supplier_organization(supplier_id),
  document_type VARCHAR(50) NOT NULL,
  storage_key TEXT NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  content_type VARCHAR(120),
  version_number INTEGER NOT NULL DEFAULT 1,
  review_status VARCHAR(25) NOT NULL DEFAULT 'PENDING',
  expiry_date DATE,
  uploaded_by UUID REFERENCES app_user(user_id),
  uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_supplier_document_version CHECK (version_number > 0)
);

CREATE TABLE supplier_review (
  review_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  supplier_id UUID NOT NULL REFERENCES supplier_organization(supplier_id),
  reviewer_user_id UUID REFERENCES app_user(user_id),
  review_stage VARCHAR(40) NOT NULL,
  decision VARCHAR(30) NOT NULL,
  remarks TEXT,
  decided_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE supplier_status_history (
  history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  supplier_id UUID NOT NULL REFERENCES supplier_organization(supplier_id),
  old_status VARCHAR(30),
  new_status VARCHAR(30) NOT NULL,
  changed_by UUID REFERENCES app_user(user_id),
  reason TEXT,
  changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE sap_vendor_mapping (
  mapping_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  supplier_id UUID NOT NULL REFERENCES supplier_organization(supplier_id),
  sap_system_code VARCHAR(40) NOT NULL,
  sap_vendor_code VARCHAR(40) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT true,
  mapped_by UUID REFERENCES app_user(user_id),
  mapped_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_sap_vendor_mapping UNIQUE (sap_system_code, sap_vendor_code)
);

CREATE TABLE rfq (
  rfq_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  rfq_number VARCHAR(40) NOT NULL UNIQUE,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  buyer_user_id UUID NOT NULL REFERENCES app_user(user_id),
  project_code VARCHAR(60),
  department VARCHAR(100),
  plant_code VARCHAR(30),
  purchasing_group VARCHAR(30),
  currency_code CHAR(3) NOT NULL,
  submission_deadline TIMESTAMPTZ NOT NULL,
  required_delivery_date DATE,
  payment_terms TEXT,
  incoterms VARCHAR(20),
  status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_rfq_deadline CHECK (submission_deadline > created_at)
);

CREATE TABLE rfq_line (
  rfq_line_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  rfq_id UUID NOT NULL REFERENCES rfq(rfq_id),
  line_number INTEGER NOT NULL,
  sap_material_code VARCHAR(80) NOT NULL,
  description TEXT NOT NULL,
  drawing_revision VARCHAR(60),
  quantity NUMERIC(18,4) NOT NULL,
  uom_code VARCHAR(12) NOT NULL,
  plant_code VARCHAR(30),
  technical_specifications TEXT,
  CONSTRAINT uq_rfq_line_number UNIQUE (rfq_id, line_number),
  CONSTRAINT ck_rfq_line_quantity CHECK (quantity > 0)
);

CREATE TABLE rfq_invitation (
  invitation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  rfq_id UUID NOT NULL REFERENCES rfq(rfq_id),
  supplier_id UUID NOT NULL REFERENCES supplier_organization(supplier_id),
  invitation_status VARCHAR(25) NOT NULL DEFAULT 'INVITED',
  decline_reason TEXT,
  invited_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  acknowledged_at TIMESTAMPTZ,
  CONSTRAINT uq_rfq_invitation UNIQUE (rfq_id, supplier_id)
);

CREATE TABLE quotation (
  quotation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  rfq_id UUID NOT NULL REFERENCES rfq(rfq_id),
  supplier_id UUID NOT NULL REFERENCES supplier_organization(supplier_id),
  lifecycle_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_quotation_rfq_supplier UNIQUE (rfq_id, supplier_id)
);

CREATE TABLE quotation_version (
  quotation_version_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  quotation_id UUID NOT NULL REFERENCES quotation(quotation_id),
  version_number INTEGER NOT NULL,
  submitted_by UUID REFERENCES app_user(user_id),
  submitted_at TIMESTAMPTZ,
  currency_code CHAR(3) NOT NULL,
  validity_date DATE,
  payment_terms TEXT,
  incoterms VARCHAR(20),
  lead_time_days INTEGER,
  minimum_order_quantity NUMERIC(18,4),
  warranty_terms TEXT,
  total_landed_cost NUMERIC(18,2),
  version_status VARCHAR(25) NOT NULL DEFAULT 'DRAFT',
  CONSTRAINT uq_quotation_version UNIQUE (quotation_id, version_number),
  CONSTRAINT ck_quotation_version_number CHECK (version_number > 0),
  CONSTRAINT ck_quotation_lead_time CHECK (lead_time_days IS NULL OR lead_time_days >= 0)
);

CREATE TABLE quotation_line (
  quotation_line_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  quotation_version_id UUID NOT NULL REFERENCES quotation_version(quotation_version_id),
  rfq_line_id UUID NOT NULL REFERENCES rfq_line(rfq_line_id),
  basic_unit_price NUMERIC(18,4) NOT NULL,
  discount_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  net_unit_price NUMERIC(18,4) NOT NULL,
  tax_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  freight_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  packing_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  tooling_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  insurance_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  other_charges NUMERIC(18,2) NOT NULL DEFAULT 0,
  landed_line_total NUMERIC(18,2) NOT NULL,
  CONSTRAINT uq_quotation_line UNIQUE (quotation_version_id, rfq_line_id),
  CONSTRAINT ck_quotation_prices CHECK (basic_unit_price >= 0 AND net_unit_price >= 0 AND landed_line_total >= 0)
);

CREATE TABLE technical_evaluation (
  technical_evaluation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  quotation_version_id UUID NOT NULL REFERENCES quotation_version(quotation_version_id),
  evaluator_user_id UUID NOT NULL REFERENCES app_user(user_id),
  technical_status VARCHAR(35) NOT NULL,
  remarks TEXT,
  evaluated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE commercial_evaluation (
  commercial_evaluation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  quotation_version_id UUID NOT NULL REFERENCES quotation_version(quotation_version_id),
  evaluator_user_id UUID NOT NULL REFERENCES app_user(user_id),
  comparison_basis VARCHAR(40) NOT NULL,
  rank_number INTEGER,
  score NUMERIC(10,4),
  remarks TEXT,
  evaluated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE negotiation_round (
  negotiation_round_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  rfq_id UUID NOT NULL REFERENCES rfq(rfq_id),
  supplier_id UUID NOT NULL REFERENCES supplier_organization(supplier_id),
  round_number INTEGER NOT NULL,
  status VARCHAR(25) NOT NULL DEFAULT 'OPEN',
  opened_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  closed_at TIMESTAMPTZ,
  CONSTRAINT uq_negotiation_round UNIQUE (rfq_id, supplier_id, round_number),
  CONSTRAINT ck_negotiation_round_number CHECK (round_number > 0)
);

CREATE TABLE negotiation_message (
  message_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  negotiation_round_id UUID NOT NULL REFERENCES negotiation_round(negotiation_round_id),
  sender_user_id UUID NOT NULL REFERENCES app_user(user_id),
  message_type VARCHAR(30) NOT NULL,
  message_text TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE approval_request (
  approval_request_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  entity_type VARCHAR(40) NOT NULL,
  entity_id UUID NOT NULL,
  workflow_code VARCHAR(50) NOT NULL,
  status VARCHAR(25) NOT NULL DEFAULT 'PENDING',
  requested_by UUID NOT NULL REFERENCES app_user(user_id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE approval_task (
  approval_task_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  approval_request_id UUID NOT NULL REFERENCES approval_request(approval_request_id),
  stage_code VARCHAR(50) NOT NULL,
  sequence_number INTEGER NOT NULL,
  assigned_user_id UUID REFERENCES app_user(user_id),
  assigned_group VARCHAR(80),
  task_status VARCHAR(25) NOT NULL DEFAULT 'PENDING',
  due_at TIMESTAMPTZ,
  CONSTRAINT ck_approval_assignee CHECK (assigned_user_id IS NOT NULL OR assigned_group IS NOT NULL),
  CONSTRAINT uq_approval_stage UNIQUE (approval_request_id, sequence_number)
);

CREATE TABLE approval_decision (
  decision_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  approval_task_id UUID NOT NULL REFERENCES approval_task(approval_task_id),
  actor_user_id UUID NOT NULL REFERENCES app_user(user_id),
  decision VARCHAR(25) NOT NULL,
  remarks TEXT,
  decided_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE supplier_recommendation (
  recommendation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  rfq_id UUID NOT NULL REFERENCES rfq(rfq_id),
  supplier_id UUID NOT NULL REFERENCES supplier_organization(supplier_id),
  quotation_version_id UUID NOT NULL REFERENCES quotation_version(quotation_version_id),
  recommended_by UUID NOT NULL REFERENCES app_user(user_id),
  is_non_l1 BOOLEAN NOT NULL DEFAULT false,
  justification TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_non_l1_justification CHECK (is_non_l1 = false OR length(trim(coalesce(justification,''))) > 0)
);

CREATE TABLE award (
  award_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  rfq_id UUID NOT NULL REFERENCES rfq(rfq_id),
  supplier_id UUID NOT NULL REFERENCES supplier_organization(supplier_id),
  quotation_version_id UUID NOT NULL REFERENCES quotation_version(quotation_version_id),
  approval_request_id UUID NOT NULL REFERENCES approval_request(approval_request_id),
  approved_value NUMERIC(18,2) NOT NULL,
  currency_code CHAR(3) NOT NULL,
  award_status VARCHAR(25) NOT NULL DEFAULT 'APPROVED',
  awarded_at TIMESTAMPTZ,
  CONSTRAINT ck_award_value CHECK (approved_value >= 0)
);

CREATE TABLE notification (
  notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  recipient_user_id UUID NOT NULL REFERENCES app_user(user_id),
  event_type VARCHAR(60) NOT NULL,
  channel VARCHAR(20) NOT NULL,
  delivery_status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
  retry_count INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  sent_at TIMESTAMPTZ
);

CREATE TABLE audit_event (
  audit_event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  actor_user_id UUID REFERENCES app_user(user_id),
  actor_type VARCHAR(25) NOT NULL,
  effective_roles TEXT[],
  action_code VARCHAR(80) NOT NULL,
  entity_type VARCHAR(50) NOT NULL,
  entity_id UUID,
  supplier_id UUID REFERENCES supplier_organization(supplier_id),
  rfq_id UUID REFERENCES rfq(rfq_id),
  quotation_id UUID REFERENCES quotation(quotation_id),
  approval_request_id UUID REFERENCES approval_request(approval_request_id),
  outcome VARCHAR(20) NOT NULL,
  reason TEXT,
  changed_fields JSONB,
  correlation_id UUID
);

CREATE TABLE sap_integration_event (
  integration_event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  supplier_id UUID REFERENCES supplier_organization(supplier_id),
  award_id UUID REFERENCES award(award_id),
  operation_type VARCHAR(50) NOT NULL,
  status VARCHAR(25) NOT NULL,
  external_reference VARCHAR(100),
  sanitized_error TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_supplier_status ON supplier_organization(lifecycle_status);
CREATE INDEX idx_supplier_document_supplier ON supplier_document(supplier_id, review_status);
CREATE INDEX idx_rfq_status_deadline ON rfq(status, submission_deadline);
CREATE INDEX idx_invitation_supplier_status ON rfq_invitation(supplier_id, invitation_status);
CREATE INDEX idx_quotation_rfq_supplier ON quotation(rfq_id, supplier_id);
CREATE INDEX idx_quotation_version_submitted ON quotation_version(submitted_at);
CREATE INDEX idx_approval_task_assignee_status ON approval_task(assigned_user_id, task_status);
CREATE INDEX idx_audit_occurred_at ON audit_event(occurred_at);
CREATE INDEX idx_audit_entity ON audit_event(entity_type, entity_id);
CREATE INDEX idx_audit_correlation ON audit_event(correlation_id);
