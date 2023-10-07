attribute vec4 aPosition;
attribute vec2 aCoordinate;
// varying变量是vertex和fragment之间做数据传递用的,
// 一般vertex shader修改varying变量的值，然后fragment shader使用该varying变量的值
// 因此varying变量在vertex和fragment shader二者之间的声明必须是一致的
varying vec2 vTextureCoordinate; // 纹理坐标
void main() {
    gl_Position = aPosition;
    vTextureCoordinate = aCoordinate;
}