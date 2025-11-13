-- Insert 50 operators (10 of each role) with NULL organization

-- Operator (10명)
INSERT INTO operator (id, operator_id, name, role, organization_code, organization_name, created_at)
VALUES
  ('op-' || gen_random_uuid()::text, 'OPR-001', '김민준', 'Operator', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'OPR-002', '이서연', 'Operator', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'OPR-003', '박지호', 'Operator', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'OPR-004', '최수진', 'Operator', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'OPR-005', '정우성', 'Operator', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'OPR-006', '강미래', 'Operator', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'OPR-007', '윤태영', 'Operator', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'OPR-008', '임지은', 'Operator', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'OPR-009', '한동훈', 'Operator', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'OPR-010', '송하늘', 'Operator', NULL, NULL, NOW());

-- Firefighter (10명)
INSERT INTO operator (id, operator_id, name, role, organization_code, organization_name, created_at)
VALUES
  ('op-' || gen_random_uuid()::text, 'FIRE-014', '박민수', 'Firefighter', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'FIRE-015', '최지훈', 'Firefighter', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'FIRE-016', '윤서아', 'Firefighter', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'FIRE-017', '송민규', 'Firefighter', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'FIRE-018', '김태희', 'Firefighter', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'FIRE-019', '이준호', 'Firefighter', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'FIRE-020', '정다은', 'Firefighter', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'FIRE-021', '강석우', 'Firefighter', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'FIRE-022', '오세훈', 'Firefighter', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'FIRE-023', '배수지', 'Firefighter', NULL, NULL, NOW());

-- Rescue Technician (10명)
INSERT INTO operator (id, operator_id, name, role, organization_code, organization_name, created_at)
VALUES
  ('op-' || gen_random_uuid()::text, 'RESC-001', '김구조', 'Rescue Technician', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'RESC-002', '이안전', 'Rescue Technician', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'RESC-003', '박레스큐', 'Rescue Technician', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'RESC-004', '최산악', 'Rescue Technician', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'RESC-005', '정해양', 'Rescue Technician', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'RESC-006', '강수난', 'Rescue Technician', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'RESC-007', '윤생명', 'Rescue Technician', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'RESC-008', '임보호', 'Rescue Technician', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'RESC-009', '한긴급', 'Rescue Technician', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'RESC-010', '송구급', 'Rescue Technician', NULL, NULL, NOW());

-- Emergency Medical Technician (10명)
INSERT INTO operator (id, operator_id, name, role, organization_code, organization_name, created_at)
VALUES
  ('op-' || gen_random_uuid()::text, 'EMT-001', '김응급', 'Emergency Medical Technician', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'EMT-002', '이의료', 'Emergency Medical Technician', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'EMT-003', '박구급', 'Emergency Medical Technician', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'EMT-004', '최간호', 'Emergency Medical Technician', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'EMT-005', '정치료', 'Emergency Medical Technician', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'EMT-006', '강생명', 'Emergency Medical Technician', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'EMT-007', '윤환자', 'Emergency Medical Technician', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'EMT-008', '임심폐', 'Emergency Medical Technician', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'EMT-009', '한소생', 'Emergency Medical Technician', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'EMT-010', '송병원', 'Emergency Medical Technician', NULL, NULL, NOW());

-- Police Officer (10명)
INSERT INTO operator (id, operator_id, name, role, organization_code, organization_name, created_at)
VALUES
  ('op-' || gen_random_uuid()::text, 'POL-011', '김순경', 'Police Officer', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'POL-012', '이경찰', 'Police Officer', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'POL-013', '박수사', 'Police Officer', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'POL-014', '최형사', 'Police Officer', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'POL-015', '정순찰', 'Police Officer', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'POL-016', '강교통', 'Police Officer', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'POL-017', '윤방범', 'Police Officer', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'POL-018', '임안전', 'Police Officer', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'POL-019', '한보안', 'Police Officer', NULL, NULL, NOW()),
  ('op-' || gen_random_uuid()::text, 'POL-020', '송경비', 'Police Officer', NULL, NULL, NOW());
