#version 330 compatibility
uniform sampler2D gtexture;
in vec2 texcoord;
in vec4 glcolor;

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

void main() {
    vec4 tex = texture(gtexture, texcoord) * glcolor;
    if (tex.a < 0.05) discard;
    outColor = vec4(tex.rgb * tex.rgb * 2.0, tex.a);        // emissive
}
