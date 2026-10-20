import ctypes as c
import os
from pathlib import Path
import numpy as np
os.environ['LIBGL_ALWAYS_SOFTWARE']='1'
os.environ['EGL_PLATFORM']='surfaceless'
e=c.CDLL('libEGL.so.1');g=c.CDLL('libGL.so.1')
def fn(lib,name,rest,args):
 f=getattr(lib,name);f.restype=rest;f.argtypes=args;return f
P=c.c_void_p;I=c.c_int;U=c.c_uint;F=c.c_float
getdisplay=fn(e,'eglGetDisplay',P,[P]);initialize=fn(e,'eglInitialize',U,[P,P,P]);choose=fn(e,'eglChooseConfig',U,[P,P,P,I,P]);bind=fn(e,'eglBindAPI',U,[U]);surface=fn(e,'eglCreatePbufferSurface',P,[P,P,P]);context=fn(e,'eglCreateContext',P,[P,P,P,P]);current=fn(e,'eglMakeCurrent',U,[P,P,P,P])
d=getdisplay(None);assert initialize(d,None,None);assert bind(0x30A2)
attrs=(I*13)(0x3033,1,0x3040,8,0x3024,8,0x3023,8,0x3022,8,0x3021,8,0x3038);config=P();count=I();assert choose(d,attrs,c.byref(config),1,c.byref(count)) and count.value
pb=(I*5)(0x3057,256,0x3056,144,0x3038);s=surface(d,config,pb);ca=(I*7)(0x3098,3,0x30FB,2,0x30FD,1,0x3038);ctx=context(d,config,None,ca);assert ctx and current(d,s,s,ctx)
def gl(name,rest,args):return fn(g,name,rest,args)
create=gl('glCreateShader',U,[U]);source=gl('glShaderSource',None,[U,I,P,P]);compile=gl('glCompileShader',None,[U]);get=gl('glGetShaderiv',None,[U,U,P]);log=gl('glGetShaderInfoLog',None,[U,I,P,P])
root=Path(__file__).resolve().parents[1] / 'src/main/resources/assets/cosmerearcanists/shaders/core'
shaders=[]
for ext,kind in [('vsh',0x8B31),('fsh',0x8B30)]:
 shader=create(kind);src=(root/f'bronze_danger.{ext}').read_bytes();ptr=c.c_char_p(src);source(shader,1,c.byref(ptr),None);compile(shader);ok=I();get(shader,0x8B81,c.byref(ok));buf=c.create_string_buffer(8192);log(shader,8192,None,buf);assert ok.value,buf.value.decode();shaders.append(shader)
