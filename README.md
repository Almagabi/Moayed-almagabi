# Swipe Tap

An Android Accessibility Service with a floating tap button that taps a configured
screen coordinate over other apps. It works without root and supports separate portrait
and landscape coordinates.

## Build the APK

The repository includes a GitHub Actions workflow. Open **Actions**, run **Build APK**,
then download the `swipe-tap-debug-apk` artifact from the completed run.

## Use the app

1. Install the APK and open **Swipe Tap**.
2. Set the portrait and landscape tap coordinates. Coordinates are screen pixels; the
   defaults match the supplied 720x1612 portrait and 1612x720 landscape recordings.
3. Set an optional delay and repeat count, then enable the service.
4. In Android Accessibility settings, enable **Swipe Tap**. The service only performs
   taps requested by the floating button; it does not monitor swipes or request touch
   exploration.
5. Return to the app, choose the **Trigger gesture** (tap or directional swipe), choose
   the trigger icon, target size, and target opacity, then press **Save settings**. The
   green target reticle is always a tap-only output point.
6. Allow floating windows, press **Start floating TAP button**, and tap the floating
   **TAP** button to perform the configured gesture for the current orientation.
   Drag the green circular **TARGET** marker to position the exact output tap point
   manually; its red center dot is the tap coordinate. The purple trigger icon is where
   you perform the input tap or configured swipe. Hold it briefly and drag to reposition
   it. The target appears after a two-second hold, fades after four seconds of inactivity,
   and double-tapping toggles it. The **X** beside the trigger stops the floating service.

The app does not request global swipe detection or touch exploration, avoiding changes
to normal touch behavior.
