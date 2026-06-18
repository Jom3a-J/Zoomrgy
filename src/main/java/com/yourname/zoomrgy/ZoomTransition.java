package com.yourname.zoomrgy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ZoomTransition {

    public enum Type {
        INSTANT,
        LINEAR,
        EASE_IN_SINE,
        EASE_OUT_SINE,
        EASE_IN_OUT_SINE,
        EASE_IN_QUAD,
        EASE_OUT_QUAD,
        EASE_IN_OUT_QUAD,
        EASE_IN_CUBIC,
        EASE_OUT_CUBIC,
        EASE_IN_OUT_CUBIC,
        EASE_IN_EXPONENTIAL,
        EASE_OUT_EXPONENTIAL,
        EASE_IN_OUT_EXPONENTIAL,

        // Backwards compatibility aliases/legacy types:
        @Deprecated EASE_IN,
        @Deprecated EASE_OUT,
        @Deprecated EASE_IN_OUT,
        @Deprecated SINE,
        @Deprecated EXPONENTIAL,

        SMOOTHSTEP,
        CIRCULAR,
        BACK,
        ELASTIC,
        BOUNCE;

        /**
         * Returns a human-readable display name for this transition type.
         */
        public String getDisplayName() {
            return switch (this) {
                case INSTANT -> "Instant";
                case LINEAR -> "Linear";
                case EASE_IN_SINE -> "Ease In (Sine)";
                case EASE_OUT_SINE -> "Ease Out (Sine)";
                case EASE_IN_OUT_SINE -> "Ease In-Out (Sine)";
                case EASE_IN_QUAD -> "Ease In (Quadratic)";
                case EASE_OUT_QUAD -> "Ease Out (Quadratic)";
                case EASE_IN_OUT_QUAD -> "Ease In-Out (Quadratic)";
                case EASE_IN_CUBIC -> "Ease In (Cubic)";
                case EASE_OUT_CUBIC -> "Ease Out (Cubic)";
                case EASE_IN_OUT_CUBIC -> "Ease In-Out (Cubic)";
                case EASE_IN_EXPONENTIAL -> "Ease In (Exponential)";
                case EASE_OUT_EXPONENTIAL -> "Ease Out (Exponential)";
                case EASE_IN_OUT_EXPONENTIAL -> "Ease In-Out (Exponential)";
                case SMOOTHSTEP -> "Smoothstep";
                case CIRCULAR -> "Circular";
                case BACK -> "Back (Overshoot)";
                case ELASTIC -> "Elastic";
                case BOUNCE -> "Bounce";
                default -> name(); // Deprecated types fallback to raw name
            };
        }
    }

    /**
     * Returns only the non-deprecated transition types for use in config GUIs.
     */
    public static Type[] getSelectableTypes() {
        return java.util.Arrays.stream(Type.values())
            .filter(t -> {
                try {
                    return !Type.class.getField(t.name()).isAnnotationPresent(Deprecated.class);
                } catch (NoSuchFieldException e) {
                    return true;
                }
            })
            .toArray(Type[]::new);
    }

    /**
     * Apply an easing curve to a raw linear progress value t ∈ [0, 1].
     */
    public static double apply(double t, Type type) {
        t = Math.max(0.0, Math.min(1.0, t)); // clamp
        return switch (type) {
            case INSTANT                -> t > 0.0 ? 1.0 : 0.0;
            case LINEAR                 -> t;
            
            case EASE_IN_SINE           -> 1.0 - Math.cos((t * Math.PI) / 2.0);
            case EASE_OUT_SINE          -> Math.sin((t * Math.PI) / 2.0);
            case EASE_IN_OUT_SINE, SINE -> -(Math.cos(Math.PI * t) - 1.0) / 2.0;
            
            case EASE_IN_QUAD, EASE_IN  -> t * t;
            case EASE_OUT_QUAD, EASE_OUT -> t * (2.0 - t);
            case EASE_IN_OUT_QUAD, EASE_IN_OUT -> {
                double term = -2.0 * t + 2.0;
                yield t < 0.5 ? 2.0 * t * t : 1.0 - (term * term) / 2.0;
            }
            
            case EASE_IN_CUBIC          -> t * t * t;
            case EASE_OUT_CUBIC         -> {
                double term = 1.0 - t;
                yield 1.0 - term * term * term;
            }
            case EASE_IN_OUT_CUBIC      -> {
                double term = -2.0 * t + 2.0;
                yield t < 0.5 ? 4.0 * t * t * t : 1.0 - (term * term * term) / 2.0;
            }
            
            case EASE_IN_EXPONENTIAL    -> t == 0.0 ? 0.0 : Math.pow(2.0, 10.0 * t - 10.0);
            case EASE_OUT_EXPONENTIAL   -> t == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * t);
            case EASE_IN_OUT_EXPONENTIAL, EXPONENTIAL -> t == 0.0 ? 0.0 : t == 1.0 ? 1.0 : t < 0.5 
                                                         ? Math.pow(2.0, 20.0 * t - 10.0) / 2.0 
                                                         : (2.0 - Math.pow(2.0, -20.0 * t + 10.0)) / 2.0;
            
            case SMOOTHSTEP   -> t * t * (3.0 - 2.0 * t);
            case CIRCULAR     -> {
                double term = -2.0 * t + 2.0;
                yield t < 0.5 
                      ? (1.0 - Math.sqrt(1.0 - 4.0 * t * t)) / 2.0 
                      : (Math.sqrt(1.0 - term * term) + 1.0) / 2.0;
            }
            case BACK         -> {
                                 double c1 = 1.70158;
                                 double c3 = c1 + 1.0;
                                 double term = t - 1.0;
                                 yield 1.0 + c3 * (term * term * term) + c1 * (term * term);
                               }
            case ELASTIC      -> {
                                 double c4 = (2.0 * Math.PI) / 3.0;
                                 yield t == 0 ? 0.0 : t == 1 ? 1.0 
                                       : Math.pow(2.0, -10.0 * t) * Math.sin((t * 10.0 - 0.75) * c4) + 1.0;
                               }
            case BOUNCE       -> {
                                 double n1 = 7.5625;
                                 double d1 = 2.75;
                                 if (t < 1.0 / d1) {
                                     yield n1 * t * t;
                                 } else if (t < 2.0 / d1) {
                                     double t2 = t - 1.5 / d1;
                                     yield n1 * t2 * t2 + 0.75;
                                 } else if (t < 2.5 / d1) {
                                     double t2 = t - 2.25 / d1;
                                     yield n1 * t2 * t2 + 0.9375;
                                 } else {
                                     double t2 = t - 2.625 / d1;
                                     yield n1 * t2 * t2 + 0.984375;
                                 }
                               }
        };
    }
}
