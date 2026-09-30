#version 330 compatibility
#include "/lib/lit_fs.glsl"

uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform float frameTimeCounter;

in vec3 worldPos;
flat in int isWater;

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

void main() {
    vec4 tex = texture(gtexture, texcoord);
    vec3 n = normalize(vNormal);

    if (isWater == 0) {                       // glass, ice, slime, portals...
        if (tex.a < 0.02) discard;
        vec3 col = lightIt(toLinear(tex.rgb * glcolor.rgb), n);
        outColor = vec4(fogIt(col), tex.a * glcolor.a);
        return;
    }

    vec3 tint = toLinear(glcolor.rgb);
    float tl = dot(tex.rgb, vec3(0.333));
    vec3 albedo = tint * (0.45 + 0.9 * tl);
    float alpha = 0.62;

#ifdef WATER_FX
    // gentle animated normal on the water surface
    vec3 nW = mat3(gbufferModelViewInverse) * n;
    if (nW.y > 0.9) {
        vec2 p = worldPos.xz;
        float t = frameTimeCounter;
        float a = cos(p.x * 1.3 + t * 1.1) * 0.5 + cos((p.x + p.y) * 2.1 - t * 1.7) * 0.3;
        float b = cos(p.y * 1.7 + t * 0.9) * 0.5 + cos((p.x - p.y) * 2.3 + t * 1.5) * 0.3;
        nW = normalize(vec3(-a * 0.05, 1.0, -b * 0.05));
        n = mat3(gbufferModelView) * nW;
    }
    vec3 V = normalize(vPos);
    float fres = pow(1.0 - clamp(dot(-V, n), 0.0, 1.0), 4.0);
    vec3 R = reflect(V, n);
    float h = sunHeight();
    float day = dayFactor(h);
    vec3 skyRefl = hasSkylight
        ? skyGradient(R, normalize(sunPosition), normalize(upPosition), day, twilightFactor(h))
        : vFog;
    float skyVis = pow(lmcoord.y, 6.0);
    vec3 col = lightIt(albedo, n);
    col = mix(col, skyRefl, clamp(fres * 0.9 + 0.04, 0.0, 1.0) * skyVis);
    // sun / moon glint
    float glint = pow(max(dot(R, vL), 0.0), 350.0);
    col += vSun * glint * 3.0 * skyVis;
    alpha = mix(alpha, 0.96, fres);
#else
    vec3 col = lightIt(albedo, n);
#endif

    outColor = vec4(fogIt(col), alpha);
}
