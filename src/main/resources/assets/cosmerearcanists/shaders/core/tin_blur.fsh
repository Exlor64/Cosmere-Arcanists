#version 150
uniform vec4 ColorModulator;
in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    vec2 offset = (texCoord0 - 0.5) * 2.0;
    float distanceSquared = dot(offset, offset);
    float softness = exp(-3.5 * distanceSquared) * (1.0 - smoothstep(0.45, 1.0, distanceSquared));
    float alpha = vertexColor.a * ColorModulator.a * softness;
    if (alpha < 0.001) discard;
    fragColor = vec4(vec3(1.0), alpha);
}
