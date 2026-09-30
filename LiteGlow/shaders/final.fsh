#version 330 compatibility
#include "/lib/settings.glsl"

// HDR scene buffer: half the bandwidth of RGBA16F
const int colortex0Format = R11F_G11F_B10F;

uniform sampler2D colortex0;

in vec2 texcoord;

layout(location = 0) out vec4 outColor;

vec3 aces(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

void main() {
    vec3 c = texture(colortex0, texcoord).rgb * EXPOSURE;

    c *= vec3(1.0 + WARMTH, 1.0, 1.0 - WARMTH);
    c = aces(c);
    c = sqrt(c);                                      // ~gamma 2.0, cheaper than pow

    float l = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(l), c, SATURATION);
    c = (c - 0.5) * CONTRAST + 0.5;

    vec2 q = texcoord - 0.5;
    c *= 1.0 - dot(q, q) * VIGNETTE * 2.2;

    // tiny ordered-ish noise to hide banding in the sky gradient
    float n = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    c += (n - 0.5) / 255.0;

    outColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
