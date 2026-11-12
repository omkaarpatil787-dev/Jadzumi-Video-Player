# Jadzumi Pure — Interactive Prototype

This standalone prototype implements the approved **Jadzumi Pure** editorial design language and **Continuous Spatial Navigation** system for Jadzumi Player.

---

## 1. How to Launch and Test

Simply double-click or open `index.html` in any modern web browser:

```
file:///C:/Users/onkar/OneDrive/Documents/GitHub/Jadzumi-Video-Player/design-prototypes/jadzumi-pure/index.html
```

Or run a local HTTP server:
```powershell
cd "C:\Users\onkar\OneDrive\Documents\GitHub\Jadzumi-Video-Player\design-prototypes\jadzumi-pure"
python -m http.server 8080
# Open http://localhost:8080 in Chrome / Edge
```

---

## 2. Interactive Features to Test

1. **Continuous Spatial Navigation Journey**:
   - On the **Home** screen, click on the **The Silent Horizon** hero card.
   - Observe the smooth 420ms spatial expansion into the **Movie Details** screen.
   - Click the **Play** button: observe the cinematic expansion into the **Immersive Player**.
   - Click the **Back** arrow in the player: smoothly return to **Movie Details** with preserved time/progress.
   - Click the **Back** arrow in Details: return seamlessly to the exact originating **Library** position.

2. **Device Adaptations**:
   - Use the top control bar to switch between:
     - **Tablet (Xiaomi Pad 5 11" 16:10)**: Persistent Left Navigation Rail, large editorial hero card, horizontal scrollers.
     - **Tablet Portrait (Xiaomi Pad 5 16:10)**: Adjusted density.
     - **Phone Portrait**: Bottom navigation bar, vertical mobile stack.
     - **Fullscreen**: Responsive layout.

3. **Accent Color Comparison**:
   - Toggle between **Pure Yellow** (`#FFD600`, reference editorial) and **Pure Lavender** (`#BB86FC`, Jadzumi heritage) in real-time.

4. **Player Edge Gestures**:
   - In the player, click **Brightness** or **Volume** to see the side-docked minimal horizontal pills (Left: Brightness, Right: Volume), leaving the center 100% clean.

5. **21 Screen Jump**:
   - Use the "Screen" dropdown in the top header to instantly inspect all 21 states including Audio Studio, Color Studio, Subtitle modals, Storage Cleaner, and Splash screen.
