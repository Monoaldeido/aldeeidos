import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
a=root/'src/main/resources/assets/whip_cinematic'
model=json.loads((a/'geo/whip_guardian.geo.json').read_text())
an=json.loads((a/'animations/whip_guardian.animation.json').read_text())['animations']
bones=model['minecraft:geometry'][0]['bones']
orig=json.loads((root/'whip_guardian.bbmodel').read_text())
assert len(bones)==len(orig['groups'])==30
assert sum(len(b.get('cubes',[])) for b in bones)==len(orig['elements'])==113
assert abs(an['animation.whip_guardian.aura_farm']['animation_length']-8.0)<1e-5
assert abs(an['animation.whip_guardian.ground_slam']['animation_length']-3.0)<1e-5
assert (a/'textures/entity/whip_guardian.png').read_bytes()[:8]==b'\x89PNG\r\n\x1a\n'
assert abs(an['animation.whip_guardian.portal_arrival']['animation_length']-1.75)<1e-5
for name in (x['name'] for x in orig['animations']):
    src=next(x for x in orig['animations'] if x['name']==name)
    for source in src['animators'].values():
        for k in source.get('keyframes',[]):
            if k['channel'] not in ('rotation','position','scale'):continue
            time=f'{float(k["time"]):.4f}'.rstrip('0').rstrip('.')
            channel=an[name]['bones'][source['name']][k['channel']]
            assert time in channel
            if k.get('interpolation')=='catmullrom':
                assert channel[time]['lerp_mode']=='catmullrom'
print('OK: original animations, 30 bones, 113 cubes, texture intact.')
