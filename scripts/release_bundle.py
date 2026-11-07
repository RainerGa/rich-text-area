"""Package the fork with its unpublished Noto dependency and corresponding sources."""
import argparse
import hashlib
from pathlib import Path
import shutil
import xml.etree.ElementTree as ET
import zipfile


def version(pom):
    return ET.parse(pom).findtext('{*}version')


def bundle(root, emoji):
    root, emoji = Path(root), Path(emoji)
    release = version(root / 'rta/pom.xml')
    dependency = version(emoji / 'pom.xml')
    declared = ET.parse(root / 'rta/pom.xml').findtext('{*}properties/{*}emoji.version')
    if dependency != declared:
        raise ValueError(f'Expected emoji {declared}, found {dependency}')
    files = {}
    for classifier in ('', '-sources', '-javadoc'):
        name = f'rich-text-area-{release}{classifier}.jar'
        files[name] = root / 'rta/target' / name
    files[f'rich-text-area-{release}.pom'] = root / 'rta/pom.xml'
    files[f'emoji-noto-{dependency}.jar'] = emoji / 'target' / f'emoji-noto-{dependency}.jar'
    files[f'emoji-noto-{dependency}.pom'] = emoji / 'pom.xml'
    for name, path in files.items():
        if not path.is_file():
            raise ValueError(f'Missing release artifact: {path}')
        if name.endswith('.jar'):
            with zipfile.ZipFile(path) as archive:
                if archive.testzip() or not archive.namelist():
                    raise ValueError(f'Invalid release artifact: {path}')
    output = root / 'target/release'
    output.mkdir(parents=True, exist_ok=True)
    for name, path in files.items():
        shutil.copyfile(path, output / name)
    archive_path = output / f'rich-text-area-{release}-bundle.zip'
    with zipfile.ZipFile(archive_path, 'w', zipfile.ZIP_DEFLATED) as archive:
        for name, path in files.items():
            group, artifact, artifact_version = ('de/richmarkdown', 'emoji-noto', dependency) if name.startswith('emoji-') else ('com/gluonhq', 'rich-text-area', release)
            archive.write(path, f'repository/{group}/{artifact}/{artifact_version}/{name}')
        for name in ('README.md', 'LICENSE', 'RELEASING.md', f'releases/{release}.md'):
            archive.write(root / name, name)
        # Exact dependency sources and notices accompany the binary; no local build output.
        archive.write(emoji / 'pom.xml', 'sources/emoji-noto/pom.xml')
        for path in sorted((emoji / 'src').rglob('*')):
            if path.is_file():
                archive.write(path, 'sources/emoji-noto/' + path.relative_to(emoji).as_posix())
    assets = [output / name for name in files] + [archive_path]
    (output / 'SHA256SUMS').write_text(''.join(
        f'{hashlib.sha256(path.read_bytes()).hexdigest()}  {path.name}\n'
        for path in sorted(assets)), encoding='utf-8')
    return archive_path


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--emoji', type=Path, required=True)
    args = parser.parse_args()
    print(bundle(Path(__file__).resolve().parent.parent, args.emoji))
