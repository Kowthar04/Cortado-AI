# CafeShop Backend

A standalone Node.js + TypeScript + Express backend for the CafeShop Android app. It is the
portfolio "centerpiece feature" of the revamp: an AI ordering assistant (Claude API) grounded in
live Firestore data, a small JSON API for the app, and a minimal admin dashboard.

## Architecture

```
backend/
  src/
    index.ts          entrypoint (starts the HTTP server)
    app.ts             builds the Express app (routes, middleware, static admin page)
    config/            typed env-var config, fails fast on missing required vars in production
    middleware/
      auth.ts          Firebase ID token verification + Firestore role check
      errors.ts        ApiError class, consistent error JSON, async handler wrapper
    routes/
      chat.ts          POST /api/chat
      menu.ts          GET  /api/menu
      orders.ts        GET  /api/orders, GET /api/orders/:id
    services/
      firebase.ts      firebase-admin app/Firestore/Auth initialization
      menuService.ts   Firestore reads for menuItems
      orderService.ts  Firestore reads for orders
      promptBuilder.ts pure prompt-construction functions (unit tested, no I/O)
      claudeService.ts wraps @anthropic-ai/sdk calls
    types/models.ts    TypeScript types mirroring the Kotlin models
    public/            static admin dashboard (HTML/CSS/vanilla JS), served at /admin
  tests/               Jest unit tests (mocked Firestore/Anthropic clients)
```

Firestore collections used (matching the Android app's models under
`app/src/main/java/com/example/cafeshopassignment/models/`):

- `menuItems`: `name`, `category`, `price`, `imageUrl`, `availability`
- `orders`: `userId`, `customerName`, `items`, `subtotal`, `serviceFee`, `discount`,
  `totalPrice`, `paymentMethod`, `status`, `createdAt`, `paymentStatus`
- `users`: `firstname`, `surname`, `role` (`"admin"` or `"customer"`) — keyed by Firebase Auth uid
- `notifications`, `reviews`: present in the schema, not yet read by this backend

Firebase project id: `cafeshopassignment`.

## Why Node/Express instead of a Firebase Cloud Function

This backend is deliberately built as an independently deployable Express service rather than a
Cloud Function, because the point of this portfolio piece is to demonstrate backend API design,
not just "glue code" behind Firestore triggers. A standalone service: (1) can be deployed to any
host (Render, Railway, Fly.io, a VPS) independent of the Firebase project, showing infra
portability; (2) has full control over routing, middleware, and error-handling conventions, which
is what interviewers actually look at in a full-stack/AI engineer portfolio; (3) is easier to load
test, version, and add non-Firebase integrations to (here, the Anthropic API) without fighting
Cloud Functions' request/response model and cold-start constraints; and (4) mirrors how a real
product team would split a "core CRUD backed by Firestore" concern from a "growing AI feature"
concern into a dedicated service, rather than cramming both into triggers.

## Setup

1. **Install dependencies**

   ```bash
   cd backend
   npm install
   ```

2. **Get a Firebase service account key** (for `firebase-admin` to read Firestore):
   - Firebase Console → Project settings → Service accounts (for the `cafeshopassignment`
     project) → "Generate new private key". This downloads a JSON file.
   - **Never commit this file.** Either:
     - Set `FIREBASE_SERVICE_ACCOUNT_JSON` to the file's contents as a single-line JSON string, or
     - Set `GOOGLE_APPLICATION_CREDENTIALS` to the absolute path of the downloaded file.

3. **Get an Anthropic API key**: create one at https://console.anthropic.com/settings/keys and
   set `ANTHROPIC_API_KEY`.

4. **Copy the env file and fill in values**

   ```bash
   cp .env.example .env
   ```

5. **Run locally**

   ```bash
   npm run dev      # ts-node/tsx watch mode
   # or
   npm run build && npm start
   ```

   The server listens on `PORT` (default `3000`). Health check: `GET /health`.

### Environment variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `PORT` | no | `3000` | HTTP port |
| `NODE_ENV` | no | `development` | `production` enables fail-fast on missing required vars |
| `FIREBASE_PROJECT_ID` | no | `cafeshopassignment` | Firebase project id |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | one of these two | — | Full service account JSON as a string |
| `GOOGLE_APPLICATION_CREDENTIALS` | one of these two | — | Path to a service account JSON file |
| `ANTHROPIC_API_KEY` | yes (production) | — | Claude API key |
| `CLAUDE_MODEL` | no | `claude-sonnet-5` | Claude model id used for `/api/chat` |
| `CHAT_ORDER_HISTORY_LIMIT` | no | `5` | Max past orders fed into the chat context |

## API Reference

All responses are JSON. Errors use a consistent shape:

```json
{ "error": { "message": "Human readable message", "code": "SOME_CODE" } }
```

Codes used: `BAD_REQUEST` (400), `UNAUTHORIZED` (401), `FORBIDDEN` (403), `NOT_FOUND` (404),
`INTERNAL_ERROR` (500).

