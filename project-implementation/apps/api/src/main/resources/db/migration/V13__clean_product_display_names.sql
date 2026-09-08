-- Remove the former sample suffix from product master data. Historical order and
-- inquiry snapshots remain unchanged and use the cleaned name for display only.
UPDATE catalog_products
SET name = REGEXP_REPLACE(name, '[[:space:]]*(·[[:space:]]*)?示例[[:space:]]*$', ''),
    version = version + 1,
    updated_at = UTC_TIMESTAMP(6)
WHERE name REGEXP '示例[[:space:]]*$'
  AND CHAR_LENGTH(REGEXP_REPLACE(name, '[[:space:]]*(·[[:space:]]*)?示例[[:space:]]*$', '')) > 0;
