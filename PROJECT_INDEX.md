# Project Index: find-my-shtuff

**Generated**: 2025-02-04
**Project**: Spatial Understanding-inspired Gaussian Splat-assisted Object Locator
**Description**: Unlock Gemini's vision! Detect objects in images or screenshares with interactive 2D/3D bounding boxes and points.

---

## 📁 Project Structure

```
find-my-shtuff/
├── src/
│   └── cloud_backend/       # Python cloud backend (placeholders)
│       ├── ImageProcessor.py
│       ├── ObjectDatabase.py
│       └── SearchAPI.py
├── docs/                     # Documentation
│   ├── Architecture.md       # Full architecture documentation
│   ├── PRD-*.md              # Product Requirements Document
│   ├── checklists/           # Session checklists
│   └── architecture/         # Session architecture notes
├── *.tsx                     # React components (root level)
├── atoms.tsx                 # Jotai state management
├── hooks.tsx                 # Custom React hooks
├── utils.tsx                 # Utility functions
├── consts.tsx                # Constants
├── Types.tsx                 # TypeScript type definitions
├── vite.config.ts            # Vite configuration
├── tsconfig.json             # TypeScript configuration
├── package.json              # Node.js dependencies
└── .env.local                # Environment variables (GEMINI_API_KEY)
```

---

## 🚀 Entry Points

| File | Purpose |
|------|---------|
| [index.tsx](index.tsx) | React application entry point, renders App |
| [App.tsx](App.tsx) | Main application component, initializes state |
| [vite.config.ts](vite.config.ts) | Vite dev server configuration |

---

## 📦 Core Modules

### Frontend Components (Root Level)

| Component | Purpose |
|-----------|---------|
| [App.tsx](App.tsx) | Main app layout, state initialization, dark mode handling |
| [Content.tsx](Content.tsx) | Main content area with image detection display |
| [Prompt.tsx](Prompt.tsx) | User prompt input for custom object queries |
| [TopBar.tsx](TopBar.tsx) | Header/top navigation bar |
| [SideControls.tsx](SideControls.tsx) | Side panel controls (color palette, clear) |
| [DetectTypeSelector.tsx](DetectTypeSelector.tsx) | Detection type selector (2D/3D boxes, masks, points) |
| [ExtraModeControls.tsx](ExtraModeControls.tsx) | Additional detection mode controls |
| [ExampleImages.tsx](ExampleImages.tsx) | Sample image gallery for testing |
| [Palette.tsx](Palette.tsx) | Color palette for annotation colors |
| [ScreenshareButton.tsx](ScreenshareButton.tsx) | Screen capture functionality |

### State & Utilities

| File | Purpose |
|------|---------|
| [atoms.tsx](atoms.tsx) | Jotai atoms for state management (ImageSrc, DetectType, etc.) |
| [hooks.tsx](hooks.tsx) | Custom React hooks (useResetState, useGemini) |
| [utils.tsx](utils.tsx) | Utility functions |
| [consts.tsx](consts.tsx) | Application constants |
| [Types.tsx](Types.tsx) | TypeScript type definitions |

### Backend (Python - Placeholders)

| Module | Purpose | Status |
|--------|---------|--------|
| [ImageProcessor.py](src/cloud_backend/ImageProcessor.py) | Process uploaded images with cloud ML APIs | Placeholder |
| [ObjectDatabase.py](src/cloud_backend/ObjectDatabase.py) | Database interface for detection results | Placeholder |
| [SearchAPI.py](src/cloud_backend/SearchAPI.py) | Search API endpoint for querying detections | Placeholder |

---

## 🔧 Configuration

| File | Purpose |
|------|---------|
| [package.json](package.json) | Node.js dependencies, scripts (dev, build, preview) |
| [tsconfig.json](tsconfig.json) | TypeScript compiler configuration |
| [vite.config.ts](vite.config.ts) | Vite bundler configuration |
| [.env.local](.env.local) | `GEMINI_API_KEY` environment variable |
| [metadata.json](metadata.json) | AI Studio app metadata |

---

## 📚 Documentation

| Document | Topic |
|----------|-------|
| [Architecture.md](docs/Architecture.md) | Complete system architecture (4 phases) |
| [PRD-FindMyShtuff Gemini Edition 2jun2025.md](docs/PRD-FindMyShtuff%20Gemini%20Edition%202jun2025.md) | Product Requirements Document |
| [android_architecture.md](docs/android_architecture.md) | Android-specific architecture |
| [checklists/session*.md](docs/checklists/) | Development session checklists |
| [architecture/session*.md](docs/architecture/) | Session-specific architecture notes |

---

## 🧪 Test Coverage

**Status**: No test files detected in project

---

## 🔗 Key Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| `@google/genai` | ^0.7.0 | Google Gemini AI SDK |
| `react` | ^19.0.0 | UI framework |
| `react-dom` | ^19.0.0 | React DOM renderer |
| `jotai` | ^2.10.0 | State management |
| `@tailwindcss/browser` | ^4.0.17 | CSS framework |
| `perfect-freehand` | ^1.2.2 | Freehand drawing |
| `react-resize-detector` | ^12.0.2 | Resize detection |
| `vite` | ^6.2.0 | Build tool |
| `typescript` | ~5.7.2 | TypeScript compiler |

---

## 📝 Quick Start

```bash
# 1. Install dependencies
npm install

# 2. Set GEMINI_API_KEY in .env.local
echo "GEMINI_API_KEY=your_key_here" > .env.local

# 3. Run development server
npm run dev

# 4. Build for production
npm run build
```

---

## 📊 Architecture Overview

### Current Implementation
- **Frontend**: React/TypeScript with Vite, Google Gemini Vision API integration
- **State Management**: Jotai atoms
- **Styling**: Tailwind CSS v4
- **Detection Types**: 2D bounding boxes, 3D bounding boxes, segmentation masks, points

### Planned Architecture (4 Phases)

1. **Phase 1**: Core On-Device Real-time Detection (Android, TFLite)
2. **Phase 2**: Cloud Integration & Deep Analysis (Cloud ML, Storage, Database)
3. **Phase 3**: "Find Arbitrary Objects" (Search functionality)
4. **Phase 4**: Refinements & Advanced Features (3D mapping, AR, optimization)

See [Architecture.md](docs/Architecture.md) for complete details.

---

## 🎯 Key Features

- Image upload and screenshare capture
- Multiple detection types (2D/3D boxes, masks, points)
- Interactive annotation display
- Custom prompt queries for object detection
- Color palette for annotations
- Example image gallery

---

## 📋 File Count Summary

| Category | Count |
|----------|-------|
| TypeScript/React Components | 16 |
| Python Backend Files | 3 (placeholders) |
| Markdown Documentation | 12 |
| Configuration Files | 5 |

---

**Token Savings**: Reading this index (~3KB) vs full codebase (~58KB) = **94% reduction**
