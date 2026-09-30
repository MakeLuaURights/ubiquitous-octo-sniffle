#version 330 compatibility
#include "/lib/lit_vs.glsl"

in vec4 mc_Entity;

uniform mat4 gbufferModelViewInverse;
uniform vec3 cameraPosition;

out vec3 worldPos;
flat out int isWater;

void main() {
    vec4 pos = gl_ModelViewMatrix * gl_Vertex;
    gl_Position = gl_ProjectionMatrix * pos;
    litSetup(pos.xyz);
    worldPos = (gbufferModelViewInverse * pos).xyz + cameraPosition;
    isWater = int(int(mc_Entity.x + 0.5) == 10010);
}
