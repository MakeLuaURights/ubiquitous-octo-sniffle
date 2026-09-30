// Shared lighting / sky / fog code. Everything is computed per-vertex where
// possible; the fragment stage only does a few multiply-adds.
#include "/lib/settings.glsl"

uniform vec3 sunPosition;
uniform vec3 upPosition;
uniform vec3 shadowLightPosition;
uniform float rainStrength;
uniform float nightVision;
uniform float far;
uniform int isEyeInWater;
uniform bool hasCeiling;
uniform bool hasSkylight;

vec3 toLinear(vec3 c) { return c * (c * (c * 0.305306011 + 0.682171111) + 0.012522878); }

float sunHeight() { return dot(normalize(sunPosition), normalize(upPosition)); }
float dayFactor(float h) { return smoothstep(-0.08, 0.22, h); }
float twilightFactor(float h) {
    return pow(1.0 - min(abs(h), 1.0), 8.0) * smoothstep(-0.3, 0.0, h);
}

// Sky gradient (linear HDR). d, s, u: view-space view dir / sun dir / up dir.
vec3 skyGradient(vec3 d, vec3 s, vec3 u, float day, float twi) {
    float h  = dot(d, u);
    float sd = max(dot(d, s), 0.0);
    float t  = pow(1.0 - clamp(h, 0.0, 1.0), 3.2);

    vec3 zenith  = mix(vec3(0.004, 0.008, 0.028), vec3(0.070, 0.200, 0.600), day);
    vec3 horizon = mix(vec3(0.018, 0.030, 0.070), vec3(0.480, 0.640, 0.900), day);
    vec3 col = mix(zenith, horizon, t);

    // sunrise / sunset band, strongest towards the sun
    col = mix(col, vec3(1.0, 0.40, 0.14), clamp(twi * t * (0.25 + 0.75 * pow(sd, 4.0)), 0.0, 1.0) * 0.85);
    // soft sun halo
    col += vec3(1.0, 0.80, 0.55) * (pow(sd, 24.0) * 0.45 + pow(sd, 4.0) * 0.05) * day;
    // faint moon glow
    col += vec3(0.10, 0.15, 0.30) * pow(max(dot(d, -s), 0.0), 18.0) * (1.0 - day) * 0.35;

    vec3 overcast = mix(vec3(0.015, 0.018, 0.03), vec3(0.34, 0.38, 0.43), day);
    return mix(col, overcast, rainStrength * 0.85);
}

vec3 netherFog() { return vec3(0.17, 0.035, 0.02); }
vec3 endFog()    { return vec3(0.035, 0.015, 0.06); }

// Fog / horizon colour for a view-space direction (vertex shaders).
vec3 fogColorFor(vec3 d) {
    if (isEyeInWater == 1) {
        float day = dayFactor(sunHeight());
        return vec3(0.015, 0.14, 0.20) * (0.25 + 0.75 * day);
    }
    if (isEyeInWater == 2) return vec3(0.9, 0.25, 0.04);
    if (hasCeiling) return netherFog();
    if (!hasSkylight) return endFog();
    float h = sunHeight();
    return skyGradient(d, normalize(sunPosition), normalize(upPosition), dayFactor(h), twilightFactor(h));
}

float fogAmount(float dist) {
    if (isEyeInWater == 1) return 1.0 - exp(-dist * 0.11);
    if (isEyeInWater == 2) return 1.0 - exp(-dist * 0.7);
    float f = clamp((dist - far * 0.4) / (far * 0.6), 0.0, 1.0);
    f *= f;
    float k = 0.0013 * HAZE + 0.006 * rainStrength;
    if (hasCeiling) k = 0.012;
    else if (!hasSkylight) k = 0.004;
    float haze = (1.0 - exp(-dist * k)) * 0.9;
    return 1.0 - (1.0 - f) * (1.0 - haze);
}

// Light environment (vertex shaders).
void lightEnvironment(out vec3 L, out vec3 sunCol, out vec3 ambCol) {
    float h = sunHeight();
    float day = dayFactor(h);
    float twi = twilightFactor(h);
    L = normalize(shadowLightPosition);
    if (hasCeiling) {            // Nether
        sunCol = vec3(0.0);
        ambCol = vec3(1.25, 0.62, 0.42);
    } else if (!hasSkylight) {   // End
        sunCol = vec3(0.0);
        ambCol = vec3(0.62, 0.50, 0.85);
    } else {
        vec3 sunTint = mix(vec3(1.0, 0.45, 0.18), vec3(1.0, 0.93, 0.80), smoothstep(0.0, 0.4, h));
        vec3 moon = vec3(0.30, 0.42, 0.75) * 0.22 * (1.0 - day);
        sunCol = sunTint * 1.8 * day + moon;
        vec3 ambDay   = mix(vec3(0.42, 0.58, 0.85), vec3(0.75, 0.50, 0.42), twi * 0.6) * 0.75;
        vec3 ambNight = vec3(0.020, 0.030, 0.065) * 1.4;
        ambCol = mix(ambNight, ambDay, day);
        float rain = rainStrength;
        sunCol *= 1.0 - 0.9 * rain;
        ambCol  = mix(ambCol, vec3(dot(ambCol, vec3(0.333))) * 0.9, rain * 0.6);
    }
}

// Forward lighting (fragment shaders).
vec3 shade(vec3 albedo, vec2 lm, vec3 n, float ny, vec3 L, vec3 sunCol, vec3 ambCol) {
    float sky = hasSkylight ? lm.y : 1.0;
    float sl = sky * sky;
    vec3 amb = ambCol * sl * (0.85 + 0.15 * ny) + vec3(0.010 + 0.25 * nightVision);
    if (!hasSkylight) amb = ambCol * (0.30 + 0.15 * ny) + vec3(0.02 + 0.25 * nightVision);

    float ndl = max(dot(n, L), 0.0);
    vec3 sun = sunCol * ndl * pow(sky, 10.0);

    float bl = lm.x;
    float bright = 1.0 - 0.7 * sl * (hasSkylight ? dayFactor(sunHeight()) : 0.0);
    vec3 blockL = vec3(1.0, 0.52, 0.22) * (bl * bl * bl * 1.5 + pow(bl, 12.0) * 0.9) * bright;

    return albedo * (amb + sun + blockL);
}

// vanilla lightmap coords -> 0..1
vec2 fixLightmap(vec2 lm) { return clamp((lm - 0.03125) * 1.0666667, 0.0, 1.0); }
