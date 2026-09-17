-- Report: DEALER_LEDGER
-- params:
--   bukrs      string  company code, e.g. '1000'
--   kunnr      string  dealer code, e.g. '0000010015'
--   from_date  date    window start, e.g. '2026-07-17' (one-month half-open window: [from_date, from_date + 1 month))
SELECT *
FROM sap_dynpro.bumblebee.f_dealer_ledger(
        CAST(:bukrs     AS STRING),
        CAST(:kunnr     AS STRING),
        CAST(:from_date AS DATE),
        ADD_MONTHS(CAST(:from_date AS DATE), 1)   -- exclusive, half-open window
     )
ORDER BY credit_control_area, sort_grp, post_date, doc_reference_no, line_item
