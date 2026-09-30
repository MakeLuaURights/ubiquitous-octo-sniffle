#version 330 compatibility
out vec4 starData;
void main() {
    gl_Position = ftransform();
    starData = gl_Color;
}
