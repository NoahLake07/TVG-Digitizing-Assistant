# Digitizing Assistant v1.6.1

A Java-based application for managing video digitization projects with enhanced damage tracking and export capabilities.

## New Features in v1.6.1

### Enhanced Damage Management
- **Damage Status Tracking**: New status options for tracking tape damage:
  - `DAMAGE_FIXED`: Mark tapes that were damaged but have been repaired
  - `DAMAGE_IRREVERSIBLE`: Mark tapes with permanent damage that cannot be recovered
- **Damage Event History**: Record detailed damage events with timestamps and technician notes
- **Visual Status Indicators**: Color-coded status display for easy identification

### Export Capabilities
- **Digitizing Sheet Export**: Generate comprehensive reports in multiple formats:
  - **Client Version**: Clean, minimal information suitable for client delivery
  - **Archival Version**: Detailed information including damage history and technician notes
  - **Technician Version**: Complete technical details including file paths and workflow logs
- **File Map Export**: Generate comprehensive file maps with:
  - File paths and associated tape IDs
  - Conversion statuses and metadata
  - Optional MD5 checksums for file integrity verification
- **Multiple Export Formats**: Support for CSV and JSON formats

### Bug Fixes
- **Timestamp Preservation**: Fixed issue where conversion dates/times were reset when loading projects
- **Enhanced Data Persistence**: Improved handling of damage history and technician notes

## Features

### Project Management
- Create and manage digitization projects
- Track multiple video formats (VHS, Betamax, etc.)
- Organize conversions with notes and status tracking
- Link media files to conversions

### Conversion Tracking
- Status management (Not Started, In Progress, Basic Editing, Completed, Damaged, etc.)
- Date and time tracking for conversions
- Duration tracking for completed conversions
- Technician notes and damage history

### File Management
- Link multiple files to conversions
- Support for various video file formats
- File validation and integrity checking
- Bulk file operations and relinking

### Export and Reporting
- Export project data as JSON
- Generate digitizing sheets for clients and archives
- Create comprehensive file maps
- Media statistics and analysis

## Installation

1. Ensure you have Java 11 or higher installed
2. Download the latest release
3. Run the application using your preferred Java IDE or command line

## Usage

### Creating a Project
1. Launch the application
2. Click "New Project" and enter a project name
3. Add conversions for each tape you want to digitize

### Managing Conversions
1. Select a conversion from the list
2. Set the format type (VHS, Betamax, etc.)
3. Add notes and technician information
4. Link media files as they become available
5. Update status as work progresses

### Damage Management
1. Use the damage management panel to record damage events
2. Mark tapes as damaged, fixed, or irreversible
3. Add detailed descriptions and technician notes
4. Track the complete damage history

### Exporting Data
1. Use the project menu to access export options
2. Choose export type and format
3. Select output location
4. Generate reports for clients, archives, or technical analysis

## File Structure

```
DigitizingAssistant/
├── src/
│   └── com/thevideogoat/digitizingassistant/
│       ├── data/           # Data models and persistence
│       ├── ui/             # User interface components
│       └── util/           # Utility classes and export functions
├── res/                    # Application resources
└── target/                 # Compiled classes
```

## Dependencies

- Java 11+
- Gson (for JSON handling)
- VLCJ (for video processing)

## Contributing

This project is maintained by The Video Goat. For issues or feature requests, please contact the development team.

## License

Proprietary software - All rights reserved.
