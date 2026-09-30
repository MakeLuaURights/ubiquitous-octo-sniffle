#version 330 compatibility
#include "/lib/lit_fs.glsl"

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

void main() {
    vec4 tex = texture(gtexture, texcoord);
    if (tex.a < 0.1) discard;
    vec3 albedo = toLinear(tex.rgb * glcolor.rgb);
    vec3 col = lightIt(albedo, normalize(vNormal));
    outColor = vec4(fogIt(col), tex.a);
}
