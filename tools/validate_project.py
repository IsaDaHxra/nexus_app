from pathlib import Path
import json
import sys

root = Path(__file__).resolve().parents[1]
assets = root / 'app/src/main/assets'
scenes = json.loads((assets / 'episode_1.json').read_text(encoding='utf-8'))
ids = {s['id'] for s in scenes}
assert len(scenes) == 53, f'Expected 53 scenes, found {len(scenes)}'
assert len(ids) == len(scenes)
assert '83:29' in ids
for scene in scenes:
    for target in [scene.get('next')] + [c['next'] for c in scene.get('choices', [])]:
        assert target is None or target in ids, (scene['id'], target)
    for layer in scene['layers']:
        assert layer['w'] > 0 and layer['h'] > 0, layer['id']
visited, active = set(), set()
def traverse(scene_id):
    assert scene_id not in active, f'Cycle at {scene_id}'
    if scene_id in visited:
        return
    active.add(scene_id)
    scene = next(s for s in scenes if s['id'] == scene_id)
    for child in [scene.get('next')] + [c['next'] for c in scene.get('choices', [])]:
        if child is not None:
            traverse(child)
    active.remove(scene_id)
    visited.add(scene_id)
traverse('83:29')
assert visited == ids, f'Unreachable scenes: {ids - visited}'
for name in ('characters.json', 'asset_aliases.json'):
    json.loads((assets / name).read_text(encoding='utf-8'))
source = root / 'app/src/main/java/com/nexus/mobile'
story = (source / 'ui/StoryScreen.kt').read_text(encoding='utf-8')
assert 'internal fun SceneCanvas(' in story
assert 'reveal.coerceIn(' in story
assert 'Int.MAX_VALUE' not in story
main = (source / 'MainActivity.kt').read_text(encoding='utf-8')
for route in ('home', 'characters', 'search', 'episodes', 'story', 'journal', 'profile', 'portal', 'journey'):
    assert f'composable("{route}")' in main, route
for name in ('PortalScreen', 'JourneyScreen', 'DimensionalCard', 'Motion', 'Design'):
    assert (source / f'ui/{name}.kt').is_file(), name
aliases = json.loads((assets / 'asset_aliases.json').read_text(encoding='utf-8'))
missing = sorted({aliases.get(layer['asset'], layer['asset']) for scene in scenes for layer in scene['layers']
                  if layer.get('kind') == 'image' and not (assets / aliases.get(layer['asset'], layer['asset'])).is_file()})
print(f'PASS: {len(scenes)} scenes; all routes reachable; navigation and scene renderer present.')
if missing:
    print(f'ASSETS: {len(missing)} images must be copied from your working project. This package has no later story images.')
    if '--strict-assets' in sys.argv:
        for image in missing:
            print(image)
        sys.exit(1)
else:
    print('PASS: all story image assets found.')
