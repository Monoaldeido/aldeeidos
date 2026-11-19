// Original Whip Guardian scene VFX - Rimulib 1.3.0 common.glsl is prepended automatically.
// No #version and no duplicate declarations of texCoord/fragColor/DepthSampler.
uniform vec3 Centre;  // Relative to the current render camera, not player position.
uniform float SceneAge; // Ticks plus partial ticks from server-synchronised entity.
uniform float Quality;

const float PI = 3.14159265359;
const vec3 CYAN = vec3(0.12, 0.96, 0.77);
const vec3 VIOLET = vec3(0.38, 0.16, 0.80);
const vec3 GOLD = vec3(1.00, 0.72, 0.23);

float sat(float x){return clamp(x,0.,1.);}
float ease(float a,float b,float x){float t=sat((x-a)/max(.001,b-a));return t*t*(3.-2.*t);}
float win(float a,float b,float c,float d,float t){return ease(a,b,t)*(1.-ease(c,d,t));}
float h12(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453123);}
float h13(vec3 p){return fract(sin(dot(p,vec3(12.9898,78.233,43.719)))*43758.5453);}
float ridged(float x,float width){return exp(-abs(x)/max(.002,width));}
float softBand(float x,float c,float w){return exp(-pow((x-c)/max(w,0.001),2.));}

// Fast, depth-aware ground projection: energy follows real block positions.
vec3 groundFx(vec3 scene,vec3 p,float age) {
    vec3 local=p-Centre;
    float r=length(local.xz);
    float glow=0.;
    float sigil=win(0.,17.,214.,246.,age);
    float impact=win(261.,265.,305.,348.,age);
    if(r<100.){
        float polar=atan(local.z,local.x);
        float rings=0.;
        for(int i=0;i<4;i++){
            float rr=12.+float(i)*12.+3.*sin(age*.025+float(i)*1.5);
            rings+=softBand(r,rr,.65+float(i)*.13)*(.7+.3*sin(polar*18.+age*.17));
        }
        // Rune spokes, geometric wedges and travelling signal, not random particles.
        float sector=abs(sin(polar*12.));
        float wedges=pow(max(0.,1.-sector),12.)*softBand(r,27.,11.);
        float cell=abs(fract(polar*16./(PI*2.)+.5)-.5);
        float lettering=softBand(cell,.49,.014)*softBand(r,31.,2.);
        glow+=(rings*.47+wedges*.45+lettering*.3)*sigil;
        // Physical impact has its own expanding front and fractured secondary rings.
        float expanding=(age-263.)*2.1;
        float wave=softBand(r,expanding,1.4)*(1.-ease(262.,345.,age));
        float cracks=pow(max(0.,1.-abs(sin(polar*15.+sin(r*.37+polar*7.)*.8))),20.)
                     *(1.-ease(25.,86.,r))*(.6+.4*sin(r*.83+age*.12));
        float pyro=win(263.,268.,296.,337.,age)*max(0.,cracks);
        glow+=impact*(wave*2.15+pyro*.7);
    }
    float lit=glow*(.43+.57*max(0.,dot(normalize(vec3(local.x,10.,local.z)),vec3(0.,1.,0.))));
    vec3 radiant=mix(CYAN,GOLD,impact*.74);
    return scene*(1.+radiant*lit*.5)+radiant*glow*.63;
}

// Circular rim lying in a camera-facing vertical plane (roughly 39 blocks tall).
vec3 portalFx(vec3 scene,vec3 ro,vec3 rd,float depth,float age){
    float phase=win(6.,19.,54.,96.,age);
    if(phase<.001) return scene;
    vec3 center=Centre+vec3(0.,17.,0.);
    vec3 n=normalize(-center);
    vec3 side=normalize(cross(vec3(0.,1.,0.),n));
    if(length(side)<.1)side=vec3(1.,0.,0.);
    float den=dot(rd,n);
    if(abs(den)<.002)return scene;
    float t=dot(center,n)/den;
    if(t<0. || t>depth)return scene;
    vec3 hit=rd*t-center;
    vec2 uv=vec2(dot(hit,side)/12.,hit.y/19.);
    float radius=length(uv);
    float angular=atan(uv.y,uv.x);
    float waves=sin(angular*24.-age*.28+radius*45.);
    float outer=softBand(radius,1.,.058)+softBand(radius,1.10,.12)*.3;
    float blades=softBand(radius,1.0+sin(angular*12.-age*.09)*.04,.025);
    float core=sat((1.01-radius)*11.0)*(.55+.45*sin(radius*80.-age*.4+waves));
    float pulse=(.42+.58*sin(age*.17))*phase;
    vec3 plasma=mix(VIOLET,CYAN,softBand(radius,.86,.32));
    vec3 rift=mix(vec3(.015,.02,.08),plasma*.65,core*.8);
    float mask=phase*(core*.83+outer*.95+blades*.66);
    scene=mix(scene,rift,sat(mask));
    scene+=(plasma*outer*1.9 + CYAN*blades*.56 + vec3(.85,.96,1.)*softBand(radius,1.,.009)*2.1)*pulse;
    // A delayed split of the rift, i.e. a transition rather than a hard cut.
    float shutter=win(60.,70.,84.,95.,age);
    scene+=CYAN*shutter*softBand(abs(uv.y),.04,.06)*softBand(radius,.85,.35);
    return scene;
}

