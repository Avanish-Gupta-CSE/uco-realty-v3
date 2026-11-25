# UCO Realty → Home Loan lead page: what we need (`utm=REALTY`)

Hand this page to the developer who owns the bank's lead page (`/Lead_App/lead_web.jsp`).

## The request in one sentence
When a customer lands on `lead_web.jsp?utm=REALTY` and submits the form, **their details must be saved as a new row in the table `LG_ENTRY_REVAMP`, with `CHANNEL = 'REALTY'`**, exactly like the page already saves the leads of other channels (`WEB`, ...). UCO Realty then reads those rows from that table; it keeps no copy.

## 1. What happens today
1. A customer browses approved housing projects on the UCO Realty portal.
2. On a project page they press **Apply for Home Loan**.
3. The browser opens, in the same tab:

   `https://customerservice.ucoonline.bank.in/Lead_App/lead_web.jsp?utm=REALTY`

   (the host comes from our setting `uco.homeloan.base-url`, so UAT and production can differ; the path and `?utm=REALTY` are fixed).
4. The customer fills the bank's lead form. The bank stores the lead in the HRMS table `LG_ENTRY_REVAMP`.

Nothing personal travels in the link. UCO Realty keeps no copy of the lead.

## 2. What we ask the lead-page developer to do
1. **Read the `utm` parameter** on `lead_web.jsp`. It must survive every step of the form (hidden field or session), including the OTP step.
2. **Save it in `LG_ENTRY_REVAMP.CHANNEL`** when the lead row is inserted: the value must be exactly **`REALTY`** (capital letters, no spaces). Other visits keep their current channel (`WEB`, ...).
3. **Validate the value** before storing: accept only known channels (for example `REALTY`); turn `realty` into `REALTY`; ignore anything else or longer than 50 characters (the column is `VARCHAR2(50)`); never put the raw parameter into SQL.
4. **Fill the date**: `SUBMIT_DATE` (or `CRE_DT`) must be set; our page sorts and filters on it.
5. **Keep the usual columns filled** (we show them): `LEAD_ID, REF_NAME, MOBILE_NO, EMAIL, STATE, DISTRICT, FACILITY_INT, LOANAMOUNT / TENT_LOAN_AMT, STATUS`.
6. **Check what `CHANNEL` drives on the bank side** (RM or branch assignment, SMS / e-mail, reports) so a new channel `REALTY` does not break or silently drop leads.
7. **Tell us which `STATUS` values** a REALTY lead goes through. We show the status exactly as stored.

### Optional (recommended): know which project the customer looked at
We can add the project code to the link, for example `...lead_web.jsp?utm=REALTY&pid=UCR-PROJ-AB12CD34`.
- The lead page would store `pid` in an existing text column (which one is free? `REMARKS` is only 50 characters, `ANY_OTHER_INFO` is 200). A code is 17 characters.
- We would then show the project name next to each lead.
- Needs a small change on both sides. Say yes or no.

## 3. What we read (so nothing is renamed by accident)
Table `LG_ENTRY_REVAMP`, read only, with
`WHERE CHANNEL = 'REALTY'`, newest first by `NVL(SUBMIT_DATE, CRE_DT)`:

`LEAD_ID, REF_NAME, MOBILE_NO, EMAIL, STATE, DISTRICT, FACILITY_INT, LOANAMOUNT, TENT_LOAN_AMT, STATUS, SUBMIT_DATE, CRE_DT, CHANNEL`

Our database user is `HRMS_DBA`; it can already read the table.

## 4. Tests to run together (UAT first)
| # | Do | Expect |
|---|----|--------|
| 1 | Open the link with `?utm=REALTY`, complete the form | New row with `CHANNEL = 'REALTY'`, date filled |
| 2 | Open the page without `utm` | Row as before (`WEB` or whatever it is today) |
| 3 | `?utm=realty` | Stored as `REALTY` (or ignored, as agreed) |
| 4 | `?utm=<very long text or quotes>` | No error, nothing harmful stored |
| 5 | Leave the form half way, then finish later | Channel still `REALTY` |
| 6 | Open our Leads page (any staff sign-in) | The lead appears with the right name, mobile, status |

## 5. What I (UCO Realty owner) must do
- [ ] Give this page to the lead-page developer and agree a UAT date.
- [ ] Ask for the **UAT host name** of the lead page and set `uco.homeloan.base-url` for UAT; later the production host.
- [ ] Run the two Oracle checks on UAT (all column names of `LG_ENTRY_REVAMP`; the 13-column test query) and confirm `EMAIL` exists.
- [ ] Oracle is 19c (confirmed), so the Leads page's `OFFSET ... FETCH` is fine. Ask the DBA only: one server or several for the portal.
- [ ] Decide yes / no on the optional project code (`pid`).
- [ ] After they deploy, run test 1 to 6 above and note the real `STATUS` values.
- [ ] Before production: set the production host, check `uco.sso.dev-login-enabled=false`, rebuild the bundle.
