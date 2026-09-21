# Spec: Fix Parts Packing List columns to match real Databricks schema

## Problem
`parts-packing-list.json`'s `base` columnGroup declared `dealerCode`/`dealerName` (camelCase),
but the live `fn_parts_packing_list` Databricks function actually returns snake_case columns:
`dealer_code, dealer_name, dealer_city, invoice_number, delivery_number, case_number,
carton_box, part_number, part_description, quantity` (confirmed by logging the live
ResultSet's column labels). Every row therefore projected to `{}` in `/data` responses.

The user shared the expected report shape (an existing packing-list export): Dealer Code, Name,
City, Invoice Number, Delivery Number, Case Number, Carton Box, Part No, Part Description, Qty —
which lines up one-to-one with the real column names above.

## Fix
`src/main/resources/reports/parts-packing-list.json` — replace the `base` columnGroup's two
placeholder columns with all ten real fields, field names matching Databricks' snake_case output
exactly (`dealer_code`, `dealer_name`, `dealer_city`, `invoice_number`, `delivery_number`,
`case_number`, `carton_box`, `part_number`, `part_description`, `quantity`), labeled per the
reference sheet.

## Out of scope
- No change to the query itself or `StubReportQueryExecutor`'s PARTS_PACKING_LIST branch.
- No aggregate/summary row (the sheet's "PACKED QUANTITY IS: 9" header) — out of scope for the
  generic grid response; that's presentation-layer, not part of this data-shape fix.

## Verification
- `mvn -q compile` succeeds.
- `POST /api/v1/reports/PARTS_PACKING_LIST/data` for dealer 0000010015, a range known to have
  data, returns fully populated rows (not `{}`), with fields matching the ten columns above.
