// Common vertex-side setup for forward-lit geometry programs.
#include "/lib/common.glsl"

out vec2 texcoord;
out vec2 lmcoord;
out vec4 glcolor;
out vec3 vNormal;
out vec3 vPos;
out float vUp;
out vec3 vL;
out vec3 vSun;
out vec3 vAmb;
out vec3 vFog;

void litSetup(vec3 viewPos) {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord  = fixLightmap((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy);
    glcolor  = gl_Color;
    vNormal  = normalize(gl_NormalMatrix * gl_Normal);
    vUp      = dot(vNormal, normalize(upPosition));
    vPos     = viewPos;
    lightEnvironment(vL, vSun, vAmb);
    vFog     = fogColorFor(normalize(viewPos));
}
