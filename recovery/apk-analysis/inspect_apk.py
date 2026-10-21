import hashlib, json, pathlib, struct, zipfile, re

apk = pathlib.Path(r'C:\Users\Haw\xwechat_files\wxid_yxtnrywji5i112_1d48\msg\file\2026-10\base.apk(1).1')
out = pathlib.Path(__file__).parent
summary = {'sha256': hashlib.sha256(apk.read_bytes()).hexdigest(), 'bytes': apk.stat().st_size, 'dex': []}
with zipfile.ZipFile(apk) as z:
    (out/'archive-files.json').write_text(json.dumps([{'path':e.filename,'bytes':e.file_size} for e in z.infolist()],indent=2),encoding='utf-8')
    for name in z.namelist():
        if not re.fullmatch(r'classes\d*\.dex',name): continue
        data=z.read(name)
        def u32(p): return struct.unpack_from('<I',data,p)[0]
        strings=[]
        for i in range(u32(56)):
            p=u32(u32(60)+4*i)
            while data[p]&128: p+=1
            p+=1
            strings.append(data[p:data.index(b'\0',p)].decode('utf-8','replace'))
        types=[strings[u32(u32(68)+i*4)] for i in range(u32(64))]
        classes=[types[u32(u32(100)+i*32)] for i in range(u32(96))]
        (out/(name+'.classes.txt')).write_text('\n'.join(classes),encoding='utf-8')
        (out/(name+'.strings.txt')).write_text('\n'.join(strings),encoding='utf-8')
        summary['dex'].append({'name':name,'class_count':len(classes),'strings':len(strings),'app_classes':[c for c in classes if any(s in c.lower() for s in ['qingji','haw','noteapp'])]})
    for name in ['META-INF/version-control-info.textproto','META-INF/com/android/build/gradle/app-metadata.properties','kotlin-tooling-metadata.json']:
        if name in z.namelist(): summary[name]=z.read(name).decode('utf-8','replace')
(out/'summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(summary,ensure_ascii=False,indent=2))
