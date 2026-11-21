SET search_path TO srm, public;

ALTER TABLE quotation_version
  ADD COLUMN commercial_remarks TEXT,
  ADD COLUMN technical_deviations TEXT;
