#!/usr/bin/env python3
"""Use uploaded sprites unchanged; tint their models, including only the metal pixels inside vials."""
import argparse
import json
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets'
MOD=ROOT/'cosmerearcanists'
# These are metal shavings inside the bottle, not glass rim/base or the brown stopper.
VIAL_CONTENTS={(9,7),(8,8),(6,11),(7,11),(9,11),*( (x,y) for x in range(6,10) for y in (12,13)),(7,14),(8,14)}

def vial_model():
    image=Image.open(MOD/'textures/item/provided_vial.png').convert('RGBA')
    solid={(x,y) for y in range(16) for x in range(16) if image.getpixel((x,y))[3]}
    elements=[]
    for x,y in sorted(solid,key=lambda p:(p[1],p[0])):
        face={'uv':[x,y,x+1,y+1],'texture':'#layer0'}
        if (x,y) in VIAL_CONTENTS: face['tintindex']=0
        faces={'north':dict(face),'south':dict(face)}
        for direction,neighbor in [('west',(x-1,y)),('east',(x+1,y)),('up',(x,y-1)),('down',(x,y+1))]:
            if neighbor not in solid: faces[direction]=dict(face)
        elements.append({'from':[x,15-y,7.5],'to':[x+1,16-y,8.5],'shade':False,'faces':faces})
    # A non-generated root preserves explicit per-pixel tint indices. Normal item transforms retained.
    return {'parent':'minecraft:block/block','gui_light':'front','ambientocclusion':False,'render_type':'minecraft:translucent','textures':{'layer0':'cosmerearcanists:item/provided_vial','particle':'cosmerearcanists:item/provided_vial'},'display':{
        'gui':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]},
        'thirdperson_righthand':{'translation':[0,3,1],'scale':[.55,.55,.55]},
        'thirdperson_lefthand':{'translation':[0,3,1],'scale':[.55,.55,.55]},
        'firstperson_righthand':{'rotation':[0,-90,25],'translation':[1.13,3.2,1.13],'scale':[.68,.68,.68]},
        'firstperson_lefthand':{'rotation':[0,90,-25],'translation':[1.13,3.2,1.13],'scale':[.68,.68,.68]},
        'ground':{'translation':[0,2,0],'scale':[.5,.5,.5]},'head':{'rotation':[0,180,0],'translation':[0,13,7]},'fixed':{'rotation':[0,180,0]}},'elements':elements}

def expected():
    models=MOD/'models/item';out={}
    for path in sorted(models.glob('*.json')):
        if path.stem.endswith('_spike'):out[path]={'parent':'minecraft:item/handheld','textures':{'layer0':'cosmerearcanists:item/provided_spike'}}
        elif path.stem.endswith('_ingot'):out[path]={'parent':'minecraft:item/generated','textures':{'layer0':'cosmerearcanists:item/provided_ingot'}}
    out[models/'metal_filing.json']={'parent':'minecraft:item/generated','textures':{'layer0':'cosmerearcanists:item/provided_shavings'}}
    out[models/'provided_vial.json']=vial_model()
    for name in ('metal_vial','iron_vial','steel_vial','pewter_vial'):out[models/(name+'.json')]={'parent':'cosmerearcanists:item/provided_vial'}
    for metal in ('iron','copper','gold'):out[ROOT/'minecraft/models/item'/(metal+'_ingot.json')]={'parent':'minecraft:item/generated','textures':{'layer0':'cosmerearcanists:item/provided_ingot'}}
    return out

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
    models=expected()
    for path,model in models.items():
        if args.check:
            assert path.exists() and json.loads(path.read_text())==model,f'Stale or missing model: {path}'
        else:
            path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(model,indent=2)+'\n')
    print(('PASS: checked' if args.check else 'Wrote'),len(models),'item models; original PNGs unchanged.')
if __name__=='__main__':main()
