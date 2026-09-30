#version 330 compatibility
#include "/lib/lit_fs.glsl"

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

void main() {
    vec4 tex = texture(gtexture, texcoord);
    if (tex.a < 0.1) discard;
    float day = dayFactor(sunHeight());
    // soft cloud lighting: lit tops, blueish undersides
    float top = 0.75 + 0.25 * clamp(vUp, -1.0, 1.0);
    vec3 lit = mix(vec3(0.05, 0.06, 0.10), mix(vec3(1.0, 0.98, 0.95), vec3(0.75, 0.78, 0.85), rainStrength), day);
    vec3 col = lit * top * toLinear(tex.rgb * glcolor.rgb) * 1.15;
    // tint clouds with the horizon colour near sunrise / sunset
    col = mix(col, vFog * 1.3 * top, 0.25 * twilightFactor(sunHeight()));
    // fade into the haze with distance
    float f = fogAmount(length(vPos));
    outColor = vec4(mix(col, vFog, f), tex.a * glcolor.a * (1.0 - f * 0.6));
}
