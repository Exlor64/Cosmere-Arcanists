#version 150
uniform sampler2D Sampler0;
uniform vec2 ScreenSize;
uniform vec4 DangerEdges;
uniform float EdgeWidth;
uniform float BlurRadius;
in vec2 texCoord;
out vec4 fragColor;
vec3 sampleScene(vec2 offset) {
    return texture(Sampler0, clamp(texCoord + offset, .5 / ScreenSize, 1.0 - .5 / ScreenSize)).rgb;
}
void main() {
    float edge = max(max((1.0 - smoothstep(0.0, EdgeWidth, texCoord.x)) * DangerEdges.x,
                         smoothstep(1.0 - EdgeWidth, 1.0, texCoord.x) * DangerEdges.y),
                     max(smoothstep(1.0 - EdgeWidth, 1.0, texCoord.y) * DangerEdges.z,
                         (1.0 - smoothstep(0.0, EdgeWidth, texCoord.y)) * DangerEdges.w));
    vec3 original = sampleScene(vec2(0.0));
    if (edge <= .0001) { fragColor = vec4(original, 1.0); return; }
    vec2 radius = BlurRadius * sqrt(clamp(edge, 0.0, 1.0)) / ScreenSize;
    vec3 blurred = original * .20;
    blurred += (sampleScene(vec2(radius.x * .5, 0.0)) + sampleScene(vec2(-radius.x * .5, 0.0))
              + sampleScene(vec2(0.0, radius.y * .5)) + sampleScene(vec2(0.0, -radius.y * .5))) * .12;
    blurred += (sampleScene(radius * vec2(.7, .7)) + sampleScene(radius * vec2(-.7, .7))
              + sampleScene(radius * vec2(.7, -.7)) + sampleScene(radius * vec2(-.7, -.7))) * .06;
    blurred += (sampleScene(vec2(radius.x, 0.0)) + sampleScene(vec2(-radius.x, 0.0))
              + sampleScene(vec2(0.0, radius.y)) + sampleScene(vec2(0.0, -radius.y))) * .02;
    fragColor = vec4(mix(original, blurred, clamp(edge * 1.5, 0.0, 1.0)), 1.0);
}
