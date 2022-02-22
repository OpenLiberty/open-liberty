---
name: Open Liberty Feature Template
about: Steps for Feature Creation and Delivery (Open Liberty org members only)
title: 'Open Liberty Feature Template'
labels: Epic
assignees: ''

---
## Description

Replace this comment with a high level description of the feature. Include enough detail such that the feature can be [prioritized on the backlog](https://github.com/orgs/OpenLiberty/projects/2). As needed, add links to any specifications used by the feature.

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## Documents

When available, add links to required feature documents. Use "N/A" to mark particular documents which are not required by the feature.

- Aha: Externally raised RFE ([Aha](https://cloud-platform.ideas.aha.io/))
  - Link the RFE with this issue
- UFO: Link to Upcoming Feature Overview
- FTS: Link to Feature Test Summary GH Issue
- Beta Blog: Link to Beta Blog Post GH Issue
- GA Blog: Link to GA Blog Post GH Issue

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## Process Overview

- [Prioritization](#prioritization)
- [Design](#design)
- [Implementation](#implementation)
- [Legal and Translation](#legal-and-translation)
- [Beta Delivery](#beta-delivery)
- [GA Delivery](#ga-delivery)
  - [Focal Point Approvals](#focal-point-approvals-2-to-1-weeks-before-delivery)
- [Other Deliverables](#other-deliverables)

### General Instructions

The process steps occur roughly in the order as presented. Process steps occasionally overlap.

Each process step has a number of tasks which must be completed or must be marked as not applicable ("N/A").

Unless otherwise indicated, the tasks are the responsibility of the Feature Owner or a Delegate of the Feature Owner.

**Important: Labels are used to trigger particular steps and must be added as indicated.** 

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **Prioritization**

Area leads are responsible for prioritizing the features and determining which features are being actively worked on.

### **Prioritization**
- [ ] Feature added to the "New" column of the [Open Liberty project board](https://github.com/orgs/OpenLiberty/projects/2)
  - Epics can be added to the board in one of two ways:
    - From this issue, use the "Projects" section to select the appropriate project board.
    - From the appropriate project board click "Add card" and select your Feature Epic issue
- [ ] Priority assigned by Chief Architect.
  - Attend the Liberty Backlog Prioritization meeting

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
- ## **Design** (Before Development or 8 weeks before Delivery)

Design preliminaries determine whether a formal design, which will be provided by an Upcoming Feature Overview (UFO) document, must be created and reviewed.  A formal design is required if the feature requires any of the following: UI, Serviceability, SVT, Performance testing, or non-trival documentation/ID.

### **Design Preliminaries**
- [ ] UI requirements identified. (Owner and UI focal point)
- [ ] ID requirements identified. (Owner and ID focal point) ([Karen Deen](https://github.com/chirp1))
   - Rerfer to [Documenting Open Liberty](https://github.com/OpenLiberty/open-liberty/wiki/Documenting-Open-Liberty).
   - Feature Owner adds label `ID Required`, if non-trival documentation needs to be created by the ID team.
   - ID adds label `ID Required - Trivial`, if no design will be performed and only trivial ID updates are needed.
- [ ] Servicability Requirements Identified. (Owner and Servicability focal point) ([Don Bourne](https://github.com/donbourne))
- [ ] SVT Requirements Identified. (Owner and SVT focal point) ([Brian Hanczaryk](https://github.com/hanczaryk))
- [ ] Performance testing requirements identified. (Owner and Performance focal point) ([Jared Anderson](https://github.com/jhanders34))

### **Design**
- [ ] POC Design / UFO review requested.
  - Owner adds label `Design Review Request`
- [ ] POC Design / UFO review scheduled. (David Chang)
- [ ] POC Design / UFO review completed.
- [ ] POC / UFO Review follow-ons completed.
- [ ] Design / UFO approved. ([Alasdair Nottingham](https://github.com/NottyCode) or N/A
  - ([Alasdair Nottingham](https://github.com/NottyCode) adds label `Design Approved`
  - Add the public link to the UFO in Box to the [Documents](#documents) section.

### **No Design**
- [ ] No Design requested.
-- Owner adds label `No Design Approval Request`
- [ ] No Design / No UFO approved. ([Alasdair Nottingham](https://github.com/NottyCode) or N/A
-- Approver adds label `No Design Approved`

### **FAT Documentation**
- [ ] "Feature Test Summary" child task created
  - Use the [Feature Test Summary Template](https://github.com/OpenLiberty/open-liberty/issues/new?assignees=&labels=Feature+Test+Summary&template=feature_test_summary.md&title=)
  - Add FTS issue link to the [Documents](#documents) section.

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **Implementation**

A feature must be [prioritized](https://github.com/orgs/OpenLiberty/projects/2) and socialized (or `No Design Approved`) before any implementation work may begin and is the minimum before any beta code may be delivered.  GA code may not be delivered until this feature has obtained the "Design Approved" or "No Design Approved" label.

### **Feature Complete** (2 weeks before Delivery)
- [ ] Add the `In Progress` label
- [ ] Implementation completed.
- [ ] Function acceptance tests completed.
- [ ] Backlog issues reviewed for Stop Ship issues.

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **Legal and Translation**

These steps are usually concurrent with implementation, and must be completed along with the primary implementation before either Beta delivery or GA delivery may be requested.

### **Legal** (3 weeks before Delivery)
- [ ] Changed or new open source libraries are cleared and approved, or N/A. (Legal Release Services/Cass Tucker/Release PM).
- [ ] Licenses and Certificates of Originality (COOs) are updated, or N/A

### **Translation** (3 weeks before Delivery)
- [ ] PII updates are merged, or N/A. Note timing with translation shipments.

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **Beta**

In order to facilitate early feedback from users, all new features and functionality should first be released as part of a beta release.

### **Beta Delivery**
- [ ] Beta fence the functionality
  - `kind=beta`, `ibm:beta`, `ProductInfo.getBetaEdition()`
- [ ] Beta development complete and feature ready for inclusion in a beta release
  - Add label `target:beta` and the appropriate `target:YY00X-beta` (where YY00X is the targeted beta version).
- [ ] Feature delivered into beta
  - Release Management ([Sam Wong](https://github.com/samwatibm)) adds label `release:YY00X-beta` (where YY00X is the first beta version that included the functionality).

### **Beta Blog** (1 week before beta GA)
- [ ] Beta blog issue created and populated using the [Open Liberty BETA blog post](https://github.com/OpenLiberty/open-liberty/issues/new/choose) template.
  - Add a link to the beta blog issue in the [Documents](#documents) section.

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **GA Delivery**

GA delivery occurs after obtaining all necessary Focal Point Approvals. The trigger for obtaining focal point approvals is the addition of the `target:ga` label. 

### **Focal Point Approvals** (2 to 1 weeks before delivery)

These occur only after GA deliver is requested (by adding a `target:ga` label).  GA delivery will not occur until all approvals are obtained.

### **All Features**
- [ ] **FAT** All Tests complete and running successfully in SOE or N/A. (Approver; see [OpenLiberty/fat-approvers](https://github.com/orgs/OpenLiberty/teams/fat-approvers))
  - Approver adds label `focalApproved:fat`.
- [ ] **Demo** Demo is scheduled for an upcoming EOI or N/A. ([Tom Evans](https://github.com/tevans78) or [Chuck Bridgham](https://github.com/cbridgha))
  - Approver adds label `focalApproved:demo`.
- [ ] **Globalization** Translation and TVT are complete or N/A. [Sam Wong](https://github.com/samwatibm)
  - Approver adds label `focalApproved:globalization`.

### **Design Approved Features, Only**
- [ ] **Accessibility** Accessibility testing completed or N/A. ([Steven Zvonek](https://github.com/steven1046))
  - Approver adds label `focalApproved:accessibility`.
- [ ] **APIs/Externals** Externals have been reviewed or N/A.  ([Chuck Bridgham](https://github.com/cbridgha))
  - Approver adds label `focalApproved:externals` 
- [ ] **ID** Documentation is complete or N/A. ([Karen Deen](https://github.com/chirp1))
  - Approver adds label `focalApproved:id`.
  - > **_NOTE:_**  If only trivial documentation changes are required, you may reach out to the ID Feature Focal to request a `ID Required - Trivial` label.  Unlike features with regular ID requirement, those with `ID Required - Trivial` label do not have a hard requirement for a Design/UFO.
- [ ] **Performance** Performance testing is complete or N/A. ([Jared Anderson](https://github.com/jhanders34))
  - Approver adds label `focalApproved:performance`.
- [ ] **Serviceability** Serviceability has been addressed or N/A. ([Don Bourne](https://github.com/donbourne))
  - Approver adds label `focalApproved:sve`.
- [ ] **STE** STE chart deck is complete or N/A. (Swati Kasundra)
  - Approver adds label `focalApproved:ste`.
- [ ] **SVT** SVT is complete or N/A. ([Brian Hanczaryk](https://github.com/hanczaryk))
  - Approver adds label `focalApproved:svt`.

### **Remove Beta Fencing**
- [ ] Beta guards are removed, if necessary.
  - Only after all necessary Focal Point Approvals have been granted.

### **GA Delivery**
- [ ] GA development complete and feature ready for inclusion in a GA release
  - Add label `target:ga` and the appropriate `target:YY00X` (where YY00X is the targeted GA version).
  - Inclusion in a release requires the completion of all Focal Point Approvals.

### **GA Blog**
- [ ] GA Blog issue created and populated using the [Open Liberty GA release blog post](https://github.com/OpenLiberty/open-liberty/issues/new/choose) template.
  - Add a link to the GA Blog issue in the [Documents](#documents) section.

### **Ready for GA** (1 week before Delivery)
- [ ] All PRs are merged.
- [ ] All epic and child issues are closed.
- [ ] All stop ship issues are completed.
- [ ] All items in this template completed.
- [ ] Ship Readiness Review completed. (Owner and Release PM)

### **Post GA**
- [ ] Replace `target:YY00X` label with the appropriate `release:YY00X`. ([Sam Wong](https://github.com/samwatibm))

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **Other Deliverables**

- [ ] **OL Guides** OL Guides assessment is complete or N/A. ([Yee-Kang Chang](https://github.com/yeekangc))
- [ ] **Standalone Feature Blog Post** A blog post specifically about your feature or N/A. ([Grace Jansen](https://github.com/GraceJansen))
  - This should be strongly especially for larger or more promintent features.
  - Follow [instructions](https://github.com/OpenLiberty/blogs/tree/draft#writing-and-publishing-blog-posts-on-the-openlibertyio-blog) in the blogs repo.
- [ ] **WDT** WDT work is complete or N/A. (Leonard Theivendra)

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
