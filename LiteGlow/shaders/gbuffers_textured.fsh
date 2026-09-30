#version 330 compatibility
#include "/lib/lit_fs.glsl"

uniform vec4 entityColor;

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

void main() {
    vec4 tex = texture(gtexture, texcoord);
    if (tex.a < 0.05) discard;
    vec3 rgb = tex.rgb * glcolor.rgb;
    rgb = mix(rgb, entityColor.rgb, entityColor.a);   // hurt flash, creeper blink
    vec3 col = lightIt(toLinear(rgb), normalize(vNormal));
    outColor = vec4(fogIt(col), tex.a * glcolor.a);
}
