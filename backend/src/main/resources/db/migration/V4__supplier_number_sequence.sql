-- V4: Sequence for human-readable SRM supplier numbers
CREATE SEQUENCE IF NOT EXISTS srm.supplier_number_seq
    START WITH 100001
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
