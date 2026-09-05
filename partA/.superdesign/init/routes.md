# Routes

The captured static site supplies visual references only. Role A will introduce these Vue Router routes:

| URL | Page | Layout | Capability |
| --- | --- | --- | --- |
| `/login` | `LoginPage.vue` | public | public-only |
| `/register` | `RegisterPage.vue` | public | public-only |
| `/account/profile` | `ProfilePage.vue` | account | authenticated user |
| `/admin/users` | `AdminUsersPage.vue` | admin | `ADMIN_USERS_READ` |
| `/admin/users/:id` | `AdminUserDetailPage.vue` | admin | `ADMIN_USERS_READ` |

The feature exports `identityRoutes` with `routeId`, `path`, `layout`, `requiredCapability`, and lazy `loadPage` metadata for integration with member E's shell.
