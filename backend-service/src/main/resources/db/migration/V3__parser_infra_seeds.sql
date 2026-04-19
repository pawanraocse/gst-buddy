-- V3__parser_infra_seeds.sql
-- Seed initial parser templates for Phase 1 into the existing V2 schema

INSERT INTO parser_templates (template_id, doc_type, fingerprint, extraction_rules) VALUES
('GSTR1_PDF_V1', 'GSTR1_PDF', '{"keywords": ["FORM GSTR-1"]}', '{"engine": "Gstr1PdfExtractor"}'),
('GSTR3B_PDF_V1', 'GSTR3B_PDF', '{"keywords": ["Form GSTR-3B"]}', '{"engine": "Gstr3bPdfExtractor"}'),
('GSTR1_JSON_V1', 'GSTR1_JSON', '{"keys": ["gstin", "fp"]}', '{"engine": "JsonEngine"}'),
('GSTR2A_JSON_V1', 'GSTR2A_JSON', '{"keys": ["gstin", "fp"]}', '{"engine": "JsonEngine"}')
ON CONFLICT (template_id) DO NOTHING;
