# Technical Documentation

## Architecture Overview

### Core Components

1. **User Interface (UI) Layer**
   - JavaFX-based UI components
   - FXML for layout definitions
   - CSS styling for theme customization
   - Controllers for handling user interactions

2. **Service Layer**
   - `MusicPlayerService`: Core playback functionality
   - `SpotifyService`: Spotify API integration
   - `YouTubeService`: YouTube integration
   - `CloudStorageService`: Cloud storage operations

3. **Data Access Layer**
   - `SongDAO`: Music file management
   - `PlaylistDAO`: Playlist operations
   - `UserDAO`: User management
   - `SocialDAO`: Social features

4. **Model Layer**
   - `Song`: Music file metadata
   - `Playlist`: Collection of songs
   - `User`: User profile data
   - `SocialConnection`: User relationships

### Database Schema

```sql
Users Table:
- id (INT, PRIMARY KEY)
- username (VARCHAR)
- email (VARCHAR)
- password_hash (VARCHAR)
- created_at (TIMESTAMP)

Songs Table:
- id (INT, PRIMARY KEY)
- title (VARCHAR)
- artist (VARCHAR)
- album (VARCHAR)
- duration (INT)
- file_path (VARCHAR)
- created_at (TIMESTAMP)

Playlists Table:
- id (INT, PRIMARY KEY)
- user_id (INT, FOREIGN KEY)
- name (VARCHAR)
- description (TEXT)
- cover_art (VARCHAR)
- created_at (TIMESTAMP)

Playlist_Songs Table:
- playlist_id (INT, FOREIGN KEY)
- song_id (INT, FOREIGN KEY)
- position (INT)

Shared_Playlists Table:
- playlist_id (INT, FOREIGN KEY)
- shared_with_user_id (INT, FOREIGN KEY)
- shared_at (TIMESTAMP)
```

## Development Setup

### Prerequisites
- JDK 17 or later
- Maven 3.8+
- IDE with JavaFX support (IntelliJ IDEA recommended)
- Git

### Build Process
1. Clone the repository
```bash
git clone https://github.com/yourusername/moodtunes.git
```

2. Install dependencies
```bash
mvn clean install
```

3. Run the application
```bash
mvn javafx:run
```

### Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── musicapp/
│   │           ├── controllers/    # UI Controllers
│   │           ├── database/       # Data Access Objects
│   │           ├── models/         # Data Models
│   │           ├── services/       # Business Logic
│   │           └── utils/          # Utility Classes
│   └── resources/
│       ├── fxml/                   # UI Layout Files
│       ├── styles/                 # CSS Files
│       └── images/                 # Image Assets
└── test/                          # Test Classes
```

## Key Features Implementation

### Music Playback
- Uses JavaFX MediaPlayer for audio playback
- Supports MP3, WAV, and M4A formats
- Implements queue management and shuffle/repeat modes

### Playlist Management
- CRUD operations for playlists
- Drag-and-drop song reordering
- Playlist sharing functionality

### Social Features
- User following system
- Playlist sharing
- Activity feed

### Cloud Integration
- Support for multiple cloud storage providers
- Automatic sync of playlists
- Offline mode support

## Testing

### Unit Tests
- JUnit 5 for unit testing
- Mockito for mocking dependencies
- TestFX for UI testing

### Integration Tests
- Database integration tests
- API integration tests
- End-to-end tests

## Performance Considerations

### Memory Management
- Lazy loading of playlists
- Image caching system
- Memory-efficient media player

### Database Optimization
- Indexed queries
- Connection pooling
- Prepared statements

### UI Performance
- Virtual scrolling for large lists
- Background loading of images
- Efficient event handling

## Security

### Authentication
- Bcrypt password hashing
- JWT for session management
- OAuth2 for third-party integrations

### Data Protection
- Encrypted storage of sensitive data
- Secure file handling
- Input validation and sanitization

## Error Handling

### Exception Hierarchy
- Custom exceptions for business logic
- UI error feedback system
- Logging framework integration

### Recovery Mechanisms
- Auto-save feature
- Crash recovery
- Data backup system

## Deployment

### Packaging
```bash
mvn clean package
```

### Distribution
- Native installers for different platforms
- Auto-update mechanism
- Dependency bundling

## Maintenance

### Logging
- SLF4J with Logback
- Rotating log files
- Different log levels for development/production

### Monitoring
- Performance metrics collection
- Error tracking
- Usage analytics

## Future Improvements

1. **Technical Debt**
   - Code refactoring opportunities
   - Performance optimization areas
   - Test coverage improvements

2. **Feature Roadmap**
   - Mobile companion app
   - Voice commands
   - AI-powered recommendations

3. **Infrastructure**
   - Cloud deployment options
   - Scalability improvements
   - Caching strategy enhancements 