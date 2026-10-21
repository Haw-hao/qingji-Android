"""Fetch the official command-line tools with resumable transfer for this recovery."""
import pathlib, requests, time, zipfile, concurrent.futures, shutil

root = pathlib.Path(__file__).resolve().parents[1] / '.local-tools'
if (root / 'android-sdk/cmdline-tools/latest/bin/sdkmanager.bat').exists():
    print('SDK manager is already available.', flush=True)
    raise SystemExit(0)
root.mkdir(exist_ok=True)
archive = root / 'cmdline-tools.zip'
url = 'https://dl.google.com/android/repository/commandlinetools-win-15859902_latest.zip'
# Disable transfer encoding: HTTP ranges must describe the exact ZIP bytes.
headers = {'Accept-Encoding': 'identity'}
probe = requests.get(url, headers={**headers, 'Range': 'bytes=0-0'}, timeout=(20, 30))
probe.raise_for_status()
total = int(probe.headers['Content-Range'].split('/')[-1])
parts = root / 'sdk-download-parts'
parts.mkdir(exist_ok=True)
chunk = 8 * 1024 * 1024

def fetch(start):
    end = min(start + chunk, total) - 1
    part = parts / str(start)
    if part.exists() and part.stat().st_size == end - start + 1: return part
    for attempt in range(4):
        try:
            r = requests.get(url, headers={**headers, 'Range': f'bytes={start}-{end}'}, timeout=(20, 60))
            r.raise_for_status()
            if r.status_code != 206 or len(r.content) != end-start+1: raise RuntimeError('Invalid range reply')
            part.write_bytes(r.content)
            print(f'SDK chunk {start//chunk+1}/{(total+chunk-1)//chunk} ready', flush=True)
            return part
        except Exception:
            if attempt == 3: raise
            time.sleep(2)

with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
    completed = list(pool.map(fetch, range(0, total, chunk)))
with archive.open('wb') as output:
    for part in completed: output.write(part.read_bytes())
with zipfile.ZipFile(archive) as z:
    if z.testzip() is not None: raise RuntimeError('ZIP validation failed')
    z.extractall(root / 'sdk-temp')
destination = root / 'android-sdk/cmdline-tools/latest'
destination.parent.mkdir(parents=True, exist_ok=True)
shutil.copytree(root / 'sdk-temp/cmdline-tools', destination, dirs_exist_ok=True)
print(f'SDK manager ready: {destination}', flush=True)
