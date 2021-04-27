---
name: Open Liberty Feature
about: Create a Feature Epic (Open Liberty org members only)
title: ''
labels: Epic
assignees: ''

---
## Description of the high level feature, including any external spec links:  
<br/><br/><br/>  


##
#### Before proceeding to any items below (active development), this feature must be prioritized on the backlog, and have either the "Design Approved" or "No Design Approved" labels.  Follow the Feature and UFO Approval Process.
- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## When complete & mandatory, add links to the UFO (Upcoming Feature Overview) document, FTS (Feature Test Summary), and blogs post issues(s):
- UFO:
- FTS:
- Beta Blog Post (if applicable):
- Blog Post: 

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
- [ ] Design / UFO Approval ([Alasdair Nottingham](https://github.com/NottyCode)) or N/A.
- [ ] No Design / No UFO Approval ([Arthur De Magalhaes](https://github.com/arthurdm) - cloud / [Alasdair Nottingham](https://github.com/NottyCode) - server) or N/A.
- [ ] SVT Requirements identified. (Epic owner / Feature owner with SVT focal point)
- [ ] ID Requirements identified ([Documenting Open Liberty](https://github.com/OpenLiberty/open-liberty/wiki/Documenting-Open-Liberty)). (Epic owner / Feature owner with ID focal point)
- [ ] Create a child task of this epic entitled "Feature Test Summary" via [this template](https://github.com/OpenLiberty/open-liberty/issues/new?assignees=&labels=Feature+Test+Summary&template=feature_test_summary.md&title=). Add the link in above.

## **Beta**
If your feature, or portions of it, are going to be included in a beta  
Before Onboarding the beta
- [ ] Beta Fence the functionality (`kind=beta`, `ibm:beta`, `ProductInfo.getBetaEdition()`)  

1 week before beta GA
- [ ] Create, populate, and link to the [Beta blog post issue](https://github.com/OpenLiberty/open-liberty/issues/new?assignees=lauracowen%2C+jakub-pomykala&labels=&template=blog_post_beta.md&title=BETA+BLOG+-+title_of_your_update)

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
- [ ] **FAT** - ([OpenLiberty/fat-approvers](https://github.com/orgs/OpenLiberty/teams/fat-approvers)). SOE FATS are running successfully or N/A . Approver adds label focalApproved:fat to the Epic in Github.
- [ ] **Demo** - ([Tom Evans](https://github.com/tevans78) or [Chuck Bridgham](https://github.com/cbridgha)). Demo is scheduled for an upcoming EOI. Approver adds label focalApproved:demo to the Epic in Github.
- [ ] **Globalization** ([Sam Wong](https://github.com/samwatibm) - Liberty / Simy Cheeran - tWAS). Translation is complete or N/A. TVT - complete or N/A. Approver adds label focalApproved:globalization to the Epic in Github.

"Design Approved" features
- [ ] **Accessibility** - ([Steven Zvonek](https://github.com/steven1046)). Accessibility testing is complete or N/A. Approver adds label focalApproved:accessibility to the Epic in Github.
- [ ] **ID** - ([Karen Deen](https://github.com/chirp1)). Documentation work is complete or N/A . Approver adds label focalApproved:id to the Epic in Github.
- [ ] **Performance** - ([Jared Anderson](https://github.com/jhanders34)). Performance testing is complete with no high severity defects or N/A . Approver adds label focalApproved:performance to the Epic in Github.
- [ ] **Serviceability** - ([Don Bourne](https://github.com/donbourne)). Serviceability has been addressed.
- [ ] **STE** - (Swati Kasundra). STE chart deck is complete or N/A . Approver adds label focalApproved:ste to the Epic in Github.
- [ ] **SVT** - ([Brian Hanczaryk](https://github.com/hanczaryk) - APS). SVT is complete or N/A . Approver adds label focalApproved:svt to the Epic in Github.


## **Ready for GA**
1 week before Onboarding
- [ ] No Stop Ship issues for the feature. (Epic owner / Feature owner / Release PM)
- [ ] Ship Readiness Review and Release Notes completed (Epic owner / Feature owner / Release PM)
- [ ] Github Epic and Epic's issues are closed / complete. All PRs are committed to the release branch. (Epic owner / Feature owner / Backlog Subtribe PM)

1 week before GA
- [ ] Create, populate, and link to the [Blog post issue](https://github.com/OpenLiberty/open-liberty/issues/new?assignees=lauracowen%2C+jakub-pomykala&labels=&template=blog_post_ga_release.md&title=GA+BLOG+-+title_of_your_update)

## **Other deliverbles**
- [ ] **OL Guides** - ([Yee-Kang Chang](https://github.com/yeekangc)). Assessment for OL Guides is complete or N/A.
- [ ] **WDT** - (Leonard Theivendra). WDT work complete or N/A.
- [ ] **Blog** - ([Laura Cowen](https://github.com/lauracowen)) Blog article writeup (Epic owner / Feature owner / Laura Cowen)
