# CafeShop — AI-Powered Café Ordering Platform

A full-stack Android ordering platform with an AI order assistant, combining a native mobile app with a standalone backend service.

## Why this project

Most student mobile apps stop at "talks to Firebase and looks nice." CafeShop was built to go further: MVVM architecture, a real backend and an AI assistant. The goal was to build something that reflects how a full-stack, AI-integrated product actually gets put together.

## What it does

CafeShop supports two roles, each with its own experience:

- **Customer** — browses the menu, builds a cart, checks out, and watches order status update live as staff prepare it. Can ask the AI assistant menu questions, get recommendations, customise an order, or check on an existing one.
- **Admin** — manages the menu, moves orders through their lifecycle, replies to reviews and sends notifications via a dashboard.

Order status changes are pushed live to the customer through Firestore real-time listeners, no manual refresh needed. The AI assistant is backed by a Node.js service that authenticates every request with the user's Firebase ID token, pulls real menu/order context, and calls the Claude API.

## Tech stack

| Layer | Technology |
|---|---|
| Mobile | Kotlin, Android (MVVM, StateFlow, Flow), Material Components 3 |
| Backend | Node.js, TypeScript, Express, REST API |
| AI | API (Anthropic) |
| Auth & database | Firebase Authentication, Cloud Firestore (real-time listeners) |
| Networking | Retrofit 2, OkHttp 4, Gson |
| Testing | JUnit 4, kotlinx-coroutines-test, MockK, OkHttp MockWebServer |
| Tooling | ktlint, GitHub Actions CI |

## Features

- Role-based experience for customers and admins
- Real-time order tracking via Firestore snapshot listeners, cleaned up automatically per screen lifecycle
- AI assistant: menu Q&A, recommendations, order customisation, order status lookup
- Firebase-token-authenticated REST API between the app and backend, with automatic auth-refresh-and-retry
- Atomic multi-document writes (order + payment, status change + notification) for data consistency
- Automated test suite covering repositories, ViewModels and network failure handling
- CI (GitHub Actions) running lint, build and tests on every push

## Getting started

### Prerequisites

- Android Studio (recent stable release), JDK 17, Android SDK 36
- A Firebase project (Authentication + Firestore enabled)
- Node.js (for the backend)
- An Anthropic API key (for the AI assistant)

### Backend setup

```bash
git clone https://github.com/kowthar04/cafeshop.git
cd cafeshop/backend
cp .env.example .env
# add ANTHROPIC_API_KEY and Firebase service-account credentials to .env
npm install
npm run dev
```

### Android setup

1. Open the repository root in Android Studio.
2. Add an Android app in your Firebase project (package `com.example.cafeshopassignment`), download `google-services.json`, and save it to `app/google-services.json` (git-ignored — never commit it).
3. Enable Email/Password sign-in and create the Firestore collections: `users`, `menuItems`, `orders`, `payments`, `reviews`, `notifications`.
4. In `local.properties`, point the app at the backend:
   ```properties
   BACKEND_BASE_URL=http://10.0.2.2:3000/
   ```
5. Run the app from Android Studio, or:
   ```bash
   ./gradlew assembleDebug
   ./gradlew testDebugUnitTest
   ```

## Demo

No seeded demo accounts — sign up in-app, then set `role: "admin"` on that user's Firestore document to unlock the admin dashboard.

| | |
|---|---|
| Test card | `4242 4242 4242 4242`, exp `12/29`, CVC `123` |
| Promo code | `THIRSTY` |

## What I'd build next

- Firestore security rules to enforce role checks server-side (currently UI-gated only)
- A real payment provider instead of simulated checkout
- Unread notification badges and mark-as-read state
- Swap the hand-written ServiceLocator for Hilt as the app grows
- A working analytics view on the admin dashboard

## Author

Kowthar Abdiqadir

https://www.linkedin.com/in/kowthar-abdiqadir/ · kowthar.abdiqadir@outlook.com
