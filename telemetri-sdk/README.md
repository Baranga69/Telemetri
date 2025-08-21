# Telemetri SDK

A production-ready Android SDK for collecting, storing, and syncing telematics data from onboard device sensors. Built with MVVM architecture, Room, Hilt, and WorkManager for robust, efficient, and testable code.

## Architecture
- **MVVM**: Clean separation of concerns for maintainability and testability.
- **Room Database**: Local storage of telemetry events before syncing.
- **Repository Pattern**: Abstracts data access and provides a clean API.
- **Hilt DI**: Automatic dependency injection for all major components.
- **WorkManager**: Reliable background sync of unsynced events to your backend.

## Key Components
- `TelemetryEventEntity`: Room entity for telemetry events.
- `TelemetryEventRepository`: Handles CRUD and sync logic.
- `TelemetrySyncWorker`: Background worker for syncing events.
- `BackgroundSyncService`: Schedules periodic syncs.
- `TelemetryApi`: Interface for backend communication.

## Setup
1. **Add SDK to your project**
2. **Annotate your Application class with `@HiltAndroidApp`**
3. **Call `BackgroundSyncService.scheduleSync(context)` in your Application or main entry point**
4. **Implement `TelemetryApi` for your backend**

## Usage Example
```kotlin
val repository: TelemetryEventRepository = ... // Injected by Hilt
val event = TelemetryEvent(...)
repository.addEvent(event)
```

## Testing
- Unit and integration tests provided for repository, DAO, and worker.

## Optimization
- Efficient queries and minimal resource usage.
- Background sync scheduled every 15 minutes (configurable).
- Robust error handling and logging throughout the SDK.

## License
MIT

