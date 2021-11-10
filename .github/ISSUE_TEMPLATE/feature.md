---
name: Feature Template
about: Steps for Feature Creation and Delivery
title: 'Open Liberty Feature Template'
labels: Epic
assignees: ''

---
## Description

Replace this comment with a high level description of the feature. Include enough detail such that the feature can be [prioritized on the backlog](https://github.com/orgs/OpenLiberty/projects/2). As needed, add links to specifications used by the feature.

- Link to Specification

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## Documentation

When available, add links to required feature documents. Use "N/A" to mark particular documents which are not required by the feature,.

- Aha: Externally raised RFE ([Aha](https://cloud-platform.ideas.aha.io/))
-- Link the RFE with this issue
- UFO: Link to (Upcoming Feature Overview)
- FTS: Link to (Feature Test Summary)
- Beta Blog: Link to Beta Blog Post
- GA Blog: Link to GA Blog Post

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## Process Overview

- Epic Creation
- Design
- Prioritization
- Implementation
- Legal and Translation
- Beta Delivery
- GA Delivery
- Focal Point Approval
- Other Deliverables

### General Instructions

The process steps occur roughly in the order as presented. Process steps occasionally overlap.

Each process step has a number of activities which must be completed or must be marked as not applicable ("N/A").

Unless otherwise indicated, activities are the responsibility of the Epic Owner or a Delegate of the Epic Owner.

Important: Labels are used to trigger particular steps and **must be placed as indicated.** See: [Summary of Feature Labels](https://ibm.box.com/s/jf55jzpkn6qiy4wfolf9jm75ak8b5g0m)

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **Epic Creation**

- [ ] Epic Created
-- Attach this template to the epic issue or feature.
-- Add the label `Epic` or `tWAS`.
-- Cross link with Aha, if possible.

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **Design** (Before Development or 8 weeks before Delivery)

Design preliminaries determine whether a formal design, which will be provided by an Upcoming Feature Overview (UFO) document, must be created and reviewed.  A formal design is required if there are substantial UI, ID, or Serviceability requirements, or if particular SVT or Performance testing must be done.

### **Design Preliminaries**
- [ ] UI requirements identified. (Owner and UI focal point)
- [ ] ID requirements identified. (Owner and ID focal point) ([Karen Deen](https://github.com/chirp1))
-- See [Documenting Open Liberty](https://github.com/OpenLiberty/open-liberty/wiki/Documenting-Open-Liberty).
-- ID adds label `ID Required - Trivial`, if no design will be performed and only trivial ID updates are needed.
- [ ] Servicability Requirements Identified. (Owner and Servicability focal point) ([Don Bourne](https://github.com/donbourne))
- [ ] SVT Requirements Identified. (Owner and SVT focal point) ([Brian Hanczaryk](https://github.com/hanczaryk))
- [ ] Performance testing requirements identified. (Owner and Performance focal point) ([Jared Anderson](https://github.com/jhanders34))

### **Design**
- [ ] POC Design / UFO review requested.
-- Owner adds label `Design Review Request`
- [ ] POC Design / UFO review scheduled. (David Chang)
- [ ] POC Design / UFO review completed.
- [ ] POC / UFO Review follow-ons completed.
- [ ] Design / UFO approved. (Alasdair Nottingham)
-- Approver adds label `Design Approved`

### **No Design**
- [ ] No Design requested.
-- Owner adds label `No Design Approval Request`
- [ ] No Design / No UFO approved. (Cloud: Arthur De Magalhaes; Server: Alasdair Nottingham)
-- Approver adds label `No Design Approved`

### **FAT Documentation**
- [ ] "FAT Approval Test Summary" child task created
-- [Template](https://github.com/OpenLiberty/open-liberty/issues/new?assignees=&labels=Feature+Test+Summary&template=feature_test_summary.md&title=)
-- The template is preset with label `Feature Test Summary`.
-- Add a link to the task, above.

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **Prioritization**

A feature must be prioritized and design steps completed before implementation work may begin.  See [Prioritization](https://github.com/orgs/OpenLiberty/projects/2).

### **Prioritization**
- [ ] Priority assigned. (?)
-- One of the labels `Design Approved` or `No Design Approved` is required.
-- Owner adds label `In Progress`

Implementation usually begins at this time.

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **Implementation**

This step includes the work to implement the feature.

### **Feature Complete** (2 weeks before Delivery)
- [ ] Implementation completed.
- [ ] Function acceptance tests completed.
- [ ] Continuation issue created for backlog, if necessary.
- [ ] Backlog issues are created.
- [ ] Backlog issues reviewed for Stop Ship. (Owner and PM)

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **Legal and Translation**

These steps are usually concurrent with implementation, and must be completed along with the primary implementation before either Beta delivery or GA delivery may be requested.

### **Legal** (3 weeks before Delivery)
- [ ] Changed or new open source libraries are cleared and approved. (Legal Release Services/Cass Tucker/Release PM).
- [ ] Licenses and Certificates of Originality (COOs) are updated

### **Translation** (3 weeks before Delivery)
- [ ] PII updates are merged. Note timing with translation shipments.

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **Beta**

If the feature is to be included in a beta, appropriate beta guards must be placed on the function, a beta blog post might be created, and beta delivery must be requested.

### **Beta Fencing**
- [ ] Beta fence the functionality
-- `kind=beta`, `ibm:beta`, `ProductInfo.getBetaEdition()`

### **Beta Blog** (1 week before beta GA)
- [ ] Beta blog issue created.
-- [Beta Blog issue](https://github.com/OpenLiberty/open-liberty/issues/new?assignees=lauracowen%2C+jakub-pomykala&labels=&template=blog_post_beta.md&title=BETA+BLOG+-+title_of_your_update)
-- Add a link to the beta blog issue, above.

### **Beta Delivery**
- [ ] Initial pull requests (PRs) are merged into integration
- [ ] Beta delivery requested
-- Owner adds label `target:beta` or `target:YY00X-beta`.
- [ ] Feature delivered into beta (?)
-- (?) adds label `release:YY00X-beta`.

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **GA Delivery**

GA delivery is concurrent with obtaining Focal Point Approval. The trigger for obtaining focal point approvals is the addition of the `target:ga` label. Actual GA delivery occurs only after all focal point approvals have been obtained.

### **Remove Beta Fencing**
- [ ] Beta guards are removed, if necessary.

### **GA Blog**
- [ ] GA Blog issue created.
-- [GA Blog issue](https://github.com/OpenLiberty/open-liberty/issues/new?assignees=lauracowen%2C+jakub-pomykala&labels=&template=blog_post_ga_release.md&title=GA+BLOG+-+title_of_your_update)
-- Add a link to the GA Blog issue, above.
- [ ] GA Blog is written. (Owner and [Laura Cowen](https://github.com/lauracowen))

### **GA Delivery**
- [ ] GA delivery requested.
-- Owner adds label `target:ga`.
-- Inclusion in a release requires the completion of all Focal Point Approvals.
- [ ] Feature delivered into GA. (?)
-- (?) adds label `release:YY00X`.

### **Ready for GA** (1 week before Delivery)
- [ ] All PRs are merged.
- [ ] All epic and child issues are closed.
- [ ] All stop ship issues are completed.
- [ ] Ship Readiness Review completed. (Owner and Release PM)
- [ ] Release Notes completed. (Owner and Release PM)

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **Focal Point Approvals** (2 to 1 week before delivery)

These occur only after GA deliver is requested (by adding a `target:ga` label).  GA delivery will not occur until all approvals are obtained.

### **All Features**
- [ ] **FAT** SOE FATS are running. (Approver; see [OpenLiberty/fat-approvers](https://github.com/orgs/OpenLiberty/teams/fat-approvers))
-- Approver adds label `focalApproved:fat`.
- [ ] **Demo** Demo is scheduled for an upcoming EOI. ([Tom Evans](https://github.com/tevans78) or [Chuck Bridgham](https://github.com/cbridgha))
-- Approver adds label `focalApproved:demo`.
- [ ] **Globalization** Translation and TVT are complete. (Liberty: [Sam Wong](https://github.com/samwatibm); tWAS: Simy Cheeran)
-- Approver adds label `focalApproved:globalization`.

### **Design Approved Features, Only**
- [ ] **Accessibility** Accessibility testing completed. ([Steven Zvonek](https://github.com/steven1046))
-- Approver adds label `focalApproved:accessibility`.
- [ ] **ID** Documentation is complete. ([Karen Deen](https://github.com/chirp1))
-- Approver adds label `focalApproved:id`.
- [ ] **Performance** Performance testing is complete. ([Jared Anderson](https://github.com/jhanders34))
-- Approver adds label `focalApproved:performance`.
- [ ] **Serviceability** Serviceability has been addressed. ([Don Bourne](https://github.com/donbourne))
-- Approver adds label `focalApproved:sve`.
- [ ] **STE** STE chart deck is complete. (Swati Kasundra)
-- Approver adds label `focalApproved:ste`.
- [ ] **SVT** SVT is complete. ([Brian Hanczaryk](https://github.com/hanczaryk))
-- Approver adds label `focalApproved:svt`.

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **Other Deliverables**

- [ ] **OL Guides** OL Guides assessment is complete. ([Yee-Kang Chang](https://github.com/yeekangc))
- [ ] **WDT** WDT work is complete. (Leonard Theivendra)

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

