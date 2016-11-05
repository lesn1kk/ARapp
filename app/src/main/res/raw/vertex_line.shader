attribute vec4 vPosition;
uniform vec4 u_MVP;

void main() {
    gl_Position = u_MVP + vPosition;
}