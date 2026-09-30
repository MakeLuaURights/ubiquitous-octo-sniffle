#version 330 compatibility
#include "/lib/common.glsl"

uniform mat4 gbufferProjectionInverse;
uniform float viewWidth;
uniform float viewHeight;
uniform int renderStage;

in vec4 starData;

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

void main() {
    bool isStar = false;
#ifdef MC_RENDER_STAGE_STARS
    isStar = renderStage == MC_RENDER_STAGE_STARS;
#endif
    if (isStar) {
        float night = 1.0 - dayFactor(sunHeight());
        outColor = vec4(vec3(1.0, 0.95, 0.9) * 2.2 * starData.a, starData.a * night * (1.0 - rainStrength));
        return;
    }

    vec2 uv = gl_FragCoord.xy / vec2(viewWidth, viewHeight);
    vec4 v = gbufferProjectionInverse * vec4(uv * 2.0 - 1.0, 1.0, 1.0);
    vec3 dir = normalize(v.xyz / v.w);
    outColor = vec4(fogColorFor(dir), 1.0);
}
