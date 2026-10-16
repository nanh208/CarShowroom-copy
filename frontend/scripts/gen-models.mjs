// Sinh mô hình 3D placeholder (GLB) cho trình xem 3D/AR: xe con & xe RC địa hình. Chạy: node scripts/gen-models.mjs
// Vật liệu "body" được tô lại theo màu sản phẩm trong trình xem.
import { writeFileSync } from 'node:fs'
function box(cx, cy, cz, sx, sy, sz) {
  const P = [], N = [], I = []; const hx = sx / 2, hy = sy / 2, hz = sz / 2
  const faces = [[[1,0,0],[[hx,-hy,-hz],[hx,hy,-hz],[hx,hy,hz],[hx,-hy,hz]]],[[-1,0,0],[[-hx,-hy,hz],[-hx,hy,hz],[-hx,hy,-hz],[-hx,-hy,-hz]]],
    [[0,1,0],[[-hx,hy,-hz],[-hx,hy,hz],[hx,hy,hz],[hx,hy,-hz]]],[[0,-1,0],[[-hx,-hy,hz],[-hx,-hy,-hz],[hx,-hy,-hz],[hx,-hy,hz]]],
    [[0,0,1],[[-hx,-hy,hz],[hx,-hy,hz],[hx,hy,hz],[-hx,hy,hz]]],[[0,0,-1],[[hx,-hy,-hz],[-hx,-hy,-hz],[-hx,hy,-hz],[hx,hy,-hz]]]]
  for (const [n, vs] of faces) { const b = P.length / 3; for (const v of vs) { P.push(v[0] + cx, v[1] + cy, v[2] + cz); N.push(...n) } I.push(b, b + 1, b + 2, b, b + 2, b + 3) }
  return { P, N, I }
}
// bánh xe: trụ theo trục X
function wheel(cx, cy, cz, r, w, seg = 28) {
  const P = [], N = [], I = []
  for (let i = 0; i <= seg; i++) { const a = i / seg * Math.PI * 2, y = Math.cos(a), z = Math.sin(a)
    P.push(cx - w / 2, cy + y * r, cz + z * r, cx + w / 2, cy + y * r, cz + z * r); N.push(0, y, z, 0, y, z) }
  for (let i = 0; i < seg; i++) { const k = i * 2; I.push(k, k + 2, k + 1, k + 1, k + 2, k + 3) }
  for (const s of [-1, 1]) { const c = P.length / 3; P.push(cx + s * w / 2, cy, cz); N.push(s, 0, 0)
    for (let i = 0; i <= seg; i++) { const a = i / seg * Math.PI * 2; P.push(cx + s * w / 2, cy + Math.cos(a) * r, cz + Math.sin(a) * r); N.push(s, 0, 0) }
    for (let i = 0; i < seg; i++) s > 0 ? I.push(c, c + 1 + i, c + 2 + i) : I.push(c, c + 2 + i, c + 1 + i) }
  return { P, N, I }
}
function merge(parts) { const o = { P: [], N: [], I: [] }; for (const p of parts) { const b = o.P.length / 3; o.P.push(...p.P); o.N.push(...p.N); o.I.push(...p.I.map(i => i + b)) } return o }
function glb(meshes) { // meshes: [{name, geo, color:[r,g,b,a], metal, rough}]
  const bins = [], views = [], acc = [], gm = [], mats = [], nodes = []; let off = 0
  const push = (arr, Type, target) => { const buf = Buffer.from(new Type(arr).buffer); const pad = (4 - buf.length % 4) % 4; bins.push(buf, Buffer.alloc(pad)); views.push({ buffer: 0, byteOffset: off, byteLength: buf.length, target }); off += buf.length + pad; return views.length - 1 }
  meshes.forEach((m, i) => {
    const { P, N, I } = m.geo; const mn = [0, 1, 2].map(k => Math.min(...P.filter((_, j) => j % 3 === k))), mx = [0, 1, 2].map(k => Math.max(...P.filter((_, j) => j % 3 === k)))
    const pv = push(P, Float32Array, 34962), nv = push(N, Float32Array, 34962), iv = push(I, Uint16Array, 34963)
    acc.push({ bufferView: pv, componentType: 5126, count: P.length / 3, type: 'VEC3', min: mn, max: mx }, { bufferView: nv, componentType: 5126, count: N.length / 3, type: 'VEC3' }, { bufferView: iv, componentType: 5123, count: I.length, type: 'SCALAR' })
    mats.push({ name: m.name, pbrMetallicRoughness: { baseColorFactor: m.color, metallicFactor: m.metal ?? 0.1, roughnessFactor: m.rough ?? 0.5 } })
    gm.push({ name: m.name, primitives: [{ attributes: { POSITION: i * 3, NORMAL: i * 3 + 1 }, indices: i * 3 + 2, material: i }] }); nodes.push({ mesh: i, name: m.name })
  })
  const bin = Buffer.concat(bins)
  const json = { asset: { version: '2.0', generator: 'ares gen-models' }, scene: 0, scenes: [{ nodes: nodes.map((_, i) => i) }], nodes, meshes: gm, materials: mats, accessors: acc, bufferViews: views, buffers: [{ byteLength: bin.length }] }
  let js = Buffer.from(JSON.stringify(json)); js = Buffer.concat([js, Buffer.alloc((4 - js.length % 4) % 4, 0x20)])
  const h = Buffer.alloc(12); h.writeUInt32LE(0x46546c67, 0); h.writeUInt32LE(2, 4); h.writeUInt32LE(12 + 8 + js.length + 8 + bin.length, 8)
  const c = (len, type) => { const b = Buffer.alloc(8); b.writeUInt32LE(len, 0); b.writeUInt32LE(type, 4); return b }
  return Buffer.concat([h, c(js.length, 0x4e4f534a), js, c(bin.length, 0x004e4942), bin])
}
const RED = [0.75, 0.1, 0.08, 1], GLASS = [0.08, 0.1, 0.14, 1], TIRE = [0.05, 0.05, 0.05, 1], RIM = [0.75, 0.75, 0.78, 1]
// xe con ~ 0.25m (đơn vị mét, cỡ mô hình 1:18) — trục Z là chiều dài
const S = 0.25 / 4.4, car = {
  body: merge([box(0, 0.55 * S, 0, 1.8 * S, 0.55 * S, 4.4 * S), box(0, 0.95 * S, 1.75 * S, 1.7 * S, 0.25 * S, 0.9 * S), box(0, 0.95 * S, -1.8 * S, 1.7 * S, 0.25 * S, 0.8 * S)]),
  glass: box(0, 1.08 * S, -0.1 * S, 1.55 * S, 0.5 * S, 2.1 * S),
  tires: merge([[-1, 1.35], [1, 1.35], [-1, -1.35], [1, -1.35]].map(([x, z]) => wheel(x * 0.82 * S, 0.36 * S, z * S, 0.36 * S, 0.28 * S))),
  rims: merge([[-1, 1.35], [1, 1.35], [-1, -1.35], [1, -1.35]].map(([x, z]) => wheel(x * 0.97 * S, 0.36 * S, z * S, 0.2 * S, 0.02 * S)))
}
writeFileSync('public/models/car.glb', glb([{ name: 'body', geo: car.body, color: RED, metal: 0.6, rough: 0.3 }, { name: 'glass', geo: car.glass, color: GLASS, metal: 0.2, rough: 0.1 }, { name: 'tire', geo: car.tires, color: TIRE, rough: 0.9 }, { name: 'rim', geo: car.rims, color: RIM, metal: 0.9, rough: 0.25 }]))
// xe RC địa hình ~ 0.45m (1:10): gầm cao, bánh to
const T = 0.45 / 4.2, truck = {
  body: merge([box(0, 1.25 * T, 0, 1.9 * T, 0.5 * T, 3.4 * T), box(0, 1.65 * T, -0.3 * T, 1.6 * T, 0.45 * T, 1.6 * T), box(0, 0.75 * T, 0, 0.5 * T, 0.25 * T, 3.6 * T)]),
  glass: box(0, 1.72 * T, 0.45 * T, 1.5 * T, 0.32 * T, 0.12 * T),
  tires: merge([[-1, 1.3], [1, 1.3], [-1, -1.3], [1, -1.3]].map(([x, z]) => wheel(x * 1.15 * T, 0.62 * T, z * T, 0.62 * T, 0.55 * T))),
  rims: merge([[-1, 1.3], [1, 1.3], [-1, -1.3], [1, -1.3]].map(([x, z]) => wheel(x * 1.43 * T, 0.62 * T, z * T, 0.33 * T, 0.02 * T)))
}
writeFileSync('public/models/rc-truck.glb', glb([{ name: 'body', geo: truck.body, color: [0.1, 0.35, 0.8, 1], metal: 0.3, rough: 0.4 }, { name: 'glass', geo: truck.glass, color: GLASS, rough: 0.1 }, { name: 'tire', geo: truck.tires, color: TIRE, rough: 0.95 }, { name: 'rim', geo: truck.rims, color: [0.9, 0.75, 0.1, 1], metal: 0.8, rough: 0.3 }]))
console.log('ok')