// Analytic ray-cylinder intersection limits work to the portion of each tornado on screen.
vec2 cylinder(vec3 rd,vec3 center,float radius){
    vec2 delta= -center.xz;
    float a=dot(rd.xz,rd.xz);
    if(a<.0001)return vec2(-1.);
    float b=dot(rd.xz,delta);
    float c=dot(delta,delta)-radius*radius;
    float discr=b*b-a*c;
    if(discr<0.)return vec2(-1.);
    float root=sqrt(discr);
    return vec2((-b-root)/a,(-b+root)/a);
}

// Giant tornados have a broad translucent column and several independent moving ribbons.
vec3 tornadoFx(vec3 scene,vec3 rd,float depth,float age){
    float strength=win(137.,155.,199.,216.,age);
    if(strength<.002)return scene;
    float globalSpin=(age-140.)*.048;
    for(int i=0;i<5;i++){
        float angle=float(i)*2.*PI/5.+globalSpin;
        vec3 column=Centre+vec3(cos(angle)*28.,0.,sin(angle)*28.);
        vec2 interval=cylinder(rd,column,7.);
        if(interval.y<=0. || interval.x>depth)continue;
        float begin=max(.01,interval.x),finish=min(depth,interval.y);
        if(finish<=begin)continue;
        // 4 samples across a bounded interval; no full-screen long-distance raymarch.
        vec3 fog=vec3(0.);
        float density=0.;
        for(int k=0;k<4;k++){
            float t=mix(begin,finish,(float(k)+.5)/4.);
            vec3 p=rd*t-column;
            float h=p.y;
            if(h<.0 || h>35.)continue;
            float r=length(p.xz);
            float cone=mix(1.25,6.4,sat(h/35.));
            float theta=atan(p.z,p.x);
            float helix=theta*4.5+h*.50-age*.37+float(i)*3.;
            float thread=pow(max(0.,.5+.5*cos(helix)),18.);
            float thread2=pow(max(0.,.5+.5*cos(helix*1.6+1.8)),28.);
            float shell=softBand(r,cone,.73+cone*.04);
            float knots=softBand(sin(h*.49-age*.17),.0,.18);
            float cloud=softBand(r,cone,1.9)*.08;
            float weight=shell*(thread*.85+thread2*.8+knots*.22)+cloud;
            density+=weight;
            fog+=mix(VIOLET,CYAN,sat(h/32.))*weight;
        }
        float a=sat(density*.23*strength);
        scene=mix(scene,scene*.38+fog*.28,a);
        scene+=fog*.27*strength;
    }
    return scene;
}

vec3 atmosphericFx(vec3 scene,vec3 rd,float age){
    float shade=win(4.,30.,304.,350.,age);
    float invasion=win(15.,45.,250.,315.,age);
    vec3 sky=scene*mix(1.,.20,.65*shade)+VIOLET*.11*shade;
    // Fine procedural meteor-line stars, only on actual sky pixels.
    vec2 cell=floor((rd.xz/(.4+abs(rd.y)))*95.);
    float star=step(.9945,h12(cell));
    float glint=star*pow(max(0.,sin(age*.033+h12(cell*3.)*6.28)),12.);
    float streaks=softBand(fract(atan(rd.z,rd.x)*30./PI+age*.01),.5,.02)
                 *pow(1.-abs(rd.y),4.)*.15;
    sky+=(CYAN*glint*1.4+VIOLET*streaks)*shade;
    float lightburst=win(261.,263.,264.,269.,age);
    sky+=mix(CYAN,GOLD,.8)*lightburst*.30;
    return mix(scene,sky,shade);
}

void main(){
    float age=SceneAge;
    vec3 rd=normalize(viewDir(texCoord));
    bool sky=skyAt(texCoord);
    vec3 original=sampleScene(texCoord);
    float depth=sky?100000.:length(scenePosAt(texCoord));
    vec3 scene=original;
    if(sky) scene=atmosphericFx(scene,rd,age);
    else scene=groundFx(scene,scenePosAt(texCoord),age);
    // World-space layers are clipped against the depth of already drawn world.
    scene=portalFx(scene,vec3(0.),rd,depth,age);
    scene=tornadoFx(scene,rd,depth,age);
    // Energy snap: only 3 frames, not several seconds of blinding light.
    float snap=win(263.,264.,265.,267.,age);
    float mono=dot(scene,vec3(.299,.587,.114));
    scene=mix(scene,vec3(1.-mono)*.94+CYAN*.13,snap*.60);
    // Saturated colour fringe emanating from the contact point.
    float post=win(265.,268.,305.,332.,age);
    vec2 offset=texCoord-.5;
    float rays=pow(max(0.,cos(atan(offset.y,offset.x)*18.+age*.04)),24.);
    scene+=mix(GOLD,CYAN,.4)*rays*post*.07;
    // Fade the visual system cleanly; outside its lifetime the original scene returns.
    float visibility=win(0.,7.,325.,350.,age);
    fragColor=vec4(mix(original,scene,visibility),1.);
}
