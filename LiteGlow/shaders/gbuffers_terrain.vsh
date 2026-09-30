#version 330 compatibility
#include "/lib/lit_vs.glsl"

in vec4 mc_Entity;
in vec2 mc_midTexCoord;

uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform vec3 cameraPosition;
uniform float frameTimeCounter;

void main() {
    vec4 pos = gl_ModelViewMatrix * gl_Vertex;

#ifdef WAVING
    int id = int(mc_Entity.x + 0.5);
    if (id == 10001 || id == 10002) {
        vec3 wp = (gbufferModelViewInverse * pos).xyz + cameraPosition;
        float t = frameTimeCounter;
        float gust = (0.65 + 0.35 * sin(t * 0.45 + wp.x * 0.06 + wp.z * 0.04)) * (1.0 + rainStrength * 0.8);
        vec3 d = vec3(0.0);
        if (id == 10001) {
            float top = float(gl_MultiTexCoord0.t < mc_midTexCoord.t);
            d.x = sin(t * 1.9 + wp.x * 0.9 + wp.z * 0.6) * 0.055 * top;
            d.z = cos(t * 1.5 + wp.z * 0.9 + wp.x * 0.4) * 0.040 * top;
        } else {
            d.x = sin(t * 1.3 + wp.x * 0.7 + wp.y * 0.5) * 0.030;
            d.y = sin(t * 1.1 + wp.z * 0.6 + wp.x * 0.3) * 0.015;
            d.z = cos(t * 1.2 + wp.z * 0.7 + wp.y * 0.5) * 0.030;
        }
        pos.xyz += mat3(gbufferModelView) * (d * gust);
    }
#endif

    gl_Position = gl_ProjectionMatrix * pos;
    litSetup(pos.xyz);
}
