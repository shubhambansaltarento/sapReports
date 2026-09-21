-- Report: PARTS_PACKING_LIST
-- params:
--   dealerCode  string  e.g. '0000010015'
--   fromDate    date    e.g. '2026-08-01'
--   toDate      date    e.g. '2026-09-01'
--   limit       int     max rows returned, e.g. 100
SELECT *
FROM sap_dynpro.bumblebee.fn_parts_packing_list(
        CAST(:dealerCode AS STRING),
        CAST(:fromDate   AS DATE),
        CAST(:toDate     AS DATE)
     )
LIMIT :limit
