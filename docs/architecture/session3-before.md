# Architecture Snapshot Before Session 3

This diagram captures the structure of the React web application together with new Android and cloud backend modules present before merging updates from `master`.

```mermaid
graph TD
    index.tsx --> App[App]
    App --> TopBar
    App --> Content
    App --> ExtraModeControls
    App --> ExampleImages
    App --> SideControls
    App --> DetectTypeSelector
    App --> Prompt
    SideControls --> ScreenshareButton
    ExtraModeControls --> Palette

    subgraph AndroidApp
        MainActivity --> CameraProcessor
        CameraProcessor --> RealtimeObjectDetector
        RealtimeObjectDetector --> BoundingBoxView
    end

    subgraph CloudBackend
        SearchAPI.py --> ImageProcessor.py
        ImageProcessor.py --> ObjectDatabase.py
    end
```
