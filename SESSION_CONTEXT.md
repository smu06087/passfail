# Project Context History: Battle & RogueMap

## [2026-05-08] Battle Mode Core Loop & Transition System
**Status:** Completed Battle Mode Core Loop & Synchronized Transition

### 1. Core Architecture State
#### Backend (Spring Boot)
- **Advanced State Management**: Introduced `STARTING` status in `BattleRoomStatus` and `DISCONNECTED` in `BattleParticipantStatus` to handle MPA page transitions safely.
- **Synchronized Game Start**: `BattleRoomService` now handles a 10-second timeout for entry confirmation, force-transitioning to `IN_PROGRESS` if necessary.
- **Deterministic Problem Selection**: `ProblemService.getBattleProblem` uses `roomId`, `floor`, and `seed` to ensure all participants face the same challenge, with difficulty scaling by floor (EASY -> MEDIUM -> HARD).
- **Security & Permissions**: Created `BattleCompetitionPermissionEntity` to restrict Tournament Mode creation to authorized users.

#### Frontend (Thymeleaf + JS)
- **RogueMap (Slay the Spire style)**:
    - Implemented `playMode` vs `viewMode`.
    - Added floating arrow guidance for new players.
    - Persistence via `sessionStorage` (visited nodes, paths, current position) per `roomId`.
- **Editor Integration**: `editor.html` now detects `mode=battle`, transforming the 'Exit' button and auto-redirecting back to the map upon successful submission with progress tracking.
- **Real-time Sync**: `WSManager` updated with `sendConfirm` to notify server of successful page load/entry.

### 2. Recent Changes & Fixes
- **SQL Bug (ORA-01747)**: Resolved Oracle reserved word conflict by renaming the `mode` column to `battle_mode` (field name `battleMode`).
- **Race Condition Fix**: Ensured room status is set to `STARTING` *before* broadcasting `GAME_START` to prevent unintended `leaveRoom` calls during page transitions.
- **Standardization**: Standardized all BattleRoom fields to camelCase (`endAt`, `createdAt`, `startAt`).

### 3. Resumption Guide
1. **Testing**: Verify the 10-second force-start by closing a tab during the "STARTING" phase.
2. **Expansion**: Implement "Event" nodes in RogueMap (currently treated as standard Puzzles).
3. **UI/UX**: Add real-time visual indicators on the map showing where other participants are currently located.
4. **Data Integrity**: Ensure `sessionStorage` is cleared or updated correctly when a user joins a *new* room of the same ID after a long period.

---

## [2026-05-07] Battle Room & Lobby Refinement
**Status:** Completed Phase 1-5 of Room/Lobby Enhancements

### 1. Core Architecture State
#### Backend (Spring Boot)
- **Service Layer Integration**: All real-time logic (join, leave, status) moved from `BattleRoomController` to `BattleRoomService` for consistency.
- **WebSocket & SSE Hybrid**: WebSocket handles bi-directional lobby actions, while SSE provides a fallback/notification channel (managed via `SseService` and `SSEManager.js`).
- **Slot System**: Participants are assigned a fixed `slotIndex` (0-7) to maintain consistent UI positioning across all clients.
- **Entity & DTO**: `BattleRoomEntity` and `BattleRoomDTO` synchronized with all new fields (`maxParticipants`, `difficulty`, `mode`, etc.).

#### Frontend (Thymeleaf + JS)
- **Grid Layout**: Dynamic 8-slot grid in `room.html` using Lucide icons for status (Host, Ready, Empty).
- **Navigation Safety**: `isNavigating` and `dataset.leaving` flags prevent "ghost" leave events during refreshes or valid redirects.
- **Lobby UI**: Enhanced card-based room list with search and "Enterable Only" filtering.

### 2. Recent Changes & Fixes
- **Fix**: Resolved "User Disappearing" on refresh by adding a `pagehide` vs `beforeunload` distinction and a short delay in the server's disconnect listener.
- **Feature**: Added "Tournament/Competition" mode UI toggle and date-time inputs in room creation.
- **Feature**: Integrated "Quick Match" algorithm (score-based proximity) in `BattleRoomService`.

### 3. Resumption Guide
1. **Refresh Logic**: Check `room.html` line 440+ for the `handleExit` and `isNavigating` interaction if users still drop unexpectedly.
2. **Slot Indexes**: Any UI misalignment should be traced to `BattleParticipantEntity.slotIndex` assignment in `BattleRoomService.joinRoom`.
3. **Current Hardcoding**: `hostId` is currently defaulted to authenticated user; Fallback logic in `BattleRoomController.postCreate`.

---
*End of Context History*
