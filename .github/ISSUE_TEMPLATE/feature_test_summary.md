---
name: Feature Test Summary
about: Create a summary of the planned testing for a feature
title: ''
labels: Feature Test Summary
assignees: ''

---

**1) Describe the test strategy & approach for this feature, and describe how the approach verifies the functions delivered by this feature. The description should include the positive and negative testing done, whether all testing is automated, what manual tests exist (if any) and where the tests are stored (source control). Automated testing is expected for all features with manual testing considered an exception to the rule.**

For any feature, be aware that only FAT tests (not unit or BVT) are executed in our cross platform testing. To ensure cross platform testing ensure you have sufficient FAT coverage to verify the feature.

If delivering tests outside of the standard Liberty FAT framework, do the tests push the results into cognitive testing database (if not, consult with the CSI Team who can provide advice and verify if results are being received)?_

**2) Collectively as a team you need to assess your confidence in the testing delivered based on the values below.  This should be done as a team and not an individual to ensure more eyes are on it and that pressures to deliver quickly are absorbed by the team as a whole.**

Please indicate your confidence in the testing (up to and including FAT) delivered with this feature by selecting one of these values:

0 - No automated testing delivered

1 - We have minimal automated coverage of the feature including golden paths.  There is a relatively high risk that defects or issues could be found in this feature.

2 - We have delivered a reasonable automated coverage of the golden paths of this feature but are aware of gaps and extra testing that could be done here.  Error/outlying scenarios are not really covered.  There are likely risks that issues may exist in the golden paths

3 - We have delivered all automated testing we believe is needed for the golden paths of this feature and minimal coverage of the error/outlying scenarios.  There is a risk when the feature is used outside the golden paths however we are confident on the golden path.  Note:  This may still be a valid end state for a feature... things like Beta features may well suffice at this level.

4 - We have delivered all automated testing we believe is needed for the golden paths of this feature and have good coverage of the error/outlying scenarios.  While more testing of the error/outlying scenarios could be added we believe there is minimal risk here and the cost of providing these is considered higher than the benefit they would provide.

5 - We have delivered all automated testing we believe is needed for this feature.  The testing covers all golden path cases as well as all the error/outlying scenarios that make sense.  We are not aware of any gaps in the testing at this time. No manual testing is required to verify this feature.

Based on your answer above, for any answer other than a 4 or 5 please provide details of what drove your answer.  Please be aware, it may be perfectly reasonable in some scenarios to deliver with any value above.  We may accept no automated testing is needed for some features, we may be happy with low levels of testing on samples for instance so please don't feel the need to drive to a 5.  We need your honest assessment as a team and the reasoning for why you believe shipping at that level is valid. What are the gaps, what is the risk etc.  Please also provide links to the follow on work that is needed to close the gaps (should you deem it needed)
