# Font Resources

`ui/theme/Type.kt` and `ui/theme/Theme.kt` reference these font resources via `R.font.*`.

## Bundled files

| Resource name   | File             | Source                                                    | Weight | Used for                          |
|-----------------|------------------|-----------------------------------------------------------|--------|-----------------------------------|
| `geist_regular` | `geist_regular.ttf` | Geist (Vercel) — [vercel/geist-font](https://github.com/vercel/geist-font) | Regular | Email bodies, previews            |
| `geist_medium`  | `geist_medium.ttf`  | Geist (Vercel) — [vercel/geist-font](https://github.com/vercel/geist-font) | Medium  | Titles, labels, small buttons     |
| `geist_bold`    | `geist_bold.ttf`    | Geist (Vercel) — [vercel/geist-font](https://github.com/vercel/geist-font) | Bold    | Emphasis                          |
| `ndot`          | `ndot.otf`          | N-Dot 57 (1.003) — [Return761/Nothing-Fonts](https://github.com/Return761/Nothing-Fonts) | Normal  | Headers, sender names, big numbers|

## Notes
- Geist is licensed under the SIL Open Font License (OFL-1.1).
- N-Dot 57 is Nothing's dot-matrix typeface, shipped as an OpenType font. Android resolves
  `R.font.ndot` from `res/font/ndot.otf` — the extension matches the actual file format.
- Android Font resource lookup uses the file name (without extension); `.ttf` and `.otf` are both accepted.
