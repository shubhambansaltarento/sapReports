-- Report: WARRANTY_COST
-- params:
--   dealerCode  string  e.g. '0000010015'
--   fromDate    date    e.g. '2026-07-17'
--   toDate      date    e.g. '2026-08-17'
--   limit       int     max rows returned, e.g. 100
SELECT *
FROM sap_dynpro.bumblebee.api_warranty_cost(:dealerCode, :fromDate, :toDate)
LIMIT :limit