program=gl('glCreateProgram',U,[])();attach=gl('glAttachShader',None,[U,U]);attrib=gl('glBindAttribLocation',None,[U,U,c.c_char_p]);link=gl('glLinkProgram',None,[U]);pget=gl('glGetProgramiv',None,[U,U,P]);plog=gl('glGetProgramInfoLog',None,[U,I,P,P])
for shader in shaders:attach(program,shader)
attrib(program,0,b'Position');attrib(program,1,b'UV0');link(program);ok=I();pget(program,0x8B82,c.byref(ok));buf=c.create_string_buffer(8192);plog(program,8192,None,buf);assert ok.value,buf.value.decode()
gl('glUseProgram',None,[U])(program);location=gl('glGetUniformLocation',I,[U,c.c_char_p]);matrix=gl('glUniformMatrix4fv',None,[I,I,U,P]);identity=np.eye(4,dtype=np.float32)
for name in ['ModelViewMat','ProjMat']:matrix(location(program,name.encode()),1,0,identity.ctypes.data)
gl('glUniform2f',None,[I,F,F])(location(program,b'ScreenSize'),256,144)
u1=gl('glUniform1f',None,[I,F]);u1(location(program,b'EdgeWidth'),.22);u1(location(program,b'BlurRadius'),14)
gl('glUniform1i',None,[I,I])(location(program,b'Sampler0'),0)
vao=U();gl('glGenVertexArrays',None,[I,P])(1,c.byref(vao));gl('glBindVertexArray',None,[U])(vao)
vbo=U();gl('glGenBuffers',None,[I,P])(1,c.byref(vbo));gl('glBindBuffer',None,[U,U])(0x8892,vbo)
vertices=np.array([[-1,-1,0,0,0],[1,-1,0,1,0],[1,1,0,1,1],[-1,-1,0,0,0],[1,1,0,1,1],[-1,1,0,0,1]],dtype=np.float32)
gl('glBufferData',None,[U,c.c_ssize_t,P,U])(0x8892,vertices.nbytes,vertices.ctypes.data,0x88E4)
for index,size,offset in [(0,3,0),(1,2,12)]:gl('glEnableVertexAttribArray',None,[U])(index);gl('glVertexAttribPointer',None,[U,I,U,U,I,P])(index,size,0x1406,0,20,P(offset))
bindtex=gl('glBindTexture',None,[U,U]);teximage=gl('glTexImage2D',None,[U,I,I,I,I,I,U,U,P]);texparam=gl('glTexParameteri',None,[U,U,I]);gen=gl('glGenTextures',None,[I,P]);textures=(U*2)();gen(2,textures)
y,x=np.indices((144,256));checker=np.where(((x//3+y//3)%2)[...,None],220,25).astype(np.uint8);original=np.concatenate([np.repeat(checker,3,axis=2),np.full((144,256,1),255,dtype=np.uint8)],axis=2)
for idx in range(2):
 bindtex(0x0DE1,textures[idx]);teximage(0x0DE1,0,0x8058,256,144,0,0x1908,0x1401,original.ctypes.data if idx==0 else None)
 for name,value in [(0x2801,0x2601),(0x2800,0x2601),(0x2802,0x812F),(0x2803,0x812F)]:texparam(0x0DE1,name,value)
fbo=U();gl('glGenFramebuffers',None,[I,P])(1,c.byref(fbo));gl('glBindFramebuffer',None,[U,U])(0x8D40,fbo);gl('glFramebufferTexture2D',None,[U,U,U,U,I])(0x8D40,0x8CE0,0x0DE1,textures[1],0);assert gl('glCheckFramebufferStatus',U,[U])(0x8D40)==0x8CD5
bindtex(0x0DE1,textures[0]);gl('glViewport',None,[I,I,I,I])(0,0,256,144)
u4=gl('glUniform4f',None,[I,F,F,F,F]);draw=gl('glDrawArrays',None,[U,I,I]);read=gl('glReadPixels',None,[I,I,I,I,U,U,P])
results={}
for label,edges in [('off',(0,0,0,0)),('left',(1,0,0,0)),('right',(0,1,0,0)),('top',(0,0,1,0)),('bottom',(0,0,0,1))]:
 u4(location(program,b'DangerEdges'),*edges);draw(4,0,6);pixels=np.empty_like(original);read(0,0,256,144,0x1908,0x1401,pixels.ctypes.data);delta=np.abs(pixels.astype(int)-original.astype(int));assert delta[45:100,85:170,:3].max()<=1,(label,'center altered');results[label]=delta
assert results['off'].max()<=1
for label,region,opposite in [('left',(slice(None),slice(0,20)),(slice(None),slice(236,None))),('right',(slice(None),slice(236,None)),(slice(None),slice(0,20))),('top',(slice(124,None),slice(None)),(slice(0,20),slice(None))),('bottom',(slice(0,20),slice(None)),(slice(124,None),slice(None)))]:
 diff=results[label];assert diff[region][:,:,:3].mean()>20,(label,'blur missing');assert diff[opposite][:,:,:3].max()<=1,(label,'opposite edge altered')
assert gl('glGetError',U,[])()==0
print('PASS: actual GLSL compiles/links; rendered no-warning pass-through and four directional blur cases; center/opposite edge preserved; no GL errors.')
