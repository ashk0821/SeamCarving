# Seam Carving Algorithm Implementation

A sophisticated implementation of the seam carving algorithm for content-aware image resizing. This project uses dynamic programming and custom graph data structures to intelligently resize images while preserving important visual content.

## Features

- **Content-Aware Resizing**: Intelligently removes low-energy seams from images to reduce dimensions
- **Real-Time Visualization**: Interactive display of seam removal process at 20 FPS
- **Multiple Operation Modes**:
  - Vertical seam removal
  - Horizontal seam removal
  - Automatic/Manual operation
  - Energy visualization
  - Seam path visualization

## Technical Implementation

- Custom graph data structure with 4-way pixel connectivity
- Dynamic programming for optimal seam detection
- Real-time energy calculation and visualization
- Memory-efficient data structures (custom Stack and Iterator implementations)
- Sentinel node pattern for border handling

## Controls

- `Space`: Pause/Resume automatic seam removal
- `v`: Remove vertical seam (manual mode)
- `h`: Remove horizontal seam (manual mode)
- `e`: Toggle energy visualization
- `s`: Toggle seam path visualization

## Algorithm Details

The implementation follows these key steps:
1. Builds a graph representation of the image with pixel connectivity
2. Calculates energy values for each pixel based on color gradients
3. Uses dynamic programming to find the lowest-energy seam
4. Provides visual feedback of the process through real-time rendering

## Dependencies

- Java Development Kit (JDK)
- javalib.impworld Library for GUI
- tester Library for unit testing

## Testing

Comprehensive test suite included with:
- Unit tests for graph operations
- Seam detection accuracy verification
- Data structure operation validation
- UI interaction testing

## Performance

- Processes 500x500 pixel images in real-time
- Maintains 20 FPS during seam removal operations
- Memory-efficient implementation for large image processing
