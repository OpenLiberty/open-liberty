# GenAI Usage for Code Contributions

## Overview

This guidance establishes a framework for using generative AI tools in code contributions while maintaining code quality and emphasizing contributor responsibility.

## Scope & Applicability

**What's Covered**: The policy is applicable to all code contributions for this project where generative AI tools were used in the development process.

## Key Requirements for Contributors

### 1. Transparent AI Code Identification

Contributors must use **commit comments** to clearly mark AI-generated contributions. This enables:
- Transparency in the development process
- Easier tracking and auditing if issues arise
- Informed decision-making by maintainers during code review

Commits that include AI-generated content must have their commit message end in the following format:

```
Co-authored-by-AI: <AI Tool/IDE/Platform> (<Model Name/Version>)
```

**Examples:**
```
Co-authored-by-AI: IBM Bob 1.0.0
Co-authored-by-AI: GitHub Copilot (gpt-4.1)
Co-authored-by-AI: Claude Code (claude-sonnet-4-6)
```

### 2. Functional Quality

AI generated code contributed should be reliable and engineered with care to be free of intellectual property or security problems and meet at least the same standard as human authored code:

- **Functionality**: Code must accomplish its intended purpose
- **Quality**: Code must adhere to project standards and best practices
- **Validation**: Contributors must test and verify AI-generated code
- **Confidence**: Contributors should understand and vouch for the code they submit

### 3. CLA Compliance

All contributions containing AI-generated code must satisfy all of the following:

- Code **created by the contributor** through prompting and guiding AI tools in accordance with [this policy](GENAI_GUIDELINES.md) may be validly contributed to the project notwithstanding that such AI-developed code may not be an original work of authorship or creation by the contributor
- Prior to submission, contributors must **validate** that the AI-generated content does not violate any copyrights
- The contributor takes ownership and responsibility for compliance with this Policy and the CLA with regard to all AI-generated code submitted to the project
- AI is a tool in the contributor's hands, not a replacement for contributor accountability for the code it contributes to the project
- CLA compliance remains the foundational requirement, contributors must still certify they have rights to submit contributions