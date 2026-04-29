# AGENTS.md - Project Context for AI Agents

## Repository Structure

- `dev/` - Main source code directory for the project
  - All source code for Open Liberty
    - Java runtime
    - Unit tests
    - Functional tests (AKA functional acceptance tests or FATs)
  - Build scripts for building and testing
- `cla/` - Contributor License Agreements (corporate and individual)
- `.ci-orchestrator/` - CI/CD infrastructure

## Common Development Tasks

All of the following commands assume that you have cloned the repository and are in the `open-liberty/dev` directory.

### Building the product

These steps will build the Open Liberty runtime and all of its features. This is the recommended way to build the product.

**Prerequisite:** The `JAVA_HOME` environment variable must point to a Java 17 or Java 21 SDK. If setting `JAVA_HOME` to Java 17, you will also need to set `JAVA_21_HOME` to a Java 21 SDK.

```bash
$ ./gradlew cnf:initialize
$ ./gradlew assemble
```

#### Perform a local release

```bash
$ ./gradlew releaseNeeded
```

This task releases all projects to the local releaseRepo. The final zip can be found in:
> open-liberty/dev/cnf/release/dev/openliberty/<version>/openliberty-<version>.zip

### Building a single project

```bash
$ ./gradlew com.ibm.ws.kernel.boot:build
```

### Running unit tests

```bash
$ ./gradlew com.ibm.ws.kernel.boot:test
```

### Running functional tests

```bash
$ ./gradlew build.example_fat:buildandrun
```

## Git Commit Message Format

### Rule: AI Co-authorship Attribution

**All commits that contain content created by AI must have the git commit message end with the following format:**

```
Co-authored-by-AI: <Agent Name> <Agent Version> (<Model Version>)
```

### Format Details

- **Agent Name**: The name of the AI tool
- **Agent Version**: The version of the AI tool used (e.g., 1.0.0, 1.2.3)
- **Model Version**: The underlying LLM model and version used by the AI tool (e.g., Claude Sonnet 4.6, GPT-5.4, Llama 3.2 90B). This value is optional but must list all models that were known to be used.

### Examples

#### Example 1: Using Claude Sonnet
```
Fix authentication bug in JWT validation

Updated the token expiration check to properly handle timezone offsets.
Added unit tests to verify the fix works across different timezones.

Co-authored-by-AI: IBM Bob 1.0.0 (Claude Sonnet 4.6)
```

#### Example 2: Using multiple AI tools
```
Add support for Jakarta EE 11 features

Implemented new Jakarta EE 11 APIs and updated configuration handling.
Includes backward compatibility for Jakarta EE 10.

Co-authored-by-AI: IBM Bob 1.0.0 (Claude Sonnet 4.6)
Co-authored-by-AI: GitHub Copilot (GPT-5.4)
```

### Important Notes

1. **Placement**: The co-authorship line must be at the **end** of the commit message, after all other content including issue references.

2. **Blank Line**: Include a blank line before the co-authorship attribution if your commit message has a body.

3. **Version Accuracy**: Always use the actual version numbers of the AI tool and the model at the time of code generation.
