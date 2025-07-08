# Changelog

## [Unreleased] - Repository Refactor for Codenames Game

### Added
- **BaseRepository**: Introduced a generic `BaseRepository` class in `app/src/main/java/com/example/gamehub/repository/BaseRepository.kt` to centralize Firestore CRUD and real-time operations.
- **CodenamesRepository**: Created `CodenamesRepository` in `app/src/main/java/com/example/gamehub/repository/CodenamesRepository.kt`, extending `BaseRepository` for Codenames-specific data access.

### Changed
- **CodenamesService**: Refactored to use `CodenamesRepository` for all Firestore operations instead of direct Firestore access.
- **CodenamesScreen**: Refactored to use `CodenamesRepository` for real-time updates and data writes, removing direct Firestore calls.

### Benefits
- Centralized and reusable data access logic for Firestore.
- Improved separation of concerns and maintainability.
- Easier to test and swap backend implementations in the future.

--- 