#version 120

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

varying vec2 texCoord0;
varying vec4 vertexColor;

void main() {
    vec4 color = texture2D(Sampler0, texCoord0);
    if (color.a < 0.1) {
        discard;
    }
    color *= vertexColor * ColorModulator;
    float luminance = 0.2126 * color.r
        + 0.7152 * color.g
        + 0.0722 * color.b;
    gl_FragColor = vec4(luminance, luminance, luminance, color.a);
}
