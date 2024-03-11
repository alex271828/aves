import 'package:aves/model/settings/settings.dart';
import 'package:aves/theme/durations.dart';
import 'package:aves_model/aves_model.dart';
import 'package:flutter/widgets.dart';

extension ExtraAccessibilityAnimations on AccessibilityAnimations {
  bool get animate {
    switch (this) {
      case AccessibilityAnimations.system:
        // as of Flutter v2.5.1, the check for `disableAnimations` is unreliable
        // so we cannot use `window.accessibilityFeatures.disableAnimations` nor `MediaQuery.of(context).disableAnimations`
        return !settings.areAnimationsRemoved;
      case AccessibilityAnimations.disabled:
        return false;
      case AccessibilityAnimations.enabled:
        return true;
    }
  }

  Duration get popUpAnimationDuration => animate ? ADurations.popupMenuAnimation : Duration.zero;

  Duration get popUpAnimationDelay => popUpAnimationDuration + const Duration(milliseconds: ADurations.transitionMarginMillis);

  AnimationStyle get popUpAnimationStyle {
    return animate
        ? AnimationStyle(
            curve: Curves.easeInOutCubic,
            duration: popUpAnimationDuration,
          )
        : AnimationStyle.noAnimation;
  }
}
