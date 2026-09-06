# Page Dependency Trees

No authored frontend exists at initialization time. Planned dependency trees for the new target:

## `/login`

- `project-implementation/apps/web/src/pages/LoginPage.vue`
  - `project-implementation/apps/web/src/components/AuthShell.vue`
  - `project-implementation/apps/web/src/stores/session.ts`
  - `project-implementation/apps/web/src/services/http.ts`

## `/register`

- `project-implementation/apps/web/src/pages/RegisterPage.vue`
  - `project-implementation/apps/web/src/components/AuthShell.vue`
  - `project-implementation/apps/web/src/services/http.ts`

## `/account/profile`

- `project-implementation/apps/web/src/pages/ProfilePage.vue`
  - `project-implementation/apps/web/src/components/SiteShell.vue`
  - `project-implementation/apps/web/src/stores/session.ts`
  - `project-implementation/apps/web/src/services/http.ts`

## `/admin/users`

- `project-implementation/apps/web/src/pages/AdminUsersPage.vue`
  - `project-implementation/apps/web/src/components/SiteShell.vue`
  - `project-implementation/apps/web/src/services/http.ts`
  - `project-implementation/apps/web/src/components/StatusBadge.vue`
