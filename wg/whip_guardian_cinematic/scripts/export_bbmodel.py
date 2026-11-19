#!/usr/bin/env python3
"""Blockbench Gecko model -> vanilla GeckoLib 4 geo/animation JSON and embedded PNG.

Supports this supplied box-based model (no meshes, no anim expressions). Keep the
original .bbmodel as your editable source. Built without third-party modules.
"""
import base64
import json
from pathlib import Path
import sys

src = Path(sys.argv[1]) if len(sys.argv) > 1 else Path('whip_guardian.bbmodel')
out = Path(sys.argv[2]) if len(sys.argv) > 2 else Path('src/main/resources/assets/whip_cinematic')
data = json.loads(src.read_text(encoding='utf-8'))
groups = {g['uuid']: g for g in data['groups']}
cubes = {e['uuid']: e for e in data['elements'] if e['type'] == 'cube'}
bones = []

def walk(node, parent=None):
    if isinstance(node, str):
        return
    group = groups[node['uuid']]
    bone = {'name': group['name'], 'pivot': group.get('origin', [0,0,0])}
    if parent: bone['parent'] = parent
    if group.get('rotation') and any(group['rotation']):
        bone['rotation'] = group['rotation']
    parts = []
    for child in node.get('children', []):
        if isinstance(child, str) and child in cubes:
            c = cubes[child]
            uv = {}
            for face, detail in c.get('faces', {}).items():
                if 'uv' not in detail or detail.get('texture') is None: continue
                a,b,d,e = detail['uv']
                entry = {'uv':[a,b],'uv_size':[d-a,e-b]}
                if detail.get('rotation'):entry['uv_rotation']=detail['rotation']
                uv[face] = entry
            if not uv: continue
            origin = c['from']
            end = c['to']
            cube = {'origin': origin, 'size':[round(e-s,6) for s,e in zip(origin,end)], 'uv':uv}
            if c.get('inflate'):cube['inflate']=c['inflate']
            if c.get('rotation') and any(c['rotation']):
                cube['rotation']=c['rotation']
                cube['pivot']=c.get('origin',[(origin[i]+end[i])/2 for i in range(3)])
            parts.append(cube)
        else:
            walk(child, group['name'])
    if parts: bone['cubes'] = parts
    bones.append(bone)

for node in data['outliner']:
    walk(node)
# Parent order is important to some loaders (parents before children).
by_name={b['name']:b for b in bones}
ordered=[]
def add(name):
    b=by_name[name]
    if b.get('parent') and b['parent'] not in [p['name'] for p in ordered]: add(b['parent'])
    if b not in ordered: ordered.append(b)
for b in bones: add(b['name'])
geo = {'format_version':'1.12.0','minecraft:geometry':[{
    'description':{
        'identifier':'geometry.whip_guardian',
        'texture_width':data['resolution']['width'],
        'texture_height':data['resolution']['height'],
        'visible_bounds_width':4,
        'visible_bounds_height':4,
        'visible_bounds_offset':[0,1.4,0]
    }, 'bones':ordered}]}
(out/'geo').mkdir(parents=True,exist_ok=True)
(out/'animations').mkdir(parents=True,exist_ok=True)
(out/'textures/entity').mkdir(parents=True,exist_ok=True)
(out/'geo/whip_guardian.geo.json').write_text(json.dumps(geo, separators=(',',':'),ensure_ascii=False),encoding='utf-8')
anim={}
for a in data['animations']:
    bones_a={}
    for obj in a['animators'].values():
        channels={}
        for k in obj.get('keyframes',[]):
            chan=k['channel']
            if chan not in ('position','rotation','scale'):continue
            # GeckoLib keyframe timestamps are seconds.
            stamp=format(float(k['time']),'.4f').rstrip('0').rstrip('.')
            vector=[float(k['data_points'][0].get(c,0.)) for c in 'xyz']
            # Preserve the authored Catmull-Rom easing. A bare vector always
            # interpolates linearly, which would change 4,023 source keys.
            channels.setdefault(chan,{})[stamp] = (
                {'post':vector,'lerp_mode':'catmullrom'}
                if k.get('interpolation') == 'catmullrom' else vector
            )
        if channels: bones_a[obj['name']]=channels
    anim[a['name']]={'loop':a.get('loop')=='loop','animation_length':a['length'],'bones':bones_a}
# Original authored animations remain untouched. This extra non-looping clip is an
# additive summon transition: it grows the existing rig from the portal.
anim['animation.whip_guardian.portal_arrival']={
    'loop':False, 'animation_length':1.75,
    'bones':{'root':{
        'position':{'0':[0,-52,0],'.6':[0,-35,0],'1.75':[0,0,0]},
        'scale':{'0':[.005,.005,.005],'.6':[.18,.18,.18],'1.75':[1,1,1]}
    }}
}
(out/'animations/whip_guardian.animation.json').write_text(json.dumps({'format_version':'1.8.0','animations':anim},separators=(',',':'),ensure_ascii=False),encoding='utf-8')
tex=data['textures'][0]['source']
tex=tex.split(',',1)[1] if tex.startswith('data:') else tex
(out/'textures/entity/whip_guardian.png').write_bytes(base64.b64decode(tex))
print('Exportado',len(ordered),'bones,',len(cubes),'cubos,',len(anim),'animações (2 originais + transição de invocação)')
