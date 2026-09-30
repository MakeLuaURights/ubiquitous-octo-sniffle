#version 330 compatibility
in vec4 glcolor;

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

void main() {
    outColor = vec4(glcolor.rgb * glcolor.rgb, glcolor.a);   // cheap gamma->linear
}
