-- The QR payload is an opaque bearer credential: staff scanners resolve it
-- back to a ticket by value, so lookups must be indexed and duplicates are a
-- security bug. 256-bit random values make collisions practically impossible;
-- the constraint turns "practically" into "guaranteed".
alter table qr_codes
    add constraint uk_qr_codes_value unique (value);
