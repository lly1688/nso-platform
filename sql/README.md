# Root SQL Policy

The production Flyway source of truth is only `nso-admin/src/main/resources/db/migration`.

- `archive/legacy-flyway-snapshots` holds historical copies retained for traceability. They are not runnable migrations and must never be combined with Flyway execution.
- `nso_platform_v1_to_v27.sql` is a one-shot, legacy empty-database convenience export. It is not part of a production upgrade path and must never be executed against a database managed by Flyway.
- New production database changes must be added as a new immutable Flyway migration after the highest deployed version. Do not edit an already applied migration.
