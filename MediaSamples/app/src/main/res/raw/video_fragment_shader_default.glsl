// GL_TEXTURE_EXTERNAL_OES
#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTextureCoordinate;
uniform samplerExternalOES uTexture;
void main() {
    gl_FragColor=texture2D(uTexture, vTextureCoordinate);
}