Authentication: protected endpoints require `Authorization: Bearer <Firebase ID token>`. The
Android app already uses Firebase Auth, so it can send `FirebaseAuth.getInstance().currentUser
.getIdToken(false)`'s resulting token as-is. Admin-only endpoints additionally require the
caller's `users/{uid}.role` Firestore field to equal `"admin"`.

### `GET /health`

No auth. Liveness check.

```json
{ "status": "ok" }
```

### `POST /api/chat`

Auth: any authenticated user.

Request body:

```json
{
  "message": "What coffees do you have under $5?",
  "conversationHistory": [
    { "role": "user", "content": "Hi!" },
    { "role": "assistant", "content": "Hello, welcome to CafeShop!" }
  ],
  "userId": "firebase-uid-optional",
  "orderId": "order-doc-id-optional"
}
```

- `conversationHistory` is optional prior turns (role `user`/`assistant`).
- `userId`, when provided, is used to fetch the caller's last `CHAT_ORDER_HISTORY_LIMIT` orders
  from Firestore so Claude can make personalized recommendations.
- `orderId`, when provided, is used to fetch that order and let Claude answer status questions
  about it in natural language.

The handler always fetches the current menu from Firestore and builds a system prompt grounded in
real menu items (name/category/price/availability) so answers never invent items or prices.

Response:

```json
{ "reply": "We have a Latte for $4.50 and a Cappuccino for $4.75, both available right now!" }
```

### `GET /api/menu`

Auth: any authenticated user.

Response:

```json
{
  "menuItems": [
    {
      "id": "abc123",
      "name": "Latte",
      "category": "Coffee",
      "price": 4.5,
      "imageUrl": "https://...",
      "availability": true
    }
  ]
}
```

### `GET /api/orders`

Auth: admin only.

Response:

```json
{
  "orders": [
    {
      "id": "order-1",
      "userId": "uid-1",
      "customerName": "Jane Doe",
      "items": [{ "name": "Latte", "quantity": 1 }],
      "subtotal": 4.5,
      "serviceFee": 0.5,
      "discount": 0,
      "totalPrice": 5.0,
      "status": "Preparing",
      "paymentMethod": "Card",
      "paymentStatus": "Paid",
      "createdAt": { "seconds": 1732500000, "nanoseconds": 0 }
    }
  ]
}
```

### `GET /api/orders/:id`

Auth: any authenticated user.

Response: `{ "order": { ...same shape as above } }`, or `404 NOT_FOUND` if the order doesn't exist.

## Admin dashboard

`GET /admin` serves a small static dashboard (`src/public/`) that polls `GET /api/orders` every 5
seconds and shows order id, customer, status, payment status, items, and total.

### Admin dashboard auth demo flow

The dashboard is a portfolio demo, not a production login system: it has a text box where you
paste a Firebase ID token, which it stores in `sessionStorage` and sends as
`Authorization: Bearer <token>` on every poll. To get a token for an admin account for a demo:

1. Make sure the account's Firestore doc at `users/{uid}` has `role: "admin"`.
2. Get an ID token any of these ways:
   - **Firebase JS SDK console snippet** (fastest): in a browser console on any page with the
     Firebase app initialized (or a scratch HTML file with the Firebase config), run:
     ```js
     const cred = await firebase.auth().signInWithEmailAndPassword(email, password);
     const token = await cred.user.getIdToken();
     console.log(token);
     ```
   - **Firebase Auth REST API**:
     ```bash
     curl -X POST \
       "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=<WEB_API_KEY>" \
       -H "Content-Type: application/json" \
       -d '{"email":"admin@example.com","password":"...","returnSecureToken":true}'
     ```
     The response's `idToken` field is what you paste in.
   - **From the Android app itself**, temporarily log `FirebaseAuth.getInstance().currentUser
     ?.getIdToken(false)?.result?.token` after an admin logs in.
3. Paste the token into the dashboard's login box and click "Connect". Tokens expire after about
   an hour — sign in again to get a fresh one.

This is intentionally simple: it is meant to look credible for a portfolio demo, not to be a
production-grade admin console.

## Tests

```bash
npm test
```

Jest + ts-jest, with `jest.mock` used to fully mock `firebase-admin` and `@anthropic-ai/sdk` calls
— no network access during tests. Coverage:

- `tests/promptBuilder.test.ts`: menu formatting, order-history formatting, order-status
  formatting, full system prompt assembly, and conversation-history-to-messages assembly.
- `tests/auth.test.ts`: `requireAuth` (missing header, malformed header, invalid token, valid
  token) and `requireAdmin` (unauthenticated, non-admin, admin).
- `tests/routes.test.ts`: `/api/menu`, `/api/orders`, `/api/orders/:id`, and `/api/chat` with
  mocked Firestore/Anthropic services, covering auth failures, not-found, and success paths.

## Lint & format

```bash
npm run lint
npm run format
```
