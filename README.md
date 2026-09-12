# Swipe Tap

An Android Accessibility Service that detects a downward swipe and taps a configured
screen coordinate. It works without root and supports separate portrait and landscape
coordinates.

## Build the APK

The repository includes a GitHub Actions workflow. Open **Actions**, run **Build APK**,
then download the `swipe-tap-debug-apk` artifact from the completed run.

## Use the app

1. Install the APK and open **Swipe Tap**.
2. Set the portrait and landscape tap coordinates. Coordinates are screen pixels; the
   defaults match the supplied 720x1612 portrait and 1612x720 landscape recordings.
3. Set an optional delay and repeat count, then enable the service.
4. In Android Accessibility settings, enable **Swipe Tap**.
5. Turn on **Detection enabled** in the app and swipe down anywhere. The service taps the
   coordinate for the current orientation.

The service intentionally only reacts to Android's downward-swipe accessibility gesture.
Disable **Detection enabled** before using the device normally.
