# Security Policy — ExactUploadFixer

## Project status: proprietary, not open source

ExactUploadFixer is proprietary software. The source is published for source visibility and transparency only. The repository [LICENSE](LICENSE) is a proprietary license notice that grants no permission to copy, modify, redistribute, or build derivative works. The [README](README.md) states this in its opening banner.

Because no permission to use the code has been granted, a defect in it is not a "vulnerability" in the open-source sense. It is a question about unauthorized use of unlicensed software, and that question belongs to the owner of the code, not to a public disclosure process. This file exists so the boundary is stated plainly instead of left to inference.

## What this repository does not offer

- **No security support.** The maintainer does not triage, investigate, or remediate security reports for ExactUploadFixer.
- **No coordinated disclosure program.** There is no embargo, no safe harbor, and no private disclosure window.
- **No bug bounty.** No reward is offered.
- **No response-time commitment.** There is no SLA and no support window.
- **No supported versions.** No release is a supported security-fix channel.

## Monetization and the entitlement boundary

This app ships in two store flavors with materially different network behavior, which is where the security-relevant boundary sits.

- **Google Play flavor:** lifetime Pro access is sold through Google Play Billing. Entitlement state is local; internet is used only when a purchase is initiated or when Play services refresh purchase state.
- **Amazon flavor:** Pro access is verified through RevenueCat with Amazon In-App Purchasing. Purchase and entitlement data leave the device and are processed by RevenueCat and Amazon under their own policies. Do not assume this build is offline-only.

Purchase and entitlement logic is therefore the primary integrity surface in this codebase. Google Play Billing, Amazon IAP, and RevenueCat are third parties operating under their own terms and security models; concerns about them belong to those providers, not here.

## Documented integrity controls

- [PRIVACY.md](PRIVACY.md) — per-flavor data disclosure: on-device JPEG processing, Photo Picker selection, no analytics SDKs, and explicit statements about what does and does not leave the device in each flavor.
- [QA_CHECKLIST.md](QA_CHECKLIST.md), [SHIP_CHECKLIST.md](SHIP_CHECKLIST.md), and [STATUS.md](STATUS.md) — the project's own verification and release record.

These are point-in-time records of the project's own work, not a guarantee that no defect exists.

## Reporting a genuine concern

If you believe you have found a genuine security concern, the honest position is that the maintainer has not accepted a support obligation, so there is no guaranteed response. If you choose to raise it anyway:

- Prefer GitHub's private vulnerability reporting for this repository (the **Security** tab → **Report a vulnerability**), if it is available to you.
- Otherwise contact the repository owner through their public profile at <https://github.com/gthgomez>.
- Do not open a public issue, and do not include working exploit code, purchase tokens, or third-party personal data in a public report.
- You receive no service commitment, no bounty, and no assurance of a fix.

## Visibility is not permission

The repository being public creates no support obligation. Publishing source does not grant a license, does not create a support contract, and does not make the maintainer a vendor to you. Opening an issue or submitting a pull request grants you no rights and creates no partnership; contributions are not accepted for reuse, and no license is granted over anything you send here.
