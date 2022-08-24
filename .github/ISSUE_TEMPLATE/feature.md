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
- UFO: Link to [Upcoming Feature Overview](https://ibm.box.com/v/UFO-Template) document
  - Set the Box link to be publicly accessible, with a long expiration (10 years)
    - Click "Share" > select "People with link" > click "Link Settings" > under "Link Expiration" select "Disable Shared Link on" > set an expiration date ~10 years into the future
    - If you lack permissions, contact [OpenLiberty/release-architect](https://github.com/orgs/OpenLiberty/teams/release-architect)
- FTS: Link to Feature Test Summary GH Issue
- Beta Blog: Link to Beta Blog Post GH Issue
- GA Blog: Link to GA Blog Post GH Issue

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## Process Overview

- [Prioritization](#prioritization)
- [Design](#design)
- [Implementation](#implementation)
- [Legal and Translation](#legal-and-translation)
- [Beta](#beta)
- [GA](#ga)
  - [Focal Point Approvals](#focal-point-approvals-complete-by-feature-complete-date)
- [Other Deliverables](#other-deliverables)

### General Instructions

The process steps occur roughly in the order as presented. Process steps occasionally overlap.

Each process step has a number of tasks which must be completed or must be marked as not applicable ("N/A").

Unless otherwise indicated, the tasks are the responsibility of the Feature Owner or a Delegate of the Feature Owner.

If you need assistance, reach out to the [OpenLiberty/release-architect](https://github.com/orgs/OpenLiberty/teams/release-architect).

**Important: Labels are used to trigger particular steps and must be added as indicated.** 

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **Prioritization** (Complete Before Development Starts)

The ([OpenLiberty/chief-architect](https://github.com/orgs/OpenLiberty/teams/chief-architect)) and area leads are responsible for prioritizing the features and determining which features are being actively worked on.

### **Prioritization**
- [ ] Feature added to the "New" column of the [Open Liberty project board](https://github.com/orgs/OpenLiberty/projects/2)
  - Epics can be added to the board in one of two ways:
    - From this issue, use the "Projects" section to select the appropriate project board.
    - From the appropriate project board click "Add card" and select your Feature Epic issue
- [ ] Priority assigned
  - Attend the Liberty Backlog Prioritization meeting

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **Design** (Complete Before Development Starts)

Design preliminaries determine whether a formal design, which will be provided by an [Upcoming Feature Overview (UFO)](https://ibm.box.com/v/UFO-Template) document, must be created and reviewed.  A formal design is required if the feature requires any of the following: UI, Serviceability, SVT, Performance testing, or non-trivial documentation/ID.

### **Design Preliminaries**
- [ ] UI requirements identified. (Owner and [UI focal point](https://github.com/orgs/OpenLiberty/teams/ui-approvers))
- [ ] ID requirements identified. (Owner and [ID focal point](https://github.com/orgs/OpenLiberty/teams/id-approvers))
   - Refer to [Documenting Open Liberty](https://github.com/OpenLiberty/open-liberty/wiki/Documenting-Open-Liberty).
   - Feature Owner adds label `ID Required`, if non-trivial documentation needs to be created by the ID team.
   - ID adds label `ID Required - Trivial`, if no design will be performed and only trivial ID updates are needed.
- [ ] Serviceability Requirements Identified. (Owner and [Serviceability focal point](https://github.com/orgs/OpenLiberty/teams/serviceability-approvers))
- [ ] SVT Requirements Identified. (Owner and [SVT focal point](https://github.com/orgs/OpenLiberty/teams/svt-approvers))
- [ ] Performance testing requirements identified. (Owner and [Performance focal point](https://github.com/orgs/OpenLiberty/teams/performance-approvers))

### **Design**
- [ ] POC Design / UFO review requested.
  - Owner adds label `Design Review Request`
- [ ] POC Design / UFO review scheduled.
  - Follow the instructions in POC-Forum repo
- [ ] POC Design / UFO review completed.
- [ ] POC / UFO Review follow-ons completed.
- [ ] Design / UFO approved. ([OpenLiberty/chief-architect](https://github.com/orgs/OpenLiberty/teams/chief-architect)) or N/A
  - ([OpenLiberty/chief-architect](https://github.com/orgs/OpenLiberty/teams/chief-architect)) adds label `Design Approved`
  - Add the public link to the UFO in Box to the [Documents](#documents) section.
  - The UFO must always accurately reflect the final implementation of the feature. Any changes must be first approved. Afterwards, update the UFO by creating a copy of the original approved slide(s) at the end of the deck and prepend "OLD" to the title(s). A single updated copy of the slide(s) should take the original's place, and have its title(s) prepended with "UPDATED".

### **No Design**
- [ ] No Design requested.
  - Owner adds label `No Design Approval Request`
- [ ] No Design / No UFO approved. ([OpenLiberty/chief-architect](https://github.com/orgs/OpenLiberty/teams/chief-architect)) or N/A
  - Approver adds label `No Design Approved`

### **FAT Documentation**
- [ ] "Feature Test Summary" child task created
  - Use the [Feature Test Summary Template](https://github.com/OpenLiberty/open-liberty/issues/new?assignees=&labels=Feature+Test+Summary&template=feature_test_summary.md&title=)
  - Add FTS issue link to the [Documents](#documents) section.

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **Implementation**

A feature must be [prioritized](https://github.com/orgs/OpenLiberty/projects/2) before any implementation work may begin to be delivered (inaccessible/no-ship).  However, a design focused approach should still be applied to features, and developers should think about the feature design prior to writing and delivering any code.  
Besides being prioritized, a feature must also be socialized (or No Design Approved) before any beta code may be delivered.  All new Liberty content must be inaccessible in our GA releases until it is [Feature Complete](#feature-complete) by either marking it `kind=noship` or [beta fencing](#beta-code) it.  
Code may not GA until this feature has obtained the "Design Approved" or "No Design Approved" label, along with all other tasks outlined in the [GA](#ga) section.

### **Feature Development Begins**
- [ ] Add the `In Progress` label

## **Legal and Translation**

In order to avoid last minute blockers and significant disruptions to the feature, the **legal items need to be done as early in the feature process as possible**, either in design or as early into the development as possible.  Similarly, translation is to be done concurrently with development.  Both **MUST** be completed before Beta or GA is requested.

### **Legal** (Complete before Feature Complete Date)
- [ ] Changed or new open source libraries are cleared and approved, or N/A. (Legal Release Services/Cass Tucker/Release PM).
- [ ] Licenses and Certificates of Originality (COOs) are updated, or N/A

### **Translation** (Complete 1 week before Feature Complete Date)
- [ ] PII updates are merged, or N/A. Note timing with translation shipments.

### **Innovation** (Complete 1 week before Feature Complete Date)
- [ ] Consider whether any aspects of the feature may be patentable. If any identified, disclosures have been submitted.

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **Beta**

In order to facilitate early feedback from users, all new features and functionality should first be released as part of a beta release.

### **Beta Code**
- [ ] Beta fence the functionality
  - `kind=beta`, `ibm:beta`, `ProductInfo.getBetaEdition()`
- [ ] Beta development complete and feature ready for inclusion in a beta release
  - Add label `target:beta` and the appropriate `target:YY00X-beta` (where YY00X is the targeted beta version).
- [ ] Feature delivered into beta
  - ([OpenLiberty/release-manager](https://github.com/orgs/OpenLiberty/teams/release-manager)) adds label `release:YY00X-beta` (where YY00X is the first beta version that included the functionality).

### **Beta Blog** (Complete 1.5 weeks before beta eGA)
- [ ] Beta blog issue created and populated using the [Open Liberty BETA blog post](https://github.com/OpenLiberty/open-liberty/issues/new/choose) template.
  - Add a link to the beta blog issue in the [Documents](#documents) section.

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **GA**

A feature is ready to GA after it is Feature Complete and has obtained all necessary Focal Point Approvals. 

### **Feature Complete**
- [ ] Feature implementation and tests completed.
  - [ ] All PRs are merged.
  - [ ] All epic and child issues are closed.
  - [ ] All stop ship issues are completed.
- [ ] Legal: all necessary approvals granted.
- [ ] Translation: All messages translated or sent for translation for upcoming release 
- [ ] GA development complete and feature ready for inclusion in a GA release
  - Add label `target:ga` and the appropriate `target:YY00X` (where YY00X is the targeted GA version).
  - Inclusion in a release requires the completion of all Focal Point Approvals.

### **Focal Point Approvals** (Complete by Feature Complete Date)

These occur only after GA of this feature is requested (by adding a `target:ga` label).  GA of this feature may not occur until all approvals are obtained.

### **All Features**
- [ ] **APIs/Externals** Externals have been reviewed or N/A.  ([OpenLiberty/externals-approvers](https://github.com/orgs/OpenLiberty/teams/externals-approvers))
  - Approver adds label `focalApproved:externals` 
- [ ] **Demo** Demo is scheduled for an upcoming EOI or N/A. ([OpenLiberty/demo-approvers](https://github.com/orgs/OpenLiberty/teams/demo-approvers))
  - Add comment `@OpenLiberty/demo-approvers Demo scheduled for EOI [Iteration Number]` to this issue.
  - Approver adds label `focalApproved:demo`.
- [ ] **FAT** All Tests complete and running successfully in SOE or N/A. ([OpenLiberty/fat-approvers](https://github.com/orgs/OpenLiberty/teams/fat-approvers))
  - Approver adds label `focalApproved:fat`.
- [ ] **Globalization** Translation and TVT are complete or N/A. ([OpenLiberty/globalization-approvers](https://github.com/orgs/OpenLiberty/teams/globalization-approvers))
  - Approver adds label `focalApproved:globalization`.

### **Design Approved Features**
- [ ] **Accessibility** Accessibility testing completed or N/A. ([OpenLiberty/accessibility-approvers](https://github.com/orgs/OpenLiberty/teams/accessibility-approvers))
  - Approver adds label `focalApproved:accessibility`.
- [ ] **ID** Documentation is complete or N/A. ([OpenLiberty/id-approvers](https://github.com/orgs/OpenLiberty/teams/id-approvers))
  - Approver adds label `focalApproved:id`.
  - > **_NOTE:_**  If only trivial documentation changes are required, you may reach out to the ID Feature Focal to request a `ID Required - Trivial` label.  Unlike features with regular ID requirement, those with `ID Required - Trivial` label do not have a hard requirement for a Design/UFO.
- [ ] **Performance** Performance testing is complete or N/A. ([OpenLiberty/performance-approvers](https://github.com/orgs/OpenLiberty/teams/performance-approvers))
  - Approver adds label `focalApproved:performance`.
- [ ] **Serviceability** Serviceability has been addressed or N/A. ([OpenLiberty/serviceability-approvers](https://github.com/orgs/OpenLiberty/teams/serviceability-approvers))
  - Approver adds label `focalApproved:sve`.
- [ ] **STE** Skills Transfer Education chart deck is complete or N/A. ([OpenLiberty/ste-approvers](https://github.com/orgs/OpenLiberty/teams/ste-approvers))
  - Approver adds label `focalApproved:ste`.
- [ ] **SVT** System Verification Test is complete or N/A. ([OpenLiberty/svt-approvers](https://github.com/orgs/OpenLiberty/teams/svt-approvers))
  - Approver adds label `focalApproved:svt`.

### **Remove Beta Fencing** (Complete by Feature Complete Date)
- [ ] Beta guards are removed, or N/A
  - Only after all necessary Focal Point Approvals have been granted.

### **GA Blog** (Complete by Feature Complete Date)
- [ ] GA Blog issue created and populated using the [Open Liberty GA release blog post](https://github.com/OpenLiberty/open-liberty/issues/new/choose) template.
  - Add a link to the GA Blog issue in the [Documents](#documents) section.

### **Post GA**
- [ ] Replace `target:YY00X` label with the appropriate `release:YY00X`. ([OpenLiberty/release-manager](https://github.com/orgs/OpenLiberty/teams/release-manager))

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
## **Other Deliverables**

- [ ] **Standalone Feature Blog Post** A blog post specifically about your feature or N/A.  ([OpenLiberty/release-architect](https://github.com/orgs/OpenLiberty/teams/release-architect))
  - This should be strongly considered for larger or more prominent features.
  - Follow [instructions](https://github.com/OpenLiberty/blogs/tree/draft#writing-and-publishing-blog-posts-on-the-openlibertyio-blog) in the blogs repo.
- [ ] **OL Guides** OL Guides assessment is complete or N/A. ([OpenLiberty/guide-assessment](https://github.com/orgs/OpenLiberty/teams/guide-assessment/members))
- [ ] **Dev Experience** Developer Experience & Tools work is complete or N/A. ([OpenLiberty/dev-experience-assessment](https://github.com/orgs/OpenLiberty/teams/dev-experience-assessment/members))
- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
