precision mediump float; // this precision is enough for our purposes
varying vec2 v_texCoord; // this vector contains informations about
uniform sampler2D s_texture; //this contains out sampler texture
uniform float alpha; // this variable contains actual alpha value

void main() {
    gl_FragColor = texture2D(s_texture,v_texCoord) * alpha;
    //gl_FragColor = vec4(v_texCoord, 0.0, 1.0);
}