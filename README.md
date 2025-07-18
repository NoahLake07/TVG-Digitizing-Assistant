# Tape Digitizing Assistant
TVG Digitizing Assistant is a Java-based desktop application designed to assist with the process of digitizing media and managing data storage devices. The application provides a user-friendly interface for managing and tracking the conversion of various media types and data recovery projects.

## Features

### Core Functionality
- **Project Management**: Create and manage projects for different clients. Each project can contain multiple conversions, and each conversion represents a single digitization or data recovery task.
- **Conversion Management**: Within each project, users can create and manage conversions with detailed tracking including name, notes, technician notes, type, date/time of conversion, and status.
- **File Management**: Link multiple files to each conversion, with support for bulk renaming, relinking, and file organization.
- **Data Persistence**: Save project state to JSON files for easy backup and sharing.

### Version 1.5 New Features
- **Data-Only Conversions**: Mark conversions as pure data storage (SD cards, hard drives, etc.) with special handling in export operations.
- **Misc Data Storage Format**: New format type specifically for data storage devices.
- **Technician Notes**: Internal logging field for technician-specific notes and observations.
- **Silent Saves**: Removed confirmation dialogs for smoother workflow.
- **Auto-Sort**: Conversions automatically sorted by name when projects are saved.
- **File Map Visualization**: "Show File Map" button displays linked files in a hierarchical tree view with:
  - Full file path structure visualization
  - File size and properties display
  - Right-click context menu for file operations
  - Missing file detection and indication
- **Professional Export System**: "Write Conversions To Destination" feature for client delivery with:
  - Destination folder selection
  - Option to create separate folders per conversion
  - File renaming options (with special handling for data-only conversions)
  - Preview functionality before export
  - Comprehensive logging and error handling
  - Support for subdirectory inclusion

### Advanced Features
- **Bulk Operations**: Rename all files in a project or conversion with various options
- **File Relinking**: Easily relink files that have been moved or renamed
- **Trimmed File Support**: Automatic detection and relinking of trimmed media files
- **Search and Filter**: Find conversions quickly with real-time search
- **Status Tracking**: Track conversion progress from not started to completed
- **Media Statistics**: View detailed statistics about project files and durations

## Supported Media Types
- VHS, VHS-C, 8mm, Betamax, MiniDV
- CD/DVD, Type II
- Misc Data Storage (SD cards, hard drives, etc.)

## Application Screenshots

![image](https://github.com/NoahLake07/Tape-Digitizing-Assistant/assets/98616672/010f61b9-0621-46b1-a73e-adfc59f22ee8)

Home - Project select page

![image](https://github.com/NoahLake07/Tape-Digitizing-Assistant/assets/98616672/208908e1-9145-4bb8-9862-541c5ba35101)

The conversion details & management page

## Framework
- **Java**: The application is written entirely in Java, making it platform-independent.
- **Swing**: The user interface is built using the Swing toolkit, providing a native look and feel across platforms.
- **Gson**: JSON serialization for project file format.

## Downloads
**Latest Version**: 1.5 - Professional project delivery system with data-only conversion support

Previous Versions:
- Version 1.4: Enhanced file management and renaming features
- Version 1.3: Improved UI and project organization
- Version 1.2: Added file linking and basic renaming
- Version 1.1: Core project management features
- Version 1.0: Initial release

## Usage

### Creating a New Project
1. Launch the application
2. Click "New Project" and enter a project name
3. Add conversions using the "+ New Conversion" button

### Managing Conversions
- Set conversion type (VHS, data storage, etc.)
- Add conversion notes and technician notes
- Mark as data-only if handling storage devices
- Link relevant files to the conversion
- Track progress with status updates

### Exporting for Client Delivery
1. Use "Write Conversions To Destination" from the project menu
2. Select destination folder
3. Choose export options (folders per conversion, file renaming, etc.)
4. Preview the export structure
5. Execute the export with full logging

### File Management
- Right-click files for individual operations
- Use bulk rename options for entire conversions or projects
- Relink files that have been moved or renamed
- Search for and link trimmed versions of media files

## Development
This application is actively developed and welcomes contributions! The codebase is structured for easy extension and modification.

Please note that this application is designed for professional digitization workflows and includes comprehensive logging and error handling for production use.
