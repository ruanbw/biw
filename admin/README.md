# biw admin

The admin UI for this repository's `biw` backend (users, roles, permissions). Vue 3 +
TypeScript + Naive UI + UnoCSS, based on [Soybean Admin](https://github.com/soybeanjs/soybean-admin)
(MIT).

## Development

The backend must be running first (see the repository `README.md`). Then:

```bash
cd admin
pnpm install
pnpm dev        # http://localhost:9527
```

`pnpm dev` uses `.env.test`, which points the Vite proxy at `http://localhost:8080`. If the
backend listens elsewhere, change `VITE_SERVICE_BASE_URL` in `.env.test` — the browser only
talks to `/proxy-default`, so no CORS rule is involved.

Build a bundle with `pnpm build` (`.env.prod`, same-origin API by default).

## How this differs from the template

The backend is the source of truth for these rules, and the template assumed otherwise, so the
following were rewritten rather than configured:

| Area        | biw                                                                              | Where                               |
| ----------- | -------------------------------------------------------------------------------- | ----------------------------------- |
| Envelope    | `{ code, message, data }`, success is `code === 0`                               | `src/service/request/index.ts`      |
| Auth header | token travels in the header named by the login response (`satoken`), no `Bearer` | `src/service/request/shared.ts`     |
| Sessions    | no refresh token; a `401` clears the session and routes to login                 | `src/service/request/index.ts`      |
| Permissions | `GET /auth/permissions` returns flat codes, `["*"]` for an admin                 | `src/store/modules/auth/index.ts`   |
| Menus       | filtered by `meta.permissions`, declared in `build/plugins/router.ts`            | `src/store/modules/route/shared.ts` |
| Pagination  | request `page`/`size`, response `records`/`total`/`page`/`size`/`pages`          | `src/hooks/business/pagination.ts`  |
| Login       | username/password only — no SMS code, registration, or password reset            | `src/views/_builtin/login/`         |

## Routes

`src/views/**/index.vue` is scanned by `@elegant-router/vue` on every dev-server start, which
regenerates `src/router/elegant/*` and `src/typings/elegant-router.d.ts`. Add a page by creating
the file, then supply its menu icon, order and `permissions` in the `routeMetaRecord` of
`build/plugins/router.ts` — edits to the generated files are lost.

## Accounts

There is no public sign-up: an administrator creates accounts, and only an administrator can
reset a password. See the seed SQL comment in `sql/init.sql` for creating the first one.
