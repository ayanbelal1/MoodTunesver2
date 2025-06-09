# Music App

A Java-based music player application that supports various audio and video formats.

## Features
- Play music files (MP3, WAV, M4A)
- Convert and play video files (MP4, AVI, MKV, etc.)
- Convert and play other audio formats (AAC, WMA, OGG, FLAC)
- Cloud storage integration
- Playlist management
- Beautiful and intuitive user interface

## Prerequisites

### Java Development Kit (JDK)
- JDK 11 or higher is required
- Download from: https://adoptium.net/

### FFmpeg
This application requires FFmpeg for media conversion. Please install it before running the application.

#### Windows
1. Download FFmpeg from: https://www.gyan.dev/ffmpeg/builds/
2. Extract the archive
3. Add the `bin` folder to your system's PATH environment variable

#### macOS
Using Homebrew:
```bash
brew install ffmpeg
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install ffmpeg
```

## Building the Application
```bash
./mvnw clean package
```

## Running the Application
```bash
./mvnw javafx:run
```

## Usage
1. Launch the application
2. Click "Open" to select a music folder or individual files
3. For video files or unsupported audio formats, the application will automatically convert them to MP3
4. Double-click a song to play
5. Use the playback controls to manage your music

## Supported Formats
### Direct Playback
- MP3
- WAV
- M4A

### Convertible Formats
#### Video
- MP4
- AVI
- MKV
- MOV
- WMV
- FLV
- WebM
- M4V

#### Audio
- AAC
- WMA
- OGG
- FLAC

## License
This project is licensed under the MIT License - see the LICENSE file for details. 