# Glossary

- Protected Core: signed application code that owns identity, policy, credentials, validation and activation.
- Mutable Extension: data/configuration stored separately and interpreted only through core-owned capabilities.
- Candidate: staged plugin manifest not yet active.
- Approval Hash: SHA-256 digest binding owner approval to exact candidate contents.
- Untrusted Data: model output, internet content, imported files and plugin manifests.
- Rollback: restoration of the last active manifest after failed promotion.
- Fail Closed: deny the requested capability when validation, permission or storage checks fail.
- Owner: person with final authority to approve capabilities and configure model credentials.
