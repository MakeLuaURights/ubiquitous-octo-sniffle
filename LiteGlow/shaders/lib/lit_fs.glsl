// Common fragment-side helpers for forward-lit geometry programs.
#include "/lib/common.glsl"

uniform sampler2D gtexture;

in vec2 texcoord;
in vec2 lmcoord;
in vec4 glcolor;
in vec3 vNormal;
in vec3 vPos;
in float vUp;
in vec3 vL;
in vec3 vSun;
in vec3 vAmb;
in vec3 vFog;

vec3 lightIt(vec3 albedoLinear, vec3 n) {
    return shade(albedoLinear, lmcoord, n, vUp, vL, vSun, vAmb);
}
vec3 fogIt(vec3 c) {
    return mix(c, vFog, fogAmount(length(vPos)));
}
