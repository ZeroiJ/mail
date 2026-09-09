# Font Resources

`ui/theme/Type.kt` and `ui/theme/Theme.kt` reference these font resources via `R.font.*`.

## Required files (place actual .ttf/.otf binaries here)

| Resource name   | Source                          | Weight    | Used for                          |
|-----------------|---------------------------------|-----------|-----------------------------------|
| `geist_regular` | Geist (Vercel) - Regular        | Normal    | Email bodies, previews            |
| `geist_medium`  | Geist (Vercel) - Medium         | Medium    | Titles, labels, small buttons     |
| `geist_bold`    | Geist (Vercel) - Bold           | Bold      | Emphasis                          |
| `ndot`          | N-Dot (Nothing OS style)        | Normal    | Headers, sender names, big numbers|

## How to obtain
- **Geist**: https://vercel.com/font (official Geist font by Vercel)
- **N-Dot**: Nothing's dot-matrix typeface (see Nothing developer/design references)

## Naming rule
The file name on disk must match the resource name with a valid font extension:
- `geist_regular.ttf` (or `.otf`)
- `geist_medium.ttf`
- `geist_bold.ttf`
- `ndot.ttf`

Android resolves `R.font.geist_regular` from `res/font/geist_regular.ttf`.
