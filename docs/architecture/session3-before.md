# Architecture Snapshot Before Session 3

The project structure before merging branches remains unchanged from the previous session. The application includes React components, placeholder Android app files, and cloud backend modules.

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
    subgraph src/android_app
        BoundingBoxView.java
    end
    subgraph src/cloud_backend
        ImageProcessor.py
        ObjectDatabase.py
        SearchAPI.py
    end
```
