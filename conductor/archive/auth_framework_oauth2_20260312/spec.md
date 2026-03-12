# Specification: Authentication Framework & OAuth 2.0 Support

## Objective
Establish a extensible authentication framework for `Amateur-Postman` to support professional API testing workflows. The primary focus is implementing **OAuth 2.0**, while also refactoring existing **Basic Auth** and **Bearer Token** into the new framework.

## Context
Currently, authentication is handled manually in the UI layer by injecting headers into the `HttpRequest` object. This approach is not scalable, especially for complex flows like OAuth 2.0 which require token fetching, persistence, and refresh logic.

## Requirements

### 1. Unified Authentication Model
- Define a `Authentication` interface or sealed class in `HttpModels.kt`.
- `HttpRequest` should optionally hold an `Authentication` object.
- `HttpRequestService` (or a dedicated `AuthService`) should resolve the `Authentication` into actual HTTP headers (and potentially other request modifications) before execution.

### 2. Supported Auth Types
- **No Auth**: Default, no modifications.
- **Basic Auth**: (Refactor) Username and Password (base64 encoded).
- **Bearer Token**: (Refactor) Direct static token.
- **OAuth 2.0**:
    - **Authorization Code**: Requires opening a browser for user consent and handling a redirect URI (local server).
    - **Client Credentials**: Direct token exchange with `client_id` and `client_secret`.
    - **Password**: Exchange `username` and `password` for a token.
    - **Implicit**: (Legacy) Direct token from redirect.

### 3. OAuth 2.0 Specifics
- **Token Persistence**: Store fetched tokens (Access Token, Refresh Token, Expiry).
- **Auto-Refresh**: If a refresh token is available and the access token is expired, automatically refresh it before sending the request.
- **UI Integration**:
    - A dedicated "Auth" tab in the Request panel.
    - Auth Type selector.
    - Configuration fields based on the selected type.
    - "Get New Access Token" button for OAuth 2.0.

### 4. Integration
- **Collection/Environment Level Auth**: (Optional but recommended) Allow setting auth at the collection level, inherited by requests.
- **Scripting**: Expose auth state to `pm` objects in GraalVM JS if needed.

## Technical Approach

### Model Layer
- Add `Authentication` to `HttpModels.kt`.
- Add `OAuth2Config` and `OAuth2Token` models.

### Service Layer
- `AuthService`: Manage token fetching, storage, and refreshing.
- `HttpRequestServiceImpl`: Intercept requests to apply authentication before dispatching via OkHttp.

### UI Layer
- Refactor `PostmanToolWindowPanel`'s auth logic into a separate `AuthPanel` component.
- Implement an `OAuth2TokenDialog` or similar for managing OAuth flows.
- For "Authorization Code" flow, start a temporary local HTTP server (Netty) to capture the `code`.

## Quality Goals
- **TDD**: Every auth type must have unit tests for header generation and (where applicable) token exchange.
- **Coverage**: >80% for new auth-related services.
- **Security**: **NEVER** store secrets in plain text if possible (use IDE's `PasswordSafe`).

## Success Criteria
- [ ] Users can configure and use Basic Auth/Bearer Token via the new framework.
- [ ] Users can successfully perform an OAuth 2.0 "Client Credentials" flow and send requests.
- [ ] Users can successfully perform an OAuth 2.0 "Authorization Code" flow (browser interaction) and send requests.
- [ ] Expired tokens are automatically refreshed when a refresh token is present.
