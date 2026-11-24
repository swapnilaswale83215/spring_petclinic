# Rollback plan: java-17-to-21-upgrade-5d8272c2

## From the platform design

- Rollback = git revert of the PR, rehearsed on a branch (SPEC-PLAT-9).
- SPEC-PLAT-9: Reverting the PR restores the Java 17 + Boot 4.1.0 pipeline, and this is rehearsed before merge.
- ADR-PLAT-6: Rollback = `git revert` of the merge commit (or the PR commits) on a branch, then CI must show `./mvnw -B verify` green on Java 17 + Boot 4.1.0. The revert restores matrix 17, adopt, the 17 Dockerfile and both build files together. devcontainer.json stays at 21 and still builds the 17 target. The rehearsal runs on a throw-away branch before merge. The target is a green main within 30 minutes of the decision, to be timed in the rehearsal. No database step is needed (ADR-APP-7).
- .devcontainer/devcontainer.json: Already requests Java 21 (21-oracle). It is the reference value the Dockerfile must now agree with. It stays unchanged, so a rollback leaves it at 21, which still builds a Java 17 target.
