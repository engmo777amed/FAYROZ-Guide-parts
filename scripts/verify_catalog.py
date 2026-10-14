#!/usr/bin/env python3
"""Static regression check for bundled V12 catalog. No Android SDK required."""
from pathlib import Path
import json
p=Path(__file__).resolve().parents[1]/'app/src/main/assets/catalog.json'
d=json.loads(p.read_text(encoding='utf-8'))
items=d['items']; prices=[offer for item in items for offer in item.get('verified_offers',[])]
assert len(d['categories'])==13, len(d['categories'])
assert len(items)==181, len(items)
assert len(prices)==403, len(prices)
assert len({i['id'] for i in items})==181,'Duplicate material ID'
assert all(i['title'] and i['category'] for i in items)
assert all(isinstance(o['price'],(int,float)) and o['price']>=0 for o in prices)
assert all(o['unit'] and o['brand'] for o in prices)
print('PASS:',len(d['categories']),'sections;',len(items),'materials;',len(prices),'offers;')
print('Photos linked:', sum('photo' in x for x in items))
print('Brands listed:', sum(len(x.get('brand_candidates',[])) for x in items))
