attribute vec4 vPosition;
uniform mat4 u_MVP;

void main() {
    gl_Position = u_MVP * vPosition;
}