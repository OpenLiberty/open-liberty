---
name: Open Liberty Feature
about: Create a Feature Epic (Open Liberty org members only)
title: ''
labels: Epic
assignees: ''

---
Describe the high level feature, including any external spec links.


When ready, add links to the Upcoming Feature Overview document and Feature Test Summary issue:
- UFO:
- FTS:

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## List of Steps to complete or get approvals / sign-offs for Onboarding to the Liberty release (GM date)

Instructions:
- Do the actions below and mark them complete in the checklist when they are done.
- Make sure all feature readiness approvers put the appropriate tag on the epic to indicate their approval.

## **Design**
Before Development Starts or 8 weeks before Onboarding
- [ ] POC Design / UFO Review Scheduled (David Chang) or N/A.
- [ ] POC Design / UFO Reviewed (Feature Owner) or N/A.
- [ ] Complete any follow-ons from the POC Review.
- [ ] Design / UFO Approval (Alasdair Nottingham) or N/A.
- [ ] No Design / No UFO Approval (Arthur De Magalhaes - cloud / Alasdair Nottingham - server) or N/A.
- [ ] SVT Requirements identified. (Epic owner / Feature owner with SVT focal point)
- [ ] ID Requirements identified. (Epic owner / Feature owner with ID focal point)
- [ ] Create a child task of this epic entitled "FAT Approval Test Summary". Add the link in above.

## **Legal**
3 weeks before Onboarding
- [ ] Identify all open source libraries that are changing or are new. Work with Legal Release Services (Cass Tucker or Release PM) to get open source cleared and approved. Or N/A. (Epic Owner).   New or changed open source impacts license and Certificate of Originality.

## **Translation**
3 weeks before Onboarding
- [ ] All new or changed PII messages are checked into the integration branch, before the last translation shipment out. (Epic Owner)

## **Feature Complete**
2 weeks before Onboarding
- [ ] Implementation complete. (Epic owner / Feature owner)
- [ ] All function tests complete. Ready for FAT Approval. (Epic owner / Feature owner)
- [ ] Review all known issues for Stop Ship. (Epic owner / Feature owner / PM)

## **Focal Point Approvals**
2 to 1 week before Onboarding
#### You **MUST** have the Design Approved or No Design Approved label before requesting focal point approvals.

All features (both "Design Approved" and "No Design Approved")
- [ ] **FAT** - (Kevin Smith). SOE FATS are running successfully or N/A . Approver adds label focalApproved:fat to the Epic in Github.
- [ ] **Demo** - (Tom Evans or Chuck Bridgham). Demo is scheduled for an upcoming EOI. Approver adds label focalApproved:demo to the Epic in Github.
- [ ] **Globalization** (Sam Wong - Liberty / Simy Cheeran - tWAS). Translation is complete or N/A. TVT - complete or N/A. Approver adds label focalApproved:globalization to the Epic in Github.

"Design Approved" features
- [ ] **Accessibility** - (Steven Zvonek). Accessibility testing is complete or N/A. Approver adds label focalApproved:accessibility to the Epic in Github.
- [ ] **ID** - (Kareen Deen). Documentation work is complete or N/A . Approver adds label focalApproved:id to the Epic in Github.
- [ ] **Performance** - (Jared Anderson). Performance testing is complete with no high severity defects or N/A . Approver adds label focalApproved:performance to the Epic in Github.
- [ ] **Serviceability** - (Don Bourne). Serviceability has been addressed.
- [ ] **STE** - (Swati Kasundra). STE chart deck is complete or N/A . Approver adds label focalApproved:ste to the Epic in Github.
- [ ] **SVT** - (Brian Hanczaryk- APS). SVT is complete or N/A . Approver adds label focalApproved:svt to the Epic in Github.


## **Ready for GA**
1 week before Onboarding
- [ ] No Stop Ship issues for the feature. (Epic owner / Feature owner / Release PM)
- [ ] Ship Readiness Review and Release Notes completed (Epic owner / Feature owner / Release PM)
- [ ] Github Epic and Epic's issues are closed / complete. All PRs are committed to the master branch. (Epic owner / Feature owner / Backlog Subtribe PM)

## **Other deliverbles**
- [ ] **OL Guides** - (Yee-Kang Chang). Assessment for OL Guides is complete or N/A.
- [ ] **WDT** - (Leonard Theivendra). WDT work complete or N/A.
- [ ] **Blog** - (Laura Cowen) Blog article writeup (Epic owner / Feature owner / Laura Cowen)
