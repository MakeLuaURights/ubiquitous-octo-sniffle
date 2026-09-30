#version 330 compatibility
#include "/lib/lit_fs.glsl"

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

void main() {
    vec4 tex = texture(gtexture, texcoord);
    if (tex.a < 0.05) discard;
    float day = dayFactor(sunHeight());
    vec3 col = mix(vec3(0.03, 0.04, 0.07), vec3(0.55, 0.62, 0.72), day) * (0.6 + 0.4 * lmcoord.y);
    outColor = vec4(col, tex.a * glcolor.a * 0.75);
}
