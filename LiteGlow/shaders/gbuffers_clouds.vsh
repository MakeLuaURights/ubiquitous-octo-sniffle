#version 330 compatibility
#include "/lib/lit_vs.glsl"

void main() {
    vec4 pos = gl_ModelViewMatrix * gl_Vertex;
    gl_Position = gl_ProjectionMatrix * pos;
    litSetup(pos.xyz);
}
