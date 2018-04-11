OpenLiberty Issue creator,

ATTENTION, READ THIS: Updated 4/11/2018 - Read and understand this completely,
then delete this entire template. If a reviewer or merger sees this template,
they should fail the review or merge.

If this issue is to raise a problem found in released code, or to document a problem
that **could** be seen in a previous full release (17.0.0.4, 18.0.0.1, etc. not a daily driver)
it MUST be labelled with “release bug” 

This directs automation to scrape this fix for inclusion in the next release's
list of bugs fixed.

Do note that an issue can have multiple Pull Requests mention it, so if an Issue is opened
and an attempt is made to fix it via pull request and is not successful, just open a new
pull request. A release bug will not be considered complete until the Issue itself is closed.
If an attempt to fix an issue is not successful, **reopen** the same issue as soon as possible. 

If this issue is NOT for describing a released bug, for example new function, fixing
a bug in unreleased function, or other improvements that do not affect the
user-space, no label is needed, but do still delete this text block.

If it's not fully clear, or the issue falls into a grey area, err on the side of marking
as "release bug" versus not marking. A changelog slightly longer is OK, but excluding an item
that really should be listed as fixed is problematic.

For full details, please see this wiki page:

https://github.com/OpenLiberty/open-liberty/wiki/Open-Liberty-Conventions

