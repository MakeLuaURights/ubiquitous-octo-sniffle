#version 330 compatibility
#include "/lib/common.glsl"

uniform sampler2D gtexture;
uniform int renderStage;

in vec2 texcoord;
in vec4 glcolor;

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

void main() {
    vec4 tex = texture(gtexture, texcoord) * glcolor;
    float boost = 1.0;
#ifdef MC_RENDER_STAGE_SUN
    if (renderStage == MC_RENDER_STAGE_SUN) boost = 4.5;
#endif
#ifdef MC_RENDER_STAGE_MOON
    if (renderStage == MC_RENDER_STAGE_MOON) boost = 1.6;
#endif
    outColor = vec4(toLinear(tex.rgb) * boost, tex.a * (1.0 - rainStrength * 0.85));
}
