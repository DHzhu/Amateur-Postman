# Track Plan: Authentication Framework & OAuth 2.0 Support

## Track Overview
**Goal**: Implement a unified authentication framework for HTTP requests, focusing on OAuth 2.0 flows.
**Tech Route**: `HttpModels` refactoring -> `AuthService` implementation -> `OAuth 2.0` logic (Client Credentials, etc.) -> Browser-based OAuth flows -> UI Integration -> Auto-refresh support.

## Tasks

### Phase 1: Core Models & Interface Refactoring (Infrastructure)
- [x] **Task 1: Define Authentication Models** (56bcbb4)
  - Add `Authentication` interface and implementations (`BasicAuth`, `BearerToken`, `OAuth2Auth`) to `HttpModels.kt`.
  - Update `HttpRequest` model to include an optional `Authentication` property.
  - [x] *Verification*: Unit tests for `Authentication` model and `HttpRequest` creation.
- [x] **Task 2: Refactor `HttpRequestService` to handle `Authentication`** (2134fe7)
  - Update `HttpRequestServiceImpl` to resolve `Authentication` into headers before calling OkHttp.
  - Support Basic Auth and Bearer Token as the first step (migrating from UI-only logic).
  - [x] *Verification*: Unit tests for `HttpRequestServiceImpl` with `BasicAuth` and `BearerToken`.

### Phase 2: OAuth 2.0 Direct Flows (Non-Interactive)
- [x] **Task 3: Implement `OAuth2Token` and `OAuth2Config` storage** (fc86f5f)
  - Create `OAuth2Service` to manage token storage and lifecycle.
  - [x] *Verification*: Persistence tests for OAuth2 data.
- [ ] **Task 4: Implement OAuth 2.0 "Client Credentials" flow**
  - Implement token exchange for Client Credentials.
  - [ ] *Verification*: Integration test with a mock server to exchange `client_id` for a token.
- [ ] **Task 5: Implement OAuth 2.0 "Password" flow**
  - Implement token exchange for Password flow.
  - [ ] *Verification*: Integration test for Password flow token exchange.

### Phase 3: Interactive OAuth 2.0 Flows (Browser-based)
- [ ] **Task 6: Implement "Authorization Code" flow**
  - Implement a temporary local Netty server to capture the redirect `code`.
  - Handle browser redirect logic and token exchange.
  - [ ] *Verification*: Mocked browser interaction test for Auth Code flow.
- [ ] **Task 7: Implement "Implicit" flow**
  - (Optional but recommended) Handle implicit flow token capture.
  - [ ] *Verification*: Test for Implicit flow logic.

### Phase 4: Auto-refresh & UI Integration
- [ ] **Task 8: Implement Auto-Refresh mechanism**
  - Add logic to `OAuth2Service` to refresh tokens using `refresh_token` when access token expires.
  - Integrate refresh logic into `HttpRequestServiceImpl` pre-request interceptor.
  - [ ] *Verification*: Test case where an expired token triggers a successful refresh.
- [ ] **Task 9: UI Refactoring & Enhanced Auth Panel**
  - Refactor `PostmanToolWindowPanel`'s auth section into a dedicated `AuthPanel`.
  - Implement complex UI for OAuth 2.0 configuration (scopes, auth/token URLs, etc.).
  - Add "Get New Access Token" button and token status display.
  - [ ] *Verification*: Manual smoke test for UI components.

### Phase 5: Advanced Features & Finalization
- [ ] **Task 10: Collection Level Auth Inheritance**
  - Allow auth settings at the collection level.
  - Requests should inherit auth from parent folder/collection unless overridden.
  - [ ] *Verification*: Test for auth inheritance across collection hierarchy.
- [ ] **Task 11: Final Integration & Regression Testing**
  - Full project build and test suite run.
  - [ ] *Verification*: `./gradlew check` and all tests passing.

## Verification Protocol
- **Unit Tests**: Mandatory for all new models and logic.
- **Integration Tests**: Required for token exchange flows (mocked).
- **UI Tests**: Manual verification in the IDE.
- **Coverage**: Target >80% for `AuthService` and `OAuth2Service`.

## Audit Traceability
- **Commits**: Each task completion MUST be committed via `git-commit`.
- **Sync**: State tracking via `sync-mem` after each task.
